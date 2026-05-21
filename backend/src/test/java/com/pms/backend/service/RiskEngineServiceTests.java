package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.model.RiskLevel;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskEngineServiceTests {
    private final RiskEngineService riskEngineService = new RiskEngineService();

    @Test
    void lowRiskAssessmentHasCarePrepFollowUpsWithoutUrgentWarning() {
        RiskEngineService.RiskResult result = riskEngineService.calculate(
                new AssessmentRequest(List.of("Mild headache"), 2, 1, false, null, "None")
        );

        assertEquals(RiskLevel.LOW, result.level());
        assertNull(result.urgentWarning());
        assertTrue(result.followUps().size() >= 4);
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("Temperature was not available")));
    }

    @Test
    void moderateAssessmentExplainsDurationAndSeverity() {
        RiskEngineService.RiskResult result = riskEngineService.calculate(
                new AssessmentRequest(List.of("Fever", "Weakness"), 6, 4, true, 100.8, "None")
        );

        assertEquals(RiskLevel.MEDIUM, result.level());
        assertNull(result.urgentWarning());
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("Severity is moderate")));
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("more than three days")));
    }

    @Test
    void redFlagAssessmentStaysHighAndAddsUrgentWarning() {
        RiskEngineService.RiskResult result = riskEngineService.calculate(
                new AssessmentRequest(List.of("Chest pain", "Shortness of breath"), 8, 1, false, null, "Blood pressure")
        );

        assertEquals(RiskLevel.HIGH, result.level());
        assertNotNull(result.urgentWarning());
        assertTrue(result.score() >= 85);
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("cannot be lowered")));
    }

    @Test
    void followUpsCannotLowerExistingUrgentWarning() {
        RiskEngineService.RiskResult first = riskEngineService.calculate(
                new AssessmentRequest(List.of("Chest pain", "Shortness of breath"), 8, 1, false, null, "Blood pressure")
        );
        com.pms.backend.model.Assessment assessment = new com.pms.backend.model.Assessment();
        assessment.setRiskScore(first.score());
        assessment.setRiskLevel(first.level());
        assessment.setReasons(first.reasons());
        assessment.setFollowUpQuestions(first.followUps());
        assessment.setUrgentWarning(first.urgentWarning());
        assessment.setSeverity(8);

        RiskEngineService.RiskResult refined = riskEngineService.refineWithFollowUps(assessment, List.of("It feels better now"));

        assertEquals(RiskLevel.HIGH, refined.level());
        assertEquals(first.urgentWarning(), refined.urgentWarning());
    }
}
