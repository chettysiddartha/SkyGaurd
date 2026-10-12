package com.example.skyguard;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final AircraftRepository aircraftRepo;
    private final FlightRepository flightRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final IncidentRepository incidentRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepo,
                           AircraftRepository aircraftRepo,
                           FlightRepository flightRepo,
                           MaintenanceRepository maintenanceRepo,
                           IncidentRepository incidentRepo,
                           PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.aircraftRepo = aircraftRepo;
        this.flightRepo = flightRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.incidentRepo = incidentRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUsers();
        initAircraft();
        initFlights();
        initMaintenance();
        initIncidents();
    }

    private void initUsers() {
        if (userRepo.count() == 0) {
            // Admin user
            userRepo.save(new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "Chief Operations Commander",
                    "admin@skyguard.aero",
                    "ROLE_ADMIN"
            ));

            // Operations Staff user
            userRepo.save(new User(
                    "staff",
                    passwordEncoder.encode("staff123"),
                    "Flight Dispatcher Smith",
                    "dispatch@skyguard.aero",
                    "ROLE_STAFF"
            ));

            // Viewer / Pilot user
            userRepo.save(new User(
                    "pilot",
                    passwordEncoder.encode("pilot123"),
                    "Captain Sarah Connor",
                    "pilot@skyguard.aero",
                    "ROLE_VIEWER"
            ));
        }
    }

    private void initAircraft() {
        if (aircraftRepo.count() == 0) {
            Aircraft a1 = new Aircraft("N737AA", "Boeing 737-800", "Boeing", 189, "JFK", "AVAILABLE", LocalDate.now().plusMonths(3));
            a1.totalFlightHours = 2450;
            a1.lastMaintenance = LocalDate.now().minusMonths(3);

            Aircraft a2 = new Aircraft("N320DL", "Airbus A320neo", "Airbus", 180, "SFO", "IN_FLIGHT", LocalDate.now().plusMonths(5));
            a2.totalFlightHours = 1180;
            a2.lastMaintenance = LocalDate.now().minusMonths(1);

            Aircraft a3 = new Aircraft("N787UA", "Boeing 787-9 Dreamliner", "Boeing", 290, "LAX", "AVAILABLE", LocalDate.now().plusDays(4));
            a3.totalFlightHours = 3400;
            a3.lastMaintenance = LocalDate.now().minusMonths(5);

            Aircraft a4 = new Aircraft("N220SW", "Airbus A220-300", "Airbus", 140, "ORD", "MAINTENANCE", LocalDate.now().plusMonths(6));
            a4.totalFlightHours = 890;
            a4.lastMaintenance = LocalDate.now().minusDays(2);

            Aircraft a5 = new Aircraft("N900BA", "Bombardier CRJ-900", "Bombardier", 76, "ATL", "AVAILABLE", LocalDate.now().plusMonths(4));
            a5.totalFlightHours = 1950;
            a5.lastMaintenance = LocalDate.now().minusMonths(2);

            Aircraft a6 = new Aircraft("N350AF", "Airbus A350-900", "Airbus", 325, "LHR", "GROUNDED", LocalDate.now().minusDays(2));
            a6.totalFlightHours = 4120;
            a6.lastMaintenance = LocalDate.now().minusMonths(7);

            aircraftRepo.save(a1);
            aircraftRepo.save(a2);
            aircraftRepo.save(a3);
            aircraftRepo.save(a4);
            aircraftRepo.save(a5);
            aircraftRepo.save(a6);
        }
    }

    private void initFlights() {
        if (flightRepo.count() == 0) {
            flightRepo.save(new Flight(
                    "SG-101",
                    "JFK",
                    "LHR",
                    "N737AA",
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(9),
                    "BOARDING",
                    "B14",
                    "T4",
                    178
            ));

            flightRepo.save(new Flight(
                    "SG-204",
                    "SFO",
                    "HND",
                    "N320DL",
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusHours(8),
                    "IN_FLIGHT",
                    "A02",
                    "T1",
                    164
            ));

            flightRepo.save(new Flight(
                    "SG-315",
                    "LAX",
                    "ORD",
                    "N787UA",
                    LocalDateTime.now().plusHours(4),
                    LocalDateTime.now().plusHours(8),
                    "SCHEDULED",
                    "C22",
                    "T2",
                    245
            ));

            flightRepo.save(new Flight(
                    "SG-408",
                    "DXB",
                    "SIN",
                    "N900BA",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(8),
                    "DELAYED",
                    "D08",
                    "T3",
                    72
            ));

            flightRepo.save(new Flight(
                    "SG-512",
                    "FRA",
                    "CDG",
                    "N737AA",
                    LocalDateTime.now().minusHours(4),
                    LocalDateTime.now().minusHours(2),
                    "LANDED",
                    "E05",
                    "T1",
                    152
            ));
        }
    }

    private void initMaintenance() {
        if (maintenanceRepo.count() == 0) {
            MaintenanceRecord m1 = new MaintenanceRecord(
                    "N220SW",
                    "Turbine Fan Blade Ultrasonic Inspection & Lubrication",
                    "INSPECTION",
                    "HIGH",
                    "IN_PROGRESS",
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(2)
            );
            m1.estimatedCost = 8500.0;
            m1.technicianNotes = "Borescope inspection completed. Awaiting replacement bearing seal.";

            MaintenanceRecord m2 = new MaintenanceRecord(
                    "N350AF",
                    "Weather Radar Transceiver Calibration & Firmware Update",
                    "AVIONICS",
                    "CRITICAL",
                    "OPEN",
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(1) // Overdue!
            );
            m2.estimatedCost = 12000.0;
            m2.technicianNotes = "Intermittent radar signal loss reported by captain during descent.";

            MaintenanceRecord m3 = new MaintenanceRecord(
                    "N787UA",
                    "Scheduled 400-Hour Phase-A Airframe Routine Service",
                    "ROUTINE",
                    "MEDIUM",
                    "OPEN",
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(4)
            );
            m3.estimatedCost = 3500.0;
            m3.technicianNotes = "Standard oil filter change, tire tread depth gauge measurement.";

            maintenanceRepo.save(m1);
            maintenanceRepo.save(m2);
            maintenanceRepo.save(m3);
        }
    }

    private void initIncidents() {
        if (incidentRepo.count() == 0) {
            Incident i1 = new Incident(
                    "INC-2026-0104",
                    "BIRD_STRIKE",
                    "Flock of geese struck right wing leading edge on initial climb out of runway 31L.",
                    "JFK Airport",
                    "HIGH",
                    "INVESTIGATING",
                    "Tower ATC / Pilot Report"
            );
            i1.investigationNotes = "Visual inspection revealed minor denting on slat #3. Structural NDT technician dispatched.";

            Incident i2 = new Incident(
                    "INC-2026-0098",
                    "WEATHER",
                    "Severe unforecast clear-air turbulence encountered at FL340 over Rocky Mountains.",
                    "Waypoint DEN-08",
                    "MEDIUM",
                    "RESOLVED",
                    "Captain Evans"
            );
            i2.resolvedAt = LocalDateTime.now().minusDays(1);
            i2.investigationNotes = "Seatbelt sign was illuminated. No passenger or cabin crew injuries reported. G-meter within limits.";

            incidentRepo.save(i1);
            incidentRepo.save(i2);
        }
    }
}
