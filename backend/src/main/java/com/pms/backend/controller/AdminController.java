package com.pms.backend.controller;

import com.pms.backend.dto.AdminDtos.ActiveRequest;
import com.pms.backend.dto.AdminDtos.QuestionRequest;
import com.pms.backend.dto.AdminDtos.QuestionResponse;
import com.pms.backend.dto.AdminDtos.RuleRequest;
import com.pms.backend.dto.AdminDtos.RuleResponse;
import com.pms.backend.dto.AssessmentDtos.AnalyticsResponse;
import com.pms.backend.model.AdminRule;
import com.pms.backend.model.HealthQuestion;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.repository.AdminRuleRepository;
import com.pms.backend.repository.HealthQuestionRepository;
import com.pms.backend.service.AnalyticsService;
import com.pms.backend.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final AnalyticsService analyticsService;
    private final HealthQuestionRepository questionRepository;
    private final AdminRuleRepository ruleRepository;

    public AdminController(
            AuthService authService,
            AnalyticsService analyticsService,
            HealthQuestionRepository questionRepository,
            AdminRuleRepository ruleRepository
    ) {
        this.authService = authService;
        this.analyticsService = analyticsService;
        this.questionRepository = questionRepository;
        this.ruleRepository = ruleRepository;
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics(@RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return analyticsService.getAnalytics();
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions(@RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return questionRepository.findAllByOrderBySymptomKeyAsc().stream().map(this::toQuestionResponse).toList();
    }

    @PostMapping("/questions")
    public QuestionResponse addQuestion(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody QuestionRequest request) {
        authService.requireAdmin(authHeader);
        HealthQuestion question = new HealthQuestion();
        question.setSymptomKey(request.symptomKey().trim());
        question.setPrompt(request.prompt().trim());
        question.setInputType("choice");
        question.setActive(request.active() == null || request.active());
        return toQuestionResponse(questionRepository.save(question));
    }

    @PatchMapping("/questions/{questionId}/active")
    public QuestionResponse updateQuestionActive(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long questionId,
            @Valid @RequestBody ActiveRequest request
    ) {
        authService.requireAdmin(authHeader);
        HealthQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment question not found."));
        question.setActive(request.active());
        return toQuestionResponse(questionRepository.save(question));
    }

    @GetMapping("/rules")
    public List<RuleResponse> rules(@RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return ruleRepository.findAll().stream().map(this::toRuleResponse).toList();
    }

    @PostMapping("/rules")
    public RuleResponse addRule(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody RuleRequest request) {
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

    @PatchMapping("/rules/{ruleId}/active")
    public RuleResponse updateRuleActive(
            @RequestHeader("Authorization") String authHeader,
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
