package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.DriverProfile;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.DriverProfileRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/driver-profiles")
@CrossOrigin(origins = "*")
public class DriverProfileController {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;

    public DriverProfileController(DriverProfileRepository driverProfileRepository, UserRepository userRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<DriverProfile> getAll() {
        return driverProfileRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public DriverProfile getByUserId(@PathVariable Long userId) {
        return driverProfileRepository.findByUser_Id(userId);
    }

    @PostMapping("/user/{userId}")
    public DriverProfile createForUser(@PathVariable Long userId, @RequestBody DriverProfile profile) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        profile.setUser(user);
        return driverProfileRepository.save(profile);
    }

    @PutMapping("/user/{userId}")
    public DriverProfile updateForUser(@PathVariable Long userId, @RequestBody DriverProfile updated) {
        DriverProfile existing = driverProfileRepository.findByUser_Id(userId);
        if (existing == null) return null;

        existing.setLicenceNumber(updated.getLicenceNumber());
        existing.setVehicleType(updated.getVehicleType());
        existing.setVehiclePlate(updated.getVehiclePlate());
        existing.setArea(updated.getArea());
        existing.setExperience(updated.getExperience());

        return driverProfileRepository.save(existing);
    }

    @Transactional
    @DeleteMapping("/user/{userId}")
    public void deleteForUser(@PathVariable Long userId) {
        driverProfileRepository.deleteByUser_Id(userId);
    }
}