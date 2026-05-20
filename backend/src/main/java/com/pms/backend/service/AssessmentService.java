package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
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

    public AssessmentService(AssessmentRepository assessmentRepository, RiskEngineService riskEngineService) {
        this.assessmentRepository = assessmentRepository;
        this.riskEngineService = riskEngineService;
    }

    public AssessmentResponse create(AppUser user, AssessmentRequest request) {
        RiskEngineService.RiskResult result = riskEngineService.calculate(request);
        List<String> symptoms = cleanSymptoms(request.symptoms());
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
        assessment.setFollowUpQuestions(result.followUps());
        assessment.setSuggestions(result.suggestions());
        return toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse answerFollowUps(AppUser user, Long assessmentId, FollowUpAnswerRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found."));
        if (user.getRole() != Role.ADMIN && !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment does not belong to this user.");
        }
        List<String> answers = request.answers().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(7)
                .toList();
        RiskEngineService.RiskResult result = riskEngineService.refineWithFollowUps(assessment, answers);
        assessment.setFollowUpAnswers(answers);
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        assessment.setSuggestions(result.suggestions());
        return toResponse(assessmentRepository.save(assessment));
    }

    public List<AssessmentResponse> listFor(AppUser user) {
        List<Assessment> rows = user.getRole() == Role.ADMIN
                ? assessmentRepository.findAllByOrderByCreatedAtDesc()
                : assessmentRepository.findByUserOrderByCreatedAtDesc(user);
        return rows.stream().map(this::toResponse).toList();
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
                assessment.getReasons(),
                assessment.getSuggestions(),
                assessment.getFollowUpQuestions(),
                assessment.getFollowUpAnswers(),
                assessment.getCreatedAt()
        );
    }

    private List<String> cleanSymptoms(List<String> symptoms) {
        return new ArrayList<>(new LinkedHashSet<>(symptoms.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(5)
                .toList()));
    }
}
