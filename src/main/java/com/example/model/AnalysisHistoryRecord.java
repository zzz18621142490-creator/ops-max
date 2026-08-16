package com.example.model;

import java.time.Instant;

public record AnalysisHistoryRecord(
        String id,
        String serviceName,
        String environment,
        String logPreview,
        LogAnalysisResponse result,
        Instant createdAt
) {
}
