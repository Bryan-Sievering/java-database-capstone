package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TokenService tokenService;

    public AppointmentController(AppointmentService appointmentService, TokenService tokenService) {
        this.appointmentService = appointmentService;
        this.tokenService = tokenService;
    }

    // 1. Get Appointments (Doctor only)
    @GetMapping("/{date}/{patientName}/{token:.+}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token
    ) {
        if (!tokenService.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format"));
        }

        Long doctorId = tokenService.extractDoctorId(token);
        List<Appointment> appointments = appointmentService.getAppointments(doctorId, patientName, localDate);

        return ResponseEntity.ok(Map.of("appointments", appointments));
    }

    // 2. Book Appointment (Patient only)
    @PostMapping("/{token:.+}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }

        if (!appointmentService.validateAppointment(appointment)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid appointment details"));
        }

        Appointment saved = appointmentService.bookAppointment(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 3. Update Appointment (Patient only)
    @PutMapping("/{token:.+}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }

        Appointment updated = appointmentService.updateAppointment(appointment);
        return ResponseEntity.ok(Map.of(
                "message", "Appointment updated successfully",
                "id", updated.getId().toString()
        ));
    }

    // 4. Cancel Appointment (Patient only)
    @DeleteMapping("/{id}/{token:.+}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long id,
            @PathVariable String token
    ) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }

        appointmentService.cancelAppointment(id, token);
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully", "id", id.toString()));
    }
}