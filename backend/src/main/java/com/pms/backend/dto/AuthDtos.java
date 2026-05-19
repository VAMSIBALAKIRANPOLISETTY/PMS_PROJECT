package com.pms.backend.dto;

import com.pms.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 40) String username,
            @NotBlank String fullName,
            @NotBlank @Size(min = 8, max = 80) String password,
            Role role
    ) {}

    public record LoginRequest(
            @NotBlank String identifier,
            @NotBlank String password
    ) {}

    public record UserResponse(
            Long id,
            String email,
            String username,
            String fullName,
            Role role,
            Integer age,
            String gender,
            Double heightCm,
            Double weightKg,
            String allergies,
            String chronicConditions,
            String lifestyle
    ) {}

    public record AuthResponse(String token, UserResponse user) {}
}
