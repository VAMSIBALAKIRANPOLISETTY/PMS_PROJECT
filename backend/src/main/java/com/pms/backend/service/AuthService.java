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
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse register(RegisterRequest request) {
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
        user.setRole(request.role() == Role.ADMIN ? Role.ADMIN : Role.USER);
        user.setAge(request.age());
        user.setGender(clean(request.sex(), "Not set"));
        user.setHeightCm(request.heightCm());
        user.setWeightKg(request.weightKg());
        userRepository.save(user);
        return makeAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = findByEmailOrUsername(request.identifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email/username or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email/username or password.");
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
                user.getAllergies(),
                user.getChronicConditions(),
                user.getLifestyle(),
                user.getMedications(),
                user.getFamilyHistory(),
                user.getMentalHealthHistory(),
                user.getSleepQuality(),
                profileCompletion(user)
        );
    }

    public UserResponse updateProfile(AppUser user, ProfileUpdateRequest request) {
        user.setFullName(request.fullName().trim());
        user.setAge(request.age());
        user.setHeightCm(request.heightCm());
        user.setWeightKg(request.weightKg());
        user.setGender(clean(request.sex(), "Not set"));
        user.setAllergies(clean(request.allergies(), "No known allergies"));
        user.setChronicConditions(clean(request.chronicConditions(), "None"));
        user.setLifestyle(clean(request.lifestyle(), "Moderate activity"));
        user.setMedications(clean(request.medications(), "None"));
        user.setFamilyHistory(clean(request.familyHistory(), "Not set"));
        user.setMentalHealthHistory(clean(request.mentalHealthHistory(), "Not set"));
        user.setSleepQuality(clean(request.sleepQuality(), "Not set"));
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
        if (hasValue(user.getAllergies())) completed++;
        if (hasValue(user.getChronicConditions())) completed++;
        if (hasValue(user.getLifestyle())) completed++;
        if (hasValue(user.getMedications())) completed++;
        if (hasValue(user.getFamilyHistory()) && !"not set".equalsIgnoreCase(user.getFamilyHistory())) completed++;
        if (hasValue(user.getMentalHealthHistory()) && !"not set".equalsIgnoreCase(user.getMentalHealthHistory())) completed++;
        if (hasValue(user.getSleepQuality()) && !"not set".equalsIgnoreCase(user.getSleepQuality())) completed++;
        return Math.round((completed * 100f) / total);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value, String fallback) {
        return hasValue(value) ? value.trim() : fallback;
    }
}
