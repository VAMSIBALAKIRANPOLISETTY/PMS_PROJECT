package com.pms.backend.dto;

import com.pms.backend.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminDtos {
    public record QuestionResponse(Long id, String symptomKey, String prompt, String inputType, boolean active) {}

    public record RuleResponse(Long id, String conditionLabel, RiskLevel riskLevel, Integer score, String explanation) {}

    public record RuleRequest(
            @NotBlank String conditionLabel,
            @NotNull RiskLevel riskLevel,
            @NotNull Integer score,
            @NotBlank String explanation
    ) {}
}
