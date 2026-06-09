package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.util.List;

public interface AiInsightService {
    CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result);

    QuestionSet assessmentFollowUps(AppUser user, Assessment assessment, RiskEngineService.RiskResult result);

    QuestionSet reportFollowUps(String reportName, String reportText, String connectedHealthSummary);

    CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers);

    QuestionSet suggestQuestions(String symptomKey, String focus);

    record CarePrepInsight(
            String careSummary,
            String explanation,
            List<String> possibleDirections,
            String urgentWarning,
            List<String> monitoringPlan,
            List<String> careTips,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks,
            String aiMode
    ) {}

    record QuestionSet(List<String> questions, String aiMode) {}
}
