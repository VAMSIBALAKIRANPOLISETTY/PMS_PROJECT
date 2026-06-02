package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.util.List;

public interface AiInsightService {
    CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result);

    List<String> reportFollowUps(String reportName);

    CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers);

    record CarePrepInsight(
            String careSummary,
            String explanation,
            List<String> possibleDirections,
            String urgentWarning,
            List<String> monitoringPlan,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks,
            String aiMode
    ) {}
}
