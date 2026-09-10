package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.EmergencyRequest;
import com.startup1.startup1_backend.entity.IncomingPatient;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.EmergencyRequestRepository;
import com.startup1.startup1_backend.repository.IncomingPatientRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/emergency-requests", "/requests"})
@CrossOrigin(origins = "*")
public class EmergencyRequestController {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final IncomingPatientRepository incomingPatientRepository;
    private final UserRepository userRepository;

    public EmergencyRequestController(
            EmergencyRequestRepository emergencyRequestRepository,
            IncomingPatientRepository incomingPatientRepository,
            UserRepository userRepository
    ) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.incomingPatientRepository = incomingPatientRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody EmergencyRequestCreateRequest body) {
        User patient = resolvePatient(body);
        if (!isPatient(patient)) {
            return ResponseEntity.badRequest().body("Patient not found.");
        }

        EmergencyRequest request = new EmergencyRequest();
        request.setPatient(patient);
        request.setStatus(normalizeStatus(body.status(), "PENDING"));
        request.setLocation(trimToNull(body.location()));
        request.setPhoneNumber(resolvePhoneNumber(body.phoneNumber(), patient));
        request.setEtaMinutes(body.etaMinutes());

        if (body.driverId() != null) {
            User driver = userRepository.findById(body.driverId()).orElse(null);
            if (!isDriver(driver)) {
                return ResponseEntity.badRequest().body("Driver not found.");
            }
            request.setDriver(driver);
            if ("PENDING".equals(request.getStatus())) {
                request.setStatus("DRIVER_ASSIGNED");
            }
        }

        return ResponseEntity.ok(toResponse(emergencyRequestRepository.save(request)));
    }

    @GetMapping
    public List<EmergencyRequestResponse> getAllRequests(@RequestParam(required = false) String status) {
        List<EmergencyRequest> requests = status == null || status.isBlank()
                ? emergencyRequestRepository.findAll()
                : emergencyRequestRepository.findByStatusIgnoreCase(status);

        return requests.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyRequestResponse> getRequest(@PathVariable Long id) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyRequestResponse> updateRequest(
            @PathVariable Long id,
            @RequestBody EmergencyRequestUpdateRequest body
    ) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (body.location() != null) {
            request.setLocation(trimToNull(body.location()));
        }
        if (body.phoneNumber() != null) {
            request.setPhoneNumber(trimToNull(body.phoneNumber()));
        }
        if (body.status() != null) {
            request.setStatus(normalizeStatus(body.status(), request.getStatus()));
        }
        if (body.etaMinutes() != null) {
            request.setEtaMinutes(body.etaMinutes());
        }
        if (body.driverId() != null) {
            User driver = userRepository.findById(body.driverId()).orElse(null);
            if (!isDriver(driver)) {
                return ResponseEntity.badRequest().build();
            }
            request.setDriver(driver);
        }

        return ResponseEntity.ok(toResponse(emergencyRequestRepository.save(request)));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<EmergencyRequestResponse> assignDriver(
            @PathVariable Long id,
            @RequestBody EmergencyDriverAssignRequest body
    ) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User driver = userRepository.findById(body.driverId()).orElse(null);
        if (!isDriver(driver)) {
            return ResponseEntity.badRequest().build();
        }

        request.setDriver(driver);
        request.setStatus("DRIVER_ASSIGNED");
        request.setEtaMinutes(body.etaMinutes() != null ? body.etaMinutes() : request.getEtaMinutes());
        return ResponseEntity.ok(toResponse(emergencyRequestRepository.save(request)));
    }

    @PutMapping("/{id}/update-status")
    public ResponseEntity<EmergencyRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody EmergencyStatusUpdateRequest body
    ) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        request.setStatus(normalizeStatus(body.status(), request.getStatus()));
        return ResponseEntity.ok(toResponse(emergencyRequestRepository.save(request)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<EmergencyRequestResponse> completeRequest(@PathVariable Long id) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        request.setStatus("COMPLETED");
        return ResponseEntity.ok(toResponse(emergencyRequestRepository.save(request)));
    }

    @PostMapping("/{id}/handoff-to-hospital")
    public ResponseEntity<?> handoffToHospital(@PathVariable Long id) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null || !isPatient(request.getPatient())) {
            return ResponseEntity.notFound().build();
        }

        IncomingPatient incomingPatient = incomingPatientRepository.findByEmergencyRequestId(request.getId());
        if (incomingPatient == null) {
            incomingPatient = new IncomingPatient();
            incomingPatient.setPatient(request.getPatient());
            incomingPatient.setEmergencyRequestId(request.getId());
        }

        incomingPatient.setStatus("ON_THE_WAY");
        incomingPatient.setEtaMinutes(0);
        incomingPatientRepository.save(incomingPatient);

        request.setStatus("COMPLETED");
        emergencyRequestRepository.save(request);

        return ResponseEntity.ok().build();
    }

    private EmergencyRequestResponse toResponse(EmergencyRequest request) {
        User patient = request.getPatient();
        User driver = request.getDriver();

        return new EmergencyRequestResponse(
                request.getId(),
                patient != null ? patient.getId() : null,
                patient != null ? patient.getName() : null,
                request.getPhoneNumber(),
                request.getLocation(),
                driver != null ? driver.getId() : null,
                driver != null ? driver.getName() : null,
                request.getStatus(),
                request.getEtaMinutes(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private String normalizeStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "PENDING", "DRIVER_ASSIGNED", "COMPLETED", "CANCELLED" -> normalized;
            default -> fallback;
        };
    }

    private String resolvePhoneNumber(String phoneNumber, User patient) {
        String trimmed = trimToNull(phoneNumber);
        return trimmed != null ? trimmed : patient.getPhone();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User resolvePatient(EmergencyRequestCreateRequest body) {
        if (body.patientId() != null) {
            Optional<User> patientById = userRepository.findById(body.patientId());
            if (patientById.isPresent()) {
                return patientById.get();
            }
        }

        String lookup = trimToNull(body.patientLookup());
        if (lookup == null) {
            return null;
        }

        if (lookup.contains("@")) {
            return userRepository.findByEmail(lookup.toLowerCase(Locale.ROOT)).orElse(null);
        }

        return userRepository.findByPhone(lookup).orElse(null);
    }

    private boolean isDriver(User user) {
        return user != null
                && user.getRole() != null
                && "DRIVER".equals(user.getRole().trim().toUpperCase(Locale.ROOT));
    }

    private boolean isPatient(User user) {
        if (user == null) {
            return false;
        }

        String role = user.getRole();
        if (role == null) {
            return true;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return !normalizedRole.equals("HOSPITAL")
                && !normalizedRole.equals("PHARMACY");
    }

    public record EmergencyRequestCreateRequest(
            Long patientId,
            String patientLookup,
            Long driverId,
            String status,
            String location,
            String phoneNumber,
            Integer etaMinutes
    ) {}

    public record EmergencyRequestUpdateRequest(
            Long driverId,
            String status,
            String location,
            String phoneNumber,
            Integer etaMinutes
    ) {}

    public record EmergencyDriverAssignRequest(Long driverId, Integer etaMinutes) {}

    public record EmergencyStatusUpdateRequest(String status) {}

    public record EmergencyRequestResponse(
            Long requestId,
            Long patientId,
            String patientName,
            String phoneNumber,
            String location,
            Long driverId,
            String driverName,
            String status,
            Integer etaMinutes,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
