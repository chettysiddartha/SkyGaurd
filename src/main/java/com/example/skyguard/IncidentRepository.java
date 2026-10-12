package com.example.skyguard;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(String status);
    List<Incident> findByCategory(String category);
}
