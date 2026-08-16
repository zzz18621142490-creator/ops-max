package com.example.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.entity.AnalysisHistoryEntity;
import com.example.model.AnalysisHistoryRecord;
import com.example.model.LogAnalysisRequest;
import com.example.model.LogAnalysisResponse;
import com.example.repository.AnalysisHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalysisHistoryService {
    private static final int LOG_PREVIEW_LIMIT = 180;

    private final AnalysisHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public AnalysisHistoryService(AnalysisHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public AnalysisHistoryRecord save(LogAnalysisRequest request, LogAnalysisResponse response) {
        String id = UUID.randomUUID().toString();
        AnalysisHistoryEntity entity = new AnalysisHistoryEntity(
                id,
                normalizeLabel(request.serviceName(), "unknown-service"),
                normalizeLabel(request.environment(), "unknown"),
                buildPreview(request.logText()),
                toJson(response),
                Instant.now()
        );
        return toRecord(repository.save(entity));
    }

    public List<AnalysisHistoryRecord> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toRecord)
                .toList();
    }

    public Optional<AnalysisHistoryRecord> findById(String id) {
        return repository.findById(id).map(this::toRecord);
    }

    public void clear() {
        repository.deleteAll();
    }

    private AnalysisHistoryRecord toRecord(AnalysisHistoryEntity entity) {
        return new AnalysisHistoryRecord(
                entity.getId(),
                entity.getServiceName(),
                entity.getEnvironment(),
                entity.getLogPreview(),
                fromJson(entity.getResultJson()),
                entity.getCreatedAt()
        );
    }

    private String toJson(LogAnalysisResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize analysis result", e);
        }
    }

    private LogAnalysisResponse fromJson(String resultJson) {
        try {
            return objectMapper.readValue(resultJson, LogAnalysisResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to read analysis history", e);
        }
    }

    private static String buildPreview(String logText) {
        String normalized = logText == null ? "" : logText.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= LOG_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, LOG_PREVIEW_LIMIT) + "...";
    }

    private static String normalizeLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
