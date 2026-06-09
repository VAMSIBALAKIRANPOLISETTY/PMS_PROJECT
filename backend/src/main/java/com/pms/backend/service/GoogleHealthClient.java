package com.pms.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleHealthClient {
    private static final DateTimeFormatter GOOGLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    private static final List<String> DEFAULT_SCOPES = List.of(
            "openid",
            "email",
            "profile",
            "https://www.googleapis.com/auth/fitness.activity.read",
            "https://www.googleapis.com/auth/fitness.body.read",
            "https://www.googleapis.com/auth/fitness.sleep.read"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizationBaseUrl;
    private final String tokenUrl;
    private final String apiBaseUrl;
    private final String userInfoUrl;
    private final Duration timeout;

    @Autowired
    public GoogleHealthClient(
            @Value("${pms.connected-health.google.client-id:}") String clientId,
            @Value("${pms.connected-health.google.client-secret:}") String clientSecret,
            @Value("${pms.connected-health.google.redirect-uri:http://localhost:8080/api/connections/google-health/callback}") String redirectUri,
            @Value("${pms.connected-health.google.authorization-base-url:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationBaseUrl,
            @Value("${pms.connected-health.google.token-url:https://oauth2.googleapis.com/token}") String tokenUrl,
            @Value("${pms.connected-health.google.api-base-url:https://www.googleapis.com/fitness/v1}") String apiBaseUrl,
            @Value("${pms.connected-health.google.user-info-url:https://openidconnect.googleapis.com/v1/userinfo}") String userInfoUrl,
            @Value("${pms.connected-health.google.timeout-seconds:20}") Integer timeoutSeconds
    ) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(1, timeoutSeconds == null ? 20 : timeoutSeconds))).build(),
                clientId, clientSecret, redirectUri, authorizationBaseUrl, tokenUrl, apiBaseUrl, userInfoUrl,
                Duration.ofSeconds(Math.max(1, timeoutSeconds == null ? 20 : timeoutSeconds)));
    }

    GoogleHealthClient(
            HttpClient httpClient,
            String clientId,
            String clientSecret,
            String redirectUri,
            String authorizationBaseUrl,
            String tokenUrl,
            String apiBaseUrl,
            String userInfoUrl,
            Duration timeout
    ) {
        this.httpClient = httpClient;
        this.clientId = text(clientId);
        this.clientSecret = text(clientSecret);
        this.redirectUri = text(redirectUri);
        this.authorizationBaseUrl = stripTrailingSlash(blankDefault(authorizationBaseUrl, "https://accounts.google.com/o/oauth2/v2/auth"));
        this.tokenUrl = stripTrailingSlash(blankDefault(tokenUrl, "https://oauth2.googleapis.com/token"));
        this.apiBaseUrl = stripTrailingSlash(blankDefault(apiBaseUrl, "https://www.googleapis.com/fitness/v1"));
        this.userInfoUrl = stripTrailingSlash(blankDefault(userInfoUrl, "https://openidconnect.googleapis.com/v1/userinfo"));
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    public boolean configured() {
        return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
    }

    public String authorizationUrl(String state) {
        requireConfigured();
        Map<String, String> query = new LinkedHashMap<>();
        query.put("client_id", clientId);
        query.put("response_type", "code");
        query.put("redirect_uri", redirectUri);
        query.put("scope", String.join(" ", DEFAULT_SCOPES));
        query.put("access_type", "offline");
        query.put("include_granted_scopes", "true");
        query.put("prompt", "consent");
        query.put("state", state);
        return authorizationBaseUrl + "?" + encodedQuery(query);
    }

    public TokenResponse exchangeAuthorizationCode(String code) {
        requireConfigured();
        return tokenRequest(Map.of(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
        ));
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        requireConfigured();
        return tokenRequest(Map.of(
                "refresh_token", refreshToken,
                "client_id", clientId,
                "client_secret", clientSecret,
                "grant_type", "refresh_token"
        ));
    }

    public UserProfile fetchUserProfile(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUrl))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google Health profile lookup returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return new UserProfile(text(root.path("sub").asText(null)), text(root.path("email").asText(null)), text(root.path("name").asText(null)));
        } catch (IOException exception) {
            throw new IllegalStateException("Google Health profile response could not be read.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Health profile request was interrupted.", exception);
        }
    }

    public List<ImportedTimelineRecord> fetchTimelineRecords(String accessToken) {
        List<ImportedTimelineRecord> records = new ArrayList<>();
        records.addAll(fetchStepRecords(accessToken));
        records.addAll(fetchHeartRateRecords(accessToken));
        records.addAll(fetchSleepRecords(accessToken));
        return records;
    }

    private List<ImportedTimelineRecord> fetchStepRecords(String accessToken) {
        JsonNode root = aggregate(accessToken, "com.google.step_count.delta");
        List<ImportedTimelineRecord> records = new ArrayList<>();
        for (JsonNode bucket : root.path("bucket")) {
            int steps = bucket.path("dataset").path(0).path("point").path(0).path("value").path(0).path("intVal").asInt(0);
            if (steps <= 0) {
                continue;
            }
            LocalDateTime observedAt = millisToTime(bucket.path("endTimeMillis").asText(null));
            records.add(new ImportedTimelineRecord(
                    "Activity summary",
                    "Daily steps",
                    String.valueOf(steps),
                    "steps",
                    "Google Health",
                    "Imported Google daily step total.",
                    observedAt
            ));
        }
        return records.stream().sorted((left, right) -> right.observedAt().compareTo(left.observedAt())).limit(3).toList();
    }

    private List<ImportedTimelineRecord> fetchHeartRateRecords(String accessToken) {
        JsonNode root = aggregate(accessToken, "com.google.heart_rate.summary");
        List<ImportedTimelineRecord> records = new ArrayList<>();
        for (JsonNode bucket : root.path("bucket")) {
            JsonNode values = bucket.path("dataset").path(0).path("point").path(0).path("value");
            if (!values.isArray() || values.isEmpty()) {
                continue;
            }
            double average = values.path(0).path("fpVal").asDouble(0);
            double max = values.size() > 1 ? values.path(1).path("fpVal").asDouble(0) : average;
            double min = values.size() > 2 ? values.path(2).path("fpVal").asDouble(0) : average;
            if (average <= 0) {
                continue;
            }
            LocalDateTime observedAt = millisToTime(bucket.path("endTimeMillis").asText(null));
            records.add(new ImportedTimelineRecord(
                    "Vital reading",
                    "Average heart rate",
                    formatDecimal(average),
                    "bpm",
                    "Google Health",
                    "Imported Google heart-rate summary. Max " + formatDecimal(max) + " bpm, min " + formatDecimal(min) + " bpm.",
                    observedAt
            ));
        }
        return records.stream().sorted((left, right) -> right.observedAt().compareTo(left.observedAt())).limit(3).toList();
    }

    private List<ImportedTimelineRecord> fetchSleepRecords(String accessToken) {
        try {
            String url = apiBaseUrl + "/users/me/sessions?startTime="
                    + encode(nowMinusDays(7).format(GOOGLE_TIME))
                    + "&endTime="
                    + encode(LocalDateTime.now(ZoneOffset.UTC).format(GOOGLE_TIME))
                    + "&activityType=72";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google Health sleep sync returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            List<ImportedTimelineRecord> records = new ArrayList<>();
            for (JsonNode session : root.path("session")) {
                LocalDateTime start = millisToTime(session.path("startTimeMillis").asText(null));
                LocalDateTime end = millisToTime(session.path("endTimeMillis").asText(null));
                if (start == null || end == null) {
                    continue;
                }
                double hours = Duration.between(start, end).toMinutes() / 60.0;
                if (hours <= 0) {
                    continue;
                }
                String sessionName = text(session.path("name").asText(null));
                records.add(new ImportedTimelineRecord(
                        "Sleep summary",
                        "Sleep duration",
                        formatDecimal(hours),
                        "hours",
                        "Google Health",
                        sessionName == null ? "Imported Google sleep session." : "Imported Google sleep session: " + sessionName + ".",
                        end
                ));
            }
            return records.stream().sorted((left, right) -> right.observedAt().compareTo(left.observedAt())).limit(3).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Google Health sleep response could not be read.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Health sleep request was interrupted.", exception);
        }
    }

    private JsonNode aggregate(String accessToken, String dataTypeName) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("aggregateBy", List.of(Map.of("dataTypeName", dataTypeName)));
            body.put("bucketByTime", Map.of("durationMillis", 86_400_000));
            body.put("startTimeMillis", nowMinusDays(7).toInstant(ZoneOffset.UTC).toEpochMilli());
            body.put("endTimeMillis", Instant.now().toEpochMilli());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/users/me/dataset:aggregate"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google Health aggregate request returned HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Google Health aggregate response could not be read.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Health aggregate request was interrupted.", exception);
        }
    }

    private TokenResponse tokenRequest(Map<String, String> parameters) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodedQuery(parameters)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google Health token exchange returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return new TokenResponse(
                    text(root.path("access_token").asText(null)),
                    text(root.path("refresh_token").asText(null)),
                    root.path("expires_in").asLong(0),
                    text(root.path("scope").asText(null))
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Google Health token response could not be read.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Health token request was interrupted.", exception);
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new IllegalArgumentException("Google Health is not configured on this server.");
        }
    }

    private static LocalDateTime nowMinusDays(long days) {
        return LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
    }

    private static LocalDateTime millisToTime(String millis) {
        if (!hasText(millis)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(millis)), ZoneOffset.UTC);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static String formatDecimal(double value) {
        return Math.abs(value - Math.rint(value)) < 0.05 ? String.valueOf((int) Math.round(value)) : String.format("%.1f", value);
    }

    private static String encodedQuery(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        values.forEach((key, value) -> {
            if (value != null) {
                joiner.add(encode(key) + "=" + encode(value));
            }
        });
        return joiner.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String blankDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String text(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, String scope) {}

    public record UserProfile(String subject, String email, String displayName) {}

    public record ImportedTimelineRecord(
            String recordType,
            String label,
            String valueText,
            String unit,
            String sourceName,
            String notes,
            LocalDateTime observedAt
    ) {}
}
