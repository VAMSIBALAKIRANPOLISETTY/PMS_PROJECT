package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AnalyticsResponse;
import com.pms.backend.dto.AssessmentDtos.SymptomCount;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.repository.AssessmentRepository;
import com.pms.backend.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final UserRepository userRepository;
    private final AssessmentRepository assessmentRepository;

    public AnalyticsService(UserRepository userRepository, AssessmentRepository assessmentRepository) {
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
    }

    public AnalyticsResponse getAnalytics() {
        List<Assessment> assessments = assessmentRepository.findAll();
        long high = assessments.stream().filter(item -> item.getRiskLevel() == RiskLevel.HIGH).count();
        long medium = assessments.stream().filter(item -> item.getRiskLevel() == RiskLevel.MEDIUM).count();
        long low = assessments.stream().filter(item -> item.getRiskLevel() == RiskLevel.LOW).count();

        Map<String, Long> symptomCounts = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getMainSymptom, Collectors.counting()));
        List<SymptomCount> commonSymptoms = symptomCounts.entrySet().stream()
                .map(entry -> new SymptomCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(SymptomCount::count).reversed())
                .limit(8)
                .toList();

        return new AnalyticsResponse(userRepository.count(), assessments.size(), high, medium, low, commonSymptoms);
    }
}
