package com.pms.backend.model;

import jakarta.persistence.*;

@Entity
public class AdminRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conditionLabel;
    private String primarySymptom;
    private String secondarySymptom;
    private Integer minSeverity;
    private Integer minDurationDays;
    private String chronicConditionKeyword;
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    private Integer score;
    private boolean urgent;
    private boolean active = true;
    @Column(length = 600)
    private String explanation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }
    public String getPrimarySymptom() { return primarySymptom; }
    public void setPrimarySymptom(String primarySymptom) { this.primarySymptom = primarySymptom; }
    public String getSecondarySymptom() { return secondarySymptom; }
    public void setSecondarySymptom(String secondarySymptom) { this.secondarySymptom = secondarySymptom; }
    public Integer getMinSeverity() { return minSeverity; }
    public void setMinSeverity(Integer minSeverity) { this.minSeverity = minSeverity; }
    public Integer getMinDurationDays() { return minDurationDays; }
    public void setMinDurationDays(Integer minDurationDays) { this.minDurationDays = minDurationDays; }
    public String getChronicConditionKeyword() { return chronicConditionKeyword; }
    public void setChronicConditionKeyword(String chronicConditionKeyword) { this.chronicConditionKeyword = chronicConditionKeyword; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
