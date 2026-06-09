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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OllamaInsightClient {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final boolean cloudMode;
    private final Duration timeout;
    private final double temperature;

    public OllamaInsightClient(String baseUrl, String apiKey, String model, Duration timeout, double temperature) {
        String normalizedBaseUrl = normalizedBaseUrl(baseUrl);
        this.endpoint = normalizedBaseUrl + "/chat";
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = hasText(model) ? model.trim() : "gemma4:31b";
        this.cloudMode = normalizedBaseUrl.contains("ollama.com");
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        this.temperature = temperature;
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
                - Connected health context: %s

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
                nullable(assessment.getConnectedHealthSummary()),
                result.level(),
                result.score(),
                result.reasons(),
                result.suggestions(),
                assessment.getFollowUpQuestions()
        );
        return requestInsight(input);
    }

    public AiInsightService.QuestionSet forAssessmentFollowUps(
            AppUser user,
            Assessment assessment,
            RiskEngineService.RiskResult result
    ) {
        String input = """
                Suggest extra PMS guided-assessment follow-up questions if useful.

                Patient context:
                - Age: %s
                - Sex: %s
                - Profile chronic history: %s

                Assessment:
                - Symptoms: %s
                - Severity: %s/10
                - Duration days: %s
                - Rule risk level: %s
                - Existing required questions: %s

                Return optional extra questions only. They must work with Yes, No, or Not sure choices.
                """.formatted(
                nullable(user.getAge()),
                nullable(user.getGender()),
                nullable(user.getChronicConditions()),
                assessment.getSymptoms(),
                nullable(assessment.getSeverity()),
                nullable(assessment.getDurationDays()),
                result.level(),
                result.followUps()
        );
        return requestQuestions(input, 3);
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

    public AiInsightService.QuestionSet reportFollowUps(String reportName) {
        String input = """
                Suggest PMS report follow-up questions for a patient before a care-preparation report guide is generated.

                Report name: %s

                Questions must be safe, non-diagnostic, and answerable by a normal patient.
                """.formatted(nullable(reportName));
        return requestQuestions(input, 6);
    }

    public AiInsightService.QuestionSet suggestQuestions(String symptomKey, String focus) {
        String input = """
                Suggest staff-review draft questions for the PMS managed question bank.

                Symptom or category: %s
                Staff focus: %s

                Drafts must be short and compatible with Yes, No, or Not sure answers.
                Do not diagnose, prescribe, or imply emergency triage ownership.
                """.formatted(nullable(symptomKey), nullable(focus));
        return requestQuestions(input, 5);
    }

    private AiInsightService.CarePrepInsight requestInsight(String input) {
        RuntimeException lastFailure = null;
        for (String candidate : modelCandidates()) {
            try {
                String content = send(chatBody(candidate, systemInstructions(), input, responseSchema()));
                ProviderInsight providerInsight = objectMapper.readValue(content, ProviderInsight.class);
                return toCarePrepInsight(providerInsight);
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("Ollama response could not be read for " + candidate);
            } catch (RuntimeException exception) {
                lastFailure = exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ollama request was interrupted");
            }
        }
        throw lastFailure == null ? new IllegalStateException("Ollama provider failed") : lastFailure;
    }

    private AiInsightService.QuestionSet requestQuestions(String input, int maxItems) {
        RuntimeException lastFailure = null;
        for (String candidate : modelCandidates()) {
            try {
                String content = send(chatBody(candidate, questionInstructions(), input, questionSchema(maxItems)));
                ProviderQuestions providerQuestions = objectMapper.readValue(content, ProviderQuestions.class);
                return new AiInsightService.QuestionSet(requireList(providerQuestions.questions(), "questions", maxItems), "OLLAMA");
            } catch (IOException exception) {
                lastFailure = new IllegalStateException("Ollama question response could not be read for " + candidate);
            } catch (RuntimeException exception) {
                lastFailure = exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ollama question request was interrupted");
            }
        }
        throw lastFailure == null ? new IllegalStateException("Ollama question provider failed") : lastFailure;
    }

    private String send(Map<String, Object> body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json");
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        HttpResponse<String> response = httpClient.send(
                builder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama returned HTTP " + response.statusCode());
        }
        return messageContent(response.body());
    }

    private Map<String, Object> chatBody(String selectedModel, String instructions, String input, Map<String, Object> schema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("messages", List.of(
                Map.of("role", "system", "content", instructions),
                Map.of("role", "user", "content", cloudMode ? input + "\n\nReturn only valid JSON matching this shape:\n" + schemaHint(schema) : input)
        ));
        body.put("stream", false);
        body.put("format", cloudMode ? "json" : schema);
        body.put("options", Map.of("temperature", temperature));
        return body;
    }

    private String systemInstructions() {
        return """
                You write safe PMS Health care-preparation guidance.
                Do not diagnose disease, prescribe medicine, provide dosages, or replace urgent medical care.
                Use plain language for a normal patient.
                The backend rule engine owns all risk scoring and urgent warnings.
                Set urgentWarning to null. Do not create, remove, or soften urgent warnings.
                Return only the requested structured fields.
                """;
    }

    private String questionInstructions() {
        return """
                You write PMS Health follow-up question drafts.
                Every question must be answerable with Yes, No, or Not sure.
                Do not diagnose disease, prescribe medicine, provide dosages, or replace urgent medical care.
                Return only the requested structured fields.
                """;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("careSummary", Map.of("type", "string", "minLength", 20));
        properties.put("explanation", Map.of("type", "string", "minLength", 20));
        properties.put("possibleDirections", stringArraySchema(1, 5));
        properties.put("urgentWarning", Map.of("type", List.of("string", "null")));
        properties.put("monitoringPlan", stringArraySchema(1, 5));
        properties.put("careTips", stringArraySchema(1, 5));
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
                "careTips",
                "doctorPrepQuestions",
                "trustedSourceLinks"
        ));
        return schema;
    }

    private Map<String, Object> questionSchema(int maxItems) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("questions", stringArraySchema(1, maxItems));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("questions"));
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

    private List<String> modelCandidates() {
        if (!cloudMode) {
            return List.of(model);
        }
        if ("gemma4:31b-cloud".equalsIgnoreCase(model)) {
            return List.of(model);
        }
        return List.of(model, "gemma4:31b-cloud").stream().distinct().toList();
    }

    private String schemaHint(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException exception) {
            return "JSON object with the requested PMS care-preparation fields.";
        }
    }

    private String messageContent(String responseBody) throws JsonProcessingException {
        JsonNode content = objectMapper.readTree(responseBody).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IllegalStateException("Ollama response did not include message content");
        }
        return content.asText();
    }

    private AiInsightService.CarePrepInsight toCarePrepInsight(ProviderInsight insight) {
        return new AiInsightService.CarePrepInsight(
                requireText(insight.careSummary(), "careSummary"),
                requireText(insight.explanation(), "explanation"),
                requireList(insight.possibleDirections(), "possibleDirections", 5),
                null,
                requireList(insight.monitoringPlan(), "monitoringPlan", 5),
                requireList(insight.careTips(), "careTips", 5),
                requireList(insight.doctorPrepQuestions(), "doctorPrepQuestions", 7),
                requireList(insight.trustedSourceLinks(), "trustedSourceLinks", 5),
                "OLLAMA"
        );
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalStateException("Ollama output missing " + field);
        }
        return value.trim();
    }

    private List<String> requireList(List<String> values, String field, int limit) {
        if (values == null) {
            throw new IllegalStateException("Ollama output missing " + field);
        }
        List<String> cleaned = values.stream()
                .filter(OllamaInsightClient::hasText)
                .map(String::trim)
                .distinct()
                .limit(limit)
                .toList();
        if (cleaned.isEmpty()) {
            throw new IllegalStateException("Ollama output missing " + field);
        }
        return cleaned;
    }

    private static String normalizedBaseUrl(String baseUrl) {
        String value = hasText(baseUrl) ? baseUrl.trim() : "https://ollama.com/api";
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
            List<String> careTips,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks
    ) {}

    private record ProviderQuestions(List<String> questions) {}
}
