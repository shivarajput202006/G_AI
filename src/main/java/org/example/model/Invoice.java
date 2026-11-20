package org.example.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long customerId;
    private String customerName;
    private String customerPhone;

    private String invoiceNumber;
    private Double amount;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate paidDate;

    private String status; // UPCOMING, DUE_TODAY, OVERDUE, PAID
    private String paymentRef;
    private String notes;

    @Transient
    public long getOverdueDays() {
        if ("PAID".equalsIgnoreCase(status) || dueDate == null) return 0;
        long days = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        return Math.max(0, days);
    }
}