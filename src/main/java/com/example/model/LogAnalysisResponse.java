package com.example.model;

import java.time.Instant;
import java.util.List;

public record LogAnalysisResponse(
        String severity,
        String summary,
        List<String> detectedIssues,
        List<String> possibleRootCauses,
        String impactScope,
        double confidence,
        boolean immediateActionRequired,
        List<String> recommendedActions,
        List<String> investigationSteps,
        int errorCount,
        int warningCount,
        Instant analyzedAt,
        String analysisSource,
        String modelName
) {
}
