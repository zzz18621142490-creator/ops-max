package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.config.AiModelProperties;
import com.example.model.LogAnalysisRequest;
import com.example.model.LogAnalysisResponse;

@Service
public class AiLogAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(AiLogAnalysisService.class);

    private final LogAnalysisService ruleAnalysisService;
    private final OpenAiCompatibleLogAnalysisClient modelClient;
    private final AiModelProperties properties;
    private final ModelCallAuditService auditService;
    private final LogSanitizationService sanitizationService;

    public AiLogAnalysisService(LogAnalysisService ruleAnalysisService,
                                OpenAiCompatibleLogAnalysisClient modelClient,
                                AiModelProperties properties,
                                ModelCallAuditService auditService,
                                LogSanitizationService sanitizationService) {
        this.ruleAnalysisService = ruleAnalysisService;
        this.modelClient = modelClient;
        this.properties = properties;
        this.auditService = auditService;
        this.sanitizationService = sanitizationService;
    }

    public LogAnalysisResponse analyze(LogAnalysisRequest request) {
        LogAnalysisResponse ruleResult = ruleAnalysisService.analyze(request.logText());
        if (!properties.isConfigured()) {
            return ruleResult;
        }

        long startedAt = System.nanoTime();
        try {
            LogAnalysisResponse modelResult = modelClient.analyze(request, ruleResult);
            auditService.recordSuccess(request, properties.getModel(), elapsedMillis(startedAt));
            return modelResult;
        } catch (RuntimeException exception) {
            long durationMs = elapsedMillis(startedAt);
            auditService.recordFailure(request, properties.getModel(), durationMs, exception);
            log.warn("AI model analysis failed; using rule-based fallback: {}",
                    sanitizationService.sanitize(exception.getMessage()));
            return ruleResult;
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
