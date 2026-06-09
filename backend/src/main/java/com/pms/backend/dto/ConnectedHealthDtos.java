package com.pms.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class ConnectedHealthDtos {
    public record ConnectionResponse(
            Long id,
            String provider,
            String displayName,
            String status,
            LocalDateTime connectedAt,
            LocalDateTime lastSyncAt,
            String permissionSummary
    ) {}

    public record ConnectionStartResponse(
            String provider,
            String authorizationUrl,
            String permissionSummary
    ) {}

    public record ConnectionCallbackRequest(
            @Size(max = 160) String externalAccountId,
            @Size(max = 160) String displayName
    ) {}

    public record TimelineRecordRequest(
            @NotBlank @Size(max = 80) String recordType,
            @NotBlank @Size(max = 160) String label,
            @Size(max = 120) String valueText,
            @Size(max = 40) String unit,
            @Size(max = 160) String sourceName,
            @Size(max = 500) String notes
    ) {}

    public record TimelineSyncRequest(
            @Valid List<TimelineRecordRequest> records
    ) {}

    public record TimelineRecordResponse(
            Long id,
            String sourceType,
            String recordType,
            String label,
            String valueText,
            String unit,
            String sourceName,
            String notes,
            LocalDateTime observedAt
    ) {}
}
