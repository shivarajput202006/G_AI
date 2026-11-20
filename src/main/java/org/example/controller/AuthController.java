package org.example.controller;

import org.example.model.BusinessUser;
import org.example.repository.BusinessUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final BusinessUserRepository userRepo;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody BusinessUser user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        return ResponseEntity.ok(userRepo.save(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        return userRepo.findByEmail(req.get("email"))
                .filter(u -> u.getPassword().equals(req.get("password")))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid email or password")));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody BusinessUser updated) {
        return userRepo.findById(id).map(u -> {
            u.setBusinessName(updated.getBusinessName());
            u.setOwnerName(updated.getOwnerName());
            u.setPhone(updated.getPhone());
            u.setRegisteredUpiId(updated.getRegisteredUpiId());
            u.setGstNumber(updated.getGstNumber());
            u.setUdyamNumber(updated.getUdyamNumber());
            return ResponseEntity.ok(userRepo.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }
}