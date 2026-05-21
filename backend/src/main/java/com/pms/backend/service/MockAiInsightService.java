package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MockAiInsightService implements AiInsightService {
    @Override
    public CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
        String symptoms = String.join(", ", assessment.getSymptoms());
        String profileContext = profileContext(user, assessment);
        String careSummary = "PMS reviewed " + symptoms + " with severity " + assessment.getSeverity()
                + "/10 over " + assessment.getDurationDays() + " day(s). This is a care-preparation guide, not a diagnosis.";
        String explanation = "The rule engine set a " + result.level().name().toLowerCase(Locale.ROOT)
                + " awareness level because: " + String.join(" ", result.reasons())
                + " " + profileContext;
        List<String> directions = possibleDirections(assessment.getSymptoms(), assessment.getChronicCondition(), result.level());
        List<String> monitoring = monitoringPlan(result.level(), assessment);
        List<String> doctorQuestions = doctorQuestions(result.followUps(), assessment.getSymptoms(), result.level());
        return new CarePrepInsight(
                careSummary,
                explanation,
                directions,
                result.urgentWarning(),
                monitoring,
                doctorQuestions,
                trustedSources(result.urgentWarning() != null),
                activeMode()
        );
    }

    @Override
    public List<String> reportFollowUps(String reportName) {
        return List.of(
                "What main symptom or concern made you upload this report?",
                "Did a doctor already review this report with you?",
                "Are any values marked high, low, critical, abnormal, or outside range?",
                "Do you currently have fever, pain, breathing difficulty, dizziness, weakness, or confusion?",
                "Are you taking medicines related to this report?",
                "Do you have a chronic condition connected to these results?"
        );
    }

    @Override
    public CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
        String combined = (safeText(reportText) + " " + String.join(" ", answers)).toLowerCase(Locale.ROOT);
        String urgentWarning = reportUrgentWarning(combined);
        List<String> directions = new ArrayList<>();
        if (combined.matches(".*(high|low|critical|abnormal|outside range).*")) {
            directions.add("Marked or abnormal values should be reviewed with a clinician who can compare them with your history and symptoms.");
        }
        if (combined.matches(".*(medicine|medication|tablet|dose).*")) {
            directions.add("Medication context may matter. Bring your medicine names and doses when discussing this report.");
        }
        if (combined.matches(".*(diabetes|blood pressure|asthma|heart|kidney|thyroid).*")) {
            directions.add("A chronic condition may change how report values should be interpreted.");
        }
        directions.add("Report values alone do not confirm a condition. They should be interpreted with symptoms, exam findings, and medical history.");

        return new CarePrepInsight(
                "PMS organized follow-up answers for " + reportName + " into a doctor-preparation summary.",
                "The report guide is based on copied report text or notes plus your answers about abnormal values, current symptoms, medicines, and chronic conditions. It does not interpret the PDF as a diagnosis.",
                dedupe(directions),
                urgentWarning,
                List.of(
                        "Keep the report file, date, and lab reference ranges available for the appointment.",
                        "Track any symptoms that started before or after the report was created.",
                        "Write down medicines, supplements, allergies, and chronic conditions before the visit."
                ),
                List.of(
                        "Which values in this report need attention, and how urgent are they?",
                        "Could my symptoms or medicines affect these report values?",
                        "Do I need repeat testing, lifestyle changes, or a specialist review?",
                        "What symptoms should make me seek urgent care before the next visit?"
                ),
                trustedSources(urgentWarning != null),
                activeMode()
        );
    }

    private List<String> possibleDirections(List<String> symptoms, String chronicCondition, RiskLevel level) {
        String text = String.join(" ", symptoms).toLowerCase(Locale.ROOT);
        List<String> directions = new ArrayList<>();
        if (text.contains("fever") || text.contains("cough")) {
            directions.add("Fever or cough can come from many causes. Discuss duration, temperature pattern, exposure history, and breathing symptoms with a clinician.");
        }
        if (text.contains("headache")) {
            directions.add("Headache context matters. Note sudden onset, vision changes, vomiting, neck stiffness, sleep, hydration, and stress.");
        }
        if (text.contains("dizziness")) {
            directions.add("Dizziness can be affected by hydration, blood pressure, blood sugar, medicines, and inner-ear symptoms.");
        }
        if (text.contains("chest") || text.contains("breath")) {
            directions.add("Chest or breathing symptoms need careful review. Severe or sudden symptoms should be treated as urgent.");
        }
        if (text.contains("anxiety") || text.contains("fatigue") || text.contains("weakness")) {
            directions.add("Energy, sleep, stress, appetite, mood, and daily functioning are useful details to share.");
        }
        if (chronicCondition != null && !"none".equalsIgnoreCase(chronicCondition.trim())) {
            directions.add("Your existing chronic condition may change how these symptoms should be interpreted.");
        }
        if (level == RiskLevel.LOW) {
            directions.add("No major red-flag pattern was found from the entered details, but persistent or worsening symptoms still deserve medical review.");
        }
        directions.add("These are discussion directions only, not possible diagnoses confirmed by PMS.");
        return dedupe(directions).stream().limit(5).toList();
    }

    private List<String> monitoringPlan(RiskLevel level, Assessment assessment) {
        List<String> plan = new ArrayList<>();
        plan.add("Track symptom changes, duration, and severity at least twice a day.");
        if (Boolean.TRUE.equals(assessment.getTemperatureAvailable())) {
            plan.add("Record temperature readings with time of day and any medicine taken.");
        } else {
            plan.add("If possible, measure temperature later instead of guessing it.");
        }
        plan.add("Note new symptoms, triggers, food, sleep, hydration, medicines, and activity changes.");
        if (level == RiskLevel.HIGH) {
            plan.add("Do not wait if symptoms are severe, sudden, or worsening. Seek urgent professional care.");
        }
        return plan;
    }

    private List<String> doctorQuestions(List<String> followUps, List<String> symptoms, RiskLevel level) {
        List<String> questions = new ArrayList<>();
        questions.add("Which of these symptoms should I mention first: " + String.join(", ", symptoms) + "?");
        questions.add("What signs would mean I should seek urgent care?");
        questions.add("What should I monitor at home before my appointment?");
        questions.addAll(followUps.stream().map(question -> "Follow-up detail: " + question).toList());
        if (level == RiskLevel.HIGH) {
            questions.add("Should I be seen urgently today based on these symptoms?");
        }
        return dedupe(questions).stream().limit(7).toList();
    }

    private String profileContext(AppUser user, Assessment assessment) {
        List<String> parts = new ArrayList<>();
        if (user.getAge() != null) parts.add("age " + user.getAge());
        if (user.getGender() != null && !"Not set".equalsIgnoreCase(user.getGender())) parts.add("sex " + user.getGender());
        if (user.getChronicConditions() != null && !user.getChronicConditions().isBlank()) parts.add("profile history " + user.getChronicConditions());
        if (assessment.getChronicCondition() != null && !"None".equalsIgnoreCase(assessment.getChronicCondition())) parts.add("assessment history " + assessment.getChronicCondition());
        return parts.isEmpty() ? "No completed profile context was added." : "Profile context used for preparation: " + String.join(", ", parts) + ".";
    }

    private String reportUrgentWarning(String value) {
        if (value.matches(".*(breathing difficulty|difficulty breathing|shortness of breath|chest pain|chest pressure|confusion|faint|seizure|heavy bleeding|severe weakness).*")) {
            return "Your report follow-up answers mention a red-flag symptom. Seek urgent medical advice or local emergency care if this is severe, sudden, or worsening.";
        }
        return null;
    }

    private List<String> trustedSources(boolean includeEmergency) {
        List<String> sources = new ArrayList<>();
        if (includeEmergency) {
            sources.add("CDC emergency warning signs: https://www.cdc.gov/flu/signs-symptoms/");
        }
        sources.add("MedlinePlus evaluating health information: https://medlineplus.gov/evaluatinghealthinformation.html");
        sources.add("Mayo Clinic symptom checker: https://www.mayoclinic.org/symptom-checker/select-symptom/itt-20009075");
        sources.add("WHO ethics and governance of AI for health: https://www.who.int/publications/i/item/9789240029200");
        return sources;
    }

    private String activeMode() {
        return "MOCK";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }
}
