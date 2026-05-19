package com.pms.backend.dto;

import com.pms.backend.model.RiskLevel;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class AssessmentDtos {
    public record AssessmentRequest(
            @NotBlank @Size(min = 2, max = 120) String mainSymptom,
            @NotNull @Min(1) @Max(10) Integer severity,
            @NotNull @Min(0) @Max(365) Integer durationDays,
            @NotNull @DecimalMin("90.0") @DecimalMax("110.0") Double temperatureF,
            @NotNull @Min(50) @Max(100) Integer oxygenLevel,
            @NotNull @Min(35) @Max(220) Integer heartRate,
            @Size(max = 100) String chronicCondition
    ) {}

    public record AssessmentResponse(
            Long id,
            Long userId,
            String patient,
            String mainSymptom,
            Integer severity,
            Integer durationDays,
            Double temperatureF,
            Integer oxygenLevel,
            Integer heartRate,
            String chronicCondition,
            Integer riskScore,
            RiskLevel riskLevel,
            List<String> reasons,
            List<String> suggestions,
            List<String> followUpQuestions,
            LocalDateTime createdAt
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
