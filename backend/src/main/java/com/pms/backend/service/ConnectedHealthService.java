package com.pms.backend.service;

import com.pms.backend.dto.ConnectedHealthDtos.ConnectionCallbackRequest;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionResponse;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionStartResponse;
import com.pms.backend.dto.ConnectedHealthDtos.TimelineRecordRequest;
import com.pms.backend.dto.ConnectedHealthDtos.TimelineRecordResponse;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.AssessmentSourceType;
import com.pms.backend.model.ConnectionProvider;
import com.pms.backend.model.ConnectionStatus;
import com.pms.backend.model.HealthConnection;
import com.pms.backend.model.HealthTimelineRecord;
import com.pms.backend.model.Role;
import com.pms.backend.repository.HealthConnectionRepository;
import com.pms.backend.repository.HealthTimelineRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ConnectedHealthService {
    private final HealthConnectionRepository connectionRepository;
    private final HealthTimelineRecordRepository timelineRepository;

    public ConnectedHealthService(
            HealthConnectionRepository connectionRepository,
            HealthTimelineRecordRepository timelineRepository
    ) {
        this.connectionRepository = connectionRepository;
        this.timelineRepository = timelineRepository;
    }

    public List<ConnectionResponse> listConnections(AppUser user) {
        requirePatient(user);
        return connectionRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::toConnectionResponse).toList();
    }

    public ConnectionStartResponse start(AppUser user, String providerValue) {
        requirePatient(user);
        ConnectionProvider provider = parseProvider(providerValue);
        return new ConnectionStartResponse(
                provider.name(),
                "pms-health://connections/" + provider.name().toLowerCase(Locale.ROOT) + "/authorize",
                permissionSummary(provider)
        );
    }

    public ConnectionResponse callback(AppUser user, String providerValue, ConnectionCallbackRequest request) {
        requirePatient(user);
        ConnectionProvider provider = parseProvider(providerValue);
        HealthConnection connection = new HealthConnection();
        connection.setUser(user);
        connection.setProvider(provider);
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setExternalAccountId(clean(request.externalAccountId()));
        connection.setDisplayName(clean(request.displayName()) == null ? displayName(provider) : clean(request.displayName()));
        connection.setConnectedAt(LocalDateTime.now());
        return toConnectionResponse(connectionRepository.save(connection));
    }

    public List<TimelineRecordResponse> sync(AppUser user, Long connectionId, List<TimelineRecordRequest> records) {
        requirePatient(user);
        HealthConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found."));
        if (!connection.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Connection does not belong to this user.");
        }
        if (connection.getStatus() != ConnectionStatus.CONNECTED) {
            throw new IllegalArgumentException("Connection is not active.");
        }
        if (records == null || records.isEmpty()) {
            connection.setLastSyncAt(LocalDateTime.now());
            connectionRepository.save(connection);
            return List.of();
        }
        List<TimelineRecordRequest> incoming = records;
        List<HealthTimelineRecord> saved = incoming.stream().limit(50).map(record -> {
            HealthTimelineRecord row = new HealthTimelineRecord();
            row.setUser(user);
            row.setConnection(connection);
            row.setSourceType(sourceTypeFor(connection.getProvider()));
            row.setRecordType(record.recordType());
            row.setLabel(record.label());
            row.setValueText(clean(record.valueText()));
            row.setUnit(clean(record.unit()));
            row.setSourceName(clean(record.sourceName()) == null ? connection.getDisplayName() : clean(record.sourceName()));
            row.setNotes(clean(record.notes()));
            row.setObservedAt(LocalDateTime.now());
            return timelineRepository.save(row);
        }).toList();
        connection.setLastSyncAt(LocalDateTime.now());
        connectionRepository.save(connection);
        return saved.stream().map(this::toTimelineResponse).toList();
    }

    public void revoke(AppUser user, Long connectionId) {
        requirePatient(user);
        HealthConnection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found."));
        if (!connection.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Connection does not belong to this user.");
        }
        connection.setStatus(ConnectionStatus.REVOKED);
        connectionRepository.save(connection);
    }

    public List<TimelineRecordResponse> timeline(AppUser user) {
        requirePatient(user);
        return timelineRepository.findByUserOrderByObservedAtDescCreatedAtDesc(user).stream()
                .map(this::toTimelineResponse)
                .toList();
    }

    private ConnectionResponse toConnectionResponse(HealthConnection connection) {
        return new ConnectionResponse(
                connection.getId(),
                connection.getProvider().name(),
                connection.getDisplayName(),
                connection.getStatus().name(),
                connection.getConnectedAt(),
                connection.getLastSyncAt(),
                permissionSummary(connection.getProvider())
        );
    }

    private TimelineRecordResponse toTimelineResponse(HealthTimelineRecord record) {
        return new TimelineRecordResponse(
                record.getId(),
                record.getSourceType().name(),
                record.getRecordType(),
                record.getLabel(),
                record.getValueText(),
                record.getUnit(),
                record.getSourceName(),
                record.getNotes(),
                record.getObservedAt()
        );
    }

    private AssessmentSourceType sourceTypeFor(ConnectionProvider provider) {
        return switch (provider) {
            case HOSPITAL_PORTAL -> AssessmentSourceType.HOSPITAL;
            case LAB_REPORT_UPLOAD -> AssessmentSourceType.REPORT;
            default -> AssessmentSourceType.DEVICE;
        };
    }

    private ConnectionProvider parseProvider(String value) {
        try {
            return ConnectionProvider.valueOf(value.trim().toUpperCase(Locale.ROOT).replace("-", "_"));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported connection provider.");
        }
    }

    private String permissionSummary(ConnectionProvider provider) {
        return switch (provider) {
            case APPLE_HEALTH -> "Patient-approved import of HealthKit activity, sleep, vitals, and available health records through a future iOS companion app.";
            case ANDROID_HEALTH_CONNECT -> "Patient-approved import of Health Connect activity, sleep, vitals, and supported medical records through a future Android companion app.";
            case SAMSUNG_HEALTH -> "Patient-approved import through Health Connect or Samsung Health Data SDK where supported.";
            case HOSPITAL_PORTAL -> "Patient-authorized SMART on FHIR connection for clinical records where the hospital supports it.";
            case LAB_REPORT_UPLOAD -> "Patient-owned lab and diagnostic report uploads normalized into report assessments.";
            case MANUAL_ENTRY -> "Manual vitals and health events entered by the patient when direct connection is unavailable.";
        };
    }

    private String displayName(ConnectionProvider provider) {
        return switch (provider) {
            case APPLE_HEALTH -> "Apple Health";
            case ANDROID_HEALTH_CONNECT -> "Android Health Connect";
            case SAMSUNG_HEALTH -> "Samsung Health";
            case HOSPITAL_PORTAL -> "Hospital Portal";
            case LAB_REPORT_UPLOAD -> "Lab Report Upload";
            case MANUAL_ENTRY -> "Manual Entry";
        };
    }

    private void requirePatient(AppUser user) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required for connected health records.");
        }
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
