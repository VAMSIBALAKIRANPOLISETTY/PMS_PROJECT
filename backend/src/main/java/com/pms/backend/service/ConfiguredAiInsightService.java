package com.pms.backend.service;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.time.Duration;
import java.util.Arrays;
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
    private static final String OLLAMA_PROVIDER = "ollama";
    private static final String OPENAI_PROVIDER = "openai";
    private final MockAiInsightService mockAiInsightService;
    private final OllamaInsightClient ollamaInsightClient;
    private final OpenAiInsightClient openAiInsightClient;
    private final String mode;
    private final List<String> providerChain;
    private final String ollamaApiKey;
    private final String openAiApiKey;

    @Autowired
    public ConfiguredAiInsightService(
            @Value("${pms.ai.mode:mock}") String mode,
            @Value("${pms.ai.provider-chain:ollama,openai}") String providerChain,
            @Value("${pms.ai.ollama.api-key:}") String ollamaApiKey,
            @Value("${pms.ai.ollama.model:gemma4:31b}") String ollamaModel,
            @Value("${pms.ai.ollama.base-url:https://ollama.com/api}") String ollamaBaseUrl,
            @Value("${pms.ai.openai.api-key:}") String openAiApiKey,
            @Value("${pms.ai.openai.model:gpt-4o-mini}") String openAiModel,
            @Value("${pms.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            @Value("${pms.ai.timeout-seconds:30}") Integer timeoutSeconds,
            @Value("${pms.ai.temperature:0.2}") Double temperature
    ) {
        this(
                new MockAiInsightService(),
                new OllamaInsightClient(ollamaBaseUrl, ollamaApiKey, ollamaModel, timeout(timeoutSeconds), safeTemperature(temperature)),
                new OpenAiInsightClient(openAiBaseUrl, openAiApiKey, openAiModel, timeout(timeoutSeconds), safeTemperature(temperature)),
                mode,
                providerChain,
                ollamaApiKey,
                openAiApiKey
        );
    }

    ConfiguredAiInsightService(
            MockAiInsightService mockAiInsightService,
            OllamaInsightClient ollamaInsightClient,
            OpenAiInsightClient openAiInsightClient,
            String mode,
            String providerChain,
            String ollamaApiKey,
            String openAiApiKey
    ) {
        this.mockAiInsightService = mockAiInsightService;
        this.ollamaInsightClient = ollamaInsightClient;
        this.openAiInsightClient = openAiInsightClient;
        this.mode = mode == null ? "mock" : mode.trim();
        this.providerChain = parseProviderChain(providerChain);
        this.ollamaApiKey = ollamaApiKey == null ? "" : ollamaApiKey.trim();
        this.openAiApiKey = openAiApiKey == null ? "" : openAiApiKey.trim();
    }

    @Override
    public CarePrepInsight forAssessment(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
        if (!providerMode()) {
            return mockAiInsightService.forAssessment(user, assessment, result);
        }
        for (String provider : providerChain) {
            try {
                if (OLLAMA_PROVIDER.equals(provider) && !ollamaApiKey.isBlank()) {
                    return withoutAiUrgentWarning(ollamaInsightClient.forAssessment(user, assessment, result));
                }
                if (OPENAI_PROVIDER.equals(provider) && !openAiApiKey.isBlank()) {
                    return withoutAiUrgentWarning(openAiInsightClient.forAssessment(user, assessment, result));
                }
            } catch (RuntimeException exception) {
                log.warn("{} assessment insight failed; trying next fallback: {}", provider, exception.getMessage());
            }
        }
        return mockAiInsightService.forAssessment(user, assessment, result);
    }

    @Override
    public QuestionSet assessmentFollowUps(AppUser user, Assessment assessment, RiskEngineService.RiskResult result) {
        if (!providerMode()) {
            return mockAiInsightService.assessmentFollowUps(user, assessment, result);
        }
        for (String provider : providerChain) {
            try {
                if (OLLAMA_PROVIDER.equals(provider) && !ollamaApiKey.isBlank()) {
                    return ollamaInsightClient.forAssessmentFollowUps(user, assessment, result);
                }
                if (OPENAI_PROVIDER.equals(provider) && !openAiApiKey.isBlank()) {
                    return openAiInsightClient.forAssessmentFollowUps(user, assessment, result);
                }
            } catch (RuntimeException exception) {
                log.warn("{} assessment question suggestion failed; trying next fallback: {}", provider, exception.getMessage());
            }
        }
        return mockAiInsightService.assessmentFollowUps(user, assessment, result);
    }

    @Override
    public QuestionSet reportFollowUps(String reportName) {
        if (!providerMode()) {
            return mockAiInsightService.reportFollowUps(reportName);
        }
        for (String provider : providerChain) {
            try {
                if (OLLAMA_PROVIDER.equals(provider) && !ollamaApiKey.isBlank()) {
                    return ollamaInsightClient.reportFollowUps(reportName);
                }
                if (OPENAI_PROVIDER.equals(provider) && !openAiApiKey.isBlank()) {
                    return openAiInsightClient.reportFollowUps(reportName);
                }
            } catch (RuntimeException exception) {
                log.warn("{} report follow-up generation failed; trying next fallback: {}", provider, exception.getMessage());
            }
        }
        return mockAiInsightService.reportFollowUps(reportName);
    }

    @Override
    public CarePrepInsight forReport(AppUser user, String reportName, String reportText, List<String> answers) {
        CarePrepInsight safetyScan = mockAiInsightService.forReport(user, reportName, reportText, answers);
        if (!providerMode()) {
            return safetyScan;
        }
        for (String provider : providerChain) {
            try {
                CarePrepInsight providerInsight = null;
                if (OLLAMA_PROVIDER.equals(provider) && !ollamaApiKey.isBlank()) {
                    providerInsight = ollamaInsightClient.forReport(user, reportName, reportText, answers);
                }
                if (OPENAI_PROVIDER.equals(provider) && !openAiApiKey.isBlank()) {
                    providerInsight = openAiInsightClient.forReport(user, reportName, reportText, answers);
                }
                if (providerInsight != null) {
                    return withJavaOwnedUrgentWarning(providerInsight, safetyScan.urgentWarning());
                }
            } catch (RuntimeException exception) {
                log.warn("{} report insight failed; trying next fallback: {}", provider, exception.getMessage());
            }
        }
        return safetyScan;
    }

    @Override
    public QuestionSet suggestQuestions(String symptomKey, String focus) {
        if (!providerMode()) {
            return mockAiInsightService.suggestQuestions(symptomKey, focus);
        }
        for (String provider : providerChain) {
            try {
                if (OLLAMA_PROVIDER.equals(provider) && !ollamaApiKey.isBlank()) {
                    return ollamaInsightClient.suggestQuestions(symptomKey, focus);
                }
                if (OPENAI_PROVIDER.equals(provider) && !openAiApiKey.isBlank()) {
                    return openAiInsightClient.suggestQuestions(symptomKey, focus);
                }
            } catch (RuntimeException exception) {
                log.warn("{} managed question suggestion failed; trying next fallback: {}", provider, exception.getMessage());
            }
        }
        return mockAiInsightService.suggestQuestions(symptomKey, focus);
    }

    private boolean providerMode() {
        return PROVIDER_MODE.equalsIgnoreCase(mode);
    }

    private CarePrepInsight withoutAiUrgentWarning(CarePrepInsight insight) {
        return withJavaOwnedUrgentWarning(insight, null);
    }

    private CarePrepInsight withJavaOwnedUrgentWarning(CarePrepInsight insight, String urgentWarning) {
        return new CarePrepInsight(
                insight.careSummary(),
                insight.explanation(),
                insight.possibleDirections(),
                urgentWarning,
                insight.monitoringPlan(),
                insight.careTips(),
                insight.doctorPrepQuestions(),
                insight.trustedSourceLinks(),
                insight.aiMode()
        );
    }

    private List<String> parseProviderChain(String value) {
        List<String> providers = Arrays.stream((value == null ? "" : value).split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(provider -> OLLAMA_PROVIDER.equals(provider) || OPENAI_PROVIDER.equals(provider))
                .toList();
        return providers.isEmpty() ? List.of(OLLAMA_PROVIDER, OPENAI_PROVIDER) : providers;
    }

    private static Duration timeout(Integer timeoutSeconds) {
        return Duration.ofSeconds(Math.max(1, timeoutSeconds == null ? 30 : timeoutSeconds));
    }

    private static double safeTemperature(Double temperature) {
        return temperature == null ? 0.2 : Math.max(0, Math.min(1, temperature));
    }
}
