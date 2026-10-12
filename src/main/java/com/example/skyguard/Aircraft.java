package com.example.skyguard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "aircraft")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    public String registration;

    @NotBlank
    public String model;

    public String manufacturer = "Boeing";

    public Integer capacity = 180;

    public String location = "JFK";

    public String status = "AVAILABLE"; // AVAILABLE, IN_FLIGHT, MAINTENANCE, GROUNDED

    public LocalDate nextMaintenance;

    public LocalDate lastMaintenance;

    public Integer totalFlightHours = 1250;

    public Aircraft() {}

    public Aircraft(String registration, String model, String manufacturer, Integer capacity, String location, String status, LocalDate nextMaintenance) {
        this.registration = registration;
        this.model = model;
        this.manufacturer = manufacturer;
        this.capacity = capacity;
        this.location = location;
        this.status = status;
        this.nextMaintenance = nextMaintenance;
        this.lastMaintenance = LocalDate.now().minusMonths(2);
        this.totalFlightHours = 1200;
    }
}
