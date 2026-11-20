package org.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "business_users")
@Data
public class BusinessUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessName;
    private String ownerName;
    @Column(unique = true)
    private String email;
    private String password;
    private String phone;
    private String registeredUpiId;
    private String gstNumber;
    private String udyamNumber;
}