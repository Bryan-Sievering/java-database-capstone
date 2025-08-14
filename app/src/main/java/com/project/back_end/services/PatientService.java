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
        return patientRepository.findByEmailOrPhone(email, phone).isPresent();
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
    public String validatePatientLogin(Login login) {
        Optional<Patient> optionalPatient = patientRepository.findByEmail(login.getEmail());
        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();
            // Compare the password
            if (patient.getPassword().equals(login.getPassword())) {
                // Generate a token with the patient ID as identifier
                return tokenService.generateToken(patient.getId().toString());
            }
        }
        return null; // invalid credentials
    }

    public List<AppointmentDTO> filterByDoctor(String doctorName, Long patientId, String token) {
        // You can reuse existing filtering logic
        List<AppointmentDTO> allAppointments = getPatientAppointment(patientId, token);
        return allAppointments.stream()
                .filter(appt -> appt.getDoctorName().toLowerCase().contains(doctorName.toLowerCase()))
                .collect(Collectors.toList());
    }


}
