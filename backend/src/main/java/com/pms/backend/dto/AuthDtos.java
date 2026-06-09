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
import jakarta.validation.constraints.AssertTrue;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 40) String username,
            @NotBlank String fullName,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotNull @Min(18) @Max(120) Integer age,
            @NotNull @DecimalMin("30.0") @DecimalMax("260.0") Double heightCm,
            @NotNull @DecimalMin("2.0") @DecimalMax("350.0") Double weightKg,
            @NotBlank @Size(max = 40) String sex,
            @AssertTrue(message = "Privacy notice acceptance is required.") Boolean privacyNoticeAccepted,
            @AssertTrue(message = "Terms acceptance is required.") Boolean termsAccepted
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
            @Size(max = 120) String sleepQuality,
            @Size(max = 40) String dateOfBirth,
            @Size(max = 40) String sexAtBirth,
            @Size(max = 60) String genderIdentity,
            @Size(max = 80) String preferredLanguage,
            @Size(max = 40) String phone,
            @Size(max = 240) String address,
            @Size(max = 20) String bloodType,
            @Size(max = 80) String pregnancyStatus,
            @Size(max = 120) String emergencyContactName,
            @Size(max = 80) String emergencyContactRelationship,
            @Size(max = 40) String emergencyContactPhone,
            @Size(max = 160) String preferredHospital,
            @Size(max = 260) String surgeries,
            @Size(max = 260) String immunizations,
            @Size(max = 160) String primaryDoctor,
            @Size(max = 220) String specialistNames,
            @Size(max = 160) String hospitalClinic,
            @Size(max = 160) String insuranceProvider,
            @Size(max = 120) String insuranceMemberId,
            @Size(max = 40) String baselineHeartRate,
            @Size(max = 60) String baselineBloodPressure,
            @Size(max = 220) String tobaccoAlcoholUse,
            @Size(max = 220) String dietNotes,
            Boolean connectedDataConsent,
            @Size(max = 80) String notificationPreference,
            @Size(max = 40) String exportFormatPreference,
            @Size(max = 120) String dataSharingPreference
    ) {
        public ProfileUpdateRequest(
                String fullName,
                Integer age,
                Double heightCm,
                Double weightKg,
                String sex,
                String allergies,
                String chronicConditions,
                String lifestyle,
                String medications,
                String familyHistory,
                String mentalHealthHistory,
                String sleepQuality
        ) {
            this(fullName, age, heightCm, weightKg, sex, allergies, chronicConditions, lifestyle, medications,
                    familyHistory, mentalHealthHistory, sleepQuality, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null);
        }
    }

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
            String dateOfBirth,
            String sexAtBirth,
            String genderIdentity,
            String preferredLanguage,
            String phone,
            String address,
            String bloodType,
            String pregnancyStatus,
            String emergencyContactName,
            String emergencyContactRelationship,
            String emergencyContactPhone,
            String preferredHospital,
            String surgeries,
            String immunizations,
            String primaryDoctor,
            String specialistNames,
            String hospitalClinic,
            String insuranceProvider,
            String insuranceMemberId,
            String baselineHeartRate,
            String baselineBloodPressure,
            String tobaccoAlcoholUse,
            String dietNotes,
            Boolean connectedDataConsent,
            String notificationPreference,
            String exportFormatPreference,
            String dataSharingPreference,
            String profilePhotoDataUrl,
            Integer profileCompletion,
            Boolean profileSetupComplete
    ) {}

    public record AuthResponse(String token, UserResponse user) {}
}
