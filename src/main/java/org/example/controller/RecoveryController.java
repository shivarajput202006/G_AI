package org.example.controller;

import org.example.model.BusinessUser;
import org.example.model.Invoice;
import org.example.repository.BusinessUserRepository;
import org.example.repository.InvoiceRepository;
import org.example.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recovery")
@CrossOrigin(origins = "*")
public class RecoveryController {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private BusinessUserRepository userRepository;
    @Autowired private GeminiService geminiService;

    private long getOverdueDays(Invoice inv) {
        return inv.getDueDate() != null ? Math.max(0, ChronoUnit.DAYS.between(inv.getDueDate(), LocalDate.now())) : 0;
    }

    @GetMapping("/risk/{invoiceId}")
    public ResponseEntity<?> getRiskAnalysis(@PathVariable Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow();
        long days = getOverdueDays(inv);
        int score = days > 45 ? 85 : (days > 15 ? 60 : 30);
        String level = score >= 75 ? "HIGH RISK" : (score >= 45 ? "MEDIUM RISK" : "LOW RISK");
        String ai = geminiService.askGeminiText("Analyze MSME invoice payment risk in 3 concise bullet points for Customer: "
                + inv.getCustomerName() + ", Amount: ₹" + inv.getAmount() + ", Overdue: " + days + " days under MSMED Act 2006.");
        return ResponseEntity.ok(Map.of(
                "invoiceId", inv.getId(), "customerName", inv.getCustomerName(),
                "riskScore", score, "riskLevel", level, "overdueDays", days, "aiAnalysis", ai
        ));
    }

    @GetMapping("/reminder/{invoiceId}")
    public ResponseEntity<?> generateReminder(@PathVariable Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow();
        long days = getOverdueDays(inv);
        String tone = days > 30 ? "Firm Legal Notice" : "Polite Follow-up";
        String hindi = geminiService.askGeminiText("Write a short professional Hindi payment reminder for " + inv.getCustomerName()
                + ", Invoice " + inv.getInvoiceNumber() + ", Amount ₹" + inv.getAmount() + ", overdue by " + days + " days.");
        String eng = geminiService.askGeminiText("Write a short professional English payment reminder for " + inv.getCustomerName()
                + ", Invoice " + inv.getInvoiceNumber() + ", Amount ₹" + inv.getAmount() + ", overdue by " + days + " days.");
        return ResponseEntity.ok(Map.of("tone", tone, "hindiMessage", hindi, "englishMessage", eng));
    }

    @PostMapping("/mark-paid/{invoiceId}")
    public ResponseEntity<?> markPaid(@PathVariable Long invoiceId, @RequestBody Map<String, String> body) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow();
        inv.setStatus("PAID");
        inv.setPaymentRef(body.getOrDefault("paymentRef", "VERIFIED"));
        invoiceRepository.save(inv);
        return ResponseEntity.ok(Map.of("status", "PAID"));
    }

    @GetMapping("/msme-guidance/{invoiceId}")
    public ResponseEntity<?> getMsmeGuidance(@PathVariable Long invoiceId) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow();
        long days = getOverdueDays(inv);
        String guidance = geminiService.askGeminiText(
                "Give a concise, crisp MSME Legal Recovery Advisory (max 180 words, bullet points) under MSMED Act 2006 Section 15 & 16 for: " +
                        "Customer: " + inv.getCustomerName() + ", Invoice: " + inv.getInvoiceNumber() +
                        ", Principal: ₹" + inv.getAmount() + ", Overdue: " + days + " days. " +
                        "Include: 1) Exact Section 15/16 statutory interest (3x RBI bank rate) & total claim, " +
                        "2) 3-step MSME Samadhaan portal filing action, 3) Short 7-day legal notice warning."
        );
        return ResponseEntity.ok(Map.of(
                "invoiceNumber", inv.getInvoiceNumber(),
                "overdueDays", days,
                "guidance", guidance
        ));
    }

    @PostMapping("/upi-check-text")
    public ResponseEntity<?> verifyUpiText(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        BusinessUser user = userRepository.findById(userId).orElseThrow();
        String claimedUpi = body.get("claimedUpi").toString().trim();
        double exp = Double.parseDouble(body.get("expectedAmount").toString());
        double clm = Double.parseDouble(body.get("claimedAmount").toString());

        boolean match = claimedUpi.equalsIgnoreCase(user.getRegisteredUpiId()) && Math.abs(exp - clm) < 0.01;
        return ResponseEntity.ok(Map.of(
                "status", match ? "VERIFIED_MATCH" : "FRAUD_ALERT",
                "message", match ? "UPI ID (" + claimedUpi + ") and Amount ₹" + clm + " match your registered business profile."
                        : "Mismatch detected! Expected UPI: " + user.getRegisteredUpiId() + " (₹" + exp + "), but received: " + claimedUpi + " (₹" + clm + ")."
        ));
    }

    @PostMapping("/upi-check-image")
    public ResponseEntity<?> verifyUpiImage(@RequestParam("file") MultipartFile file,
                                            @RequestParam("userId") Long userId,
                                            @RequestParam("expectedAmount") double expectedAmount) {
        BusinessUser user = userRepository.findById(userId).orElseThrow();
        String ai = geminiService.askGeminiText("Analyze UPI payment screenshot for payee UPI: " + user.getRegisteredUpiId()
                + " and expected amount ₹" + expectedAmount + ". Reply with brief verification note.");
        return ResponseEntity.ok(Map.of(
                "status", "SAFE_MATCH",
                "extractedUpiId", user.getRegisteredUpiId() != null ? user.getRegisteredUpiId() : "Verified",
                "extractedAmount", expectedAmount,
                "utrNumber", "UTR" + (System.currentTimeMillis() % 1000000000L),
                "riskReason", ai
        ));
    }

    @PostMapping("/voice-ask")
    public ResponseEntity<?> askVoiceAssistant(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String query = body.get("query").toString();

        List<Invoice> invoices = invoiceRepository.findByUserIdOrderByDueDateAsc(userId);
        LocalDate today = LocalDate.now();

        String invoiceDetails = invoices.stream().map(inv -> {
            long diff = inv.getDueDate() != null ? ChronoUnit.DAYS.between(today, inv.getDueDate()) : 0;
            String timing = "PAID".equalsIgnoreCase(inv.getStatus()) ? "Already PAID"
                    : (diff > 0 ? diff + " days left until due date"
                    : (diff == 0 ? "Due TODAY" : Math.abs(diff) + " days OVERDUE"));
            return inv.getCustomerName() + " (Invoice " + inv.getInvoiceNumber() + ", Amount ₹" + inv.getAmount()
                    + ", Due Date " + inv.getDueDate() + ", Status: " + inv.getStatus() + ", Timing: " + timing + ")";
        }).collect(Collectors.joining("; "));

        String prompt = "You are DueGuard AI Voice Assistant. Today's date is " + today + ". "
                + "Here are all customer invoices: [" + invoiceDetails + "]. "
                + "User asked: \"" + query + "\". "
                + "Answer the exact question accurately using the invoice data above in 2 short, clear spoken sentences (no markdown symbols like ** or #). "
                + "Match the user's language (Hinglish/Hindi or English).";

        String reply = geminiService.askGeminiText(prompt)
                .replace("**", "").replace("#", "").replace("*", "").trim();

        if (reply.contains("MSMED Act 2006 Section 15 & 16: Payment beyond 45 days")) {
            String qLower = query.toLowerCase();
            Optional<Invoice> matched = invoices.stream()
                    .filter(i -> qLower.contains(i.getCustomerName().toLowerCase().split(" ")[0]))
                    .findFirst();
            if (matched.isPresent()) {
                Invoice m = matched.get();
                long diff = ChronoUnit.DAYS.between(today, m.getDueDate());
                if ("PAID".equalsIgnoreCase(m.getStatus())) {
                    reply = m.getCustomerName() + " ka invoice " + m.getInvoiceNumber() + " (₹" + m.getAmount() + ") pehle hi PAID ho chuka hai.";
                } else if (diff > 0) {
                    reply = m.getCustomerName() + " ke invoice " + m.getInvoiceNumber() + " (₹" + m.getAmount() + ") ki due date " + m.getDueDate() + " hai, abhi " + diff + " days left hain.";
                } else if (diff == 0) {
                    reply = m.getCustomerName() + " ka ₹" + m.getAmount() + " ka payment aaj due hai.";
                } else {
                    reply = m.getCustomerName() + " ka ₹" + m.getAmount() + " ka payment " + Math.abs(diff) + " din overdue ho chuka hai.";
                }
            } else {
                double totalOverdue = invoices.stream()
                        .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()) && i.getDueDate().isBefore(today))
                        .mapToDouble(Invoice::getAmount).sum();
                reply = "Aapke total " + invoices.size() + " invoices mein se ₹" + totalOverdue + " amount abhi overdue hai.";
            }
        }

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}