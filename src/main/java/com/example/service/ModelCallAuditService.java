package com.example.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.entity.ModelCallAuditEntity;
import com.example.model.LogAnalysisRequest;
import com.example.model.ModelCallAuditRecord;
import com.example.repository.ModelCallAuditRepository;

@Service
public class ModelCallAuditService {
    private static final Logger log = LoggerFactory.getLogger(ModelCallAuditService.class);
    private static final int ERROR_SUMMARY_LIMIT = 800;

    private final ModelCallAuditRepository repository;
    private final LogSanitizationService sanitizationService;

    public ModelCallAuditService(ModelCallAuditRepository repository,
                                 LogSanitizationService sanitizationService) {
        this.repository = repository;
        this.sanitizationService = sanitizationService;
    }

    public void recordSuccess(LogAnalysisRequest request, String modelName, long durationMs) {
        save(request, modelName, "SUCCESS", durationMs, null);
    }

    public void recordFailure(LogAnalysisRequest request, String modelName, long durationMs, Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        save(request, modelName, "FAILED", durationMs, truncate(sanitizationService.sanitize(message)));
    }

    public List<ModelCallAuditRecord> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRecord)
                .toList();
    }

    private void save(LogAnalysisRequest request, String modelName, String status,
                      long durationMs, String errorSummary) {
        try {
            repository.save(new ModelCallAuditEntity(
                    UUID.randomUUID().toString(),
                    normalize(request.serviceName(), "unknown-service"),
                    normalize(request.environment(), "unknown"),
                    normalize(modelName, "unknown-model"),
                    status,
                    Math.max(0, durationMs),
                    errorSummary,
                    Instant.now()
            ));
        } catch (RuntimeException exception) {
            log.error("Failed to persist model call audit: {}", exception.getClass().getSimpleName());
        }
    }

    private ModelCallAuditRecord toRecord(ModelCallAuditEntity entity) {
        return new ModelCallAuditRecord(
                entity.getId(),
                entity.getServiceName(),
                entity.getEnvironment(),
                entity.getModelName(),
                entity.getStatus(),
                entity.getDurationMs(),
                entity.getErrorSummary(),
                entity.getCreatedAt()
        );
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= ERROR_SUMMARY_LIMIT) {
            return value;
        }
        return value.substring(0, ERROR_SUMMARY_LIMIT);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
