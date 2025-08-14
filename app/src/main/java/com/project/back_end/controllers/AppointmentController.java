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

    // ✅ 1. Get Appointments (Doctor only)
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token
    ) {
        // Validate token (doctor only)
        if (!tokenService.validateToken(token, "doctor")) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid token"));
        }

        // Convert string to LocalDate
        LocalDate localDate = LocalDate.parse(date);

        // Extract doctor ID from token
        Long doctorId = tokenService.extractDoctorId(token);

        // Call service
        List<Appointment> appointments = appointmentService.getAppointments(doctorId, patientName, localDate);

        // Wrap in a Map for response
        Map<String, Object> result = new HashMap<>();
        result.put("appointments", appointments);

        return ResponseEntity.ok(result);
    }



    // ✅ 2. Book Appointment (Patient only)
    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment
    ) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid token"));
        }

        if (!appointmentService.validateAppointment(appointment)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid appointment details"));
        }

        Appointment saved = appointmentService.bookAppointment(appointment);
        return ResponseEntity.status(201).body(saved);
    }


    // ✅ 3. Update Appointment (Patient only)
    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(@PathVariable String token,
                                               @RequestBody Appointment appointment) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token for patient"));
        }

        Appointment updated = appointmentService.updateAppointment(appointment);
        return ResponseEntity.ok(Map.of("message", "Appointment updated successfully",
                "id", updated.getId().toString()));
    }

    // ✅ 4. Cancel Appointment (Patient only)
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id,
                                               @PathVariable String token) {
        if (!tokenService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token for patient"));
        }

        appointmentService.cancelAppointment(id, token);
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully", "id", id.toString()));
    }
}


// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.


// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.


// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.

