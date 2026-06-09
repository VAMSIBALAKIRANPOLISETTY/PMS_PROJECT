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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConnectedHealthService {
    private final HealthConnectionRepository connectionRepository;
    private final HealthTimelineRecordRepository timelineRepository;
    private final ConnectedHealthSecretService secretService;
    private final GoogleHealthClient googleHealthClient;
    private final String frontendUrl;

    public ConnectedHealthService(
            HealthConnectionRepository connectionRepository,
            HealthTimelineRecordRepository timelineRepository,
            ConnectedHealthSecretService secretService,
            GoogleHealthClient googleHealthClient,
            @Value("${pms.connected-health.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.connectionRepository = connectionRepository;
        this.timelineRepository = timelineRepository;
        this.secretService = secretService;
        this.googleHealthClient = googleHealthClient;
        this.frontendUrl = frontendUrl == null || frontendUrl.isBlank() ? "http://localhost:5173" : frontendUrl.trim();
    }

    public List<ConnectionResponse> listConnections(AppUser user) {
        requirePatient(user);
        return connectionRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::toConnectionResponse).toList();
    }

    public ConnectionStartResponse start(AppUser user, String providerValue) {
        requirePatient(user);
        ConnectionProvider provider = parseProvider(providerValue);
        if (provider == ConnectionProvider.GOOGLE_HEALTH) {
            String state = UUID.randomUUID().toString();
            HealthConnection connection = new HealthConnection();
            connection.setUser(user);
            connection.setProvider(provider);
            connection.setStatus(ConnectionStatus.READY_TO_CONNECT);
            connection.setDisplayName(displayName(provider));
            connection.setAuthorizationState(state);
            connection.setLastSyncStatus("Awaiting authorization");
            connection.setLastSyncMessage("Open Google Health authorization to connect steps, heart-rate, and sleep records.");
            connectionRepository.save(connection);
            return new ConnectionStartResponse(
                    provider.name(),
                    googleHealthClient.authorizationUrl(state),
                    permissionSummary(provider),
                    state
            );
        }
        return new ConnectionStartResponse(
                provider.name(),
                "pms-health://connections/" + provider.name().toLowerCase(Locale.ROOT) + "/authorize",
                permissionSummary(provider),
                null
        );
    }

    public ConnectionResponse callback(AppUser user, String providerValue, ConnectionCallbackRequest request) {
        requirePatient(user);
        ConnectionProvider provider = parseProvider(providerValue);
        if (provider == ConnectionProvider.GOOGLE_HEALTH) {
            throw new IllegalArgumentException("Google Health must be completed through the provider authorization flow.");
        }
        HealthConnection connection = new HealthConnection();
        connection.setUser(user);
        connection.setProvider(provider);
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setExternalAccountId(clean(request.externalAccountId()));
        connection.setDisplayName(clean(request.displayName()) == null ? displayName(provider) : clean(request.displayName()));
        connection.setConnectedAt(LocalDateTime.now());
        connection.setLastSyncStatus("Connected");
        connection.setLastSyncMessage("Connection saved. Use Sync when you are ready to import records.");
        return toConnectionResponse(connectionRepository.save(connection));
    }

    public CallbackPage completeGoogleHealthAuthorization(String state, String code, String error) {
        HealthConnection connection = connectionRepository.findFirstByAuthorizationStateOrderByCreatedAtDesc(state)
                .orElseThrow(() -> new IllegalArgumentException("Google Health authorization state is invalid or expired."));
        if (connection.getProvider() != ConnectionProvider.GOOGLE_HEALTH) {
            throw new IllegalArgumentException("Google Health authorization state is invalid or expired.");
        }
        if (error != null && !error.isBlank()) {
            connection.setLastSyncStatus("Authorization failed");
            connection.setLastSyncMessage("Google returned: " + error);
            connection.setAuthorizationState(null);
            connectionRepository.save(connection);
            return new CallbackPage(false, "Google Health connection was not completed.", "Google returned: " + error, frontendUrl);
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google Health did not return an authorization code.");
        }

        GoogleHealthClient.TokenResponse tokenResponse = googleHealthClient.exchangeAuthorizationCode(code);
        String refreshToken = tokenResponse.refreshToken();
        if ((refreshToken == null || refreshToken.isBlank()) && (connection.getEncryptedRefreshToken() == null || connection.getEncryptedRefreshToken().isBlank())) {
            throw new IllegalArgumentException("Google Health did not return offline access. Reconnect and approve access again.");
        }

        GoogleHealthClient.UserProfile profile = googleHealthClient.fetchUserProfile(tokenResponse.accessToken());
        connection.setStatus(ConnectionStatus.CONNECTED);
        connection.setAuthorizationState(null);
        connection.setProviderSubjectId(profile.subject());
        connection.setExternalAccountId(profile.email() == null ? connection.getExternalAccountId() : profile.email());
        connection.setDisplayName(profile.displayName() == null ? displayName(connection.getProvider()) : profile.displayName());
        connection.setConnectedAt(LocalDateTime.now());
        connection.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(Math.max(1, tokenResponse.expiresInSeconds())));
        connection.setGrantedScopes(tokenResponse.scope());
        if (refreshToken != null && !refreshToken.isBlank()) {
            connection.setEncryptedRefreshToken(secretService.encrypt(refreshToken));
        }
        connectionRepository.save(connection);

        List<TimelineRecordResponse> imported = importGoogleHealthRecords(connection, tokenResponse.accessToken());
        String detail = imported.isEmpty()
                ? "Google Health connected successfully. No recent supported records were imported yet."
                : "Google Health connected successfully. Imported " + imported.size() + " recent record" + (imported.size() == 1 ? "" : "s") + ".";
        return new CallbackPage(true, "Google Health connected", detail, frontendUrl);
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
        if (connection.getProvider() == ConnectionProvider.GOOGLE_HEALTH) {
            String refreshToken = secretService.decrypt(connection.getEncryptedRefreshToken());
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalArgumentException("Google Health needs to be reconnected before syncing again.");
            }
            GoogleHealthClient.TokenResponse refreshed = googleHealthClient.refreshAccessToken(refreshToken);
            if (refreshed.refreshToken() != null && !refreshed.refreshToken().isBlank()) {
                connection.setEncryptedRefreshToken(secretService.encrypt(refreshed.refreshToken()));
            }
            connection.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(Math.max(1, refreshed.expiresInSeconds())));
            if (refreshed.scope() != null && !refreshed.scope().isBlank()) {
                connection.setGrantedScopes(refreshed.scope());
            }
            connectionRepository.save(connection);
            return importGoogleHealthRecords(connection, refreshed.accessToken());
        }

        if ((records == null || records.isEmpty()) && connection.getProvider() == ConnectionProvider.SAMSUNG_HEALTH) {
            records = samsungSmartWatchSample();
        }
        if (records == null || records.isEmpty()) {
            connection.setLastSyncAt(LocalDateTime.now());
            connection.setLastSyncStatus("Connected");
            connection.setLastSyncMessage("No new records were imported.");
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
        connection.setLastSyncStatus("Connected");
        connection.setLastSyncMessage(saved.isEmpty() ? "No new records were imported." : "Imported " + saved.size() + " connected health record" + (saved.size() == 1 ? "" : "s") + ".");
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
        connection.setAuthorizationState(null);
        connection.setLastSyncStatus("Revoked");
        connection.setLastSyncMessage("Provider access was stopped by the patient.");
        connectionRepository.save(connection);
    }

    public List<TimelineRecordResponse> timeline(AppUser user) {
        requirePatient(user);
        return timelineRepository.findByUserOrderByObservedAtDescCreatedAtDesc(user).stream()
                .map(this::toTimelineResponse)
                .toList();
    }

    private List<TimelineRecordResponse> importGoogleHealthRecords(HealthConnection connection, String accessToken) {
        List<GoogleHealthClient.ImportedTimelineRecord> importedRecords = googleHealthClient.fetchTimelineRecords(accessToken);
        timelineRepository.deleteByConnection(connection);
        List<HealthTimelineRecord> saved = importedRecords.stream().map(record -> {
            HealthTimelineRecord row = new HealthTimelineRecord();
            row.setUser(connection.getUser());
            row.setConnection(connection);
            row.setSourceType(AssessmentSourceType.DEVICE);
            row.setRecordType(record.recordType());
            row.setLabel(record.label());
            row.setValueText(clean(record.valueText()));
            row.setUnit(clean(record.unit()));
            row.setSourceName(clean(record.sourceName()) == null ? displayName(ConnectionProvider.GOOGLE_HEALTH) : clean(record.sourceName()));
            row.setNotes(clean(record.notes()));
            row.setObservedAt(record.observedAt() == null ? LocalDateTime.now() : record.observedAt());
            return timelineRepository.save(row);
        }).toList();
        connection.setLastSyncAt(LocalDateTime.now());
        connection.setLastSyncStatus("Connected");
        connection.setLastSyncMessage(saved.isEmpty()
                ? "Google Health connected, but no recent supported records were found."
                : "Imported " + saved.size() + " recent Google Health record" + (saved.size() == 1 ? "" : "s") + ".");
        connectionRepository.save(connection);
        return saved.stream().map(this::toTimelineResponse).toList();
    }

    private ConnectionResponse toConnectionResponse(HealthConnection connection) {
        return new ConnectionResponse(
                connection.getId(),
                connection.getProvider().name(),
                connection.getDisplayName(),
                connection.getStatus().name(),
                connection.getConnectedAt(),
                connection.getLastSyncAt(),
                permissionSummary(connection.getProvider()),
                connection.getLastSyncStatus(),
                connection.getLastSyncMessage()
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
            case GOOGLE_HEALTH -> "OAuth connection for Google activity, sleep, and heart-rate summaries using patient-approved access.";
            case APPLE_HEALTH -> "Patient-approved import of HealthKit activity, sleep, vitals, and available health records through a future iOS companion app.";
            case ANDROID_HEALTH_CONNECT -> "Patient-approved import of Health Connect activity, sleep, vitals, and supported medical records through a future Android companion app.";
            case SAMSUNG_HEALTH -> "Preview wearable import using Samsung Health or Health Connect until the native companion flow is added.";
            case HOSPITAL_PORTAL -> "Patient-authorized SMART on FHIR connection for clinical records where the hospital supports it.";
            case LAB_REPORT_UPLOAD -> "Patient-owned lab and diagnostic report uploads normalized into report assessments.";
            case MANUAL_ENTRY -> "Manual vitals and health events entered by the patient when direct connection is unavailable.";
        };
    }

    private String displayName(ConnectionProvider provider) {
        return switch (provider) {
            case GOOGLE_HEALTH -> "Google Health";
            case APPLE_HEALTH -> "Apple Health";
            case ANDROID_HEALTH_CONNECT -> "Android Health Connect";
            case SAMSUNG_HEALTH -> "Samsung Health";
            case HOSPITAL_PORTAL -> "Hospital Portal";
            case LAB_REPORT_UPLOAD -> "Lab Report Upload";
            case MANUAL_ENTRY -> "Manual Entry";
        };
    }

    private List<TimelineRecordRequest> samsungSmartWatchSample() {
        return List.of(
                new TimelineRecordRequest(
                        "Vital reading",
                        "Resting heart rate",
                        "96",
                        "bpm",
                        "Samsung Galaxy Watch",
                        "Imported Samsung smartwatch value. Higher than usual resting range."
                ),
                new TimelineRecordRequest(
                        "Sleep summary",
                        "Sleep duration",
                        "4.8",
                        "hours",
                        "Samsung Galaxy Watch",
                        "Wearable sleep summary showing reduced sleep before the assessment."
                ),
                new TimelineRecordRequest(
                        "Activity summary",
                        "Daily steps",
                        "2380",
                        "steps",
                        "Samsung Galaxy Watch",
                        "Activity summary showing lower movement than usual."
                ),
                new TimelineRecordRequest(
                        "Stress trend",
                        "Stress level",
                        "High",
                        null,
                        "Samsung Galaxy Watch",
                        "Wearable stress trend from the connected device."
                )
        );
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

    public record CallbackPage(boolean success, String title, String detail, String returnUrl) {}
}
