package com.example.model;

public record LogAnalysisRequest(
        String serviceName,
        String environment,
        String logText
) {
}
