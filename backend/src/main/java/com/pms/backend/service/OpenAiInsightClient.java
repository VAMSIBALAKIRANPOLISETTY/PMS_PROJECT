package com.pms.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiInsightClient {
    private static final String RESPONSE_SCHEMA_NAME = "pms_care_prep_insight";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiInsightClient(String baseUrl, String apiKey, String model, Duration timeout) {
        this.endpoint = normalizedBaseUrl(baseUrl) + "/responses";
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = hasText(model) ? model.trim() : "gpt-4o-mini";
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    public AiInsightService.CarePrepInsight forAssessment(
            AppUser user,
            Assessment assessment,
            RiskEngineService.RiskResult result
    ) {
        String input = """
                Create PMS care-preparation wording from this backend-safe assessment context.

                Patient context:
                - Age: %s
                - Sex: %s
                - Profile chronic history: %s

                Assessment:
                - Symptoms: %s
                - Severity: %s/10
                - Duration days: %s
                - Temperature available: %s
                - Temperature F: %s
                - Assessment chronic condition: %s

                Rule engine result:
                - Risk level: %s
                - Risk score: %s
                - Rule reasons: %s
                - Existing next-step suggestions: %s
                - Follow-up questions answered: %s
                """.formatted(
                nullable(user.getAge()),
                nullable(user.getGender()),
                nullable(user.getChronicConditions()),
                assessment.getSymptoms(),
                nullable(assessment.getSeverity()),
                nullable(assessment.getDurationDays()),
                nullable(assessment.getTemperatureAvailable()),
                nullable(assessment.getTemperatureF()),
                nullable(assessment.getChronicCondition()),
                result.level(),
                result.score(),
                result.reasons(),
                result.suggestions(),
                assessment.getFollowUpQuestions()
        );
        return requestInsight(input);
    }

    public AiInsightService.CarePrepInsight forReport(
            AppUser user,
            String reportName,
            String reportText,
            List<String> answers
    ) {
        String input = """
                Create PMS care-preparation wording from these report notes and follow-up answers.

                Patient context:
                - Age: %s
                - Sex: %s
                - Profile chronic history: %s

                Report:
                - Report name: %s
                - Report notes or copied text: %s
                - Follow-up answers: %s
                """.formatted(
                nullable(user.getAge()),
                nullable(user.getGender()),
                nullable(user.getChronicConditions()),
                nullable(reportName),
                nullable(reportText),
                answers == null ? List.of() : answers
        );
        return requestInsight(input);
    }

    private AiInsightService.CarePrepInsight requestInsight(String input) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(input))))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI returned HTTP " + response.statusCode());
            }
            String outputText = outputText(response.body());
            ProviderInsight providerInsight = objectMapper.readValue(outputText, ProviderInsight.class);
            return toCarePrepInsight(providerInsight);
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI response could not be read");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request was interrupted");
        }
    }

    private Map<String, Object> requestBody(String input) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", """
                You write safe PMS Health care-preparation guidance.
                Do not diagnose disease, prescribe medicine, provide dosages, or replace urgent medical care.
                Use plain language for a normal patient.
                The backend rule engine owns all risk scoring and urgent warnings.
                Set urgentWarning to null. Do not create, remove, or soften urgent warnings.
                Return only the requested structured fields.
                """);
        body.put("input", input);
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", RESPONSE_SCHEMA_NAME,
                "strict", true,
                "schema", responseSchema()
        )));
        return body;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("careSummary", Map.of("type", "string", "minLength", 20));
        properties.put("explanation", Map.of("type", "string", "minLength", 20));
        properties.put("possibleDirections", stringArraySchema(1, 5));
        properties.put("urgentWarning", Map.of("type", List.of("string", "null")));
        properties.put("monitoringPlan", stringArraySchema(1, 5));
        properties.put("doctorPrepQuestions", stringArraySchema(1, 7));
        properties.put("trustedSourceLinks", stringArraySchema(1, 5));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of(
                "careSummary",
                "explanation",
                "possibleDirections",
                "urgentWarning",
                "monitoringPlan",
                "doctorPrepQuestions",
                "trustedSourceLinks"
        ));
        return schema;
    }

    private Map<String, Object> stringArraySchema(int minItems, int maxItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("minItems", minItems);
        schema.put("maxItems", maxItems);
        schema.put("items", Map.of("type", "string", "minLength", 8));
        return schema;
    }

    private String outputText(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("output_text").isTextual() && !root.path("output_text").asText().isBlank()) {
            return root.path("output_text").asText();
        }

        List<String> chunks = new ArrayList<>();
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                JsonNode text = content.path("text");
                if (text.isTextual() && !text.asText().isBlank()) {
                    chunks.add(text.asText());
                }
            }
        }
        String combined = String.join("", chunks).trim();
        if (combined.isBlank()) {
            throw new IllegalStateException("OpenAI response did not include output text");
        }
        return combined;
    }

    private AiInsightService.CarePrepInsight toCarePrepInsight(ProviderInsight insight) {
        String careSummary = requireText(insight.careSummary(), "careSummary");
        String explanation = requireText(insight.explanation(), "explanation");
        List<String> possibleDirections = requireList(insight.possibleDirections(), "possibleDirections", 5);
        List<String> monitoringPlan = requireList(insight.monitoringPlan(), "monitoringPlan", 5);
        List<String> doctorPrepQuestions = requireList(insight.doctorPrepQuestions(), "doctorPrepQuestions", 7);
        List<String> trustedSourceLinks = requireList(insight.trustedSourceLinks(), "trustedSourceLinks", 5);
        return new AiInsightService.CarePrepInsight(
                careSummary,
                explanation,
                possibleDirections,
                null,
                monitoringPlan,
                doctorPrepQuestions,
                trustedSourceLinks,
                "PROVIDER"
        );
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalStateException("OpenAI output missing " + field);
        }
        return value.trim();
    }

    private List<String> requireList(List<String> values, String field, int limit) {
        if (values == null) {
            throw new IllegalStateException("OpenAI output missing " + field);
        }
        List<String> cleaned = values.stream()
                .filter(OpenAiInsightClient::hasText)
                .map(String::trim)
                .distinct()
                .limit(limit)
                .toList();
        if (cleaned.isEmpty()) {
            throw new IllegalStateException("OpenAI output missing " + field);
        }
        return cleaned;
    }

    private static String normalizedBaseUrl(String baseUrl) {
        String value = hasText(baseUrl) ? baseUrl.trim() : "https://api.openai.com/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String nullable(Object value) {
        return value == null || value.toString().isBlank() ? "Not provided" : value.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ProviderInsight(
            String careSummary,
            String explanation,
            List<String> possibleDirections,
            String urgentWarning,
            List<String> monitoringPlan,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks
    ) {}
}
