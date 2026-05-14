package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.model.RiskLevel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {
    public RiskResult calculate(AssessmentRequest request) {
        String symptom = request.mainSymptom().toLowerCase();
        int score = 15;
        List<String> reasons = new ArrayList<>();

        if (request.severity() >= 8) {
            score += 25;
            reasons.add("Severity is high based on the selected 1 to 10 scale.");
        } else if (request.severity() >= 5) {
            score += 12;
            reasons.add("Severity is moderate and should be watched.");
        }

        if (request.durationDays() >= 7) {
            score += 22;
            reasons.add("Symptoms have continued for one week or more.");
        } else if (request.durationDays() >= 3) {
            score += 14;
            reasons.add("Symptoms have lasted more than three days.");
        }

        if (request.temperatureF() >= 103.0) {
            score += 32;
            reasons.add("Temperature is very high.");
        } else if (request.temperatureF() >= 100.4) {
            score += 14;
            reasons.add("Temperature is above normal range.");
        }

        if (request.oxygenLevel() < 92) {
            score += 38;
            reasons.add("Oxygen level is below the safe demo threshold.");
        } else if (request.oxygenLevel() < 95) {
            score += 18;
            reasons.add("Oxygen level is lower than expected.");
        }

        if (request.heartRate() > 130 || request.heartRate() < 45) {
            score += 24;
            reasons.add("Heart rate is outside the expected demo range.");
        }

        if (symptom.contains("chest pain") && (symptom.contains("breath") || symptom.contains("shortness"))) {
            score += 45;
            reasons.add("Chest pain with breathing difficulty is treated as a red flag.");
        }

        if (symptom.contains("dizziness") && request.chronicCondition() != null
                && request.chronicCondition().toLowerCase().contains("diabetes")) {
            score += 20;
            reasons.add("Dizziness with diabetes history needs extra follow-up.");
        }

        if (reasons.isEmpty()) {
            reasons.add("No major red-flag indicator was detected from the provided values.");
        }

        score = Math.min(score, 100);
        RiskLevel level = score >= 70 ? RiskLevel.HIGH : score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW;

        List<String> followUps = followUpsFor(symptom, level);
        List<String> suggestions = suggestionsFor(level);
        return new RiskResult(score, level, reasons, followUps, suggestions);
    }

    private List<String> followUpsFor(String symptom, RiskLevel level) {
        List<String> questions = new ArrayList<>();
        if (symptom.contains("cough")) {
            questions.add("Is your cough dry or with mucus?");
            questions.add("Do you have chest pain while coughing?");
            questions.add("Do you feel shortness of breath?");
        }
        if (symptom.contains("fever")) {
            questions.add("What was the highest temperature you measured?");
            questions.add("Do you have chills, weakness, or body pain?");
            questions.add("Did the fever reduce after rest or fluids?");
        }
        if (symptom.contains("headache")) {
            questions.add("Is the headache sudden or gradual?");
            questions.add("Do you have vomiting, vision changes, or neck stiffness?");
        }
        if (symptom.contains("chest pain")) {
            questions.add("Does the pain spread to arm, jaw, back, or shoulder?");
            questions.add("Do you have sweating, nausea, or breathing difficulty?");
        }
        if (symptom.contains("dizziness")) {
            questions.add("Did you faint or nearly faint?");
            questions.add("Have you checked blood sugar or blood pressure recently?");
        }
        if (questions.isEmpty() || level == RiskLevel.HIGH) {
            questions.add("Are symptoms getting worse compared with yesterday?");
            questions.add("Do you have any new severe symptom that started suddenly?");
        }
        return questions.stream().limit(6).toList();
    }

    private List<String> suggestionsFor(RiskLevel level) {
        List<String> suggestions = new ArrayList<>();
        if (level == RiskLevel.HIGH) {
            suggestions.add("Seek urgent medical advice for severe, sudden, or worsening symptoms.");
        }
        suggestions.add("Track your symptoms, vitals, and duration carefully.");
        suggestions.add("Use this result only for awareness, not diagnosis.");
        suggestions.add("Consult a qualified doctor if symptoms persist or concern you.");
        return suggestions;
    }

    public record RiskResult(
            int score,
            RiskLevel level,
            List<String> reasons,
            List<String> followUps,
            List<String> suggestions
    ) {}
}
