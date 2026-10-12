package com.example.skyguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Test
    void testGetSummaryTelemetry() throws Exception {
        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraft", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.flights", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.systemStatus", notNullValue()));
    }

    @Test
    void testGetAlerts() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void testGetAircraftList() throws Exception {
        mockMvc.perform(get("/api/aircraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].registration", notNullValue()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAddAircraftSuccess() throws Exception {
        String testReg = "TEST" + (int)(Math.random() * 9000 + 1000);
        String json = String.format("""
            {
                "registration": "%s",
                "model": "Boeing 737 MAX",
                "manufacturer": "Boeing",
                "capacity": 178,
                "location": "MIA",
                "status": "AVAILABLE"
            }
        """, testReg);

        mockMvc.perform(post("/api/aircraft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registration", is(testReg)));
    }

    @Test
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void testScheduleFlightOnGroundedAircraftThrowsConflict() throws Exception {
        // Find grounded aircraft or make one
        Aircraft grounded = aircraftRepository.findAll().stream()
                .filter(a -> "GROUNDED".equalsIgnoreCase(a.status))
                .findFirst()
                .orElse(null);

        if (grounded != null) {
            String json = String.format("""
                {
                    "flightNumber": "SG-999",
                    "origin": "JFK",
                    "destination": "ORD",
                    "aircraftRegistration": "%s",
                    "departureTime": "2026-10-15T10:00:00",
                    "arrivalTime": "2026-10-15T12:30:00",
                    "status": "SCHEDULED"
                }
            """, grounded.registration);

            mockMvc.perform(post("/api/flights")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void testAiCopilotEndpoint() throws Exception {
        String json = """
            {
                "question": "What is the procedure for an overdue C-check?"
            }
        """;

        mockMvc.perform(post("/api/ai/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", containsString("Maintenance")));
    }

    @Test
    void testAuthLoginSuccess() throws Exception {
        String json = """
            {
                "username": "admin",
                "password": "admin123"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
    }

    @Test
    void testAuthLoginInvalidCredentials() throws Exception {
        String json = """
            {
                "username": "admin",
                "password": "wrongpassword"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthRegisterSuccess() throws Exception {
        String uniqueUser = "pilot_" + System.currentTimeMillis();
        String json = String.format("""
            {
                "username": "%s",
                "password": "securepassword",
                "fullName": "Test Pilot",
                "email": "testpilot@skyguard.aero"
            }
        """, uniqueUser);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.username", is(uniqueUser)));
    }
}
