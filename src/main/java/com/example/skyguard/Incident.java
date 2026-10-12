package com.example.skyguard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String incidentNumber;

    @NotBlank
    public String category; // BIRD_STRIKE, WEATHER, MECHANICAL, SYSTEM_FAILURE, SECURITY, RUNWAY_INCURSION, MEDICAL, OTHER

    @NotBlank
    @Column(length = 3000)
    public String description;

    public String location;

    public String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    public String status = "OPEN"; // OPEN, INVESTIGATING, RESOLVED, CLOSED

    public String reportedBy = "Flight Operations";

    public LocalDateTime reportedAt = LocalDateTime.now();

    public LocalDateTime resolvedAt;

    @Column(length = 3000)
    public String investigationNotes;

    public Incident() {}

    public Incident(String incidentNumber, String category, String description, String location, String priority, String status, String reportedBy) {
        this.incidentNumber = incidentNumber;
        this.category = category;
        this.description = description;
        this.location = location;
        this.priority = priority;
        this.status = status;
        this.reportedBy = reportedBy;
        this.reportedAt = LocalDateTime.now();
    }
}
