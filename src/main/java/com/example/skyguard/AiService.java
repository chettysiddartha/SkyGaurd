package com.example.skyguard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class AiService {

    @Value("${ai.api-url:https://api.openai.com/v1/chat/completions}")
    String url;

    @Value("${ai.api-key:}")
    String key;

    @Value("${ai.model:gpt-4o-mini}")
    String model;

    final RestClient client = RestClient.create();

    public String answer(String q) {
        if (q == null || q.isBlank()) {
            return "Please enter a valid aviation query.";
        }

        // If an API key is configured, query the external model
        if (key != null && !key.isBlank() && !key.startsWith("your-")) {
            try {
                Map<String, Object> body = Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content",
                                        "You are SkyGuard AI, an expert Aviation Operations, Fleet Optimization, and Air Safety Copilot. "
                                                + "Provide concise, practical, regulation-compliant operational recommendations. "
                                                + "Do not provide live air-traffic control instructions. "
                                                + "Include ICAO/FAA standard best practices where applicable."),
                                Map.of("role", "user", "content", q)
                        )
                );

                Map<?, ?> r = client.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                if (r != null && r.containsKey("choices")) {
                    List<?> choices = (List<?>) r.get("choices");
                    if (!choices.isEmpty()) {
                        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                        return String.valueOf(((Map<?, ?>) choice.get("message")).get("content"));
                    }
                }
            } catch (Exception e) {
                // Fall back to built-in aviation knowledge base if network/key fails
                return getBuiltInAviationKnowledge(q) + "\n\n*(Note: Cloud AI request failed; provided from SkyGuard Offline Knowledge Engine)*";
            }
        }

        // Built-in intelligent aviation operations reasoning engine
        return getBuiltInAviationKnowledge(q);
    }

    private String getBuiltInAviationKnowledge(String query) {
        String lower = query.toLowerCase();

        if (lower.contains("maintenance") || lower.contains("a-check") || lower.contains("c-check") || lower.contains("inspection")) {
            return "**SkyGuard Maintenance Protocol Advice:**\n\n"
                    + "1. **Routine A-Checks:** Scheduled every 400-600 flight hours (approx. 200-300 cycles). Focuses on fluid filters, avionics diagnostics, tire pressure, and primary control surfaces.\n"
                    + "2. **C-Checks:** Extensive inspection every 20-24 months. Structural integrity, corrosion evaluation, and critical component life-limit checks.\n"
                    + "3. **Action Recommendation:** If an aircraft is flagged with an overdue maintenance status in SkyGuard, it must be immediately shifted to `GROUNDED` or `MAINTENANCE` before any flight dispatching.";
        }

        if (lower.contains("bird") || lower.contains("strike") || lower.contains("engine")) {
            return "**SkyGuard Incident Safety Directive - Bird Strike / Engine Foreign Object Damage (FOD):**\n\n"
                    + "1. **Immediate Action:** Flight crew initiates QRH (Quick Reference Handbook) checklist. Confirm engine instrument parameters (EGT, N1, N2, vibration monitors).\n"
                    + "2. **Ground Operations:** Dispatch informs destination station engineer for a borescope inspection.\n"
                    + "3. **SkyGuard Logging:** Record incident under category `BIRD_STRIKE` with severity level `HIGH` or `CRITICAL`. Transition aircraft status to `MAINTENANCE` until technical sign-off.";
        }

        if (lower.contains("weather") || lower.contains("fog") || lower.contains("metar") || lower.contains("storm") || lower.contains("turbulence") || lower.contains("wind")) {
            return "**SkyGuard Flight Dispatch Weather Standard:**\n\n"
                    + "1. **Alternate Aerodrome Planning:** Under ICAO Part 121, if ceiling < 2,000 ft or visibility < 3 SM within ±1 hour of ETA, at least one alternate airport must be designated.\n"
                    + "2. **Fuel Reserves:** Standard reserve requires holding fuel for 45 minutes at normal cruising consumption above destination/alternate.\n"
                    + "3. **Operational Recommendation:** Delay flight status to `DELAYED` in the Flight Operations console if destination weather drops below CAT I ILS minimums.";
        }

        if (lower.contains("turnaround") || lower.contains("delay") || lower.contains("schedule") || lower.contains("ground time")) {
            return "**SkyGuard Fleet Utilization & Turnaround Optimization:**\n\n"
                    + "1. **Standard Turnaround:** 35-45 minutes for narrow-body (A320/B737) and 75-90 minutes for wide-body (B787/A350).\n"
                    + "2. **Bottleneck Mitigation:** Parallel execution of passenger deplaning, fueling (under safety supervision), cabin cleaning, and cargo offloading.\n"
                    + "3. **Rescheduling Rule:** If inbound delay exceeds 30 minutes, swap with an available reserve aircraft from the JFK or ORD hub to protect downstream rotations.";
        }

        if (lower.contains("priority") || lower.contains("alert") || lower.contains("severity")) {
            return "**SkyGuard Alert Severity Matrix:**\n\n"
                    + "• **CRITICAL:** Imminent safety compromise, uncontained engine issue, runway incursion, or overdue airworthiness directive.\n"
                    + "• **HIGH:** Active aircraft maintenance, delayed departures > 60 mins, unresolved mechanical snag.\n"
                    + "• **MEDIUM:** Maintenance due within 7 days, minor cabin system defect, weather advisory.\n"
                    + "• **LOW:** Informational updates and completed service logs.";
        }

        return "**SkyGuard Operations Copilot Advisory:**\n\n"
                + "Regarding: \"" + query + "\"\n\n"
                + "• **Safety First:** Verify compliance with civil aviation airworthiness directives and dispatch checklists.\n"
                + "• **Fleet Optimization:** Ensure assigned aircraft registration has `AVAILABLE` status and valid maintenance clearance before departure.\n"
                + "• **Operational Decision:** Cross-reference operational logs with the Maintenance Hub and active Alerts console.\n\n"
                + "*(To enable live cloud LLM integration, specify your `AI_API_KEY` in environment variables or application.properties)*";
    }
}
