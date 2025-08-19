package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Java
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder; // added

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService,
                         PasswordEncoder passwordEncoder) { // added
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder; // added
    }

    /**
     * Save a doctor, prevent duplicates by email
     * @return -1 if duplicate, 1 if success, 0 if error
     */
// Java
// Make saveDoctor return codes match DoctorController:
// 1 = created, 0 = already exists (409), -1 = internal error
@Transactional
public int saveDoctor(Doctor doctor) {
    if (doctor == null || doctor.getEmail() == null || doctor.getEmail().isBlank()) {
        return -1;
    }
    try {
        boolean exists;
        try {
            exists = doctorRepository.existsByEmail(doctor.getEmail());
        } catch (Exception ignored) {
            exists = doctorRepository.findByEmail(doctor.getEmail()) != null;
        }
        if (exists) return 0;

        doctorRepository.save(doctor);
        return 1;
    } catch (Exception e) {
        return -1;
    }
}

    /**
     * Update an existing doctor
     * @return -1 if not found, 1 if success, 0 if error
     */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        if (doctor == null || doctor.getId() == null) {
            return -1;
        }
        try {
            if (!doctorRepository.existsById(doctor.getId())) {
                return -1;
            }
            // Optional duplicate email guard (skip if unchanged)
            if (doctor.getEmail() != null && !doctor.getEmail().isBlank()) {
                Doctor byEmail = doctorRepository.findByEmail(doctor.getEmail());
                if (byEmail != null && !Objects.equals(byEmail.getId(), doctor.getId())) {
                    // Treat as general error to avoid overloading -1 which means "not found"
                    return 0;
                }
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Delete a doctor and all related appointments
     * @return -1 if not found, 1 if success, 0 if error
     */
    @Transactional
    public int deleteDoctor(Long id) {
        if (id == null) return -1;

        Optional<Doctor> doctor = doctorRepository.findById(id);
        if (doctor.isEmpty()) {
            return -1;
        }
        try {
            // Use the repository method that exists
            List<Appointment> related = appointmentRepository.findByDoctor_Id(id);
            if (!related.isEmpty()) {
                appointmentRepository.deleteAll(related);
            }
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Validate doctor credentials and return a token
     */
// Replace the token generation in validateDoctor(...) to use doctor ID
public String validateDoctor(String email, String password) {
    if (email == null || password == null) return null;

    Doctor doctor = doctorRepository.findByEmail(email);
    if (doctor != null && passwordMatches(password, doctor.getPassword())) {
        return tokenService.generateToken(doctor.getId().toString());
    }
    return null;
}

    // Add this helper
    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder != null
                    ? passwordEncoder.matches(rawPassword, encodedPassword)
                    : Objects.equals(rawPassword, encodedPassword); // fallback if no encoder
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get doctor availability for a specific date
     */
    public List<LocalTime> getDoctorAvailability(Long doctorId, LocalDate date) {
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(
                        doctorId,
                        date.atStartOfDay(),
                        date.atTime(23, 59)
                );

        List<LocalTime> bookedTimes = appointments.stream()
                .map(a -> a.getAppointmentTime().toLocalTime())
                .toList();

        List<LocalTime> allSlots = generateTimeSlots();
        return allSlots.stream()
                .filter(slot -> !bookedTimes.contains(slot))
                .collect(Collectors.toList());
    }

    private List<LocalTime> generateTimeSlots() {
        List<LocalTime> slots = new ArrayList<>();
        for (int hour = 9; hour <= 17; hour++) {
            slots.add(LocalTime.of(hour, 0));
            slots.add(LocalTime.of(hour, 30));
        }
        return slots;
    }

    /**
     * Filter doctors by name, specialty, and time availability
     */
    public List<Doctor> filterDoctorsByNameSpecialtyAndTime(String name, String specialty, String timePeriod) {
        // Fetch doctors matching name and specialty
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                name != null ? name : "",
                specialty != null ? specialty : ""
        );

        // Filter by time period if provided
        if (timePeriod != null && !timePeriod.isEmpty()) {
            doctors = filterDoctorByTime(doctors, timePeriod);
        }

        return doctors;
    }

    /**
     * Filter doctors by name and time availability
     */
    public List<Doctor> filterDoctorByNameAndTime(String name, String timePeriod) {
        List<Doctor> doctors = doctorRepository.findByNameLike("%" + name + "%");
        return filterDoctorByTime(doctors, timePeriod);
    }

    /**
     * Filter doctors by AM/PM availability
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String timePeriod) {
        return doctors.stream()
                .filter(doctor -> doctor.getAvailableTimes() != null && doctor.getAvailableTimes().stream()
                        .map(LocalTime::parse) // convert string to LocalTime
                        .anyMatch(t -> timePeriod.equalsIgnoreCase("AM") ? t.isBefore(LocalTime.NOON) : t.isAfter(LocalTime.NOON))
                )
                .collect(Collectors.toList());
    }

    // Existing simpler queries
    public Doctor getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(email);
    }

    public List<Doctor> getBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyIgnoreCase(specialty);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }
}