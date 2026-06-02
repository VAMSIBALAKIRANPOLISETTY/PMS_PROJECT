package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockAiInsightServiceTests {
    private final MockAiInsightService aiInsightService = new MockAiInsightService();

    @Test
    void assessmentInsightReturnsStructuredCarePrepFields() {
        AppUser user = new AppUser();
        user.setFullName("Anaya Rao");
        user.setAge(24);
        user.setGender("Female");

        Assessment assessment = new Assessment();
        assessment.setSymptoms(List.of("Fever", "Weakness"));
        assessment.setSeverity(6);
        assessment.setDurationDays(4);
        assessment.setTemperatureAvailable(true);
        assessment.setTemperatureF(100.4);
        assessment.setChronicCondition("None");

        RiskEngineService.RiskResult result = new RiskEngineService.RiskResult(
                52,
                RiskLevel.MEDIUM,
                List.of("Temperature is above normal range."),
                List.of("Do you have chills?"),
                List.of("Consult a qualified doctor if symptoms persist or concern you."),
                null
        );

        AiInsightService.CarePrepInsight insight = aiInsightService.forAssessment(user, assessment, result);

        assertEquals("MOCK", insight.aiMode());
        assertNotNull(insight.careSummary());
        assertFalse(insight.possibleDirections().isEmpty());
        assertFalse(insight.monitoringPlan().isEmpty());
        assertFalse(insight.doctorPrepQuestions().isEmpty());
        assertTrue(insight.trustedSourceLinks().stream().anyMatch(source -> source.contains("MedlinePlus")));
    }

    @Test
    void reportInsightKeepsUrgentWarningWhenAnswersMentionRedFlags() {
        AppUser user = new AppUser();
        user.setFullName("Anaya Rao");

        AiInsightService.CarePrepInsight insight = aiInsightService.forReport(
                user,
                "lab-report.pdf",
                "Several values are abnormal.",
                List.of("I have chest pain and shortness of breath.", "Some values are abnormal.")
        );

        assertEquals("MOCK", insight.aiMode());
        assertNotNull(insight.urgentWarning());
        assertTrue(insight.possibleDirections().stream().anyMatch(direction -> direction.contains("abnormal values")));
    }
}
