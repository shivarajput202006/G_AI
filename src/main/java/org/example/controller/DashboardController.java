package org.example.controller;

import org.example.model.Invoice;
import org.example.repository.CustomerRepository;
import org.example.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DashboardController {

    private final InvoiceRepository invoiceRepo;
    private final CustomerRepository customerRepo;

    @GetMapping("/{userId}")
    public Map<String, Object> getDashboard(@PathVariable Long userId) {
        List<Invoice> invoices = invoiceRepo.findByUserIdOrderByDueDateAsc(userId);
        LocalDate today = LocalDate.now();

        double totalReceivables = 0;
        double overdueAmount = 0;
        double upcomingAmount = 0;
        double dueTodayAmount = 0;
        double paidAmount = 0;

        for (Invoice inv : invoices) {
            if (!"PAID".equalsIgnoreCase(inv.getStatus())) {
                if (inv.getDueDate().isBefore(today)) {
                    inv.setStatus("OVERDUE");
                    overdueAmount += inv.getAmount();
                } else if (inv.getDueDate().isEqual(today)) {
                    inv.setStatus("DUE_TODAY");
                    dueTodayAmount += inv.getAmount();
                    upcomingAmount += inv.getAmount();
                } else {
                    inv.setStatus("UPCOMING");
                    upcomingAmount += inv.getAmount();
                }
                totalReceivables += inv.getAmount();
                invoiceRepo.save(inv);
            } else {
                paidAmount += inv.getAmount();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReceivables", totalReceivables);
        result.put("overdueAmount", overdueAmount);
        result.put("upcomingAmount", upcomingAmount);
        result.put("dueTodayAmount", dueTodayAmount);
        result.put("paidAmount", paidAmount);
        result.put("totalCustomers", customerRepo.findByUserId(userId).size());
        result.put("invoices", invoices);
        return result;
    }
}