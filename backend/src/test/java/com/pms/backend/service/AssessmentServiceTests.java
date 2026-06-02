package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.AdminRule;
import com.pms.backend.model.AssessmentStatus;
import com.pms.backend.model.HealthQuestion;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AdminRuleRepository;
import com.pms.backend.repository.HealthQuestionRepository;
import com.pms.backend.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AssessmentServiceTests {
    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRuleRepository ruleRepository;

    @Autowired
    private HealthQuestionRepository questionRepository;

    @Test
    void intakeCreatesResumableDraftWithoutCareGuideAndFinalizesAfterEveryAnswer() {
        AppUser user = patient();
        long completedBefore = analyticsService.getAnalytics().totalAssessments();

        var draft = assessmentService.create(user,
                new AssessmentRequest(List.of("Fever", "Weakness"), 6, 3, false, null, "None"));

        assertEquals(AssessmentStatus.PENDING_FOLLOW_UP, draft.status());
        assertNull(draft.careSummary());
        assertEquals(draft.id(), assessmentService.pendingFor(user).id());
        assertEquals(completedBefore, analyticsService.getAnalytics().totalAssessments());

        var incomplete = new FollowUpAnswerRequest(List.of("No"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> assessmentService.answerFollowUps(user, draft.id(), incomplete));
        assertTrue(error.getMessage().contains("every follow-up"));

        var completed = assessmentService.answerFollowUps(user, draft.id(),
                new FollowUpAnswerRequest(draft.followUpQuestions().stream().map(question -> "No").toList()));

        assertEquals(AssessmentStatus.COMPLETED, completed.status());
        assertNotNull(completed.careSummary());
        assertNull(assessmentService.pendingFor(user));
        assertEquals(completedBefore + 1, analyticsService.getAnalytics().totalAssessments());
    }

    @Test
    void redFlagDraftShowsUrgentWarningBeforeCareGuide() {
        AppUser user = patient();

        var draft = assessmentService.create(user,
                new AssessmentRequest(List.of("Chest pressure", "Breathing difficulty"), 8, 1, false, null, "None"));

        assertEquals(AssessmentStatus.PENDING_FOLLOW_UP, draft.status());
        assertNotNull(draft.urgentWarning());
        assertNull(draft.careSummary());
    }

    @Test
    void patientCanDiscardOwnDraft() {
        AppUser user = patient();
        var draft = assessmentService.create(user,
                new AssessmentRequest(List.of("Headache"), 3, 1, false, null, "None"));

        assessmentService.discardDraft(user, draft.id());

        assertNull(assessmentService.pendingFor(user));
    }

    @Test
    void activeOperationalRuleRaisesScoreFloorWithoutReplacingBuiltInSafety() {
        AdminRule rule = rule("Palpitations review", "Palpitations", 68, true);
        ruleRepository.save(rule);

        var draft = assessmentService.create(patient(),
                new AssessmentRequest(List.of("Palpitations"), 3, 1, false, null, "None"));

        assertTrue(draft.riskScore() >= 85);
        assertNotNull(draft.urgentWarning());
        assertTrue(draft.reasons().stream().anyMatch(reason -> reason.contains("Palpitations review")));
    }

    @Test
    void inactiveOperationalRuleDoesNotChangeAssessment() {
        AdminRule rule = rule("Inactive joint review", "Joint pain", 92, false);
        ruleRepository.save(rule);

        var draft = assessmentService.create(patient(),
                new AssessmentRequest(List.of("Joint pain"), 2, 1, false, null, "None"));

        assertTrue(draft.riskScore() < 92);
        assertNull(draft.urgentWarning());
    }

    @Test
    void activeManagedQuestionsJoinMatchingAssessmentDrafts() {
        HealthQuestion active = question("Palpitations", "Do your palpitations happen while resting?", true);
        HealthQuestion inactive = question("Palpitations", "This inactive question should not appear?", false);
        questionRepository.saveAll(List.of(active, inactive));

        var draft = assessmentService.create(patient(),
                new AssessmentRequest(List.of("Palpitations"), 2, 1, false, null, "None"));

        assertTrue(draft.followUpQuestions().contains(active.getPrompt()));
        assertFalse(draft.followUpQuestions().contains(inactive.getPrompt()));
        assertTrue(draft.followUpQuestions().size() >= 4);
        assertTrue(draft.followUpQuestions().size() <= 7);
    }

    private AdminRule rule(String label, String symptom, int score, boolean active) {
        AdminRule rule = new AdminRule();
        rule.setConditionLabel(label);
        rule.setPrimarySymptom(symptom);
        rule.setRiskLevel(score >= 70 ? RiskLevel.HIGH : score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW);
        rule.setScore(score);
        rule.setUrgent(active);
        rule.setActive(active);
        rule.setExplanation("Operational review test.");
        return rule;
    }

    private HealthQuestion question(String symptom, String prompt, boolean active) {
        HealthQuestion question = new HealthQuestion();
        question.setSymptomKey(symptom);
        question.setPrompt(prompt);
        question.setInputType("choice");
        question.setActive(active);
        return question;
    }

    private AppUser patient() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        AppUser user = new AppUser();
        user.setEmail("patient-" + suffix + "@example.com");
        user.setUsername("patient" + suffix);
        user.setFullName("Assessment Patient");
        user.setPasswordHash("test-only");
        user.setRole(Role.USER);
        user.setAge(30);
        user.setGender("Prefer not to say");
        user.setHeightCm(170.0);
        user.setWeightKg(68.0);
        return userRepository.save(user);
    }
}
