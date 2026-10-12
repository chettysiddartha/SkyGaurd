package com.example.skyguard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(nullable = false)
    public String aircraftRegistration;

    @NotBlank
    public String description;

    public String maintenanceType = "ROUTINE"; // ROUTINE, INSPECTION, ENGINE_OVERHAUL, AVIONICS, EMERGENCY

    public String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    public String status = "OPEN"; // OPEN, IN_PROGRESS, COMPLETED, CANCELLED

    public LocalDate scheduledDate;

    public LocalDate dueDate;

    public LocalDate completedDate;

    public Double estimatedCost = 4500.0;

    public String technicianNotes;

    public MaintenanceRecord() {}

    public MaintenanceRecord(String aircraftRegistration, String description, String maintenanceType, String priority, String status, LocalDate scheduledDate, LocalDate dueDate) {
        this.aircraftRegistration = aircraftRegistration;
        this.description = description;
        this.maintenanceType = maintenanceType;
        this.priority = priority;
        this.status = status;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
    }
}
