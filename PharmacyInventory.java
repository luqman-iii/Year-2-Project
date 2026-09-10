package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.IncomingPatient;
import com.startup1.startup1_backend.entity.TreatmentNote;
import com.startup1.startup1_backend.repository.IncomingPatientRepository;
import com.startup1.startup1_backend.repository.TreatmentNoteRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


import java.util.List;

@RestController
@RequestMapping("/treatment-notes")
@CrossOrigin(origins = "*")
public class TreatmentNoteController {

    private final TreatmentNoteRepository treatmentNoteRepository;
    private final IncomingPatientRepository incomingPatientRepository;

    public TreatmentNoteController(TreatmentNoteRepository treatmentNoteRepository,
                                   IncomingPatientRepository incomingPatientRepository) {
        this.treatmentNoteRepository = treatmentNoteRepository;
        this.incomingPatientRepository = incomingPatientRepository;
    }

    @GetMapping("/incoming-patient/{incomingPatientId}")
    public List<TreatmentNote> getNotesForIncomingPatient(@PathVariable Long incomingPatientId) {
        return treatmentNoteRepository.findByIncomingPatient_Id(incomingPatientId);
    }

    @PostMapping("/incoming-patient/{incomingPatientId}")
    public TreatmentNote addNote(@PathVariable Long incomingPatientId, @RequestBody TreatmentNote note) {
        IncomingPatient incomingPatient = incomingPatientRepository.findById(incomingPatientId).orElse(null);
        if (incomingPatient == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incoming patient was not found.");
        }

        String noteText = note != null ? note.getNoteText() : null;
        if (noteText == null || noteText.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visit summary cannot be empty.");
        }

        note.setNoteText(noteText.trim());
        note.setIncomingPatient(incomingPatient);
        return treatmentNoteRepository.save(note);
    }

    @DeleteMapping("/{id}")
    public void deleteTreatmentNote(@PathVariable Long id) {
        treatmentNoteRepository.deleteById(id);
    }

    @Transactional
    @DeleteMapping("/incoming-patient/{incomingPatientId}")
    public void deleteNotesForIncomingPatient(@PathVariable Long incomingPatientId) {
        treatmentNoteRepository.deleteByIncomingPatient_Id(incomingPatientId);
    }
}
