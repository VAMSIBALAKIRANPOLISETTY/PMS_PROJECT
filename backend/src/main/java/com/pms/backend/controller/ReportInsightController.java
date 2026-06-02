package com.pms.backend.controller;

import com.pms.backend.dto.AssessmentDtos.ReportFollowUpRequest;
import com.pms.backend.dto.AssessmentDtos.ReportFollowUpResponse;
import com.pms.backend.dto.AssessmentDtos.ReportInsightRequest;
import com.pms.backend.dto.AssessmentDtos.ReportInsightResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AiInsightService;
import com.pms.backend.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportInsightController {
    private final AuthService authService;
    private final AiInsightService aiInsightService;

    public ReportInsightController(AuthService authService, AiInsightService aiInsightService) {
        this.authService = authService;
        this.aiInsightService = aiInsightService;
    }

    @PostMapping("/follow-ups")
    public ReportFollowUpResponse followUps(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReportFollowUpRequest request
    ) {
        authService.requireUser(authHeader);
        List<String> questions = aiInsightService.reportFollowUps(request.reportName());
        return new ReportFollowUpResponse(request.reportName(), questions, "MOCK");
    }

    @PostMapping("/insight")
    public ReportInsightResponse insight(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ReportInsightRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        AiInsightService.CarePrepInsight insight = aiInsightService.forReport(user, request.reportName(), request.reportText(), request.answers());
        return new ReportInsightResponse(
                request.reportName(),
                aiInsightService.reportFollowUps(request.reportName()),
                request.answers(),
                insight.careSummary(),
                insight.explanation(),
                insight.possibleDirections(),
                insight.urgentWarning(),
                insight.monitoringPlan(),
                insight.doctorPrepQuestions(),
                insight.trustedSourceLinks(),
                insight.aiMode()
        );
    }
}
