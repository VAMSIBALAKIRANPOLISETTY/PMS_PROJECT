package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredAiInsightService implements AiInsightService {
    private static final Logger log = LoggerFactory.getLogger(ConfiguredAiInsightService.class);
    private static final String PROVIDER_MODE = "provider";
    private static final String OPENAI_PROVIDER = "openai";
    private final MockAiInsightService mockAiInsightService;
    private final OpenAiInsightClient openAiInsightClient;
    private final String mode;
    private final String provider;
    private final String apiKey;

    @Autowired
    public ConfiguredAiInsightService(
            @Value("${pms.ai.mode:mock}") String mode,
            @Value("${pms.ai.provider:openai}") String provider,
            @Value("${pms.ai.api-key:}") String apiKey,
            @Value("${pms.ai.model:gpt-4o-mini}") String model,
            @Value("${pms.ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${pms.ai.timeout-seconds:20}") Integer timeoutSeconds
    ) {
        this(
                new MockAiInsightService(),
                new OpenAiInsightClient(baseUrl, apiKey, model, Duration.ofSeconds(Math.max(1, timeoutSeconds == null ? 20 : timeoutSeconds))),
                mode,
                provider,
                apiKey
        );
    }

    ConfiguredAiInsightService(
            MockAiInsightService mockAiInsightService,
            OpenAiInsightClient openAiInsightClient,
            String mode,
            String provider,
            String apiKey
    ) {
        this.mockAiInsightService = mockAiInsightService;
        this.openAiInsightClient = openAiInsightClient;
        this.mode = mode == null ? "mock" : mode.trim();
        this.provider = provider == null ? "" : provider.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
        if (!providerReady()) {
            return mockAiInsightService.forAssessment(user, assessment, result);
        }
        try {
            return withoutAiUrgentWarning(openAiInsightClient.forAssessment(user, assessment, result));
        } catch (RuntimeException exception) {
            log.warn("OpenAI assessment insight failed; falling back to mock mode: {}", exception.getMessage());
            return mockAiInsightService.forAssessment(user, assessment, result);
        }
    }

    @Override
    public List<String> reportFollowUps(String reportName) {
        return mockAiInsightService.reportFollowUps(reportName);
    }

    @Override
    public CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
        CarePrepInsight safetyScan = mockAiInsightService.forReport(user, reportName, reportText, answers);
        if (!providerReady()) {
            return safetyScan;
        }
        try {
            CarePrepInsight providerInsight = openAiInsightClient.forReport(user, reportName, reportText, answers);
            return new CarePrepInsight(
                    providerInsight.careSummary(),
                    providerInsight.explanation(),
                    providerInsight.possibleDirections(),
                    safetyScan.urgentWarning(),
                    providerInsight.monitoringPlan(),
                    providerInsight.doctorPrepQuestions(),
                    providerInsight.trustedSourceLinks(),
                    "PROVIDER"
            );
        } catch (RuntimeException exception) {
            log.warn("OpenAI report insight failed; falling back to mock mode: {}", exception.getMessage());
            return safetyScan;
        }
    }

    private boolean providerReady() {
        return PROVIDER_MODE.equalsIgnoreCase(mode)
                && OPENAI_PROVIDER.equalsIgnoreCase(provider)
                && !apiKey.isBlank();
    }

    private CarePrepInsight withoutAiUrgentWarning(CarePrepInsight insight) {
        return new CarePrepInsight(
                insight.careSummary(),
                insight.explanation(),
                insight.possibleDirections(),
                null,
                insight.monitoringPlan(),
                insight.doctorPrepQuestions(),
                insight.trustedSourceLinks(),
                "PROVIDER"
        );
    }
}
