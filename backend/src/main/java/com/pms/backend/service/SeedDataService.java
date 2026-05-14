package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.model.AdminRule;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.HealthQuestion;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AdminRuleRepository;
import com.pms.backend.repository.HealthQuestionRepository;
import com.pms.backend.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedDataService implements CommandLineRunner {
    private final UserRepository userRepository;
    private final HealthQuestionRepository questionRepository;
    private final AdminRuleRepository ruleRepository;
    private final AssessmentService assessmentService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${pms.demo.password:password123}")
    private String demoPassword;

    public SeedDataService(
            UserRepository userRepository,
            HealthQuestionRepository questionRepository,
            AdminRuleRepository ruleRepository,
            AssessmentService assessmentService
    ) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.ruleRepository = ruleRepository;
        this.assessmentService = assessmentService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        AppUser admin = makeUser("admin@example.com", "admin", "Demo Admin", Role.ADMIN);
        AppUser anaya = makeUser("user@example.com", "anaya", "Anaya Rao", Role.USER);
        AppUser rohan = makeUser("rohan@example.com", "rohan", "Rohan Mehta", Role.USER);
        userRepository.saveAll(List.of(admin, anaya, rohan));

        assessmentService.create(anaya, new AssessmentRequest("Fever and weakness", 6, 4, 100.4, 97, 88, "None"));
        assessmentService.create(anaya, new AssessmentRequest("Mild headache", 3, 1, 98.4, 99, 74, "None"));
        assessmentService.create(rohan, new AssessmentRequest("Chest pain with shortness of breath", 9, 1, 98.8, 93, 132, "Blood pressure"));

        seedQuestions();
        seedRules();
    }

    private AppUser makeUser(String email, String username, String fullName, Role role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setAge(role == Role.ADMIN ? 30 : 24);
        user.setGender(role == Role.ADMIN ? "Not applicable" : "Female");
        user.setHeightCm(role == Role.ADMIN ? 0 : 162.0);
        user.setWeightKg(role == Role.ADMIN ? 0 : 58.0);
        return user;
    }

    private void seedQuestions() {
        List<String[]> questions = List.of(
                new String[]{"cough", "How long have you had the cough?"},
                new String[]{"cough", "Is the cough dry or with mucus?"},
                new String[]{"fever", "What is the highest temperature you measured?"},
                new String[]{"headache", "Do you have vision changes or vomiting?"},
                new String[]{"chest pain", "Does the pain spread to arm, jaw, back, or shoulder?"}
        );
        for (String[] row : questions) {
            HealthQuestion question = new HealthQuestion();
            question.setSymptomKey(row[0]);
            question.setPrompt(row[1]);
            questionRepository.save(question);
        }
    }

    private void seedRules() {
        addRule("Chest pain + breathing difficulty", RiskLevel.HIGH, 90, "Red-flag symptom combination.");
        addRule("Fever more than 3 days + weakness", RiskLevel.MEDIUM, 55, "Duration and fatigue increase awareness risk.");
        addRule("Mild headache + no red flags", RiskLevel.LOW, 20, "No severe indicator in entered values.");
    }

    private void addRule(String label, RiskLevel level, int score, String explanation) {
        AdminRule rule = new AdminRule();
        rule.setConditionLabel(label);
        rule.setRiskLevel(level);
        rule.setScore(score);
        rule.setExplanation(explanation);
        ruleRepository.save(rule);
    }
}
