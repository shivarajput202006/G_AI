package org.example;

import org.example.model.BusinessUser;
import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.repository.BusinessUserRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final BusinessUserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final InvoiceRepository invoiceRepo;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        BusinessUser user = new BusinessUser();
        user.setBusinessName("Sharma Manufacturing Pvt Ltd");
        user.setOwnerName("Rajesh Sharma");
        user.setEmail("demo@dueguard.in");
        user.setPassword("123456");
        user.setPhone("919876543210");
        user.setRegisteredUpiId("sharmamfg@okicici");
        user.setGstNumber("06AABCS1429B1Z5");
        user.setUdyamNumber("UDYAM-HR-05-0048291");
        user = userRepo.save(user);

        Customer c1 = saveCustomer(user.getId(), "ABC Traders", "919811112222", "abctraders@okaxis", 4, 3, 14.0);
        Customer c2 = saveCustomer(user.getId(), "XYZ Industries", "919822223333", "xyzind@ybl", 3, 0, 0.0);
        Customer c3 = saveCustomer(user.getId(), "RK Suppliers", "919833334444", "rksuppliers@icici", 5, 0, 0.0);
        Customer c4 = saveCustomer(user.getId(), "Verma Distributors", "919844445555", "vermadist@paytm", 2, 2, 32.0);

        saveInvoice(user.getId(), c1, "INV-2026-101", 25000.0, LocalDate.now().minusDays(22), LocalDate.now().minusDays(7), "OVERDUE", null);
        saveInvoice(user.getId(), c4, "INV-2026-089", 48000.0, LocalDate.now().minusDays(63), LocalDate.now().minusDays(48), "OVERDUE", null);
        saveInvoice(user.getId(), c2, "INV-2026-108", 35000.0, LocalDate.now().minusDays(5), LocalDate.now().plusDays(10), "UPCOMING", null);
        saveInvoice(user.getId(), c2, "INV-2026-112", 18000.0, LocalDate.now().minusDays(15), LocalDate.now(), "DUE_TODAY", null);
        saveInvoice(user.getId(), c3, "INV-2026-095", 70000.0, LocalDate.now().minusDays(30), LocalDate.now().minusDays(15), "PAID", "UTR4291084721");
    }

    private Customer saveCustomer(Long uid, String name, String phone, String upi, int total, int delayed, double avg) {
        Customer c = new Customer();
        c.setUserId(uid);
        c.setName(name);
        c.setPhone(phone);
        c.setKnownUpiId(upi);
        c.setTotalInvoices(total);
        c.setDelayedInvoices(delayed);
        c.setAvgDelayDays(avg);
        return customerRepo.save(c);
    }

    private void saveInvoice(Long uid, Customer c, String no, double amt, LocalDate invDate, LocalDate dueDate, String status, String ref) {
        Invoice inv = new Invoice();
        inv.setUserId(uid);
        inv.setCustomerId(c.getId());
        inv.setCustomerName(c.getName());
        inv.setCustomerPhone(c.getPhone());
        inv.setInvoiceNumber(no);
        inv.setAmount(amt);
        inv.setInvoiceDate(invDate);
        inv.setDueDate(dueDate);
        inv.setStatus(status);
        inv.setPaymentRef(ref);
        invoiceRepo.save(inv);
    }
}