package com.pms.backend.model;

import jakarta.persistence.*;

@Entity
public class HealthQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symptomKey;
    @Column(nullable = false)
    private String prompt;
    private String inputType = "text";
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymptomKey() { return symptomKey; }
    public void setSymptomKey(String symptomKey) { this.symptomKey = symptomKey; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
