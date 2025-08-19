// Java
package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;

    @Autowired
    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            TokenService tokenService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    // ------------------- BOOK -------------------
    @Transactional
    public Appointment bookAppointment(Appointment appointment) {
        validateEntitiesOrThrow(appointment);
        if (!validateAppointment(appointment)) {
            throw new RuntimeException("Appointment validation failed or doctor unavailable");
        }
        return appointmentRepository.save(appointment);
    }

    // ------------------- UPDATE -------------------
    @Transactional
    public Appointment updateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            throw new RuntimeException("Invalid appointment payload");
        }

        Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Appointment not found");
        }

        Appointment existing = existingOpt.get();

        if (appointment.getPatient() == null || appointment.getPatient().getId() == null
                || !Objects.equals(existing.getPatient().getId(), appointment.getPatient().getId())) {
            throw new RuntimeException("Unauthorized update attempt");
        }

        // Ensure referenced entities exist
        validateEntitiesOrThrow(appointment);

        // Check doctor availability if appointment time changed
        if (!Objects.equals(existing.getAppointmentTime(), appointment.getAppointmentTime())) {
            if (!validateAppointment(appointment)) {
                throw new RuntimeException("Doctor is unavailable at the requested time");
            }
        }

        return appointmentRepository.save(appointment);
    }

    // ------------------- CANCEL -------------------
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(Long id, String token) {
        Map<String, String> response = new HashMap<>();
        if (id == null || token == null || token.isBlank()) {
            response.put("message", "Invalid request");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Appointment> existing = appointmentRepository.findById(id);
        if (existing.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Long patientIdFromToken;
        try {
            patientIdFromToken = tokenService.extractPatientId(token);
        } catch (Exception ex) {
            response.put("message", "Invalid or expired token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (!Objects.equals(existing.get().getPatient().getId(), patientIdFromToken)) {
            response.put("message", "Unauthorized cancel attempt.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        appointmentRepository.delete(existing.get());
        response.put("message", "Appointment canceled successfully.");
        return ResponseEntity.ok(response);
    }

    // ------------------- GET APPOINTMENTS -------------------
    @Transactional(readOnly = true)
    public List<Appointment> getAppointments(Long doctorId, String patientName, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        if (patientName != null && !patientName.trim().isEmpty()) {
            return appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctorId, patientName.trim(), start, end
            );
        }

        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
    }

    // ------------------- CHANGE STATUS -------------------
    @Transactional
    public boolean changeStatus(long id, int status) {
        return appointmentRepository.updateStatus(status, id) > 0;
    }

    // ------------------- VALIDATE -------------------
    // Checks minimal constraints and overlapping slots.
    public boolean validateAppointment(Appointment appointment) {
        if (appointment == null
                || appointment.getAppointmentTime() == null
                || appointment.getPatient() == null
                || appointment.getPatient().getId() == null
                || appointment.getDoctor() == null
                || appointment.getDoctor().getId() == null) {
            return false;
        }

        // Optional: ensure time is in the future
        if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Prevent overlap within +-30 minutes for the same doctor
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                appointment.getDoctor().getId(),
                appointment.getAppointmentTime().minusMinutes(30),
                appointment.getAppointmentTime().plusMinutes(30)
        );

        // If updating, allow the existing appointment itself
        if (appointment.getId() != null) {
            existing.removeIf(a -> Objects.equals(a.getId(), appointment.getId()));
        }

        return existing.isEmpty();
    }

    @Transactional
    public boolean markPrescriptionAdded(Long appointmentId) {
        if (appointmentId == null) return false;

        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();

            // If your Appointment entity does not have a 'prescriptionAdded' field,
            // replace the next two lines with the appropriate logic or remove this method.
            appointment.setPrescriptionAdded(true);
            appointmentRepository.save(appointment);

            return true;
        }
        return false;
    }

    // ------------------- HELPERS -------------------
    private void validateEntitiesOrThrow(Appointment appointment) {
        if (appointment == null
                || appointment.getPatient() == null || appointment.getPatient().getId() == null
                || appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new RuntimeException("Invalid appointment payload");
        }

        boolean patientExists = patientRepository.existsById(appointment.getPatient().getId());
        boolean doctorExists = doctorRepository.existsById(appointment.getDoctor().getId());

        if (!patientExists) {
            throw new RuntimeException("Patient does not exist");
        }
        if (!doctorExists) {
            throw new RuntimeException("Doctor does not exist");
        }
    }
}