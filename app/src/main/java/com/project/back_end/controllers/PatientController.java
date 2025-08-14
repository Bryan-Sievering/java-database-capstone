package com.project.back_end.controllers;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.models.Appointment;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final TokenService tokenService;

    @Autowired
    public PatientController(PatientService patientService, TokenService tokenService) {
        this.patientService = patientService;
        this.tokenService = tokenService;
    }

    // 1. Get Patient Details
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        return patientService.getPatientDetails(token);
    }





    // 2. Create a New Patient
    @PostMapping()
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        boolean exists = patientService.patientExists(patient.getEmail(), patient.getPhone());
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Patient with email or phone already exists"));
        }

        int createdInt = patientService.createPatient(patient);
        boolean created = createdInt == 1; // convert int to boolean

        if (created) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Signup successful"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }


    // 3. Patient Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        String token = patientService.validatePatientLogin(login);
        if (token != null) {
            return ResponseEntity.ok(Map.of("token", token, "message", "Login successful"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }


    // 4. Get Patient Appointments
    @GetMapping("/{id}/{token}")
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

    // 5. Filter Patient Appointments
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointments(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {

        try {
            // Extract the patient ID from the token
            Long patientId = tokenService.extractPatientId(token);

            // Call the service method with the patientId
            List<AppointmentDTO> filtered = patientService.filterByCondition(condition, patientId, token);

            return ResponseEntity.ok(Map.of("appointments", filtered));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}


// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.


