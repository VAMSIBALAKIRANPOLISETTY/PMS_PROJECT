package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {
    public RiskResult calculate(AssessmentRequest request) {
        String symptom = String.join(" ", request.symptoms()).toLowerCase(Locale.ROOT);
        String safetyContext = (symptom + " " + safeText(request.chronicCondition())).toLowerCase(Locale.ROOT);
        int score = 15;
        List<String> reasons = new ArrayList<>();
        String urgentWarning = urgentWarningFor(safetyContext, request.severity());

        if (request.symptoms().size() >= 4) {
            score += 12;
            reasons.add("Multiple symptoms were selected, so the assessment keeps a closer watch.");
        } else if (request.symptoms().size() >= 2) {
            score += 6;
            reasons.add("More than one symptom was selected for this assessment.");
        }

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

        if (Boolean.TRUE.equals(request.temperatureAvailable()) && request.temperatureF() != null) {
            if (request.temperatureF() >= 103.0) {
                score += 32;
                reasons.add("Temperature is very high.");
            } else if (request.temperatureF() >= 100.4) {
                score += 14;
                reasons.add("Temperature is above normal range.");
            }
        } else {
            reasons.add("Temperature was not available, so the result avoids assuming fever details.");
        }

        if (urgentWarning != null) {
            score = Math.max(score, 85);
            reasons.add("A rule-based red-flag pattern was detected and cannot be lowered by AI wording.");
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
        return new RiskResult(score, level, reasons, followUps, suggestions, urgentWarning);
    }

    public RiskResult refineWithFollowUps(Assessment assessment, List<String> answers) {
        int score = assessment.getRiskScore() == null ? 15 : assessment.getRiskScore();
        List<String> reasons = new ArrayList<>(assessment.getReasons());
        String combinedAnswers = String.join(" ", answers).toLowerCase(Locale.ROOT);
        String urgentWarning = assessment.getUrgentWarning();
        String followUpUrgentWarning = urgentWarningFor(combinedAnswers, assessment.getSeverity());

        if (followUpUrgentWarning != null) {
            score = Math.max(score, 85);
            urgentWarning = urgentWarning == null ? followUpUrgentWarning : urgentWarning;
            reasons.add("Follow-up answers mention a possible red-flag symptom that needs closer review.");
        } else if (combinedAnswers.matches(".*(breath|shortness|chest|faint|unconscious|severe).*")) {
            score += 18;
            reasons.add("Follow-up answers mention a symptom that needs closer review.");
        }
        if (combinedAnswers.matches(".*(vomit|vision|neck stiffness|confusion|blood).*")) {
            score += 12;
            reasons.add("Follow-up answers include symptoms that can change the awareness result.");
        }
        if (combinedAnswers.matches(".*(improving|better|reduced|normal).*")) {
            score -= 6;
            reasons.add("Follow-up answers suggest at least one symptom may be improving.");
        }

        score = Math.max(0, Math.min(score, 100));
        RiskLevel level = score >= 70 ? RiskLevel.HIGH : score >= 40 ? RiskLevel.MEDIUM : RiskLevel.LOW;
        List<String> suggestions = new ArrayList<>(suggestionsFor(level));
        suggestions.add("Review the updated result after answering follow-up questions.");
        return new RiskResult(score, level, dedupe(reasons), assessment.getFollowUpQuestions(), dedupe(suggestions), urgentWarning);
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
        if (symptom.contains("anxiety") || symptom.contains("fatigue") || symptom.contains("weakness")) {
            questions.add("Have sleep, stress, or appetite changed recently?");
            questions.add("Have you felt unusually low, panicked, or unable to function?");
        }
        if (!symptom.contains("fever")) {
            questions.add("Do you have a measured temperature right now?");
        }
        questions.add("Are symptoms getting worse compared with yesterday?");
        questions.add("Do you have any new severe symptom that started suddenly?");
        questions.add("Do you have allergies, medicines, or chronic conditions that may be related?");
        if (level == RiskLevel.HIGH) {
            questions.add("Is there severe pain, breathing trouble, confusion, fainting, or bleeding?");
        }
        return dedupe(questions).stream().limit(7).toList();
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

    private String urgentWarningFor(String text, Integer severity) {
        String value = safeText(text).toLowerCase(Locale.ROOT);
        boolean breathingTrouble = value.matches(".*(shortness of breath|breathing difficulty|difficulty breathing|cannot breathe|can't breathe|breathlessness).*");
        boolean chestConcern = value.matches(".*(chest pain|chest pressure|chest tightness|pressure in chest).*");
        boolean severeNeurologic = value.matches(".*(confusion|confused|faint|fainted|unconscious|seizure|seizures).*");
        boolean severeBleeding = value.matches(".*(heavy bleeding|bleeding heavily|blood loss).*");
        boolean severeWeakness = value.matches(".*(severe weakness|unable to stand|cannot stand).*");
        boolean suddenSevere = value.contains("sudden") && severity != null && severity >= 8;
        boolean chronicWorsening = value.contains("worsening") && value.contains("chronic");

        if (chestConcern && breathingTrouble) {
            return "Chest pain or pressure with breathing difficulty can be urgent. Seek emergency medical care now if this is happening.";
        }
        if (breathingTrouble || severeNeurologic || severeBleeding || severeWeakness || suddenSevere || chronicWorsening) {
            return "A red-flag symptom was reported. Seek urgent medical advice or local emergency care if symptoms are severe, sudden, or worsening.";
        }
        return null;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    public record RiskResult(
            int score,
            RiskLevel level,
            List<String> reasons,
            List<String> followUps,
            List<String> suggestions,
            String urgentWarning
    ) {}
}
