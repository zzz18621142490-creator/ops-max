package com.example.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.model.LogAnalysisRequest;

@Service
public class LogSanitizationService {
    private static final List<RedactionRule> RULES = List.of(
            new RedactionRule(
                    Pattern.compile("(?i)(\\b(?:authorization\\s*[:=]\\s*)?bearer\\s+)[A-Za-z0-9._~+/=-]{8,}"),
                    "$1[REDACTED]"),
            new RedactionRule(
                    Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}(?:\\.[A-Za-z0-9_-]{8,})?\\b"),
                    "[REDACTED_JWT]"),
            new RedactionRule(
                    Pattern.compile("(?i)(\\b(?:api[_-]?key|access[_-]?token|refresh[_-]?token|token|secret|password|passwd|pwd)\\b\\s*[=:]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;&]+)"),
                    "$1[REDACTED]"),
            new RedactionRule(
                    Pattern.compile("(?i)([a-z][a-z0-9+.-]*://)[^\\s/@:]+:[^\\s/@]+@"),
                    "$1[REDACTED]@"),
            new RedactionRule(
                    Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"),
                    "[REDACTED_EMAIL]"),
            new RedactionRule(
                    Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"),
                    "[REDACTED_PHONE]"),
            new RedactionRule(
                    Pattern.compile("(?<!\\d)(?:10(?:\\.\\d{1,3}){3}|192\\.168(?:\\.\\d{1,3}){2}|172\\.(?:1[6-9]|2\\d|3[01])(?:\\.\\d{1,3}){2}|127(?:\\.\\d{1,3}){3})(?!\\d)"),
                    "[REDACTED_IP]")
    );

    public LogAnalysisRequest sanitize(LogAnalysisRequest request) {
        return new LogAnalysisRequest(
                sanitize(request.serviceName()),
                sanitize(request.environment()),
                sanitize(request.logText())
        );
    }

    public String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String sanitized = value;
        for (RedactionRule rule : RULES) {
            sanitized = rule.pattern().matcher(sanitized).replaceAll(rule.replacement());
        }
        return sanitized;
    }

    private record RedactionRule(Pattern pattern, String replacement) {
    }
}
