package com.project.back_end.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull(message = "Doctor is required")
    private Doctor doctor;

    @ManyToOne
    @NotNull(message = "Patient is required")
    private Patient patient;

    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime;

    @NotNull(message = "Appointment status is required")
    private int status;

    @NotNull
    private boolean prescriptionAdded = false;


    @Transient
    public LocalDateTime getEndTime(){
        return this.appointmentTime.plusHours(1);
    }

    public LocalDate getAppointmentDate(){
        LocalDate appointmentDate = this.appointmentTime.toLocalDate();
        return appointmentDate;
    }

    public LocalTime getAppointmentTimeOnly(){
        LocalTime appointmentTimeOnly = this.appointmentTime.toLocalTime();
        return  appointmentTimeOnly;
    }

    public Appointment(){

    }


    public Appointment(Long id, Doctor doctor, Patient patient, LocalDateTime appointmentTime, int status) {
        this.id = id;
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isPrescriptionAdded() {
        return prescriptionAdded;
    }

    public void setPrescriptionAdded(boolean prescriptionAdded) {
        this.prescriptionAdded = prescriptionAdded;
    }
}

