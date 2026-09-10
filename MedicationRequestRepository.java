package com.startup1.startup1_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String licenceNumber;
    private String vehicleType;
    private String vehiclePlate;
    private String area;
    private String experience;

    public DriverProfile() {}

    public DriverProfile(User user, String licenceNumber, String vehicleType, String vehiclePlate, String area, String experience) {
        this.user = user;
        this.licenceNumber = licenceNumber;
        this.vehicleType = vehicleType;
        this.vehiclePlate = vehiclePlate;
        this.area = area;
        this.experience = experience;
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
}