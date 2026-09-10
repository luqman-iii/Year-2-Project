package com.startup1.startup1_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "medication_requests")
public class MedicationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Requester requester;

    @ManyToOne
    @JoinColumn(name = "requester_user_id")
    @JsonIgnore
    private User requesterUser;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false, length = 20)
    private String requestStatus = "PENDING";

    @Column(length = 255)
    private String notes;

    public MedicationRequest() {
    }

    public MedicationRequest(
            Requester requester,
            User requesterUser,
            Pharmacy pharmacy,
            Medication medication,
            LocalDate requestDate,
            String requestStatus,
            String notes
    ) {
        this.requester = requester;
        this.requesterUser = requesterUser;
        this.pharmacy = pharmacy;
        this.medication = medication;
        this.requestDate = requestDate;
        this.requestStatus = requestStatus;
        this.notes = notes;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Requester getRequester() {
        return requester;
    }

    public void setRequester(Requester requester) {
        this.requester = requester;
    }

    public User getRequesterUser() {
        return requesterUser;
    }

    public void setRequesterUser(User requesterUser) {
        this.requesterUser = requesterUser;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
