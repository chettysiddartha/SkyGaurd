package com.example.skyguard;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByAircraftRegistration(String aircraftRegistration);
    List<MaintenanceRecord> findByStatus(String status);
}
