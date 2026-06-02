package com.pms.backend.controller;

import com.pms.backend.dto.AuthDtos.AuthResponse;
import com.pms.backend.dto.AuthDtos.LoginRequest;
import com.pms.backend.dto.AuthDtos.ProfileUpdateRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.dto.AuthDtos.UserResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return authService.toUserResponse(user);
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return authService.updateProfile(user, request);
    }
}
