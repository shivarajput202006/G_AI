package org.example.controller;

import org.example.model.Customer;
import org.example.model.Invoice;
import org.example.repository.CustomerRepository;
import org.example.repository.InvoiceRepository;
import org.example.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin("*")
public class InvoiceController {

    private final InvoiceRepository invoiceRepo;
    private final CustomerRepository customerRepo;
    private final GeminiService geminiService;

    @PostMapping
    public Invoice createInvoice(@RequestBody Invoice invoice) {
        Customer customer = customerRepo
                .findByUserIdAndNameIgnoreCase(invoice.getUserId(), invoice.getCustomerName())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setUserId(invoice.getUserId());
                    c.setName(invoice.getCustomerName());
                    c.setPhone(invoice.getCustomerPhone());
                    return c;
                });

        if (invoice.getCustomerPhone() != null && !invoice.getCustomerPhone().isBlank()) {
            customer.setPhone(invoice.getCustomerPhone());
        }
        customer.setTotalInvoices(customer.getTotalInvoices() + 1);
        customer = customerRepo.save(customer);

        invoice.setCustomerId(customer.getId());
        if (invoice.getInvoiceDate() == null) invoice.setInvoiceDate(LocalDate.now());
        if (invoice.getDueDate() == null) invoice.setDueDate(invoice.getInvoiceDate().plusDays(15));

        LocalDate today = LocalDate.now();
        if (invoice.getDueDate().isBefore(today)) invoice.setStatus("OVERDUE");
        else if (invoice.getDueDate().isEqual(today)) invoice.setStatus("DUE_TODAY");
        else invoice.setStatus("UPCOMING");

        return invoiceRepo.save(invoice);
    }

    @PostMapping(value = "/extract-ai", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> extractFromImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(geminiService.extractInvoiceFromFile(file));
    }

    @GetMapping("/user/{userId}")
    public List<Invoice> getAllInvoices(@PathVariable Long userId) {
        return invoiceRepo.findByUserIdOrderByDueDateAsc(userId);
    }

    @GetMapping("/customers/{userId}")
    public List<Customer> getAllCustomers(@PathVariable Long userId) {
        return customerRepo.findByUserId(userId);
    }
}
