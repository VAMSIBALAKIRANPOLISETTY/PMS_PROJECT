package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.model.AdminRule;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.HealthQuestion;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AdminRuleRepository;
import com.pms.backend.repository.HealthQuestionRepository;
import com.pms.backend.repository.UserRepository;
import java.util.List;
import java.util.Map;
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
        if (userRepository.count() == 0) {
            AppUser admin = makeUser("admin@example.com", "admin", "Clinical Administrator", Role.ADMIN);
            AppUser anaya = makeUser("user@example.com", "anaya", "Anaya Rao", Role.USER);
            AppUser rohan = makeUser("rohan@example.com", "rohan", "Rohan Mehta", Role.USER);
            userRepository.saveAll(List.of(admin, anaya, rohan));

            completeSeedAssessment(anaya, new AssessmentRequest(List.of("Fever", "Weakness"), 6, 4, true, 100.4, "None"));
            completeSeedAssessment(anaya, new AssessmentRequest(List.of("Headache"), 3, 1, true, 98.4, "None"));
            completeSeedAssessment(rohan, new AssessmentRequest(List.of("Chest pain", "Shortness of breath"), 9, 1, false, null, "Blood pressure"));
        }
        normalizeAdministratorName();
        seedQuestions();
        seedRules();
    }

    private void completeSeedAssessment(AppUser user, AssessmentRequest request) {
        var draft = assessmentService.create(user, request);
        assessmentService.answerFollowUps(user, draft.id(), new FollowUpAnswerRequest(draft.followUpQuestions().stream()
                .map(question -> "No")
                .toList()));
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
        user.setFamilyHistory(role == Role.ADMIN ? "Not applicable" : "Diabetes in family");
        user.setMentalHealthHistory(role == Role.ADMIN ? "Not applicable" : "No psychiatric treatment history");
        user.setSleepQuality(role == Role.ADMIN ? "Not applicable" : "7 hours, regular");
        return user;
    }

    private void seedQuestions() {
        normalizeLegacyQuestions();
        List<String[]> questions = List.of(
                new String[]{"general", "Have your symptoms changed noticeably since they started?"},
                new String[]{"cough", "Has your cough continued for more than three days?"},
                new String[]{"cough", "Are you coughing up mucus?"},
                new String[]{"fever", "Have you measured a temperature above 100.4 F?"},
                new String[]{"headache", "Do you have vision changes or vomiting?"},
                new String[]{"chest pain", "Does the pain spread to your arm, jaw, back, or shoulder?"}
        );
        for (String[] row : questions) {
            if (questionRepository.existsBySymptomKeyIgnoreCaseAndPromptIgnoreCase(row[0], row[1])) {
                continue;
            }
            HealthQuestion question = new HealthQuestion();
            question.setSymptomKey(row[0]);
            question.setPrompt(row[1]);
            question.setInputType("choice");
            questionRepository.save(question);
        }
    }

    private void normalizeLegacyQuestions() {
        Map<String, String> replacements = Map.of(
                "How long have you had the cough?", "Has your cough continued for more than three days?",
                "Is the cough dry or with mucus?", "Are you coughing up mucus?",
                "What is the highest temperature you measured?", "Have you measured a temperature above 100.4 F?",
                "Does the pain spread to arm, jaw, back, or shoulder?", "Does the pain spread to your arm, jaw, back, or shoulder?"
        );
        questionRepository.findAll().forEach(question -> {
            String replacement = replacements.get(question.getPrompt());
            if (replacement != null) {
                question.setPrompt(replacement);
                question.setInputType("choice");
                questionRepository.save(question);
            }
        });
    }

    private void seedRules() {
        addRule("Chest pain + breathing difficulty", "Chest pain", "Breathing difficulty", null, null, null, 90, true, "Chest symptoms with breathing difficulty require urgent attention.");
        addRule("Fever more than 3 days + weakness", "Fever", "Weakness", null, 3, null, 55, false, "Duration and weakness increase awareness risk.");
        addRule("Mild headache + no red flags", "Headache", null, null, null, null, 20, false, "No severe indicator in entered values.");
        addRule("Severe weakness", "Severe weakness", null, null, null, null, 85, true, "Severe weakness can need urgent medical attention.");
        addRule("Confusion", "Confusion", null, null, null, null, 85, true, "Confusion can be a red-flag symptom.");
        addRule("Fainting", "Fainting", null, null, null, null, 85, true, "Fainting can need urgent medical attention.");
        addRule("Seizure", "Seizure", null, null, null, null, 90, true, "Seizures require urgent medical attention.");
        addRule("Heavy bleeding", "Heavy bleeding", null, null, null, null, 90, true, "Heavy bleeding requires urgent medical attention.");
    }

    private void addRule(
            String label,
            String primarySymptom,
            String secondarySymptom,
            Integer minSeverity,
            Integer minDurationDays,
            String chronicConditionKeyword,
            int score,
            boolean urgent,
            String explanation
    ) {
        AdminRule rule = ruleRepository.findByConditionLabelIgnoreCase(label).orElseGet(AdminRule::new);
        rule.setConditionLabel(label);
        rule.setPrimarySymptom(primarySymptom);
        rule.setSecondarySymptom(secondarySymptom);
        rule.setMinSeverity(minSeverity);
        rule.setMinDurationDays(minDurationDays);
        rule.setChronicConditionKeyword(chronicConditionKeyword);
        rule.setRiskLevel(score >= 70 ? RiskLevel.HIGH : score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW);
        rule.setScore(score);
        rule.setUrgent(urgent);
        rule.setActive(true);
        rule.setExplanation(explanation);
        ruleRepository.save(rule);
    }

    private void normalizeAdministratorName() {
        userRepository.findByUsernameIgnoreCase("admin").ifPresent(admin -> {
            if ("Demo Admin".equalsIgnoreCase(admin.getFullName())) {
                admin.setFullName("Clinical Administrator");
                userRepository.save(admin);
            }
        });
    }
}
