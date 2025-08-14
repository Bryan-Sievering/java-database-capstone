package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Prescription;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.TokenService;
import com.project.back_end.services.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.project.back_end.repo.AppointmentRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final TokenService service; // shared service for token validation
    private final AppointmentService appointmentService;
    private AppointmentRepository appointmentRepository;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  TokenService service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    /**
     * Save a new prescription
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @RequestBody Prescription prescription,
            @PathVariable String token) {

        // Validate doctor token
        if (!service.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid doctor token"));
        }

        try {
            // Update appointment status if needed
            appointmentService.markPrescriptionAdded(prescription.getAppointmentId());

            // Save the prescription
            prescriptionService.savePrescription(prescription);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Prescription saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save prescription"));
        }
    }

    /**
     * Get prescription by appointment ID
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {

        // Validate doctor token
        if (!service.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid doctor token"));
        }

        List<Prescription> prescriptions = prescriptionService.getPrescriptionByAppointmentId(appointmentId);

        if (prescriptions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No prescription found for this appointment"));
        }

        // Return all prescriptions
        return ResponseEntity.ok(prescriptions);
    }


    // ------------------- PRESCRIPTION ADDED -------------------
    @Transactional
    public void markPrescriptionAdded(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);

        if (appointmentOpt.isEmpty()) {
            throw new RuntimeException("Appointment not found");
        }

        Appointment appointment = appointmentOpt.get();
        // Ensure your Appointment model has this field
        appointment.setPrescriptionAdded(true);
        appointmentRepository.save(appointment);
    }

}


// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("${api.path}prescription")` to set the base path for all prescription-related endpoints.
//    - This controller manages creating and retrieving prescriptions tied to appointments.


// 2. Autowire Dependencies:
//    - Inject `PrescriptionService` to handle logic related to saving and fetching prescriptions.
//    - Inject the shared `Service` class for token validation and role-based access control.
//    - Inject `AppointmentService` to update appointment status after a prescription is issued.


// 3. Define the `savePrescription` Method:
//    - Handles HTTP POST requests to save a new prescription for a given appointment.
//    - Accepts a validated `Prescription` object in the request body and a doctor’s token as a path variable.
//    - Validates the token for the `"doctor"` role.
//    - If the token is valid, updates the status of the corresponding appointment to reflect that a prescription has been added.
//    - Delegates the saving logic to `PrescriptionService` and returns a response indicating success or failure.


// 4. Define the `getPrescription` Method:
//    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
//    - Accepts the appointment ID and a doctor’s token as path variables.
//    - Validates the token for the `"doctor"` role using the shared service.
//    - If the token is valid, fetches the prescription using the `PrescriptionService`.
//    - Returns the prescription details or an appropriate error message if validation fails.

