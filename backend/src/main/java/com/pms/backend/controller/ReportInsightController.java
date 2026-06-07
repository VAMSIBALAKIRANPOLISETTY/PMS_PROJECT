package com.pms.backend.controller;

import com.pms.backend.config.OpenApiConfig;
import com.pms.backend.dto.AssessmentDtos.ReportFollowUpRequest;
import com.pms.backend.dto.AssessmentDtos.ReportFollowUpResponse;
import com.pms.backend.dto.AssessmentDtos.ReportInsightRequest;
import com.pms.backend.dto.AssessmentDtos.ReportInsightResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AiInsightService;
import com.pms.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Report follow-up prompts and report-based care-preparation insight generation.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ReportInsightController {
    private final AuthService authService;
    private final AiInsightService aiInsightService;

    public ReportInsightController(AuthService authService, AiInsightService aiInsightService) {
        this.authService = authService;
        this.aiInsightService = aiInsightService;
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
