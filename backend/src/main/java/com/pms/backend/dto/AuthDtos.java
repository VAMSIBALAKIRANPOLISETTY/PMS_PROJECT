package com.pms.backend.dto;

import com.pms.backend.model.Role;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 40) String username,
            @NotBlank String fullName,
            @NotBlank @Size(min = 8, max = 80) String password,
            Role role,
            @NotNull @Min(1) @Max(120) Integer age,
            @NotNull @DecimalMin("30.0") @DecimalMax("260.0") Double heightCm,
            @NotNull @DecimalMin("2.0") @DecimalMax("350.0") Double weightKg,
            @NotBlank @Size(max = 40) String sex
    ) {}

    public record LoginRequest(
            @NotBlank String identifier,
            @NotBlank String password
    ) {}

    public record ProfileUpdateRequest(
            @NotBlank String fullName,
            @NotNull @Min(1) @Max(120) Integer age,
            @NotNull @DecimalMin("30.0") @DecimalMax("260.0") Double heightCm,
            @NotNull @DecimalMin("2.0") @DecimalMax("350.0") Double weightKg,
            @NotBlank @Size(max = 40) String sex,
            @Size(max = 180) String allergies,
            @Size(max = 220) String chronicConditions,
            @Size(max = 220) String lifestyle,
            @Size(max = 220) String medications,
            @Size(max = 220) String familyHistory,
            @Size(max = 220) String mentalHealthHistory,
            @Size(max = 120) String sleepQuality
    ) {}

    public record UserResponse(
            Long id,
            String email,
            String username,
            String fullName,
            Role role,
            Integer age,
            String sex,
            Double heightCm,
            Double weightKg,
            String allergies,
            String chronicConditions,
            String lifestyle,
            String medications,
            String familyHistory,
            String mentalHealthHistory,
            String sleepQuality,
            Integer profileCompletion
    ) {}

    public record AuthResponse(String token, UserResponse user) {}
}
