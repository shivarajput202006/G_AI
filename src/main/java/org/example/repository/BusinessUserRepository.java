package org.example.repository;

import org.example.model.BusinessUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BusinessUserRepository extends JpaRepository<BusinessUser, Long> {
    Optional<BusinessUser> findByEmail(String email);
}