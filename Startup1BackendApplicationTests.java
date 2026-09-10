package com.startup1.startup1_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "treatment_notes")
public class TreatmentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "incoming_patient_id", nullable = false)
    private IncomingPatient incomingPatient;

    @Column(length = 2000)
    private String noteText;

    private LocalDateTime createdAt;

    public TreatmentNote() {
        this.createdAt = LocalDateTime.now();
    }

    public TreatmentNote(IncomingPatient incomingPatient, String noteText) {
        this.incomingPatient = incomingPatient;
        this.noteText = noteText;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public IncomingPatient getIncomingPatient() { return incomingPatient; }
    public void setIncomingPatient(IncomingPatient incomingPatient) { this.incomingPatient = incomingPatient; }

    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}