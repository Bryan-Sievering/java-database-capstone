package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmail(String email);

    // Added for efficient existence checks
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Keep if used elsewhere
    Optional<Patient> findByEmailOrPhone(String email, String phone);
}