package com.pms.backend.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class LabObservation {
    private String testName;
    private String valueText;
    private String unit;
    private String referenceRange;
    private String flag;

    public LabObservation() {}

    public LabObservation(String testName, String valueText, String unit, String referenceRange, String flag) {
        this.testName = testName;
        this.valueText = valueText;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.flag = flag;
    }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
}
