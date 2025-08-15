package com.project.back_end.services;

import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import com.project.back_end.DTO.Login;
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
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());

            if (admin != null) {
                if (admin.getPassword().equals(receivedAdmin.getPassword())) {
                    String token = tokenService.generateToken(admin.getUsername());
                    response.put("token", token);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("error", "Incorrect password");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            } else {
                response.put("error", "Admin not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
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
    public int validateAppointment(Appointment appointment) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctorOpt.isEmpty()) return -1;

        List<LocalDateTime> availableSlots = doctorService.getDoctorAvailability(
                        appointment.getDoctor().getId(),
                        appointment.getAppointmentTime().toLocalDate()
                ).stream()
                .map(time -> appointment.getAppointmentTime().toLocalDate().atTime(time))
                .collect(Collectors.toList());

        return availableSlots.contains(appointment.getAppointmentTime()) ? 1 : 0;
    }

    // ------------------ PATIENT VALIDATION ------------------
    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()).isEmpty();
    }

    // ------------------ PATIENT LOGIN ------------------
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Patient> patientOpt = patientRepository.findByEmail(login.getEmail());
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                if (patient.getPassword().equals(login.getPassword())) {
                    String token = tokenService.generateToken(patient.getEmail());
                    response.put("token", token);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("message", "Incorrect password");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            } else {
                response.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            response.put("message", "Patient login error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ------------------ HELPER: Get Patient from Token ------------------
    private Patient getPatientFromToken(String token) {
        Long patientId = tokenService.extractPatientId(token);
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Invalid token or patient not found"));
    }


    // ------------------ FILTER PATIENT APPOINTMENTS ------------------
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            Patient patient = getPatientFromToken(token);

            List<?> appointments;
            if (condition != null && !condition.isEmpty() && name != null && !name.isEmpty()) {
                appointments = patientService.filterByDoctorAndCondition(condition, name, patient.getId(), token);
            } else if (condition != null && !condition.isEmpty()) {
                appointments = patientService.filterByCondition(condition, patient.getId(), token);
            } else if (name != null && !name.isEmpty()) {
                appointments = patientService.filterByDoctor(name, patient.getId(), token);
            } else {
                appointments = patientService.getPatientAppointment(patient.getId(), token);
            }

            response.put("appointments", appointments);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public boolean validateAdminLogin(Admin admin) {
        // Example: check if username/password exist in DB
        return adminRepository.findByUsernameAndPassword(admin.getUsername(), admin.getPassword()).isPresent();
    }
}


// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.

