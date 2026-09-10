package com.startup1.startup1_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "mother_profiles")
public class MotherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each mother profile belongs to exactly one user
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String pregnancyStatus;
    private LocalDate dueDate;

    @Column(length = 1000)
    private String conditions;

    @Column(length = 1000)
    private String medication;

    public MotherProfile() {}

    public MotherProfile(User user, String pregnancyStatus, LocalDate dueDate, String conditions, String medication) {
        this.user = user;
        this.pregnancyStatus = pregnancyStatus;
        this.dueDate = dueDate;
        this.conditions = conditions;
        this.medication = medication;
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPregnancyStatus() { return pregnancyStatus; }
    public void setPregnancyStatus(String pregnancyStatus) { this.pregnancyStatus = pregnancyStatus; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
}