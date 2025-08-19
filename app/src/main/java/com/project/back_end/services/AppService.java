package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppService {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AppService(TokenService tokenService,
                      AdminRepository adminRepository,
                      DoctorRepository doctorRepository,
                      PatientRepository patientRepository,
                      DoctorService doctorService,
                      PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // ------------------ TOKEN VALIDATION ------------------
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> response = new HashMap<>();
        try {
            boolean valid = tokenService.validateToken(token, user);
            if (!valid) {
                response.put("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Token validation failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ------------------ ADMIN LOGIN ------------------
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
                response.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Optional<Admin> adminOpt = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (adminOpt.isEmpty()) {
                response.put("error", "Admin not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Admin admin = adminOpt.get();
            if (!passwordMatches(receivedAdmin.getPassword(), admin.getPassword())) {
                response.put("error", "Incorrect password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Use admin ID as JWT subject
            String token = tokenService.generateToken(admin.getId().toString());
            response.put("token", token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ------------------ DOCTOR FILTER ------------------
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorService.filterDoctorsByNameSpecialtyAndTime(name, specialty, time);
        response.put("doctors", doctors);
        return response;
    }

    // ------------------ APPOINTMENT VALIDATION ------------------
    // Returns:
    //   1  -> valid (slot available)
    //   0  -> invalid (slot not in availability)
    //  -1  -> invalid doctor or missing data
    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || appointment.getAppointmentTime() == null) {
            return -1;
        }

        Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctorOpt.isEmpty()) return -1;

        var date = appointment.getAppointmentTime().toLocalDate();
        List<LocalDateTime> availableSlots = doctorService.getDoctorAvailability(appointment.getDoctor().getId(), date)
                .stream()
                .map(time -> date.atTime(time))
                .collect(Collectors.toList());

        return availableSlots.contains(appointment.getAppointmentTime()) ? 1 : 0;
    }

    // ------------------ PATIENT VALIDATION ------------------
    // Returns true if email/phone are not already taken
    public boolean validatePatient(Patient patient) {
        if (patient == null) return false;
        String email = patient.getEmail();
        String phone = patient.getPhone();

        // Prefer checking individually to avoid null pitfalls in OR queries
        boolean emailExists = false;
        boolean phoneExists = false;

        if (email != null && !email.isBlank()) {
            emailExists = patientRepository.findByEmail(email).isPresent();
        }
        if (phone != null && !phone.isBlank()) {
            // If you have existsByPhone in repository, prefer that. Otherwise fall back to OR query.
            try {
                // Attempt using OR method when existsByPhone is not available
                phoneExists = patientRepository.findByEmailOrPhone("", phone).isPresent();
            } catch (Exception ignored) {
                // You can replace this with a repository method like existsByPhone for better semantics
                phoneExists = false;
            }
        }

        return !(emailExists || phoneExists);
    }

    // ------------------ PATIENT LOGIN ------------------
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            if (login == null || login.getEmail() == null || login.getPassword() == null) {
                response.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            Optional<Patient> patientOpt = patientRepository.findByEmail(login.getEmail());
            if (patientOpt.isEmpty()) {
                response.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Patient patient = patientOpt.get();
            if (!passwordMatches(login.getPassword(), patient.getPassword())) {
                response.put("message", "Incorrect password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Use patient ID as JWT subject
            String token = tokenService.generateToken(patient.getId().toString());
            response.put("token", token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", "Patient login error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Add this helper method at class level (not inside another method)
    private boolean passwordMatches(String raw, String stored) {
        return stored != null && stored.equals(raw);
    }
}