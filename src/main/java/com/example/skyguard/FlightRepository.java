package com.example.skyguard;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightNumber(String flightNumber);
    List<Flight> findByAircraftRegistration(String aircraftRegistration);
    List<Flight> findByStatus(String status);
}
