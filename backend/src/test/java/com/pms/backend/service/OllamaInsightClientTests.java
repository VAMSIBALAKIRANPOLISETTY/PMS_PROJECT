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

class OllamaInsightClientTests {
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
    void parsesStructuredChatOutput() throws Exception {
        startServer(200, ollamaResponse(validStructuredOutput()));
        OllamaInsightClient client = client();

        var insight = client.forAssessment(patient(), assessment(), riskResult());

        assertEquals("OLLAMA", insight.aiMode());
        assertEquals("Ollama summary for PMS care preparation and clinician conversation planning.", insight.careSummary());
        assertNull(insight.urgentWarning());
        assertEquals(List.of("Track symptom changes and temperature readings."), insight.monitoringPlan());
        assertEquals(List.of("Bring a symptom timeline to the clinician conversation."), insight.careTips());
        assertEquals("Bearer test-ollama-key", capturedAuthorization);
        assertTrue(capturedBody.contains("\"model\":\"gemma4:31b\""));
        assertTrue(capturedBody.contains("\"stream\":false"));
        assertTrue(capturedBody.contains("\"format\""));
        assertTrue(capturedBody.contains("\"temperature\":0.2"));
        assertTrue(capturedBody.contains("The backend rule engine owns all risk scoring and urgent warnings."));
    }

    @Test
    void parsesQuestionSuggestions() throws Exception {
        startServer(200, ollamaResponse("""
                {"questions":["Is the fever worse today?","Did this begin suddenly?"]}
                """));
        OllamaInsightClient client = client();

        var suggestions = client.suggestQuestions("Fever", "duration");

        assertEquals("OLLAMA", suggestions.aiMode());
        assertEquals(List.of("Is the fever worse today?", "Did this begin suddenly?"), suggestions.questions());
    }

    @Test
    void cloudModeUsesJsonFormatAndCloudModelAliasFallback() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ollama.com/api/chat", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            capturedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            capturedBody = body;
            String responseBody;
            int status;
            if (body.contains("\"model\":\"gemma4:31b-cloud\"")) {
                status = 200;
                responseBody = ollamaResponse(validStructuredOutput());
            } else {
                status = 404;
                responseBody = "{\"error\":\"model not available\"}";
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        OllamaInsightClient client = new OllamaInsightClient(
                "http://localhost:" + server.getAddress().getPort() + "/ollama.com/api",
                "test-ollama-key",
                "gemma4:31b",
                Duration.ofSeconds(3),
                0.2
        );

        var insight = client.forAssessment(patient(), assessment(), riskResult());

        assertEquals("OLLAMA", insight.aiMode());
        assertEquals("Bearer test-ollama-key", capturedAuthorization);
        assertTrue(capturedBody.contains("\"model\":\"gemma4:31b-cloud\""));
        assertTrue(capturedBody.contains("\"format\":\"json\""));
        assertTrue(capturedBody.contains("Return only valid JSON matching this shape"));
    }

    @Test
    void rejectsMissingMessageContent() throws Exception {
        startServer(200, "{\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true}");
        OllamaInsightClient client = client();

        assertThrows(IllegalStateException.class, () -> client.forAssessment(patient(), assessment(), riskResult()));
    }

    @Test
    void rejectsNonSuccessfulProviderStatus() throws Exception {
        startServer(503, "{\"error\":\"model unavailable\"}");
        OllamaInsightClient client = client();

        assertThrows(IllegalStateException.class, () -> client.forAssessment(patient(), assessment(), riskResult()));
    }

    private OllamaInsightClient client() {
        return new OllamaInsightClient("http://localhost:" + server.getAddress().getPort(), "test-ollama-key", "gemma4:31b", Duration.ofSeconds(3), 0.2);
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat", exchange -> respond(exchange, status, responseBody));
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

    private String ollamaResponse(String structuredOutput) throws IOException {
        return """
                {"message":{"role":"assistant","content":%s},"done":true}
                """.formatted(objectMapper.writeValueAsString(structuredOutput));
    }

    private String validStructuredOutput() {
        return """
                {
                  "careSummary": "Ollama summary for PMS care preparation and clinician conversation planning.",
                  "explanation": "Ollama explanation stays non-diagnostic and explains why these symptoms should be organized before care.",
                  "possibleDirections": ["Discuss symptom pattern and duration with a clinician."],
                  "urgentWarning": "This warning should be removed by backend service code.",
                  "monitoringPlan": ["Track symptom changes and temperature readings."],
                  "careTips": ["Bring a symptom timeline to the clinician conversation."],
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
