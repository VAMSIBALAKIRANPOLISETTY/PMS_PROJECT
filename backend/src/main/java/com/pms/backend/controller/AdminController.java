package com.pms.backend.controller;

import com.pms.backend.dto.AdminDtos.QuestionResponse;
import com.pms.backend.dto.AdminDtos.RuleRequest;
import com.pms.backend.dto.AdminDtos.RuleResponse;
import com.pms.backend.dto.AssessmentDtos.AnalyticsResponse;
import com.pms.backend.model.AdminRule;
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
        return questionRepository.findByActiveTrueOrderBySymptomKeyAsc().stream()
                .map(item -> new QuestionResponse(item.getId(), item.getSymptomKey(), item.getPrompt(), item.getInputType(), item.isActive()))
                .toList();
    }

    @GetMapping("/rules")
    public List<RuleResponse> rules(@RequestHeader("Authorization") String authHeader) {
        authService.requireAdmin(authHeader);
        return ruleRepository.findAll().stream()
                .map(item -> new RuleResponse(item.getId(), item.getConditionLabel(), item.getRiskLevel(), item.getScore(), item.getExplanation()))
                .toList();
    }

    @PostMapping("/rules")
    public RuleResponse addRule(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody RuleRequest request) {
        authService.requireAdmin(authHeader);
        AdminRule rule = new AdminRule();
        rule.setConditionLabel(request.conditionLabel());
        rule.setRiskLevel(request.riskLevel());
        rule.setScore(request.score());
        rule.setExplanation(request.explanation());
        AdminRule saved = ruleRepository.save(rule);
        return new RuleResponse(saved.getId(), saved.getConditionLabel(), saved.getRiskLevel(), saved.getScore(), saved.getExplanation());
    }
}
