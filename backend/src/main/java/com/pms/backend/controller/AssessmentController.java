package com.pms.backend.controller;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AssessmentService;
import com.pms.backend.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {
    private final AuthService authService;
    private final AssessmentService assessmentService;

    public AssessmentController(AuthService authService, AssessmentService assessmentService) {
        this.authService = authService;
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<AssessmentResponse> list(@RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.listFor(user);
    }

    @PostMapping
    public AssessmentResponse create(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AssessmentRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.create(user, request);
    }
}
