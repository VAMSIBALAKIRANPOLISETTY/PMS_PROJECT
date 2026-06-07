package com.pms.backend.dto;

import com.pms.backend.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminDtos {
    public record QuestionResponse(Long id, String symptomKey, String prompt, String inputType, boolean active) {}

    public record QuestionRequest(
            @NotBlank @Size(max = 80) String symptomKey,
            @NotBlank @Size(max = 220) String prompt,
            Boolean active
    ) {}

    public record QuestionSuggestionRequest(
            @NotBlank @Size(max = 80) String symptomKey,
            @Size(max = 240) String focus
    ) {}

    public record QuestionSuggestionResponse(
            String symptomKey,
            java.util.List<String> suggestions,
            String aiMode
    ) {}

    public record ActiveRequest(@NotNull Boolean active) {}

    public record RuleResponse(
            Long id,
            String conditionLabel,
            String primarySymptom,
            String secondarySymptom,
            Integer minSeverity,
            Integer minDurationDays,
            String chronicConditionKeyword,
            RiskLevel riskLevel,
            Integer score,
            boolean urgent,
            boolean active,
            String explanation
    ) {}

    public record RuleRequest(
            @NotBlank @Size(max = 120) String conditionLabel,
            @NotBlank @Size(max = 80) String primarySymptom,
            @Size(max = 80) String secondarySymptom,
            @Min(1) @Max(10) Integer minSeverity,
            @Min(0) @Max(365) Integer minDurationDays,
            @Size(max = 100) String chronicConditionKeyword,
            @NotNull @Min(0) @Max(100) Integer score,
            Boolean urgent,
            Boolean active,
            @NotBlank @Size(max = 600) String explanation
    ) {}
}
