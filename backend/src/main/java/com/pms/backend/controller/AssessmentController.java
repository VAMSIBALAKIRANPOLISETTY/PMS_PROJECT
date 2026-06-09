package com.pms.backend.controller;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.config.OpenApiConfig;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AssessmentService;
import com.pms.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments")
@Tag(name = "Assessments", description = "Patient assessment drafts, follow-up finalization, completed history, and draft discard operations.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AssessmentController {
    private final AuthService authService;
    private final AssessmentService assessmentService;

    public AssessmentController(AuthService authService, AssessmentService assessmentService) {
        this.authService = authService;
        this.assessmentService = assessmentService;
    }

    @Operation(summary = "List completed assessments", description = "Returns completed assessment history for the authenticated patient, or completed records for staff review.")
    @GetMapping
    public List<AssessmentResponse> list(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.listFor(user);
    }

    @Operation(summary = "Resume pending draft", description = "Returns the latest unfinished assessment draft for the authenticated patient, when one exists.")
    @GetMapping("/pending")
    public AssessmentResponse pending(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.pendingFor(user);
    }

    @Operation(summary = "Create assessment draft", description = "Stores assessment intake as a pending draft and returns required follow-up questions before the care guide is finalized.")
    @PostMapping
    public AssessmentResponse create(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AssessmentRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.create(user, request);
    }

    @Operation(summary = "Submit follow-up answers", description = "Finalizes a pending assessment after every follow-up question has an answer and returns the completed care-preparation guide.")
    @PostMapping("/{assessmentId}/follow-ups")
    public AssessmentResponse answerFollowUps(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long assessmentId,
            @Valid @RequestBody FollowUpAnswerRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.answerFollowUps(user, assessmentId, request);
    }

    @Operation(summary = "Discard unfinished draft", description = "Deletes a patient-owned pending assessment draft before it appears in completed history.")
    @DeleteMapping("/{assessmentId}/draft")
    public void discardDraft(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long assessmentId
    ) {
        AppUser user = authService.requireUser(authHeader);
        assessmentService.discardDraft(user, assessmentId);
    }

    @Operation(summary = "Export completed assessment", description = "Returns a printable PDF summary for a completed symptom or report assessment.")
    @GetMapping("/{assessmentId}/export")
    public ResponseEntity<byte[]> exportAssessment(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long assessmentId
    ) {
        AppUser user = authService.requireUser(authHeader);
        byte[] pdf = assessmentService.exportAssessment(user, assessmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pms-assessment-" + assessmentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
