package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    private final UserRepository userRepository;

    public PatientController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PatientSummary> getPatients() {
        return userRepository.findAll().stream()
                .filter(this::isPatientUser)
                .map(user -> new PatientSummary(
                        user.getId(),
                        user.getName(),
                        user.getPhone(),
                        user.getEmail(),
                        user.getAddress()
                ))
                .toList();
    }

    private boolean isPatientUser(User user) {
        if (user == null) {
            return false;
        }

        String role = user.getRole();
        if (role == null) {
            return true;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return !normalizedRole.equals("DRIVER")
                && !normalizedRole.equals("HOSPITAL")
                && !normalizedRole.equals("PHARMACY");
    }

    public record PatientSummary(
            Long patientId,
            String fullName,
            String phoneNumber,
            String email,
            String address
    ) {}
}
