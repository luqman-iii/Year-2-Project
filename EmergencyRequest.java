package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.EmergencyRequest;
import com.startup1.startup1_backend.entity.IncomingPatient;
import com.startup1.startup1_backend.entity.MotherProfile;
import com.startup1.startup1_backend.repository.EmergencyRequestRepository;
import com.startup1.startup1_backend.repository.IncomingPatientRepository;
import com.startup1.startup1_backend.repository.MotherProfileRepository;
import com.startup1.startup1_backend.repository.TreatmentNoteRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/incoming-patients")
@CrossOrigin(origins = "*")
public class IncomingPatientController {
    private static final int DEMO_HOSPITAL_TRAVEL_SECONDS = 30;

    private final IncomingPatientRepository incomingPatientRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final TreatmentNoteRepository treatmentNoteRepository;
    private final MotherProfileRepository motherProfileRepository;

    public IncomingPatientController(IncomingPatientRepository incomingPatientRepository,
                                     EmergencyRequestRepository emergencyRequestRepository,
                                     TreatmentNoteRepository treatmentNoteRepository,
                                     MotherProfileRepository motherProfileRepository) {
        this.incomingPatientRepository = incomingPatientRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.treatmentNoteRepository = treatmentNoteRepository;
        this.motherProfileRepository = motherProfileRepository;
    }

    @GetMapping
    public List<IncomingPatient> getIncomingPatients(@RequestParam(required = false) String status) {
        syncDriverAssignedRequests();

        List<IncomingPatient> patients;
        if (status == null || status.equalsIgnoreCase("ALL")) {
            patients = incomingPatientRepository.findAll();
        } else {
            patients = incomingPatientRepository.findByStatus(status);
        }
        return patients.stream()
                .filter(this::isEligiblePatient)
                .toList();
    }

    @GetMapping("/{id}")
    public IncomingPatient getIncomingPatient(@PathVariable Long id) {
        IncomingPatient patient = incomingPatientRepository.findById(id).orElse(null);
        if (!isEligiblePatient(patient)) {
            return null;
        }
        return patient;
    }

    @GetMapping("/{id}/details")
    public IncomingPatientDetailsResponse getIncomingPatientDetails(@PathVariable Long id) {
        IncomingPatient incomingPatient = incomingPatientRepository.findById(id).orElse(null);
        if (!isEligiblePatient(incomingPatient)) {
            return null;
        }

        Long patientUserId = incomingPatient.getPatient().getId();
        MotherProfile motherProfile = motherProfileRepository.findByUser_Id(patientUserId);

        List<NoteHistoryItem> previousNotes = new ArrayList<>();
        incomingPatientRepository.findByPatient_Id(patientUserId).stream()
                .filter((entry) -> entry.getId() != null && !entry.getId().equals(incomingPatient.getId()))
                .forEach((entry) -> treatmentNoteRepository.findByIncomingPatient_Id(entry.getId()).forEach((note) -> {
                    previousNotes.add(new NoteHistoryItem(
                            note.getId(),
                            note.getNoteText(),
                            note.getCreatedAt()
                    ));
                }));

        previousNotes.sort(Comparator.comparing(
                NoteHistoryItem::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return new IncomingPatientDetailsResponse(
                incomingPatient.getId(),
                patientUserId,
                incomingPatient.getPatient().getName(),
                incomingPatient.getPatient().getPhone(),
                incomingPatient.getPatient().getEmail(),
                incomingPatient.getPatient().getAddress(),
                incomingPatient.getStatus(),
                incomingPatient.getEtaMinutes(),
                motherProfile != null ? motherProfile.getPregnancyStatus() : null,
                motherProfile != null ? motherProfile.getDueDate() : null,
                motherProfile != null ? motherProfile.getConditions() : null,
                motherProfile != null ? motherProfile.getMedication() : null,
                previousNotes
        );
    }

    @PostMapping
    public IncomingPatient createIncomingPatient(@RequestBody IncomingPatient incomingPatient) {
        if (!isEligiblePatient(incomingPatient)) {
            return null;
        }
        return incomingPatientRepository.save(incomingPatient);
    }

    @PutMapping("/{id}")
    public IncomingPatient updateIncomingPatient(@PathVariable Long id, @RequestBody IncomingPatient updated) {
        IncomingPatient existing = incomingPatientRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setStatus(updated.getStatus());
        existing.setEtaMinutes(updated.getEtaMinutes());

        return incomingPatientRepository.save(existing);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public void deleteIncomingPatient(@PathVariable Long id) {
        // delete notes first to avoid FK constraint issues
        treatmentNoteRepository.deleteByIncomingPatient_Id(id);
        incomingPatientRepository.deleteById(id);
    }

    private void syncDriverAssignedRequests() {
        emergencyRequestRepository.findByStatusIgnoreCase("DRIVER_ASSIGNED")
                .forEach(this::syncIncomingPatientFromEmergencyRequest);
    }

    private void syncIncomingPatientFromEmergencyRequest(EmergencyRequest request) {
        if (request == null || request.getId() == null || !isPatientRequest(request)) {
            return;
        }

        int pickupEtaMinutes = request.getEtaMinutes() != null && request.getEtaMinutes() > 0
                ? request.getEtaMinutes()
                : 1;

        LocalDateTime acceptedAt = request.getUpdatedAt() != null
                ? request.getUpdatedAt()
                : (request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now());
        LocalDateTime visibleAt = acceptedAt.plusMinutes(pickupEtaMinutes);
        LocalDateTime arrivalAt = visibleAt.plusSeconds(DEMO_HOSPITAL_TRAVEL_SECONDS);

        IncomingPatient incomingPatient = incomingPatientRepository.findByEmergencyRequestId(request.getId());
        if (incomingPatient == null) {
            incomingPatient = new IncomingPatient();
            incomingPatient.setPatient(request.getPatient());
            incomingPatient.setEmergencyRequestId(request.getId());
            incomingPatient.setStatus("ON_THE_WAY");
            incomingPatient.setEtaMinutes(0);
            incomingPatient.setCreatedAt(acceptedAt);
            incomingPatient.setHospitalVisibleAt(visibleAt);
            incomingPatient.setHospitalArrivalAt(arrivalAt);
            incomingPatientRepository.save(incomingPatient);
            return;
        }

        String currentStatus = normalizeStatus(incomingPatient.getStatus());
        if ("WAITING_FOR_NOTES".equals(currentStatus)
                || "ARRIVED".equals(currentStatus)
                || "CANCELLED".equals(currentStatus)) {
            return;
        }

        incomingPatient.setPatient(request.getPatient());
        incomingPatient.setEmergencyRequestId(request.getId());
        if (incomingPatient.getStatus() == null || incomingPatient.getStatus().isBlank()) {
            incomingPatient.setStatus("ON_THE_WAY");
        }
        if (incomingPatient.getEtaMinutes() == null) {
            incomingPatient.setEtaMinutes(0);
        }
        if (incomingPatient.getCreatedAt() == null) {
            incomingPatient.setCreatedAt(acceptedAt);
        }
        if (incomingPatient.getHospitalVisibleAt() == null) {
            incomingPatient.setHospitalVisibleAt(visibleAt);
        }
        if (incomingPatient.getHospitalArrivalAt() == null) {
            incomingPatient.setHospitalArrivalAt(arrivalAt);
        }
        incomingPatientRepository.save(incomingPatient);
    }

    private boolean isEligiblePatient(IncomingPatient incomingPatient) {
        if (incomingPatient == null || incomingPatient.getPatient() == null) {
            return false;
        }

        String role = incomingPatient.getPatient().getRole();
        if (role == null || role.isBlank()) {
            return true;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return !normalizedRole.equals("HOSPITAL")
                && !normalizedRole.equals("PHARMACY");
    }

    private boolean isPatientRequest(EmergencyRequest request) {
        if (request.getPatient() == null) {
            return false;
        }

        String role = request.getPatient().getRole();
        if (role == null || role.isBlank()) {
            return true;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return !normalizedRole.equals("HOSPITAL")
                && !normalizedRole.equals("PHARMACY");
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    public record IncomingPatientDetailsResponse(
            Long incomingPatientId,
            Long patientUserId,
            String patientName,
            String patientPhone,
            String patientEmail,
            String patientAddress,
            String currentStatus,
            Integer etaMinutes,
            String pregnancyStatus,
            LocalDate dueDate,
            String conditions,
            String medication,
            List<NoteHistoryItem> previousNotes
    ) {}

    public record NoteHistoryItem(
            Long id,
            String noteText,
            LocalDateTime createdAt
    ) {}
}
