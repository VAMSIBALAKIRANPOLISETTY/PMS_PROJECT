package com.pms.backend.controller;

import com.pms.backend.config.OpenApiConfig;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.dto.AssessmentDtos.ReportFollowUpRequest;
import com.pms.backend.dto.AssessmentDtos.ReportFollowUpResponse;
import com.pms.backend.dto.AssessmentDtos.ReportInsightRequest;
import com.pms.backend.dto.AssessmentDtos.ReportInsightResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AssessmentService;
import com.pms.backend.service.AiInsightService;
import com.pms.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report follow-up prompts and report-based care-preparation insight generation.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ReportInsightController {
    private final AuthService authService;
    private final AiInsightService aiInsightService;
    private final AssessmentService assessmentService;

    public ReportInsightController(
            AuthService authService,
            AiInsightService aiInsightService,
            AssessmentService assessmentService
    ) {
        this.authService = authService;
        this.aiInsightService = aiInsightService;
        this.assessmentService = assessmentService;
    }

    @Operation(summary = "Upload report assessment", description = "Stores a readable PDF/text report as a pending report assessment and returns follow-up questions.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssessmentResponse uploadReport(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "reportText", required = false) String reportText,
            @RequestParam(value = "includeConnectedHealth", required = false) Boolean includeConnectedHealth,
            @RequestParam(value = "connectedHealthRecordIds", required = false) List<Long> connectedHealthRecordIds
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.uploadReport(user, file, reportText, includeConnectedHealth, connectedHealthRecordIds);
    }

    @Operation(summary = "Finalize report assessment", description = "Saves answered report follow-ups and returns the completed report-based care-preparation guide.")
    @PostMapping("/{assessmentId}/follow-ups")
    public AssessmentResponse answerReportFollowUps(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long assessmentId,
            @Valid @RequestBody FollowUpAnswerRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.answerReportFollowUps(user, assessmentId, request);
    }

    @Operation(summary = "Finalize report assessment", description = "Alias for report follow-up finalization.")
    @PostMapping("/{assessmentId}/finalize")
    public AssessmentResponse finalizeReport(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long assessmentId,
            @Valid @RequestBody FollowUpAnswerRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.answerReportFollowUps(user, assessmentId, request);
    }

    @Operation(summary = "List completed report assessments", description = "Returns saved report-based assessments for patient history or staff review.")
    @GetMapping("/history")
    public List<AssessmentResponse> reportHistory(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        AppUser user = authService.requireUser(authHeader);
        return assessmentService.reportHistoryFor(user);
    }

    @Operation(summary = "Generate report follow-ups", description = "Returns follow-up questions that add context before a report care-preparation guide is generated.")
    @PostMapping("/follow-ups")
    public ReportFollowUpResponse followUps(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReportFollowUpRequest request
    ) {
        authService.requireUser(authHeader);
        AiInsightService.QuestionSet questions = aiInsightService.reportFollowUps(request.reportName());
        return new ReportFollowUpResponse(request.reportName(), questions.questions(), questions.aiMode());
    }

    @Operation(summary = "Generate report insight", description = "Creates a structured, non-diagnostic care-preparation guide from report notes and answered follow-ups.")
    @PostMapping("/insight")
    public ReportInsightResponse insight(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReportInsightRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        AiInsightService.CarePrepInsight insight = aiInsightService.forReport(user, request.reportName(), request.reportText(), request.answers());
        return new ReportInsightResponse(
                request.reportName(),
                aiInsightService.reportFollowUps(request.reportName()).questions(),
                request.answers(),
                insight.careSummary(),
                insight.explanation(),
                insight.possibleDirections(),
                insight.urgentWarning(),
                insight.monitoringPlan(),
                insight.careTips(),
                insight.doctorPrepQuestions(),
                insight.trustedSourceLinks(),
                insight.aiMode()
        );
    }
}
