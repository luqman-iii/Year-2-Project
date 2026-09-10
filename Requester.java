package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.IncomingPatient;
import com.startup1.startup1_backend.entity.User;
import com.startup1.startup1_backend.repository.DriverProfileRepository;
import com.startup1.startup1_backend.repository.IncomingPatientRepository;
import com.startup1.startup1_backend.repository.MotherProfileRepository;
import com.startup1.startup1_backend.repository.TreatmentNoteRepository;
import com.startup1.startup1_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final MotherProfileRepository motherProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final IncomingPatientRepository incomingPatientRepository;
    private final TreatmentNoteRepository treatmentNoteRepository;

    public UserController(UserRepository userRepository,
                          MotherProfileRepository motherProfileRepository,
                          DriverProfileRepository driverProfileRepository,
                          IncomingPatientRepository incomingPatientRepository,
                          TreatmentNoteRepository treatmentNoteRepository) {
        this.userRepository = userRepository;
        this.motherProfileRepository = motherProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.incomingPatientRepository = incomingPatientRepository;
        this.treatmentNoteRepository = treatmentNoteRepository;
    }

    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User updated) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setRole(updated.getRole());
        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddress(updated.getAddress());

        return userRepository.save(existing);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

    motherProfileRepository.deleteByUser_Id(id);
    driverProfileRepository.deleteByUser_Id(id);

    List<IncomingPatient> cases = incomingPatientRepository.findByPatient_Id(id);
    for (IncomingPatient c : cases) {
        treatmentNoteRepository.deleteByIncomingPatient_Id(c.getId());
        incomingPatientRepository.deleteById(c.getId());
    }

    userRepository.deleteById(id);
    }
}