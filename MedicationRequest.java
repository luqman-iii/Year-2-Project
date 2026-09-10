package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.Medication;
import com.startup1.startup1_backend.entity.MedicationRequest;
import com.startup1.startup1_backend.entity.Pharmacy;
import com.startup1.startup1_backend.entity.Requester;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.MedicationRepository;
import com.startup1.startup1_backend.repository.MedicationRequestRepository;
import com.startup1.startup1_backend.repository.PharmacyRepository;
import com.startup1.startup1_backend.repository.RequesterRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class MedicationRequestController {

    private final MedicationRequestRepository medicationRequestRepository;
    private final RequesterRepository requesterRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;

    public MedicationRequestController(
            MedicationRequestRepository medicationRequestRepository,
            RequesterRepository requesterRepository,
            PharmacyRepository pharmacyRepository,
            MedicationRepository medicationRepository,
            UserRepository userRepository
    ) {
        this.medicationRequestRepository = medicationRequestRepository;
        this.requesterRepository = requesterRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.medicationRepository = medicationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<MedicationRequest> getAllRequests() {
        return medicationRequestRepository.findAll();
    }

    @GetMapping("/pending")
    public List<MedicationRequest> getPendingRequests() {
        return medicationRequestRepository.findByRequestStatus("PENDING");
    }

    @GetMapping("/mother/{userId}/accepted")
    public List<MedicationRequest> getAcceptedRequestsForMother(@PathVariable Long userId) {
        return medicationRequestRepository.findByRequesterUser_IdAndRequestStatusOrderByRequestDateDesc(userId, "ACCEPTED");
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationRequest> getRequestById(@PathVariable Long id) {
        return medicationRequestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody MedicationRequestCreateRequest body) {
        Optional<Requester> requesterOpt = resolveRequester(body);
        Optional<Pharmacy> pharmacyOpt = pharmacyRepository.findById(body.getPharmacyId());
        Optional<Medication> medicationOpt = medicationRepository.findById(body.getMedicationId());

        if (requesterOpt.isEmpty() || pharmacyOpt.isEmpty() || medicationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid requester, pharmacy, or medication details");
        }

        MedicationRequest request = new MedicationRequest();
        request.setRequester(requesterOpt.get());
        request.setRequesterUser(resolveRequesterUser(body).orElse(null));
        request.setPharmacy(pharmacyOpt.get());
        request.setMedication(medicationOpt.get());
        request.setRequestDate(body.getRequestDate() != null ? body.getRequestDate() : LocalDate.now());
        request.setRequestStatus("PENDING");
        request.setNotes(body.getNotes());

        MedicationRequest saved = medicationRequestRepository.save(request);
        return ResponseEntity.ok(saved);
    }

    private Optional<Requester> resolveRequester(MedicationRequestCreateRequest body) {
        if (body.getRequesterId() != null) {
            return requesterRepository.findById(body.getRequesterId());
        }

        if (isBlank(body.getRequesterName())) {
            return Optional.empty();
        }

        Requester requester = new Requester();
        requester.setFullName(body.getRequesterName().trim());
        requester.setPhone(trimToNull(body.getRequesterPhone()));
        requester.setEmail(trimToNull(body.getRequesterEmail()));
        return Optional.of(requesterRepository.save(requester));
    }

    private Optional<User> resolveRequesterUser(MedicationRequestCreateRequest body) {
        if (body.getRequesterUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(body.getRequesterUserId());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable Long id) {
        Optional<MedicationRequest> requestOpt = medicationRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MedicationRequest request = requestOpt.get();
        request.setRequestStatus("ACCEPTED");
        medicationRequestRepository.save(request);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/decline")
    public ResponseEntity<?> declineRequest(@PathVariable Long id) {
        Optional<MedicationRequest> requestOpt = medicationRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MedicationRequest request = requestOpt.get();
        request.setRequestStatus("DECLINED");
        medicationRequestRepository.save(request);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/{id}/picked-up")
    public ResponseEntity<?> markRequestPickedUp(@PathVariable Long id, @RequestBody MedicationRequestMotherActionRequest body) {
        return updateMotherRequestStatus(id, body, "PICKED_UP");
    }

    @PutMapping("/{id}/not-going")
    public ResponseEntity<?> markRequestNotGoing(@PathVariable Long id, @RequestBody MedicationRequestMotherActionRequest body) {
        return updateMotherRequestStatus(id, body, "NOT_GOING");
    }

    private ResponseEntity<?> updateMotherRequestStatus(
            Long id,
            MedicationRequestMotherActionRequest body,
            String nextStatus
    ) {
        Optional<MedicationRequest> requestOpt = medicationRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MedicationRequest request = requestOpt.get();
        if (!"ACCEPTED".equalsIgnoreCase(request.getRequestStatus())) {
            return ResponseEntity.badRequest().body("Only accepted requests can be updated by the requester.");
        }

        Long requesterUserId = request.getRequesterUser() != null ? request.getRequesterUser().getId() : null;
        if (requesterUserId == null || body == null || body.getRequesterUserId() == null
                || !requesterUserId.equals(body.getRequesterUserId())) {
            return ResponseEntity.badRequest().body("This request does not belong to the supplied user.");
        }

        request.setRequestStatus(nextStatus);
        medicationRequestRepository.save(request);
        return ResponseEntity.ok(request);
    }

    public static class MedicationRequestCreateRequest {
        private Long requesterId;
        private Long requesterUserId;
        private Long pharmacyId;
        private Long medicationId;
        private LocalDate requestDate;
        private String notes;
        private String requesterName;
        private String requesterPhone;
        private String requesterEmail;

        public Long getRequesterId() {
            return requesterId;
        }

        public void setRequesterId(Long requesterId) {
            this.requesterId = requesterId;
        }

        public Long getRequesterUserId() {
            return requesterUserId;
        }

        public void setRequesterUserId(Long requesterUserId) {
            this.requesterUserId = requesterUserId;
        }

        public Long getPharmacyId() {
            return pharmacyId;
        }

        public void setPharmacyId(Long pharmacyId) {
            this.pharmacyId = pharmacyId;
        }

        public Long getMedicationId() {
            return medicationId;
        }

        public void setMedicationId(Long medicationId) {
            this.medicationId = medicationId;
        }

        public LocalDate getRequestDate() {
            return requestDate;
        }

        public void setRequestDate(LocalDate requestDate) {
            this.requestDate = requestDate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getRequesterName() {
            return requesterName;
        }

        public void setRequesterName(String requesterName) {
            this.requesterName = requesterName;
        }

        public String getRequesterPhone() {
            return requesterPhone;
        }

        public void setRequesterPhone(String requesterPhone) {
            this.requesterPhone = requesterPhone;
        }

        public String getRequesterEmail() {
            return requesterEmail;
        }

        public void setRequesterEmail(String requesterEmail) {
            this.requesterEmail = requesterEmail;
        }
    }

    public static class MedicationRequestMotherActionRequest {
        private Long requesterUserId;

        public Long getRequesterUserId() {
            return requesterUserId;
        }

        public void setRequesterUserId(Long requesterUserId) {
            this.requesterUserId = requesterUserId;
        }
    }
}
