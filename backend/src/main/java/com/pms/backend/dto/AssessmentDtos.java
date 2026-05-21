package com.pms.backend.dto;

import com.pms.backend.model.RiskLevel;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class AssessmentDtos {
    public record AssessmentRequest(
            @NotEmpty @Size(max = 5) List<@NotBlank @Size(min = 2, max = 80) String> symptoms,
            @NotNull @Min(1) @Max(10) Integer severity,
            @NotNull @Min(0) @Max(365) Integer durationDays,
            Boolean temperatureAvailable,
            @DecimalMin("90.0") @DecimalMax("110.0") Double temperatureF,
            @Size(max = 100) String chronicCondition
    ) {}

    public record FollowUpAnswerRequest(
            @NotEmpty @Size(min = 1, max = 7) List<@NotBlank @Size(max = 500) String> answers
    ) {}

    public record AssessmentResponse(
            Long id,
            Long userId,
            String patient,
            String mainSymptom,
            List<String> symptoms,
            Integer severity,
            Integer durationDays,
            Boolean temperatureAvailable,
            Double temperatureF,
            String chronicCondition,
            Integer riskScore,
            RiskLevel riskLevel,
            List<String> reasons,
            List<String> suggestions,
            List<String> followUpQuestions,
            List<String> followUpAnswers,
            String careSummary,
            String explanation,
            List<String> possibleDirections,
            String urgentWarning,
            List<String> monitoringPlan,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks,
            String aiMode,
            LocalDateTime createdAt
    ) {}

    public record ReportFollowUpRequest(
            @NotBlank @Size(max = 160) String reportName
    ) {}

    public record ReportFollowUpResponse(
            String reportName,
            List<String> followUpQuestions,
            String aiMode
    ) {}

    public record ReportInsightRequest(
            @NotBlank @Size(max = 160) String reportName,
            @Size(max = 4000) String reportText,
            @NotEmpty @Size(min = 1, max = 7) List<@NotBlank @Size(max = 500) String> answers
    ) {}

    public record ReportInsightResponse(
            String reportName,
            List<String> followUpQuestions,
            List<String> followUpAnswers,
            String careSummary,
            String explanation,
            List<String> possibleDirections,
            String urgentWarning,
            List<String> monitoringPlan,
            List<String> doctorPrepQuestions,
            List<String> trustedSourceLinks,
            String aiMode
    ) {}

    public record AnalyticsResponse(
            long totalUsers,
            long totalAssessments,
            long highRiskCount,
            long mediumRiskCount,
            long lowRiskCount,
            List<SymptomCount> commonSymptoms
    ) {}

    public record SymptomCount(String symptom, long count) {}
}
