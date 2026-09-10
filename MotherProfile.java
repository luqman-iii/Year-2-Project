package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.MotherProfile;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.MotherProfileRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/mother-profiles")
@CrossOrigin(origins = "*")
public class MotherProfileController {

    private final MotherProfileRepository motherProfileRepository;
    private final UserRepository userRepository;

    public MotherProfileController(MotherProfileRepository motherProfileRepository, UserRepository userRepository) {
        this.motherProfileRepository = motherProfileRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<MotherProfile> getAll() {
        return motherProfileRepository.findAll();
    }

    @GetMapping("/user/{userId}")
    public MotherProfile getByUserId(@PathVariable Long userId) {
        return motherProfileRepository.findByUser_Id(userId);
    }

    @PostMapping("/user/{userId}")
    public MotherProfile createForUser(@PathVariable Long userId, @RequestBody MotherProfile profile) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        profile.setUser(user);
        return motherProfileRepository.save(profile);
    }

    @PutMapping("/user/{userId}")
    public MotherProfile updateForUser(@PathVariable Long userId, @RequestBody MotherProfile updated) {
        MotherProfile existing = motherProfileRepository.findByUser_Id(userId);
        if (existing == null) return null;

        existing.setPregnancyStatus(updated.getPregnancyStatus());
        existing.setDueDate(updated.getDueDate());
        existing.setConditions(updated.getConditions());
        existing.setMedication(updated.getMedication());

        return motherProfileRepository.save(existing);
    }

    @Transactional
    @DeleteMapping("/user/{userId}")
    public void deleteForUser(@PathVariable Long userId) {
        motherProfileRepository.deleteByUser_Id(userId);
    }
}