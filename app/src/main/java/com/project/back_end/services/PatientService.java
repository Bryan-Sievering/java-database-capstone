package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.DTO.AppointmentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Create a new patient
     * @return 1 if successful, 0 if error
     */
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Check if a patient exists by email or phone
     */
    public boolean patientExists(String email, String phone) {
        boolean emailTaken = false;
        boolean phoneTaken = false;

        if (email != null && !email.isBlank()) {
            try {
                emailTaken = patientRepository.existsByEmail(email);
            } catch (Exception ignored) {
                emailTaken = patientRepository.findByEmail(email).isPresent();
            }
        }
        if (phone != null && !phone.isBlank()) {
            try {
                phoneTaken = patientRepository.existsByPhone(phone);
            } catch (Exception ignored) {
                phoneTaken = patientRepository.findByEmailOrPhone("", phone).isPresent();
            }
        }
        return emailTaken || phoneTaken;
    }

    /**
     * Get patient details using token
     */
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();
        String patientId = tokenService.extractPatientId(token).toString();
        Optional<Patient> optionalPatient = patientRepository.findById(Long.parseLong(patientId));

        if (optionalPatient.isEmpty()) {
            response.put("message", "Patient not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("patient", optionalPatient.get());
        return ResponseEntity.ok(response);
    }


    /**
     * Get all appointments for a patient
     */
    public List<AppointmentDTO> getPatientAppointment(Long patientId, String token) {
        Long tokenPatientId = tokenService.extractPatientId(token);
        if (!tokenPatientId.equals(patientId)) {
            throw new RuntimeException("Unauthorized access");
        }

        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        return appointments.stream()
                .map(AppointmentDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Filter appointments by condition: "past" or "future"
     */
    public List<AppointmentDTO> filterByCondition(String condition, Long patientId, String token) {
        Long tokenPatientId = tokenService.extractPatientId(token);
        if (!tokenPatientId.equals(patientId)) {
            throw new RuntimeException("Unauthorized access");
        }

        List<Appointment> appointments;
        if ("past".equalsIgnoreCase(condition)) {
            appointments = appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, 1);
        } else if ("future".equalsIgnoreCase(condition)) {
            appointments = appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, 0);
        } else {
            throw new RuntimeException("Invalid condition: " + condition);
        }

        return appointments.stream()
                .map(AppointmentDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Filter appointments by doctor name and condition
     */
    public List<AppointmentDTO> filterByDoctorAndCondition(String condition, String doctorName, Long patientId, String token) {
        Long tokenPatientId = tokenService.extractPatientId(token);
        if (!tokenPatientId.equals(patientId)) {
            throw new RuntimeException("Unauthorized access");
        }

        List<Appointment> appointments;
        if ("past".equalsIgnoreCase(condition)) {
            appointments = appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(doctorName, patientId, 1);
        } else if ("future".equalsIgnoreCase(condition)) {
            appointments = appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(doctorName, patientId, 0);
        } else {
            throw new RuntimeException("Invalid condition: " + condition);
        }

        return appointments.stream()
                .map(AppointmentDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Validate patient login credentials and return a token if successful
     */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Patient> patientOpt = patientRepository.findByEmail(login.getEmail());
            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                if (patient.getPassword().equals(login.getPassword())) {
                    // Use patient ID as the JWT subject so TokenService can parse/validate it
                    String token = tokenService.generateToken(patient.getId().toString());
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


    public List<AppointmentDTO> filterByDoctor(String doctorName, Long patientId, String token) {
        // You can reuse existing filtering logic
        List<AppointmentDTO> allAppointments = getPatientAppointment(patientId, token);
        return allAppointments.stream()
                .filter(appt -> appt.getDoctorName().toLowerCase().contains(doctorName.toLowerCase()))
                .collect(Collectors.toList());
    }


}