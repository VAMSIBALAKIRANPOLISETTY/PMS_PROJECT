package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AssessmentRepository;
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
        Assessment assessment = new Assessment();
        assessment.setUser(user);
        assessment.setMainSymptom(request.mainSymptom().trim());
        assessment.setSeverity(request.severity());
        assessment.setDurationDays(request.durationDays());
        assessment.setTemperatureF(request.temperatureF());
        assessment.setOxygenLevel(request.oxygenLevel());
        assessment.setHeartRate(request.heartRate());
        assessment.setChronicCondition(request.chronicCondition());
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        assessment.setFollowUpQuestions(result.followUps());
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
                assessment.getSeverity(),
                assessment.getDurationDays(),
                assessment.getTemperatureF(),
                assessment.getOxygenLevel(),
                assessment.getHeartRate(),
                assessment.getChronicCondition(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getReasons(),
                assessment.getSuggestions(),
                assessment.getFollowUpQuestions(),
                assessment.getCreatedAt()
        );
    }
}
