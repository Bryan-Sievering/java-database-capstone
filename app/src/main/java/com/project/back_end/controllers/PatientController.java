package com.project.back_end.controllers;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Java
@Controller
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final TokenService tokenService;

    @Autowired
    public PatientController(PatientService patientService, TokenService tokenService) {
        this.patientService = patientService;
        this.tokenService = tokenService;
    }

    // JSON: ResponseEntity
    // Get patient details by token
    @GetMapping("/{token:.+}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        return patientService.getPatientDetails(token);
    }

    // View: String (redirect/view name)
    @GetMapping("/login")
    public String patientLoginPage() {
        return "redirect:/pages/patientDashboard.html";
    }

    // JSON: ResponseEntity
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        boolean exists = patientService.patientExists(patient.getEmail(), patient.getPhone());
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Patient with email or phone already exists"));
        }
        int createdInt = patientService.createPatient(patient);
        if (createdInt == 1) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Signup successful"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }

    // JSON: ResponseEntity
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        return patientService.validatePatientLogin(login);
    }

    // JSON: ResponseEntity
    // Get appointments by patient ID and token
    @GetMapping("/{id}/{token:.+}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Long id, @PathVariable String token) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
        try {
            List<AppointmentDTO> appointments = patientService.getPatientAppointment(id, token);
            return ResponseEntity.ok(Map.of("appointments", appointments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to fetch appointments"));
        }
    }

    // JSON: ResponseEntity
    // Filter appointments by condition and doctor name
    @GetMapping("/filter/{condition}/{name}/{token:.+}")
    public ResponseEntity<?> filterPatientAppointments(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {
        try {
            Long patientId = tokenService.extractPatientId(token);
            List<AppointmentDTO> filtered = patientService.filterByCondition(condition, patientId, token);
            return ResponseEntity.ok(Map.of("appointments", filtered));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}