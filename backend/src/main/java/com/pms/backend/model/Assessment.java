package com.pms.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @Column(nullable = false)
    private String mainSymptom;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> symptoms = new ArrayList<>();

    private Integer severity;
    private Integer durationDays;
    private Boolean temperatureAvailable = true;
    private Double temperatureF;
    private String chronicCondition;
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> reasons = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> suggestions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> followUpQuestions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> followUpAnswers = new ArrayList<>();

    @Column(length = 1200)
    private String careSummary;

    @Column(length = 2200)
    private String explanation;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 700)
    private List<String> possibleDirections = new ArrayList<>();

    @Column(length = 1200)
    private String urgentWarning;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 700)
    private List<String> monitoringPlan = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 700)
    private List<String> doctorPrepQuestions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 700)
    private List<String> trustedSourceLinks = new ArrayList<>();

    private String aiMode;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getMainSymptom() { return mainSymptom; }
    public void setMainSymptom(String mainSymptom) { this.mainSymptom = mainSymptom; }
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Boolean getTemperatureAvailable() { return temperatureAvailable; }
    public void setTemperatureAvailable(Boolean temperatureAvailable) { this.temperatureAvailable = temperatureAvailable; }
    public Double getTemperatureF() { return temperatureF; }
    public void setTemperatureF(Double temperatureF) { this.temperatureF = temperatureF; }
    public String getChronicCondition() { return chronicCondition; }
    public void setChronicCondition(String chronicCondition) { this.chronicCondition = chronicCondition; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<String> getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public List<String> getFollowUpAnswers() { return followUpAnswers; }
    public void setFollowUpAnswers(List<String> followUpAnswers) { this.followUpAnswers = followUpAnswers; }
    public String getCareSummary() { return careSummary; }
    public void setCareSummary(String careSummary) { this.careSummary = careSummary; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<String> getPossibleDirections() { return possibleDirections; }
    public void setPossibleDirections(List<String> possibleDirections) { this.possibleDirections = possibleDirections; }
    public String getUrgentWarning() { return urgentWarning; }
    public void setUrgentWarning(String urgentWarning) { this.urgentWarning = urgentWarning; }
    public List<String> getMonitoringPlan() { return monitoringPlan; }
    public void setMonitoringPlan(List<String> monitoringPlan) { this.monitoringPlan = monitoringPlan; }
    public List<String> getDoctorPrepQuestions() { return doctorPrepQuestions; }
    public void setDoctorPrepQuestions(List<String> doctorPrepQuestions) { this.doctorPrepQuestions = doctorPrepQuestions; }
    public List<String> getTrustedSourceLinks() { return trustedSourceLinks; }
    public void setTrustedSourceLinks(List<String> trustedSourceLinks) { this.trustedSourceLinks = trustedSourceLinks; }
    public String getAiMode() { return aiMode; }
    public void setAiMode(String aiMode) { this.aiMode = aiMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
