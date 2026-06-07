package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiInsightClientTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String capturedAuthorization;
    private String capturedBody;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesStructuredResponsesOutput() throws Exception {
        startServer(200, openAiResponse(validStructuredOutput()));
        OpenAiInsightClient client = client();

        var insight = client.forAssessment(patient(), assessment(), riskResult());

        assertEquals("PROVIDER", insight.aiMode());
        assertEquals("Provider summary for PMS care preparation and clinician conversation planning.", insight.careSummary());
        assertNull(insight.urgentWarning());
        assertEquals(List.of("Track symptom changes and temperature readings."), insight.monitoringPlan());
        assertEquals("Bearer test-key", capturedAuthorization);
        assertTrue(capturedBody.contains("\"model\":\"gpt-4o-mini\""));
        assertTrue(capturedBody.contains("\"json_schema\""));
        assertTrue(capturedBody.contains("The backend rule engine owns all risk scoring and urgent warnings."));
    }

    @Test
    void rejectsProviderResponseWithoutOutputText() throws Exception {
        startServer(200, "{\"output\":[{\"content\":[{\"type\":\"refusal\",\"refusal\":\"cannot answer\"}]}]}");
        OpenAiInsightClient client = client();

        assertThrows(IllegalStateException.class, () -> client.forAssessment(patient(), assessment(), riskResult()));
    }

    @Test
    void rejectsNonSuccessfulProviderStatus() throws Exception {
        startServer(503, "{\"error\":{\"message\":\"temporarily unavailable\"}}");
        OpenAiInsightClient client = client();

        assertThrows(IllegalStateException.class, () -> client.forAssessment(patient(), assessment(), riskResult()));
    }

    private OpenAiInsightClient client() {
        return new OpenAiInsightClient("http://localhost:" + server.getAddress().getPort(), "test-key", "gpt-4o-mini", Duration.ofSeconds(3));
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/responses", exchange -> respond(exchange, status, responseBody));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String responseBody) throws IOException {
        capturedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        capturedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String openAiResponse(String structuredOutput) throws IOException {
        return """
                {"output":[{"content":[{"type":"output_text","text":%s}]}]}
                """.formatted(objectMapper.writeValueAsString(structuredOutput));
    }

    private String validStructuredOutput() {
        return """
                {
                  "careSummary": "Provider summary for PMS care preparation and clinician conversation planning.",
                  "explanation": "Provider explanation stays non-diagnostic and explains why these symptoms should be organized before care.",
                  "possibleDirections": ["Discuss symptom pattern and duration with a clinician."],
                  "urgentWarning": "This warning should be removed by backend service code.",
                  "monitoringPlan": ["Track symptom changes and temperature readings."],
                  "doctorPrepQuestions": ["What symptom changes should I mention first?"],
                  "trustedSourceLinks": ["MedlinePlus evaluating health information: https://medlineplus.gov/evaluatinghealthinformation.html"]
                }
                """;
    }

    private static AppUser patient() {
        AppUser user = new AppUser();
        user.setAge(30);
        user.setGender("Prefer not to say");
        user.setChronicConditions("None");
        return user;
    }

    private static Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setSymptoms(List.of("Fever", "Weakness"));
        assessment.setSeverity(6);
        assessment.setDurationDays(3);
        assessment.setTemperatureAvailable(false);
        assessment.setChronicCondition("None");
        return assessment;
    }

    private static RiskEngineService.RiskResult riskResult() {
        return new RiskEngineService.RiskResult(
                52,
                RiskLevel.MEDIUM,
                List.of("Moderate severity should be watched."),
                List.of("Are symptoms getting worse?"),
                List.of("Track symptoms."),
                null
        );
    }
}
