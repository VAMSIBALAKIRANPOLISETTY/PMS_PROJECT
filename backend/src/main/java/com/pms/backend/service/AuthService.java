package com.pms.backend.service;

import com.pms.backend.dto.AuthDtos.AuthResponse;
import com.pms.backend.dto.AuthDtos.LoginRequest;
import com.pms.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.dto.AuthDtos.UserResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Role;
import com.pms.backend.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    static final String PRIVACY_NOTICE_VERSION = "2026-06-02";
    static final String TERMS_VERSION = "2026-06-02";
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Login required.");
        }
        String token = authHeader.substring("Bearer ".length());
        Long userId = tokenStore.get(token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid or expired token.");
        }
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
        user.setProfileSetupCompletedAt(historyComplete(user) ? LocalDateTime.now() : null);
        return toUserResponse(userRepository.save(user));
    }

    private Optional<AppUser> findByEmailOrUsername(String identifier) {
        String value = identifier.trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(value)
                .or(() -> userRepository.findByUsernameIgnoreCase(value));
    }

    private AuthResponse makeAuthResponse(AppUser user) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user.getId());
        return new AuthResponse(token, toUserResponse(user));
    }

    private int profileCompletion(AppUser user) {
        int completed = 0;
        int total = 12;
        if (hasValue(user.getFullName())) completed++;
        if (user.getAge() != null && user.getAge() > 0) completed++;
        if (user.getHeightCm() != null && user.getHeightCm() > 0) completed++;
        if (user.getWeightKg() != null && user.getWeightKg() > 0) completed++;
        if (hasValue(user.getGender()) && !"not set".equalsIgnoreCase(user.getGender())) completed++;
        if (countHistoryValue(user, user.getAllergies())) completed++;
        if (countHistoryValue(user, user.getChronicConditions())) completed++;
        if (countHistoryValue(user, user.getLifestyle())) completed++;
        if (countHistoryValue(user, user.getMedications())) completed++;
        if (countHistoryValue(user, user.getFamilyHistory())) completed++;
        if (countHistoryValue(user, user.getMentalHealthHistory())) completed++;
        if (countHistoryValue(user, user.getSleepQuality())) completed++;
        return Math.round((completed * 100f) / total);
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
