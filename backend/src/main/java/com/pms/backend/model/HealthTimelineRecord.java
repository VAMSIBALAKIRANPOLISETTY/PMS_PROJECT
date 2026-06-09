package com.pms.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class HealthTimelineRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @ManyToOne
    private HealthConnection connection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentSourceType sourceType = AssessmentSourceType.DEVICE;

    @Column(nullable = false)
    private String recordType;

    @Column(nullable = false)
    private String label;

    private String valueText;
    private String unit;
    private String sourceName;
    private String externalRecordId;

    @Column(length = 1200)
    private String notes;

    private LocalDateTime observedAt;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public HealthConnection getConnection() { return connection; }
    public void setConnection(HealthConnection connection) { this.connection = connection; }
    public AssessmentSourceType getSourceType() { return sourceType; }
    public void setSourceType(AssessmentSourceType sourceType) { this.sourceType = sourceType; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getExternalRecordId() { return externalRecordId; }
    public void setExternalRecordId(String externalRecordId) { this.externalRecordId = externalRecordId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDateTime observedAt) { this.observedAt = observedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
