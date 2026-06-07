package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfiguredAiInsightServiceTests {
    private final AppUser user = patient();
    private final Assessment assessment = assessment();
    private final RiskEngineService.RiskResult riskResult = new RiskEngineService.RiskResult(
            52,
            RiskLevel.MEDIUM,
            List.of("Moderate severity should be watched."),
            List.of("Are symptoms getting worse?"),
            List.of("Track symptoms."),
            "Rule-owned urgent warning"
    );

    @Test
    void mockModeReturnsMockInsightByDefault() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new ThrowingOllamaClient(),
                new ThrowingClient(),
                "mock",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var insight = service.forAssessment(user, assessment, riskResult);

        assertEquals("MOCK", insight.aiMode());
        assertNotNull(insight.careSummary());
    }

    @Test
    void providerModeWithMissingApiKeyFallsBackToMock() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new ThrowingOllamaClient(),
                new ThrowingClient(),
                "provider",
                "ollama,openai",
                "",
                ""
        );

        var insight = service.forAssessment(user, assessment, riskResult);

        assertEquals("MOCK", insight.aiMode());
    }

    @Test
    void ollamaProviderAssessmentOutputCannotAddUrgentWarning() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new SuccessfulOllamaClient("AI-generated warning should be removed"),
                new ThrowingClient(),
                "provider",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var insight = service.forAssessment(user, assessment, riskResult);

        assertEquals("OLLAMA", insight.aiMode());
        assertNull(insight.urgentWarning());
        assertEquals("Provider summary for PMS care preparation.", insight.careSummary());
    }

    @Test
    void ollamaFailureFallsBackToOpenAiProvider() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new ThrowingOllamaClient(),
                new SuccessfulClient("AI-generated warning should be removed"),
                "provider",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var insight = service.forAssessment(user, assessment, riskResult);

        assertEquals("OPENAI", insight.aiMode());
        assertNull(insight.urgentWarning());
    }

    @Test
    void providerExceptionsFallBackToMock() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new ThrowingOllamaClient(),
                new ThrowingClient(),
                "provider",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var insight = service.forAssessment(user, assessment, riskResult);

        assertEquals("MOCK", insight.aiMode());
    }

    @Test
    void reportUrgentWarningRemainsJavaOwnedWhenProviderSucceeds() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new SuccessfulOllamaClient("AI-generated warning should not be used"),
                new SuccessfulClient("AI-generated warning should not be used"),
                "provider",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var insight = service.forReport(user, "visit-report.pdf", "Patient notes mention chest pain and breathing difficulty.", List.of("Yes"));

        assertEquals("OLLAMA", insight.aiMode());
        assertNotNull(insight.urgentWarning());
        assertNotEquals("AI-generated warning should not be used", insight.urgentWarning());
    }

    @Test
    void questionSuggestionFallsBackThroughProviderChain() {
        ConfiguredAiInsightService service = new ConfiguredAiInsightService(
                new MockAiInsightService(),
                new ThrowingOllamaClient(),
                new SuccessfulClient(null),
                "provider",
                "ollama,openai",
                "test-ollama-key",
                "test-openai-key"
        );

        var suggestions = service.suggestQuestions("Fever", "duration");

        assertEquals("OPENAI", suggestions.aiMode());
        assertEquals(2, suggestions.questions().size());
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
        assessment.setFollowUpQuestions(List.of("Are symptoms getting worse?"));
        return assessment;
    }

    private static class SuccessfulClient extends OpenAiInsightClient {
        private final String urgentWarning;

        SuccessfulClient(String urgentWarning) {
            super("http://localhost", "test-key", "test-model", Duration.ofSeconds(1));
            this.urgentWarning = urgentWarning;
        }

        @Override
        public AiInsightService.CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
            return providerInsight(urgentWarning);
        }

        @Override
        public AiInsightService.CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
            return providerInsight(urgentWarning);
        }

        @Override
        public AiInsightService.QuestionSet suggestQuestions(String symptomKey, String focus) {
            return new AiInsightService.QuestionSet(List.of("Is this symptom worsening?", "Did this begin suddenly?"), "OPENAI");
        }
    }

    private static class ThrowingClient extends OpenAiInsightClient {
        ThrowingClient() {
            super("http://localhost", "test-key", "test-model", Duration.ofSeconds(1));
        }

        @Override
        public AiInsightService.CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
            throw new IllegalStateException("provider unavailable");
        }

        @Override
        public AiInsightService.CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
            throw new IllegalStateException("provider unavailable");
        }

        @Override
        public AiInsightService.QuestionSet suggestQuestions(String symptomKey, String focus) {
            throw new IllegalStateException("provider unavailable");
        }
    }

    private static class SuccessfulOllamaClient extends OllamaInsightClient {
        private final String urgentWarning;

        SuccessfulOllamaClient(String urgentWarning) {
            super("http://localhost", "test-key", "gemma4:31b", Duration.ofSeconds(1), 0.2);
            this.urgentWarning = urgentWarning;
        }

        @Override
        public AiInsightService.CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
            return providerInsight(urgentWarning, "OLLAMA");
        }

        @Override
        public AiInsightService.CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
            return providerInsight(urgentWarning, "OLLAMA");
        }
    }

    private static class ThrowingOllamaClient extends OllamaInsightClient {
        ThrowingOllamaClient() {
            super("http://localhost", "test-key", "gemma4:31b", Duration.ofSeconds(1), 0.2);
        }

        @Override
        public AiInsightService.CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
            throw new IllegalStateException("ollama unavailable");
        }

        @Override
        public AiInsightService.CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
            throw new IllegalStateException("ollama unavailable");
        }

        @Override
        public AiInsightService.QuestionSet suggestQuestions(String symptomKey, String focus) {
            throw new IllegalStateException("ollama unavailable");
        }
    }

    private static AiInsightService.CarePrepInsight providerInsight(String urgentWarning) {
        return providerInsight(urgentWarning, "OPENAI");
    }

    private static AiInsightService.CarePrepInsight providerInsight(String urgentWarning, String aiMode) {
        return new AiInsightService.CarePrepInsight(
                "Provider summary for PMS care preparation.",
                "Provider explanation that remains non-diagnostic and doctor-preparation focused.",
                List.of("Discuss symptom pattern and duration with a clinician."),
                urgentWarning,
                List.of("Track symptom changes and temperature readings."),
                List.of("Bring a symptom timeline to the clinician conversation."),
                List.of("What should I monitor before a visit?"),
                List.of("MedlinePlus evaluating health information: https://medlineplus.gov/evaluatinghealthinformation.html"),
                aiMode
        );
    }
}
