package com.startup1.startup1_backend.repository;

import com.startup1.startup1_backend.entity.TreatmentNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentNoteRepository extends JpaRepository<TreatmentNote, Long> {
    List<TreatmentNote> findByIncomingPatient_Id(Long incomingPatientId);
    List<TreatmentNote> findByIncomingPatient_Patient_Id(Long patientUserId);
    void deleteByIncomingPatient_Id(Long incomingPatientId);
}
