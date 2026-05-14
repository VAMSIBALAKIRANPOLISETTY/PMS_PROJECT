package com.pms.backend.model;

import jakarta.persistence.*;

@Entity
public class AdminRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String conditionLabel;
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    private Integer score;
    @Column(length = 600)
    private String explanation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConditionLabel() { return conditionLabel; }
    public void setConditionLabel(String conditionLabel) { this.conditionLabel = conditionLabel; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
