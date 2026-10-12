package com.example.skyguard;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(nullable = false)
    public String flightNumber;

    public String origin;

    public String destination;

    public String aircraftRegistration;

    public LocalDateTime departureTime;

    public LocalDateTime arrivalTime;

    public String status = "SCHEDULED"; // SCHEDULED, BOARDING, DEPARTED, IN_FLIGHT, LANDED, DELAYED, CANCELLED

    public String gate = "B12";

    public String terminal = "T4";

    public Integer passengerCount = 145;

    public Flight() {}

    public Flight(String flightNumber, String origin, String destination, String aircraftRegistration, LocalDateTime departureTime, LocalDateTime arrivalTime, String status, String gate, String terminal, Integer passengerCount) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.aircraftRegistration = aircraftRegistration;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.status = status;
        this.gate = gate;
        this.terminal = terminal;
        this.passengerCount = passengerCount;
    }
}
