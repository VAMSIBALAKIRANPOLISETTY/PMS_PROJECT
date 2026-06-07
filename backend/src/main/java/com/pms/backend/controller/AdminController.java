package com.pms.backend.controller;

import com.pms.backend.config.OpenApiConfig;
import com.pms.backend.dto.AdminDtos.ActiveRequest;
import com.pms.backend.dto.AdminDtos.QuestionRequest;
import com.pms.backend.dto.AdminDtos.QuestionResponse;
import com.pms.backend.dto.AdminDtos.QuestionSuggestionRequest;
import com.pms.backend.dto.AdminDtos.QuestionSuggestionResponse;
import com.pms.backend.dto.AdminDtos.RuleRequest;
import com.pms.backend.dto.AdminDtos.RuleResponse;
import com.pms.backend.dto.AssessmentDtos.AnalyticsResponse;
import com.pms.backend.model.AdminRule;
import com.pms.backend.model.HealthQuestion;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.repository.AdminRuleRepository;
import com.pms.backend.repository.HealthQuestionRepository;
import com.pms.backend.service.AiInsightService;
import com.pms.backend.service.AnalyticsService;
import com.pms.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Clinical operations endpoints for staff-only analytics, operational safety rules, and managed questions.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminController {
    private final AuthService authService;
    private final AnalyticsService analyticsService;
    private final HealthQuestionRepository questionRepository;
    private final AdminRuleRepository ruleRepository;
    private final AiInsightService aiInsightService;

    public AdminController(
            AuthService authService,
            AnalyticsService analyticsService,
            HealthQuestionRepository questionRepository,
            AdminRuleRepository ruleRepository,
            AiInsightService aiInsightService
    ) {
        this.authService = authService;
        this.analyticsService = analyticsService;
        this.questionRepository = questionRepository;
        this.ruleRepository = ruleRepository;
        this.aiInsightService = aiInsightService;
    }

    @Operation(tags = {"Admin Analytics"}, summary = "Get clinical operations analytics", description = "Returns staff-only totals, risk mix, and common completed-assessment symptom counts.")
    @GetMapping("/analytics")
    public AnalyticsResponse analytics(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return analyticsService.getAnalytics();
    }

    @Operation(tags = {"Admin Questions"}, summary = "List managed assessment questions", description = "Returns active and inactive follow-up prompts that staff can manage for future assessment drafts.")
    @GetMapping("/questions")
    public List<QuestionResponse> questions(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return questionRepository.findAllByOrderBySymptomKeyAsc().stream().map(this::toQuestionResponse).toList();
    }

    @Operation(tags = {"Admin Questions"}, summary = "Create managed assessment question", description = "Creates a Yes / No / Not sure compatible question for a symptom or General category.")
    @PostMapping("/questions")
    public QuestionResponse addQuestion(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader, @Valid @RequestBody QuestionRequest request) {
        authService.requireAdmin(authHeader);
        HealthQuestion question = new HealthQuestion();
        question.setSymptomKey(request.symptomKey().trim());
        question.setPrompt(request.prompt().trim());
        question.setInputType("choice");
        question.setActive(request.active() == null || request.active());
        return toQuestionResponse(questionRepository.save(question));
    }

    @Operation(tags = {"Admin Questions"}, summary = "Suggest managed assessment questions with AI", description = "Returns inactive draft prompts for staff review. Suggestions are not saved until staff creates selected questions.")
    @PostMapping("/questions/suggest")
    public QuestionSuggestionResponse suggestQuestions(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody QuestionSuggestionRequest request
    ) {
        authService.requireAdmin(authHeader);
        AiInsightService.QuestionSet suggestions = aiInsightService.suggestQuestions(request.symptomKey(), request.focus());
        return new QuestionSuggestionResponse(request.symptomKey().trim(), suggestions.questions(), suggestions.aiMode());
    }

    @Operation(tags = {"Admin Questions"}, summary = "Activate or pause a managed question", description = "Changes whether a question can join future assessment drafts.")
    @PatchMapping("/questions/{questionId}/active")
    public QuestionResponse updateQuestionActive(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long questionId,
            @Valid @RequestBody ActiveRequest request
    ) {
        authService.requireAdmin(authHeader);
        HealthQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment question not found."));
        question.setActive(request.active());
        return toQuestionResponse(questionRepository.save(question));
    }

    @Operation(tags = {"Admin Rules"}, summary = "List operational safety rules", description = "Returns active and inactive upward-only staff rules for future assessment review.")
    @GetMapping("/rules")
    public List<RuleResponse> rules(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return ruleRepository.findAll().stream().map(this::toRuleResponse).toList();
    }

    @Operation(tags = {"Admin Rules"}, summary = "Create operational safety rule", description = "Creates an additive rule that may raise a future assessment score floor when all configured conditions match.")
    @PostMapping("/rules")
    public RuleResponse addRule(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader, @Valid @RequestBody RuleRequest request) {
        authService.requireAdmin(authHeader);
        AdminRule rule = new AdminRule();
        rule.setConditionLabel(request.conditionLabel().trim());
        rule.setPrimarySymptom(request.primarySymptom().trim());
        rule.setSecondarySymptom(cleanNullable(request.secondarySymptom()));
        rule.setMinSeverity(request.minSeverity());
        rule.setMinDurationDays(request.minDurationDays());
        rule.setChronicConditionKeyword(cleanNullable(request.chronicConditionKeyword()));
        rule.setScore(request.score());
        rule.setRiskLevel(riskLevelFor(request.score()));
        rule.setUrgent(Boolean.TRUE.equals(request.urgent()));
        rule.setActive(request.active() == null || request.active());
        rule.setExplanation(request.explanation().trim());
        return toRuleResponse(ruleRepository.save(rule));
    }

    @Operation(tags = {"Admin Rules"}, summary = "Activate or pause an operational rule", description = "Changes whether a staff-created rule affects future assessments. Protected built-in urgent safeguards remain active.")
    @PatchMapping("/rules/{ruleId}/active")
    public RuleResponse updateRuleActive(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long ruleId,
            @Valid @RequestBody ActiveRequest request
    ) {
        authService.requireAdmin(authHeader);
        AdminRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Safety rule not found."));
        rule.setActive(request.active());
        return toRuleResponse(ruleRepository.save(rule));
    }

    private QuestionResponse toQuestionResponse(HealthQuestion item) {
        return new QuestionResponse(item.getId(), item.getSymptomKey(), item.getPrompt(), item.getInputType(), item.isActive());
    }

    private RuleResponse toRuleResponse(AdminRule item) {
        return new RuleResponse(
                item.getId(),
                item.getConditionLabel(),
                item.getPrimarySymptom(),
                item.getSecondarySymptom(),
                item.getMinSeverity(),
                item.getMinDurationDays(),
                item.getChronicConditionKeyword(),
                item.getRiskLevel(),
                item.getScore(),
                item.isUrgent(),
                item.isActive(),
                item.getExplanation()
        );
    }

    private RiskLevel riskLevelFor(int score) {
        return score >= 70 ? RiskLevel.HIGH : score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    private String cleanNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
