package com.startup1.startup1_backend.repository;

import com.startup1.startup1_backend.entity.PharmacyInventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyInventoryRepository extends JpaRepository<PharmacyInventory, Long> {

    @Query("""
        SELECT pi
        FROM PharmacyInventory pi
        WHERE LOWER(pi.medication.medicineName) LIKE LOWER(CONCAT('%', :medicine, '%'))
          AND LOWER(pi.pharmacy.city) LIKE LOWER(CONCAT('%', :city, '%'))
          AND pi.quantityInStock > 0
        """)
    List<PharmacyInventory> searchByMedicineAndCity(
            @Param("medicine") String medicine,
            @Param("city") String city
    );

    @Query("""
        SELECT pi
        FROM PharmacyInventory pi
        WHERE LOWER(pi.medication.medicineName) LIKE LOWER(CONCAT('%', :medicine, '%'))
          AND pi.quantityInStock > 0
        """)
    List<PharmacyInventory> searchByMedicineOnly(@Param("medicine") String medicine);
}
