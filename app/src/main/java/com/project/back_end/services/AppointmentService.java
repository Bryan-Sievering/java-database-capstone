package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (!validateAppointment(appointment)) {
            throw new RuntimeException("Appointment validation failed or doctor unavailable");
        }
        return appointmentRepository.save(appointment);
    }

    // ------------------- UPDATE -------------------
    @Transactional
    public Appointment updateAppointment(Appointment appointment) {
        Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Appointment not found");
        }

        Appointment existing = existingOpt.get();

        if (!Objects.equals(existing.getPatient().getId(), appointment.getPatient().getId())) {
            throw new RuntimeException("Unauthorized update attempt");
        }

        // Optional: check doctor availability if appointment time changed
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
        Optional<Appointment> existing = appointmentRepository.findById(id);

        if (existing.isEmpty()) {
            response.put("message", "Appointment not found.");
            return ResponseEntity.badRequest().body(response);
        }

        Long patientIdFromToken = tokenService.extractPatientId(token);
        if (!Objects.equals(existing.get().getPatient().getId(), patientIdFromToken)) {
            response.put("message", "Unauthorized cancel attempt.");
            return ResponseEntity.status(403).body(response);
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

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);

        if (patientName != null && !patientName.trim().isEmpty()) {
            appointments.removeIf(appt -> !appt.getPatient().getName().toLowerCase().contains(patientName.toLowerCase()));
        }

        return appointments;
    }

    // ------------------- CHANGE STATUS -------------------
    @Transactional
    public boolean changeStatus(long id, int status) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(id);
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();
            appointment.setStatus(status);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    // ------------------- VALIDATE -------------------
    public boolean validateAppointment(Appointment appointment) {
        if (appointment.getAppointmentTime() == null
                || appointment.getPatient() == null
                || appointment.getDoctor() == null) {
            return false;
        }

        List<Appointment> existing = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                appointment.getDoctor().getId(),
                appointment.getAppointmentTime().minusMinutes(30),
                appointment.getAppointmentTime().plusMinutes(30)
        );

        return existing.isEmpty();
    }

    @Transactional
    public boolean markPrescriptionAdded(Long appointmentId) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();
            appointment.setPrescriptionAdded(true); // assuming you have a boolean field 'prescriptionAdded' in Appointment
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

}
