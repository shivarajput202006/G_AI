package org.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "customers")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String name;
    private String phone;
    private String email;
    private String gstin;
    private String knownUpiId;
    private int totalInvoices = 0;
    private int delayedInvoices = 0;
    private double avgDelayDays = 0.0;
}