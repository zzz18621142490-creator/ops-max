package com.example.model;

import java.time.Instant;

public record ModelCallAuditRecord(
        String id,
        String serviceName,
        String environment,
        String modelName,
        String status,
        long durationMs,
        String errorSummary,
        Instant createdAt
) {
}
