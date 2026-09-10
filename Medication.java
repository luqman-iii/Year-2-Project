package com.startup1.startup1_backend.controller;

import com.startup1.startup1_backend.entity.PharmacyInventory;
import com.startup1.startup1_backend.repository.PharmacyInventoryRepository;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class MedSearchController {

    private final PharmacyInventoryRepository pharmacyInventoryRepository;

    public MedSearchController(PharmacyInventoryRepository pharmacyInventoryRepository) {
        this.pharmacyInventoryRepository = pharmacyInventoryRepository;
    }

    @GetMapping
    public List<PharmacyInventory> searchMedicine(
            @RequestParam String medicine,
            @RequestParam(required = false) String city
    ) {
        if (city != null && !city.isBlank()) {
            return pharmacyInventoryRepository.searchByMedicineAndCity(medicine, city);
        }
        return pharmacyInventoryRepository.searchByMedicineOnly(medicine);
    }
}
