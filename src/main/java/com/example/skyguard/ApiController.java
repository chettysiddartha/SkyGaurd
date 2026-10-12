package com.example.skyguard;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final AircraftRepository aircraftRepo;
    private final FlightRepository flightRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final IncidentRepository incidentRepo;
    private final AiService aiService;

    public ApiController(AircraftRepository aircraftRepo,
                         FlightRepository flightRepo,
                         MaintenanceRepository maintenanceRepo,
                         IncidentRepository incidentRepo,
                         AiService aiService) {
        this.aircraftRepo = aircraftRepo;
        this.flightRepo = flightRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.incidentRepo = incidentRepo;
        this.aiService = aiService;
    }

    // ==========================================
    // AIRCRAFT ENDPOINTS
    // ==========================================

    @GetMapping("/aircraft")
    public List<Aircraft> getAllAircraft() {
        return aircraftRepo.findAll();
    }

    @GetMapping("/aircraft/{id}")
    public Aircraft getAircraftById(@PathVariable Long id) {
        return aircraftRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aircraft not found"));
    }

    @PostMapping("/aircraft")
    @ResponseStatus(HttpStatus.CREATED)
    public Aircraft addAircraft(@Valid @RequestBody Aircraft x) {
        if (x.registration != null && aircraftRepo.findByRegistration(x.registration.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration '" + x.registration + "' already exists");
        }
        if (x.status == null || x.status.isBlank()) {
            x.status = "AVAILABLE";
        }
        return aircraftRepo.save(x);
    }

    @PutMapping("/aircraft/{id}")
    public Aircraft updateAircraft(@PathVariable Long id, @Valid @RequestBody Aircraft x) {
        Aircraft old = aircraftRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aircraft not found"));

        if (!old.registration.equalsIgnoreCase(x.registration)) {
            Optional<Aircraft> dup = aircraftRepo.findByRegistration(x.registration);
            if (dup.isPresent() && !dup.get().id.equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration already belongs to another aircraft");
            }
        }

        old.registration = x.registration;
        old.model = x.model;
        if (x.manufacturer != null) old.manufacturer = x.manufacturer;
        if (x.capacity != null) old.capacity = x.capacity;
        old.location = x.location;
        old.status = x.status;
        old.nextMaintenance = x.nextMaintenance;
        if (x.lastMaintenance != null) old.lastMaintenance = x.lastMaintenance;
        if (x.totalFlightHours != null) old.totalFlightHours = x.totalFlightHours;

        return aircraftRepo.save(old);
    }

    @DeleteMapping("/aircraft/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAircraft(@PathVariable Long id) {
        Aircraft ac = aircraftRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aircraft not found"));

        // Check if there are active flights scheduled for this aircraft
        List<Flight> flights = flightRepo.findByAircraftRegistration(ac.registration);
        boolean hasActiveFlights = flights.stream()
                .anyMatch(f -> "SCHEDULED".equalsIgnoreCase(f.status) || "IN_FLIGHT".equalsIgnoreCase(f.status) || "BOARDING".equalsIgnoreCase(f.status));

        if (hasActiveFlights) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete aircraft: active or scheduled flights are assigned to " + ac.registration);
        }

        aircraftRepo.deleteById(id);
    }

    // ==========================================
    // FLIGHT ENDPOINTS
    // ==========================================

    @GetMapping("/flights")
    public List<Flight> getAllFlights() {
        return flightRepo.findAll();
    }

    @GetMapping("/flights/{id}")
    public Flight getFlightById(@PathVariable Long id) {
        return flightRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));
    }

    @PostMapping("/flights")
    @ResponseStatus(HttpStatus.CREATED)
    public Flight addFlight(@Valid @RequestBody Flight x) {
        if (x.aircraftRegistration != null && !x.aircraftRegistration.isBlank()) {
            Aircraft ac = aircraftRepo.findByRegistration(x.aircraftRegistration.trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aircraft " + x.aircraftRegistration + " not found"));

            if ("MAINTENANCE".equalsIgnoreCase(ac.status) || "GROUNDED".equalsIgnoreCase(ac.status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Aircraft " + ac.registration + " cannot be scheduled (Status: " + ac.status + ")");
            }

            if ("IN_FLIGHT".equalsIgnoreCase(x.status)) {
                ac.status = "IN_FLIGHT";
                aircraftRepo.save(ac);
            }
        }

        if (x.status == null || x.status.isBlank()) {
            x.status = "SCHEDULED";
        }

        return flightRepo.save(x);
    }

    @PutMapping("/flights/{id}")
    public Flight updateFlight(@PathVariable Long id, @Valid @RequestBody Flight x) {
        Flight old = flightRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));

        old.flightNumber = x.flightNumber;
        old.origin = x.origin;
        old.destination = x.destination;
        old.aircraftRegistration = x.aircraftRegistration;
        old.departureTime = x.departureTime;
        old.arrivalTime = x.arrivalTime;
        old.status = x.status;
        if (x.gate != null) old.gate = x.gate;
        if (x.terminal != null) old.terminal = x.terminal;
        if (x.passengerCount != null) old.passengerCount = x.passengerCount;

        // Synchronize aircraft status if flight completed
        if ("LANDED".equalsIgnoreCase(x.status) || "CANCELLED".equalsIgnoreCase(x.status)) {
            if (old.aircraftRegistration != null) {
                aircraftRepo.findByRegistration(old.aircraftRegistration).ifPresent(ac -> {
                    if ("IN_FLIGHT".equalsIgnoreCase(ac.status)) {
                        ac.status = "AVAILABLE";
                        ac.location = old.destination != null ? old.destination : ac.location;
                        aircraftRepo.save(ac);
                    }
                });
            }
        }

        return flightRepo.save(old);
    }

    @PatchMapping("/flights/{id}/status")
    public Flight updateFlightStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Flight x = flightRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));

        String newStatus = body.getOrDefault("status", "SCHEDULED");
        x.status = newStatus;

        if ("LANDED".equalsIgnoreCase(newStatus) || "CANCELLED".equalsIgnoreCase(newStatus)) {
            if (x.aircraftRegistration != null) {
                aircraftRepo.findByRegistration(x.aircraftRegistration).ifPresent(ac -> {
                    if ("IN_FLIGHT".equalsIgnoreCase(ac.status)) {
                        ac.status = "AVAILABLE";
                        if (x.destination != null) ac.location = x.destination;
                        aircraftRepo.save(ac);
                    }
                });
            }
        } else if ("IN_FLIGHT".equalsIgnoreCase(newStatus)) {
            if (x.aircraftRegistration != null) {
                aircraftRepo.findByRegistration(x.aircraftRegistration).ifPresent(ac -> {
                    ac.status = "IN_FLIGHT";
                    aircraftRepo.save(ac);
                });
            }
        }

        return flightRepo.save(x);
    }

    @DeleteMapping("/flights/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFlight(@PathVariable Long id) {
        flightRepo.deleteById(id);
    }

    // ==========================================
    // MAINTENANCE ENDPOINTS
    // ==========================================

    @GetMapping("/maintenance")
    public List<MaintenanceRecord> getAllMaintenance() {
        return maintenanceRepo.findAll();
    }

    @GetMapping("/maintenance/{id}")
    public MaintenanceRecord getMaintenanceById(@PathVariable Long id) {
        return maintenanceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found"));
    }

    @PostMapping("/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecord addMaintenance(@Valid @RequestBody MaintenanceRecord x) {
        if (x.status == null || x.status.isBlank()) {
            x.status = "OPEN";
        }

        // Put aircraft under maintenance if priority is HIGH or CRITICAL or status is IN_PROGRESS
        if (x.aircraftRegistration != null && !x.aircraftRegistration.isBlank()) {
            aircraftRepo.findByRegistration(x.aircraftRegistration.trim()).ifPresent(ac -> {
                if ("HIGH".equalsIgnoreCase(x.priority) || "CRITICAL".equalsIgnoreCase(x.priority) || "IN_PROGRESS".equalsIgnoreCase(x.status)) {
                    ac.status = "MAINTENANCE";
                    aircraftRepo.save(ac);
                }
            });
        }

        return maintenanceRepo.save(x);
    }

    @PutMapping("/maintenance/{id}")
    public MaintenanceRecord updateMaintenance(@PathVariable Long id, @Valid @RequestBody MaintenanceRecord x) {
        MaintenanceRecord old = maintenanceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found"));

        old.aircraftRegistration = x.aircraftRegistration;
        old.description = x.description;
        if (x.maintenanceType != null) old.maintenanceType = x.maintenanceType;
        old.priority = x.priority;
        old.status = x.status;
        old.scheduledDate = x.scheduledDate;
        old.dueDate = x.dueDate;
        if (x.completedDate != null) old.completedDate = x.completedDate;
        if (x.estimatedCost != null) old.estimatedCost = x.estimatedCost;
        if (x.technicianNotes != null) old.technicianNotes = x.technicianNotes;

        if ("COMPLETED".equalsIgnoreCase(old.status)) {
            if (old.completedDate == null) {
                old.completedDate = LocalDate.now();
            }
            checkAndRestoreAircraftStatus(old.aircraftRegistration);
        }

        return maintenanceRepo.save(old);
    }

    @PatchMapping("/maintenance/{id}/status")
    public MaintenanceRecord updateMaintenanceStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        MaintenanceRecord x = maintenanceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found"));

        String newStatus = body.getOrDefault("status", "OPEN");
        x.status = newStatus;

        if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            x.completedDate = LocalDate.now();
            checkAndRestoreAircraftStatus(x.aircraftRegistration);
        } else if ("IN_PROGRESS".equalsIgnoreCase(newStatus)) {
            aircraftRepo.findByRegistration(x.aircraftRegistration).ifPresent(ac -> {
                ac.status = "MAINTENANCE";
                aircraftRepo.save(ac);
            });
        }

        return maintenanceRepo.save(x);
    }

    @DeleteMapping("/maintenance/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaintenance(@PathVariable Long id) {
        maintenanceRepo.deleteById(id);
    }

    private void checkAndRestoreAircraftStatus(String registration) {
        if (registration == null) return;
        aircraftRepo.findByRegistration(registration).ifPresent(ac -> {
            // Check if there are other open maintenance records
            List<MaintenanceRecord> openRecords = maintenanceRepo.findByAircraftRegistration(registration);
            boolean hasPending = openRecords.stream()
                    .anyMatch(r -> !"COMPLETED".equalsIgnoreCase(r.status) && !"CANCELLED".equalsIgnoreCase(r.status));

            if (!hasPending) {
                ac.status = "AVAILABLE";
                ac.lastMaintenance = LocalDate.now();
                ac.nextMaintenance = LocalDate.now().plusMonths(6);
                aircraftRepo.save(ac);
            }
        });
    }

    // ==========================================
    // INCIDENT ENDPOINTS
    // ==========================================

    @GetMapping("/incidents")
    public List<Incident> getAllIncidents() {
        return incidentRepo.findAll();
    }

    @GetMapping("/incidents/{id}")
    public Incident getIncidentById(@PathVariable Long id) {
        return incidentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public Incident addIncident(@Valid @RequestBody Incident x) {
        if (x.incidentNumber == null || x.incidentNumber.isBlank()) {
            x.incidentNumber = "INC-" + LocalDate.now().getYear() + "-" + String.format("%04d", (int) (Math.random() * 9000 + 1000));
        }
        if (x.reportedAt == null) {
            x.reportedAt = LocalDateTime.now();
        }
        if (x.status == null || x.status.isBlank()) {
            x.status = "OPEN";
        }
        return incidentRepo.save(x);
    }

    @PutMapping("/incidents/{id}")
    public Incident updateIncident(@PathVariable Long id, @Valid @RequestBody Incident x) {
        Incident old = incidentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        old.category = x.category;
        old.description = x.description;
        old.location = x.location;
        old.priority = x.priority;
        old.status = x.status;
        if (x.investigationNotes != null) old.investigationNotes = x.investigationNotes;

        if ("RESOLVED".equalsIgnoreCase(x.status) || "CLOSED".equalsIgnoreCase(x.status)) {
            if (old.resolvedAt == null) {
                old.resolvedAt = LocalDateTime.now();
            }
        }

        return incidentRepo.save(old);
    }

    @PatchMapping("/incidents/{id}/status")
    public Incident updateIncidentStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Incident old = incidentRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));

        String newStatus = body.getOrDefault("status", "OPEN");
        old.status = newStatus;
        if ("RESOLVED".equalsIgnoreCase(newStatus) || "CLOSED".equalsIgnoreCase(newStatus)) {
            old.resolvedAt = LocalDateTime.now();
        }

        return incidentRepo.save(old);
    }

    @DeleteMapping("/incidents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncident(@PathVariable Long id) {
        incidentRepo.deleteById(id);
    }

    // ==========================================
    // ALERTS & OPERATIONAL INTELLIGENCE
    // ==========================================

    @GetMapping("/alerts")
    public List<Map<String, String>> alerts() {
        List<Map<String, String>> out = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Overdue maintenance checks (CRITICAL)
        for (MaintenanceRecord m : maintenanceRepo.findAll()) {
            if (m.dueDate != null && m.dueDate.isBefore(today) && !"COMPLETED".equalsIgnoreCase(m.status) && !"CANCELLED".equalsIgnoreCase(m.status)) {
                out.add(alert("OVERDUE MAINTENANCE: " + m.aircraftRegistration + " requires " + m.description + " (Due " + m.dueDate + ")", "CRITICAL", "MAINTENANCE", m.aircraftRegistration));
            }
        }

        // 2. Aircraft status alerts
        for (Aircraft x : aircraftRepo.findAll()) {
            if ("GROUNDED".equalsIgnoreCase(x.status)) {
                out.add(alert("AIRCRAFT GROUNDED: " + x.registration + " (" + x.model + ") is out of service at " + x.location, "CRITICAL", "AIRCRAFT", x.registration));
            } else if ("MAINTENANCE".equalsIgnoreCase(x.status)) {
                out.add(alert("AIRCRAFT IN SERVICE BAY: " + x.registration + " is currently undergoing maintenance", "HIGH", "AIRCRAFT", x.registration));
            }

            if (x.nextMaintenance != null && !x.nextMaintenance.isBefore(today) && !x.nextMaintenance.isAfter(today.plusDays(7))) {
                out.add(alert("SCHEDULED INSPECTION DUE: " + x.registration + " due for service within 7 days (" + x.nextMaintenance + ")", "MEDIUM", "AIRCRAFT", x.registration));
            }
        }

        // 3. Open high/critical incidents
        for (Incident inc : incidentRepo.findAll()) {
            if (!"RESOLVED".equalsIgnoreCase(inc.status) && !"CLOSED".equalsIgnoreCase(inc.status)) {
                if ("CRITICAL".equalsIgnoreCase(inc.priority)) {
                    out.add(alert("CRITICAL SAFETY INCIDENT: [" + inc.category + "] at " + inc.location + " - " + inc.description, "CRITICAL", "INCIDENT", inc.incidentNumber != null ? inc.incidentNumber : "INC-" + inc.id));
                } else if ("HIGH".equalsIgnoreCase(inc.priority)) {
                    out.add(alert("HIGH PRIORITY INCIDENT: [" + inc.category + "] at " + inc.location + " requires investigation", "HIGH", "INCIDENT", inc.incidentNumber != null ? inc.incidentNumber : "INC-" + inc.id));
                }
            }
        }

        // 4. Delayed or cancelled flights
        for (Flight fl : flightRepo.findAll()) {
            if ("DELAYED".equalsIgnoreCase(fl.status)) {
                out.add(alert("FLIGHT DELAY: Flight " + fl.flightNumber + " (" + fl.origin + " -> " + fl.destination + ") is experiencing operational delay", "HIGH", "FLIGHT", fl.flightNumber));
            } else if ("CANCELLED".equalsIgnoreCase(fl.status)) {
                out.add(alert("FLIGHT CANCELLED: Flight " + fl.flightNumber + " (" + fl.origin + " -> " + fl.destination + ") cancelled", "MEDIUM", "FLIGHT", fl.flightNumber));
            }
        }

        return out;
    }

    private Map<String, String> alert(String msg, String priority, String category, String ref) {
        Map<String, String> m = new HashMap<>();
        m.put("message", msg);
        m.put("priority", priority);
        m.put("category", category);
        m.put("reference", ref);
        m.put("timestamp", LocalDateTime.now().toString());
        return m;
    }

    // ==========================================
    // SUMMARY / DASHBOARD KPI METRICS
    // ==========================================

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<Aircraft> allAircraft = aircraftRepo.findAll();
        List<Flight> allFlights = flightRepo.findAll();
        List<MaintenanceRecord> allMaint = maintenanceRepo.findAll();
        List<Incident> allIncidents = incidentRepo.findAll();
        List<Map<String, String>> activeAlerts = alerts();

        long availableAc = allAircraft.stream().filter(a -> "AVAILABLE".equalsIgnoreCase(a.status)).count();
        long inFlightAc = allAircraft.stream().filter(a -> "IN_FLIGHT".equalsIgnoreCase(a.status)).count();
        long maintAc = allAircraft.stream().filter(a -> "MAINTENANCE".equalsIgnoreCase(a.status)).count();
        long groundedAc = allAircraft.stream().filter(a -> "GROUNDED".equalsIgnoreCase(a.status)).count();

        long activeFlights = allFlights.stream().filter(f -> "IN_FLIGHT".equalsIgnoreCase(f.status) || "BOARDING".equalsIgnoreCase(f.status)).count();
        long delayedFlights = allFlights.stream().filter(f -> "DELAYED".equalsIgnoreCase(f.status)).count();

        long openMaint = allMaint.stream().filter(m -> "OPEN".equalsIgnoreCase(m.status) || "IN_PROGRESS".equalsIgnoreCase(m.status)).count();
        long overdueMaint = allMaint.stream().filter(m -> m.dueDate != null && m.dueDate.isBefore(LocalDate.now()) && !"COMPLETED".equalsIgnoreCase(m.status)).count();

        long openInc = allIncidents.stream().filter(i -> !"RESOLVED".equalsIgnoreCase(i.status) && !"CLOSED".equalsIgnoreCase(i.status)).count();
        long criticalInc = allIncidents.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.priority) && !"RESOLVED".equalsIgnoreCase(i.status)).count();

        long criticalAlerts = activeAlerts.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.get("priority"))).count();

        String systemStatus = "OPTIMAL";
        if (criticalAlerts > 0 || groundedAc > 0 || criticalInc > 0) {
            systemStatus = "ALERT";
        } else if (delayedFlights > 0 || openMaint > 2 || activeAlerts.size() > 2) {
            systemStatus = "WARNING";
        }

        Map<String, Object> map = new HashMap<>();
        // Backwards compatibility keys
        map.put("aircraft", (long) allAircraft.size());
        map.put("flights", (long) allFlights.size());
        map.put("maintenance", (long) allMaint.size());
        map.put("incidents", (long) allIncidents.size());
        map.put("alerts", (long) activeAlerts.size());

        // Enhanced operational KPI keys
        map.put("availableAircraft", availableAc);
        map.put("inFlightAircraft", inFlightAc);
        map.put("maintenanceAircraft", maintAc);
        map.put("groundedAircraft", groundedAc);
        map.put("activeFlights", activeFlights);
        map.put("delayedFlights", delayedFlights);
        map.put("openMaintenance", openMaint);
        map.put("overdueMaintenance", overdueMaint);
        map.put("openIncidents", openInc);
        map.put("criticalIncidents", criticalInc);
        map.put("systemStatus", systemStatus);

        return map;
    }

    // ==========================================
    // AI OPERATIONS COPILOT
    // ==========================================

    @PostMapping("/ai/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> b) {
        String q = b.getOrDefault("question", "");
        if (q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        return Map.of("answer", aiService.answer(q));
    }
}
