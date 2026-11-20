let currentUser = null;
let allInvoices = [];

function switchAuthTab(tab) {
    document.getElementById('tabLogin').classList.toggle('active', tab === 'login');
    document.getElementById('tabRegister').classList.toggle('active', tab === 'register');
    document.getElementById('loginForm').classList.toggle('hidden', tab !== 'login');
    document.getElementById('registerForm').classList.toggle('hidden', tab !== 'register');
}

async function handleLogin(e) {
    e.preventDefault();
    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: document.getElementById('loginEmail').value,
            password: document.getElementById('loginPassword').value
        })
    });
    if (!res.ok) return alert('Invalid credentials!');
    currentUser = await res.json();
    enterApp();
}

async function handleRegister(e) {
    e.preventDefault();
    const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            businessName: document.getElementById('regBizName').value,
            ownerName: document.getElementById('regOwner').value,
            registeredUpiId: document.getElementById('regUpi').value,
            email: document.getElementById('regEmail').value,
            password: document.getElementById('regPass').value
        })
    });
    if (!res.ok) return alert('Registration failed or email already exists.');
    currentUser = await res.json();
    enterApp();
}

function enterApp() {
    document.getElementById('authScreen').classList.add('hidden');
    document.getElementById('appShell').classList.remove('hidden');
    syncUserUI();
    loadDashboard();
}

function logout() {
    currentUser = null;
    location.reload();
}

function syncUserUI() {
    document.getElementById('sideBizName').innerText = currentUser.businessName;
    document.getElementById('sideUpi').innerText = 'UPI: ' + (currentUser.registeredUpiId || 'Not Set');
    document.getElementById('shieldExpectedUpi').innerText = currentUser.registeredUpiId || 'Not Set';

    document.getElementById('profBizName').value = currentUser.businessName || '';
    document.getElementById('profOwner').value = currentUser.ownerName || '';
    document.getElementById('profUpi').value = currentUser.registeredUpiId || '';
    document.getElementById('profGst').value = currentUser.gstNumber || '';
    document.getElementById('profUdyam').value = currentUser.udyamNumber || '';
}

function showSection(secId, btn) {
    document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
    document.getElementById('sec-' + secId).classList.remove('hidden');
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    if (btn) btn.classList.add('active');

    if (secId === 'dashboard') loadDashboard();
    if (secId === 'customers') loadCustomers();
}

async function loadDashboard() {
    const res = await fetch(`/api/dashboard/${currentUser.id}`);
    const data = await res.json();

    document.getElementById('kpiTotal').innerText = '₹' + data.totalReceivables.toLocaleString('en-IN');
    document.getElementById('kpiOverdue').innerText = '₹' + data.overdueAmount.toLocaleString('en-IN');
    document.getElementById('kpiUpcoming').innerText = '₹' + data.upcomingAmount.toLocaleString('en-IN');
    document.getElementById('kpiDueTodaySub').innerText = 'Due today: ₹' + data.dueTodayAmount.toLocaleString('en-IN');
    document.getElementById('kpiPaid').innerText = '₹' + data.paidAmount.toLocaleString('en-IN');

    allInvoices = data.invoices;
    renderInvoices(allInvoices);
}

function filterInvoices(status, btn) {
    document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    if (status === 'ALL') renderInvoices(allInvoices);
    else renderInvoices(allInvoices.filter(i => i.status === status));
}

function renderInvoices(list) {
    const tbody = document.getElementById('invoiceTableBody');
    tbody.innerHTML = list.map(inv => `
<tr>
    <td><b>${inv.invoiceNumber}</b></td>
    <td>${inv.customerName}<br><small class="muted">${inv.customerPhone || ''}</small></td>
    <td><b>₹${inv.amount.toLocaleString('en-IN')}</b></td>
    <td>${inv.dueDate}${inv.overdueDays > 0 ? `<br><small style="color:#f87171">${inv.overdueDays} days late</small>` : ''}</td>
    <td><span class="badge ${inv.status}">${inv.status.replace('_', ' ')}</span></td>
    <td>
        ${inv.status !== 'PAID' ? `
        <div class="act-group">
            <button class="btn-sm" onclick="openRiskModal(${inv.id})">🎯 AI Risk</button>
            <button class="btn-sm" onclick="openReminderModal(${inv.id}, '${inv.customerPhone || ''}')">💬 AI Remind</button>
            <button class="btn-sm green" onclick="openPaidModal(${inv.id}, '${inv.invoiceNumber}', ${inv.amount})">✔ Mark Paid</button>
            ${inv.overdueDays >= 15 ? `<button class="btn-sm amber" onclick="openMsmeModal(${inv.id})">⚖️ MSME Legal</button>` : ''}
        </div>` : `<small style="color:#34d399">Paid (${inv.paymentRef || 'Verified'})</small>`}
    </td>
</tr>
`).join('');
}

async function runAiOcr() {
    const file = document.getElementById('ocrFile').files[0];
    if (!file) return;
    document.getElementById('ocrStatusText').innerText = '⏳ Gemini AI scanning invoice...';

    const fd = new FormData();
    fd.append('file', file);
    const res = await fetch('/api/invoices/extract-ai', { method: 'POST', body: fd });
    const data = await res.json();

    document.getElementById('invCustName').value = data.customerName || '';
    document.getElementById('invNumber').value = data.invoiceNumber || '';
    document.getElementById('invAmount').value = data.amount || '';
    document.getElementById('invDate').value = data.invoiceDate || '';
    document.getElementById('invDueDate').value = data.dueDate || '';
    document.getElementById('invPhone').value = data.customerPhone || '';
    document.getElementById('ocrStatusText').innerText = '✅ Extracted! Review details on the right & save.';
}

async function saveNewInvoice(e) {
    e.preventDefault();
    const payload = {
        userId: currentUser.id,
        customerName: document.getElementById('invCustName').value,
        invoiceNumber: document.getElementById('invNumber').value,
        amount: parseFloat(document.getElementById('invAmount').value),
        invoiceDate: document.getElementById('invDate').value,
        dueDate: document.getElementById('invDueDate').value,
        customerPhone: document.getElementById('invPhone').value
    };
    await fetch('/api/invoices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    alert('Invoice saved & monitoring activated!');
    showSection('dashboard', document.querySelectorAll('.nav-item')[0]);
}

function openModal(title, html) {
    document.getElementById('modalTitle').innerText = title;
    document.getElementById('modalBody').innerHTML = html;
    document.getElementById('aiModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('aiModal').classList.add('hidden');
}

async function openRiskModal(id) {
    openModal('🎯 AI Payment Risk Analysis', '<p>Calculating risk score with Gemini AI...</p>');
    const res = await fetch(`/api/recovery/risk/${id}`);
    const data = await res.json();
    const color = data.riskScore >= 70 ? '#ef4444' : (data.riskScore >= 40 ? '#f59e0b' : '#10b981');
    openModal(`🎯 Risk Profile: ${data.customerName}`, `
<div class="risk-meter" style="color:${color}">${data.riskScore}/100 —${data.riskLevel}</div>
<p class="muted" style="margin-bottom:12px;">Overdue by ${data.overdueDays} days</p>
<div class="ai-box">${data.aiAnalysis}</div>
`);
}

async function openReminderModal(id, phone) {
    openModal('💬 AI Smart Follow-up Generator', '<p>Drafting Hindi & English reminders...</p>');
    const res = await fetch(`/api/recovery/reminder/${id}`);
    const data = await res.json();

    let cleanPhone = (phone || '').replace(/\D/g, '');
    if (cleanPhone.length === 10) cleanPhone = '91' + cleanPhone;

    const hindiText = (data.hindiMessage || '').trim();
    const engText = (data.englishMessage || '').trim();

    const waHindi = `https://api.whatsapp.com/send?phone=${cleanPhone}&text=${encodeURIComponent(hindiText)}`;
    const waEng = `https://api.whatsapp.com/send?phone=${cleanPhone}&text=${encodeURIComponent(engText)}`;

    openModal(`💬 Smart Follow-up (${data.tone || 'Reminder'})`, `
<label class="muted">Hindi Reminder:</label>
<div class="ai-box" id="hindiMsgBox">${hindiText}</div>
<a href="${waHindi}" target="_blank" onclick="navigator.clipboard.writeText(document.getElementById('hindiMsgBox').innerText)" class="btn-primary" style="display:inline-block;text-decoration:none;margin-bottom:16px;">📲 Send Hindi on WhatsApp</a>

<label class="muted">English Reminder:</label>
<div class="ai-box" id="engMsgBox">${engText}</div>
<a href="${waEng}" target="_blank" onclick="navigator.clipboard.writeText(document.getElementById('engMsgBox').innerText)" class="btn-primary" style="display:inline-block;text-decoration:none;">📲 Send English on WhatsApp</a>
`);
}

function openPaidModal(id, invNo, amt) {
    openModal(`✔ Confirm Payment — ${invNo}`, `
<p class="muted" style="margin-bottom:12px;">Verify bank/UPI credit of <b>₹${amt.toLocaleString('en-IN')}</b> before marking paid.</p>
<div class="form-group">
    <label>Bank UTR / UPI Reference Number</label>
    <input type="text" id="paidUtrInput" placeholder="e.g., UTR6319048211" required>
</div>
<button class="btn-primary full-width" onclick="confirmMarkPaid(${id})">Confirm & Update Ledger</button>
`);
}

async function confirmMarkPaid(id) {
    const ref = document.getElementById('paidUtrInput').value || 'BANK-VERIFIED';
    await fetch(`/api/recovery/mark-paid/${id}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paymentRef: ref }) }); closeModal(); loadDashboard(); }  async function openMsmeModal(id) { openModal('⚖️ MSME Samadhaan & Section 15 Assistant', '<p>Generating legal guidance & escalation draft...</p>'); const res = await fetch(`/api/recovery/msme-guidance/${id}`);
    const data = await res.json();
    openModal(`⚖️ MSME Recovery Guidance (${data.invoiceNumber})`, `
<p style="color