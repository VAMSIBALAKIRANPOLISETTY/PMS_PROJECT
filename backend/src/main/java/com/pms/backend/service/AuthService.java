package com.pms.backend.service;

import com.pms.backend.dto.AuthDtos.AuthResponse;
import com.pms.backend.dto.AuthDtos.LoginRequest;
import com.pms.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.dto.AuthDtos.UserResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Role;
import com.pms.backend.repository.UserRepository;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthService {
    static final String PRIVACY_NOTICE_VERSION = "2026-06-02";
    static final String TERMS_VERSION = "2026-06-02";
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final long MAX_PROFILE_PHOTO_BYTES = 750_000;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.age() == null || request.age() < 18) {
            throw new IllegalArgumentException("Patient self-registration is available for adults age 18 and older.");
        }
        if (!Boolean.TRUE.equals(request.privacyNoticeAccepted())) {
            throw new IllegalArgumentException("Review and accept the privacy notice before creating an account.");
        }
        if (!Boolean.TRUE.equals(request.termsAccepted())) {
            throw new IllegalArgumentException("Review and accept the terms of use before creating an account.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Username is already registered.");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().trim().toLowerCase());
        user.setUsername(request.username().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setAge(request.age());
        user.setGender(clean(request.sex(), "Not set"));
        user.setHeightCm(request.heightCm());
        user.setWeightKg(request.weightKg());
        LocalDateTime acceptedAt = LocalDateTime.now();
        user.setPrivacyNoticeVersion(PRIVACY_NOTICE_VERSION);
        user.setTermsVersion(TERMS_VERSION);
        user.setPrivacyNoticeAcceptedAt(acceptedAt);
        user.setTermsAcceptedAt(acceptedAt);
        userRepository.save(user);
        return makeAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        return loginForRole(request, Role.USER);
    }

    public AuthResponse staffLogin(LoginRequest request) {
        return loginForRole(request, Role.ADMIN);
    }

    private AuthResponse loginForRole(LoginRequest request, Role requiredRole) {
        AppUser user = findByEmailOrUsername(request.identifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email/username or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email/username or password.");
        }
        if (user.getRole() != requiredRole) {
            throw new IllegalArgumentException(requiredRole == Role.ADMIN
                    ? "Staff access is required. Use patient login for patient accounts."
                    : "Staff account detected. Use Staff login.");
        }
        return makeAuthResponse(user);
    }

    public AppUser requireUser(String authHeader) {
        Long userId = jwtService.userIdFromBearer(authHeader);
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public AppUser requireAdmin(String authHeader) {
        AppUser user = requireUser(authHeader);
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Admin access required.");
        }
        return user;
    }

    public UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getAge(),
                user.getGender(),
                user.getHeightCm(),
                user.getWeightKg(),
                historyForResponse(user, user.getAllergies()),
                historyForResponse(user, user.getChronicConditions()),
                historyForResponse(user, user.getLifestyle()),
                historyForResponse(user, user.getMedications()),
                historyForResponse(user, user.getFamilyHistory()),
                historyForResponse(user, user.getMentalHealthHistory()),
                historyForResponse(user, user.getSleepQuality()),
                user.getDateOfBirth(),
                user.getSexAtBirth(),
                user.getGenderIdentity(),
                user.getPreferredLanguage(),
                user.getPhone(),
                user.getAddress(),
                user.getBloodType(),
                user.getPregnancyStatus(),
                user.getEmergencyContactName(),
                user.getEmergencyContactRelationship(),
                user.getEmergencyContactPhone(),
                user.getPreferredHospital(),
                user.getSurgeries(),
                user.getImmunizations(),
                user.getPrimaryDoctor(),
                user.getSpecialistNames(),
                user.getHospitalClinic(),
                user.getInsuranceProvider(),
                user.getInsuranceMemberId(),
                user.getBaselineHeartRate(),
                user.getBaselineBloodPressure(),
                user.getTobaccoAlcoholUse(),
                user.getDietNotes(),
                Boolean.TRUE.equals(user.getConnectedDataConsent()),
                user.getNotificationPreference(),
                user.getExportFormatPreference(),
                user.getDataSharingPreference(),
                user.getProfilePhotoDataUrl(),
                profileCompletion(user),
                user.getProfileSetupCompletedAt() != null
        );
    }

    public UserResponse updateProfile(AppUser user, ProfileUpdateRequest request) {
        user.setFullName(request.fullName().trim());
        user.setAge(request.age());
        user.setHeightCm(request.heightCm());
        user.setWeightKg(request.weightKg());
        user.setGender(clean(request.sex(), "Not set"));
        user.setAllergies(cleanNullable(request.allergies()));
        user.setChronicConditions(cleanNullable(request.chronicConditions()));
        user.setLifestyle(cleanNullable(request.lifestyle()));
        user.setMedications(cleanNullable(request.medications()));
        user.setFamilyHistory(cleanNullable(request.familyHistory()));
        user.setMentalHealthHistory(cleanNullable(request.mentalHealthHistory()));
        user.setSleepQuality(cleanNullable(request.sleepQuality()));
        user.setDateOfBirth(cleanNullable(request.dateOfBirth()));
        user.setSexAtBirth(cleanNullable(request.sexAtBirth()));
        user.setGenderIdentity(cleanNullable(request.genderIdentity()));
        user.setPreferredLanguage(cleanNullable(request.preferredLanguage()));
        user.setPhone(cleanNullable(request.phone()));
        user.setAddress(cleanNullable(request.address()));
        user.setBloodType(cleanNullable(request.bloodType()));
        user.setPregnancyStatus(cleanNullable(request.pregnancyStatus()));
        user.setEmergencyContactName(cleanNullable(request.emergencyContactName()));
        user.setEmergencyContactRelationship(cleanNullable(request.emergencyContactRelationship()));
        user.setEmergencyContactPhone(cleanNullable(request.emergencyContactPhone()));
        user.setPreferredHospital(cleanNullable(request.preferredHospital()));
        user.setSurgeries(cleanNullable(request.surgeries()));
        user.setImmunizations(cleanNullable(request.immunizations()));
        user.setPrimaryDoctor(cleanNullable(request.primaryDoctor()));
        user.setSpecialistNames(cleanNullable(request.specialistNames()));
        user.setHospitalClinic(cleanNullable(request.hospitalClinic()));
        user.setInsuranceProvider(cleanNullable(request.insuranceProvider()));
        user.setInsuranceMemberId(cleanNullable(request.insuranceMemberId()));
        user.setBaselineHeartRate(cleanNullable(request.baselineHeartRate()));
        user.setBaselineBloodPressure(cleanNullable(request.baselineBloodPressure()));
        user.setTobaccoAlcoholUse(cleanNullable(request.tobaccoAlcoholUse()));
        user.setDietNotes(cleanNullable(request.dietNotes()));
        user.setConnectedDataConsent(Boolean.TRUE.equals(request.connectedDataConsent()));
        user.setNotificationPreference(cleanNullable(request.notificationPreference()));
        user.setExportFormatPreference(cleanNullable(request.exportFormatPreference()));
        user.setDataSharingPreference(cleanNullable(request.dataSharingPreference()));
        user.setProfileSetupCompletedAt(historyComplete(user) ? LocalDateTime.now() : null);
        return toUserResponse(userRepository.save(user));
    }

    public UserResponse updateProfilePhoto(AppUser user, MultipartFile file) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required to update a profile photo.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a profile photo first.");
        }
        if (file.getSize() > MAX_PROFILE_PHOTO_BYTES) {
            throw new IllegalArgumentException("Profile photo must be smaller than 750 KB.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
            throw new IllegalArgumentException("Profile photo must be a JPG, PNG, or WebP image.");
        }
        try {
            String encoded = Base64.getEncoder().encodeToString(file.getBytes());
            user.setProfilePhotoDataUrl("data:" + contentType + ";base64," + encoded);
            return toUserResponse(userRepository.save(user));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Profile photo could not be read.");
        }
    }

    private Optional<AppUser> findByEmailOrUsername(String identifier) {
        String value = identifier.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(value)
                .or(() -> userRepository.findByUsernameIgnoreCase(value));
    }

    private AuthResponse makeAuthResponse(AppUser user) {
        return new AuthResponse(jwtService.createToken(user), toUserResponse(user));
    }

    private int profileCompletion(AppUser user) {
        int completed = 0;
        int total = 7;
        if (identityComplete(user)) completed++;
        if (bodyBasicsComplete(user)) completed++;
        if (emergencyComplete(user)) completed++;
        if (clinicalBackgroundComplete(user)) completed++;
        if (careTeamComplete(user)) completed++;
        if (lifestyleComplete(user)) completed++;
        if (preferencesComplete(user)) completed++;
        return Math.round((completed * 100f) / total);
    }

    private boolean identityComplete(AppUser user) {
        return hasValue(user.getFullName())
                && user.getAge() != null
                && hasValue(user.getGender()) && !"not set".equalsIgnoreCase(user.getGender())
                && hasValue(user.getPreferredLanguage())
                && hasValue(user.getPhone());
    }

    private boolean bodyBasicsComplete(AppUser user) {
        return user.getHeightCm() != null && user.getHeightCm() > 0
                && user.getWeightKg() != null && user.getWeightKg() > 0
                && hasValue(user.getBloodType());
    }

    private boolean emergencyComplete(AppUser user) {
        return hasValue(user.getEmergencyContactName())
                && hasValue(user.getEmergencyContactRelationship())
                && hasValue(user.getEmergencyContactPhone());
    }

    private boolean clinicalBackgroundComplete(AppUser user) {
        return countHistoryValue(user, user.getAllergies())
                && countHistoryValue(user, user.getChronicConditions())
                && countHistoryValue(user, user.getMedications())
                && countHistoryValue(user, user.getFamilyHistory())
                && countHistoryValue(user, user.getMentalHealthHistory());
    }

    private boolean careTeamComplete(AppUser user) {
        return hasValue(user.getPrimaryDoctor()) || hasValue(user.getHospitalClinic());
    }

    private boolean lifestyleComplete(AppUser user) {
        return countHistoryValue(user, user.getLifestyle())
                && countHistoryValue(user, user.getSleepQuality())
                && hasValue(user.getTobaccoAlcoholUse());
    }

    private boolean preferencesComplete(AppUser user) {
        return hasValue(user.getNotificationPreference())
                && hasValue(user.getExportFormatPreference())
                && hasValue(user.getDataSharingPreference());
    }

    private boolean historyComplete(AppUser user) {
        return hasValue(user.getAllergies())
                && hasValue(user.getChronicConditions())
                && hasValue(user.getLifestyle())
                && hasValue(user.getMedications())
                && hasValue(user.getFamilyHistory())
                && hasValue(user.getMentalHealthHistory())
                && hasValue(user.getSleepQuality());
    }

    private boolean countHistoryValue(AppUser user, String value) {
        return user.getProfileSetupCompletedAt() != null
                ? hasValue(value)
                : hasValue(value) && !isLegacyGeneratedValue(value);
    }

    private String historyForResponse(AppUser user, String value) {
        return user.getProfileSetupCompletedAt() == null && isLegacyGeneratedValue(value) ? null : value;
    }

    private boolean isLegacyGeneratedValue(String value) {
        return "none".equalsIgnoreCase(value)
                || "no known allergies".equalsIgnoreCase(value)
                || "moderate activity".equalsIgnoreCase(value)
                || "not set".equalsIgnoreCase(value);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value, String fallback) {
        return hasValue(value) ? value.trim() : fallback;
    }

    private String cleanNullable(String value) {
        return hasValue(value) ? value.trim() : null;
    }
}
