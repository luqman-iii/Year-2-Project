package com.startup1.startup1_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incoming_patients")
public class IncomingPatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    private String status; 

    private Integer etaMinutes; 

    private LocalDateTime createdAt;

    private Long emergencyRequestId;

    private LocalDateTime hospitalVisibleAt;

    private LocalDateTime hospitalArrivalAt;

    public IncomingPatient() {
        this.createdAt = LocalDateTime.now();
    }

    public IncomingPatient(User patient, String status, Integer etaMinutes) {
        this.patient = patient;
        this.status = status;
        this.etaMinutes = etaMinutes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getEmergencyRequestId() { return emergencyRequestId; }
    public void setEmergencyRequestId(Long emergencyRequestId) { this.emergencyRequestId = emergencyRequestId; }

    public LocalDateTime getHospitalVisibleAt() { return hospitalVisibleAt; }
    public void setHospitalVisibleAt(LocalDateTime hospitalVisibleAt) { this.hospitalVisibleAt = hospitalVisibleAt; }

    public LocalDateTime getHospitalArrivalAt() { return hospitalArrivalAt; }
    public void setHospitalArrivalAt(LocalDateTime hospitalArrivalAt) { this.hospitalArrivalAt = hospitalArrivalAt; }
}
