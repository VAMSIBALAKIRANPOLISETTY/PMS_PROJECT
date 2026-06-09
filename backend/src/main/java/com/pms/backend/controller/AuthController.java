package com.pms.backend.controller;

import com.pms.backend.dto.AuthDtos.AuthResponse;
import com.pms.backend.dto.AuthDtos.LoginRequest;
import com.pms.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.dto.AuthDtos.UserResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AuthService;
import com.pms.backend.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Patient registration, patient login, staff login, and current-account profile access.")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register an adult patient", description = "Creates a patient account after account details, privacy acknowledgment, and terms acceptance are completed.")
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Patient login", description = "Authenticates patient accounts only. Staff accounts must use the Staff login endpoint.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Staff login", description = "Authenticates staff accounts only for the clinical operations workspace.")
    @PostMapping("/staff-login")
    public AuthResponse staffLogin(@Valid @RequestBody LoginRequest request) {
        return authService.staffLogin(request);
    }

    @Operation(summary = "Get current account", description = "Returns the profile attached to the supplied bearer token.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me")
    public UserResponse me(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return authService.toUserResponse(user);
    }

    @Operation(summary = "Update patient profile", description = "Updates profile basics and health-history setup fields for the authenticated account.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/profile")
    public UserResponse updateProfile(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return authService.updateProfile(user, request);
    }

    @Operation(summary = "Update patient profile photo", description = "Stores a patient-owned JPG, PNG, or WebP profile image for display across patient and staff review screens.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping(value = "/profile-photo", consumes = "multipart/form-data")
    public UserResponse updateProfilePhoto(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file
    ) {
        AppUser user = authService.requireUser(authHeader);
        return authService.updateProfilePhoto(user, file);
    }
}
