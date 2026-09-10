package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.DriverProfile;
import com.startup1.startup1_backend.entity.EmergencyRequest;
import com.startup1.startup1_backend.entity.IncomingPatient;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.DriverProfileRepository;
import com.startup1.startup1_backend.repository.EmergencyRequestRepository;
import com.startup1.startup1_backend.repository.IncomingPatientRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/driver", "/drivers"})
@CrossOrigin(origins = "*")
public class LegacyDriverController {
    private static final int DEMO_HOSPITAL_TRAVEL_SECONDS = 30;

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final IncomingPatientRepository incomingPatientRepository;

    public LegacyDriverController(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            IncomingPatientRepository incomingPatientRepository
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.incomingPatientRepository = incomingPatientRepository;
    }

    @GetMapping
    public List<DriverSummary> getAllDrivers() {
        return userRepository.findAll().stream()
                .filter(this::isDriver)
                .map(this::toDriverSummary)
                .toList();
    }

    @GetMapping("/requests")
    public List<EmergencyRequestResponse> getPendingRequests() {
        return emergencyRequestRepository.findByStatusIgnoreCase("PENDING").stream()
                .map(this::toEmergencyResponse)
                .toList();
    }

    @PutMapping("/requests/{id}/accept")
    public ResponseEntity<EmergencyRequestResponse> acceptRequest(
            @PathVariable Long id,
            @RequestBody(required = false) DriverActionRequest body
    ) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        User driver = resolveDriver(body, request);
        if (driver == null) {
            return ResponseEntity.badRequest().build();
        }

        request.setDriver(driver);
        request.setStatus("DRIVER_ASSIGNED");
        if (body != null && body.etaMinutes() != null) {
            request.setEtaMinutes(body.etaMinutes());
        } else if (request.getEtaMinutes() == null) {
            request.setEtaMinutes(1);
        }

        EmergencyRequest savedRequest = emergencyRequestRepository.save(request);
        syncIncomingPatientForHospital(savedRequest);
        return ResponseEntity.ok(toEmergencyResponse(savedRequest));
    }

    @PutMapping("/requests/{id}/decline")
    public ResponseEntity<EmergencyRequestResponse> declineRequest(@PathVariable Long id) {
        EmergencyRequest request = emergencyRequestRepository.findById(id).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        request.setStatus("PENDING");
        EmergencyRequest savedRequest = emergencyRequestRepository.save(request);
        cancelIncomingPatientForHospital(request);
        return ResponseEntity.ok(toEmergencyResponse(savedRequest));
    }

    private void syncIncomingPatientForHospital(EmergencyRequest request) {
        if (request.getId() == null || request.getPatient() == null || request.getPatient().getId() == null) {
            return;
        }

        int pickupEtaMinutes = request.getEtaMinutes() != null && request.getEtaMinutes() > 0
                ? request.getEtaMinutes()
                : 1;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime visibleAt = now.plusMinutes(pickupEtaMinutes);
        LocalDateTime arrivalAt = visibleAt.plusSeconds(DEMO_HOSPITAL_TRAVEL_SECONDS);

        IncomingPatient incomingPatient = incomingPatientRepository.findByEmergencyRequestId(request.getId());
        if (incomingPatient == null) {
            incomingPatient = new IncomingPatient();
        }

        incomingPatient.setPatient(request.getPatient());
        incomingPatient.setEmergencyRequestId(request.getId());
        incomingPatient.setStatus("ON_THE_WAY");
        incomingPatient.setEtaMinutes(0);
        incomingPatient.setCreatedAt(now);
        incomingPatient.setHospitalVisibleAt(visibleAt);
        incomingPatient.setHospitalArrivalAt(arrivalAt);
        incomingPatientRepository.save(incomingPatient);
    }

    private void cancelIncomingPatientForHospital(EmergencyRequest request) {
        if (request.getId() == null || request.getPatient() == null || request.getPatient().getId() == null) {
            return;
        }

        IncomingPatient incomingPatient = incomingPatientRepository.findByEmergencyRequestId(request.getId());
        if (incomingPatient == null) {
            incomingPatient = incomingPatientRepository.findByPatient_IdOrderByCreatedAtDesc(request.getPatient().getId())
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        if (incomingPatient != null) {
            incomingPatient.setStatus("CANCELLED");
            incomingPatient.setEtaMinutes(null);
            incomingPatient.setHospitalVisibleAt(null);
            incomingPatient.setHospitalArrivalAt(null);
            incomingPatientRepository.save(incomingPatient);
        }
    }

    private User resolveDriver(DriverActionRequest body, EmergencyRequest request) {
        if (body != null && body.driverId() != null) {
            return userRepository.findById(body.driverId()).filter(this::isDriver).orElse(null);
        }

        if (request.getDriver() != null && isDriver(request.getDriver())) {
            return request.getDriver();
        }

        if (body != null && body.name() != null) {
            String expectedName = body.name().trim();
            return userRepository.findAll().stream()
                    .filter(this::isDriver)
                    .filter(user -> expectedName.equalsIgnoreCase(user.getName()))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private boolean isDriver(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return "DRIVER".equals(user.getRole().trim().toUpperCase(Locale.ROOT));
    }

    private DriverSummary toDriverSummary(User user) {
        DriverProfile profile = driverProfileRepository.findByUser_Id(user.getId());
        return new DriverSummary(
                user.getId(),
                user.getName(),
                user.getPhone(),
                profile != null ? profile.getVehiclePlate() : null,
                profile != null ? profile.getVehicleType() : null,
                profile != null ? profile.getArea() : null
        );
    }

    private EmergencyRequestResponse toEmergencyResponse(EmergencyRequest request) {
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

    public record DriverSummary(
            Long driverId,
            String fullName,
            String phoneNumber,
            String licensePlate,
            String vehicleType,
            String area
    ) {}

    public record DriverActionRequest(Long driverId, String name, Integer etaMinutes) {}

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
