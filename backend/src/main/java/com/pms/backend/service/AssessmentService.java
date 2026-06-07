package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.AssessmentStatus;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AssessmentRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final RiskEngineService riskEngineService;
    private final AiInsightService aiInsightService;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            RiskEngineService riskEngineService,
            AiInsightService aiInsightService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.riskEngineService = riskEngineService;
        this.aiInsightService = aiInsightService;
    }

    public AssessmentResponse create(AppUser user, AssessmentRequest request) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required to create an assessment.");
        }
        if (assessmentRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.PENDING_FOLLOW_UP).isPresent()) {
            throw new IllegalArgumentException("Finish or discard your current assessment draft before starting another.");
        }
        List<String> symptoms = cleanSymptoms(request.symptoms());
        AssessmentRequest normalizedRequest = new AssessmentRequest(
                symptoms,
                request.severity(),
                request.durationDays(),
                request.temperatureAvailable(),
                request.temperatureF(),
                request.chronicCondition()
        );
        RiskEngineService.RiskResult result = riskEngineService.calculate(normalizedRequest);
        Assessment assessment = new Assessment();
        assessment.setUser(user);
        assessment.setSymptoms(symptoms);
        assessment.setMainSymptom(String.join(", ", symptoms));
        assessment.setSeverity(request.severity());
        assessment.setDurationDays(request.durationDays());
        assessment.setTemperatureAvailable(Boolean.TRUE.equals(request.temperatureAvailable()));
        assessment.setTemperatureF(Boolean.TRUE.equals(request.temperatureAvailable()) ? request.temperatureF() : null);
        assessment.setChronicCondition(request.chronicCondition());
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        assessment.setFollowUpQuestions(mergeFollowUps(result.followUps(), aiInsightService.assessmentFollowUps(user, assessment, result).questions()));
        assessment.setSuggestions(result.suggestions());
        assessment.setStatus(AssessmentStatus.PENDING_FOLLOW_UP);
        assessment.setUrgentWarning(result.urgentWarning());
        return toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse answerFollowUps(AppUser user, Long assessmentId, FollowUpAnswerRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found."));
        if (user.getRole() != Role.USER || !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment does not belong to this user.");
        }
        if (completed(assessment)) {
            throw new IllegalArgumentException("This assessment has already been completed.");
        }
        List<String> answers = request.answers().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(7)
                .toList();
        if (answers.size() != assessment.getFollowUpQuestions().size()) {
            throw new IllegalArgumentException("Answer every follow-up question before preparing your care guide.");
        }
        if (answers.stream().anyMatch(answer -> !answer.matches("^(Yes|No|Not sure)( \\| Note: .+)?$"))) {
            throw new IllegalArgumentException("Choose Yes, No, or Not sure for every follow-up question.");
        }
        RiskEngineService.RiskResult result = riskEngineService.refineWithFollowUps(assessment, answers);
        assessment.setFollowUpAnswers(answers);
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        assessment.setSuggestions(result.suggestions());
        applyCarePrep(assessment, aiInsightService.forAssessment(user, assessment, result), result);
        assessment.setStatus(AssessmentStatus.COMPLETED);
        return toResponse(assessmentRepository.save(assessment));
    }

    public List<AssessmentResponse> listFor(AppUser user) {
        List<Assessment> rows = user.getRole() == Role.ADMIN
                ? assessmentRepository.findAllByOrderByCreatedAtDesc()
                : assessmentRepository.findByUserOrderByCreatedAtDesc(user);
        return rows.stream().filter(this::completed).map(this::toResponse).toList();
    }

    public AssessmentResponse pendingFor(AppUser user) {
        if (user.getRole() != Role.USER) {
            return null;
        }
        return assessmentRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.PENDING_FOLLOW_UP)
                .map(this::toResponse)
                .orElse(null);
    }

    public void discardDraft(AppUser user, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment draft not found."));
        if (user.getRole() != Role.USER || !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment draft does not belong to this user.");
        }
        if (completed(assessment)) {
            throw new IllegalArgumentException("Completed assessments cannot be discarded.");
        }
        assessmentRepository.delete(assessment);
    }

    public AssessmentResponse toResponse(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getUser().getId(),
                assessment.getUser().getFullName(),
                assessment.getMainSymptom(),
                assessment.getSymptoms().isEmpty() ? List.of(assessment.getMainSymptom()) : assessment.getSymptoms(),
                assessment.getSeverity(),
                assessment.getDurationDays(),
                assessment.getTemperatureAvailable() == null ? assessment.getTemperatureF() != null : assessment.getTemperatureAvailable(),
                assessment.getTemperatureF(),
                assessment.getChronicCondition(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getStatus() == null ? AssessmentStatus.COMPLETED : assessment.getStatus(),
                assessment.getReasons(),
                assessment.getSuggestions(),
                assessment.getFollowUpQuestions(),
                assessment.getFollowUpAnswers(),
                assessment.getCareSummary(),
                assessment.getExplanation(),
                assessment.getPossibleDirections(),
                assessment.getUrgentWarning(),
                assessment.getMonitoringPlan(),
                assessment.getCareTips(),
                assessment.getDoctorPrepQuestions(),
                assessment.getTrustedSourceLinks(),
                assessment.getAiMode(),
                assessment.getCreatedAt()
        );
    }

    private void applyCarePrep(
            Assessment assessment,
            AiInsightService.CarePrepInsight insight,
            RiskEngineService.RiskResult result
    ) {
        assessment.setCareSummary(insight.careSummary());
        assessment.setExplanation(insight.explanation());
        assessment.setPossibleDirections(insight.possibleDirections());
        assessment.setUrgentWarning(result.urgentWarning() != null ? result.urgentWarning() : insight.urgentWarning());
        assessment.setMonitoringPlan(insight.monitoringPlan());
        assessment.setCareTips(insight.careTips());
        assessment.setDoctorPrepQuestions(insight.doctorPrepQuestions());
        assessment.setTrustedSourceLinks(insight.trustedSourceLinks());
        assessment.setAiMode(insight.aiMode());
    }

    private List<String> cleanSymptoms(List<String> symptoms) {
        return new ArrayList<>(new LinkedHashSet<>(symptoms.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(5)
                .toList()));
    }

    private List<String> mergeFollowUps(List<String> required, List<String> aiCandidates) {
        List<String> merged = new ArrayList<>(new LinkedHashSet<>(required == null ? List.of() : required));
        for (String candidate : aiCandidates == null ? List.<String>of() : aiCandidates) {
            String value = candidate == null ? "" : candidate.trim();
            if (!value.isBlank() && merged.size() < 7 && !merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged.stream().limit(7).toList();
    }

    private boolean completed(Assessment assessment) {
        return assessment.getStatus() == null || assessment.getStatus() == AssessmentStatus.COMPLETED;
    }
}
