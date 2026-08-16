package com.example.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.model.LogAnalysisResponse;

@Service
public class LogAnalysisService {
    public LogAnalysisResponse analyze(String logText) {
        String safeLogText = logText == null ? "" : logText;
        String normalized = safeLogText.toLowerCase(Locale.ROOT);

        int errorCount = countOccurrences(normalized, "error") + countOccurrences(normalized, "exception");
        int warningCount = countOccurrences(normalized, "warn");

        List<String> issues = new ArrayList<>();
        List<String> rootCauses = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> steps = new ArrayList<>();

        if (normalized.contains("connection refused")) {
            issues.add("Connection refused");
            rootCauses.add("Downstream process is stopped, unreachable, or listening on a different port.");
            actions.add("Check whether the target service is running and whether host, port, and firewall rules are correct.");
            steps.add("Run a connectivity check from the affected host to the downstream host and port.");
        }

        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            issues.add("Request timeout");
            rootCauses.add("Downstream latency, network packet loss, or connection pool exhaustion.");
            actions.add("Check downstream latency, network stability, connection pool settings, and timeout configuration.");
            steps.add("Compare request latency, retry count, and dependency health around the incident time.");
        }

        if (normalized.contains("outofmemoryerror") || normalized.contains("java heap space")) {
            issues.add("JVM memory pressure");
            rootCauses.add("Heap exhaustion caused by memory leak, traffic spike, or large batch workload.");
            actions.add("Capture heap usage, inspect recent traffic or batch jobs, and review JVM heap settings.");
            steps.add("Collect heap metrics, GC logs, and a heap dump if the process is still alive.");
        }

        if (normalized.contains("nullpointerexception")) {
            issues.add("Null pointer exception");
            rootCauses.add("Unexpected null input or missing defensive handling in application code.");
            actions.add("Locate the stack trace line and add input validation or null-safe handling around that code path.");
            steps.add("Find the first application stack frame and inspect request payload or upstream response data.");
        }

        if (normalized.contains("deadlock")) {
            issues.add("Potential deadlock");
            rootCauses.add("Threads are waiting on locks in a circular dependency.");
            actions.add("Collect thread dumps and inspect lock ownership and blocked threads.");
            steps.add("Take at least three thread dumps at short intervals and compare blocked thread states.");
        }

        if (normalized.contains("database") || normalized.contains("sql") || normalized.contains("jdbc")) {
            issues.add("Database access issue");
            rootCauses.add("Database connectivity, slow SQL, lock contention, or exhausted connection pool.");
            actions.add("Check database availability, slow query logs, connection pool saturation, and lock waits.");
            steps.add("Inspect datasource metrics, active connections, slow SQL, and database error logs.");
        }

        if (normalized.contains("no space left") || normalized.contains("disk full")) {
            issues.add("Disk capacity issue");
            rootCauses.add("The filesystem used by logs, temporary files, or data storage is full.");
            actions.add("Free disk space, rotate logs, and confirm retention policy for large files.");
            steps.add("Check filesystem usage and identify the fastest-growing directories.");
        }

        if (normalized.contains("unauthorized") || normalized.contains("forbidden") || normalized.contains("401") || normalized.contains("403")) {
            issues.add("Authentication or authorization failure");
            rootCauses.add("Expired credentials, missing permission, invalid token, or changed access policy.");
            actions.add("Validate credentials, token expiration, role bindings, and recent permission changes.");
            steps.add("Compare failing identity, target resource, and permission policy with the last known good request.");
        }

        if (normalized.contains("http 500") || normalized.contains("status 500") || normalized.contains("internal server error")) {
            issues.add("HTTP 5xx from service");
            rootCauses.add("The target service returned an internal error or propagated a dependency failure.");
            actions.add("Inspect the upstream service logs and correlate request IDs across service boundaries.");
            steps.add("Trace the request ID through gateway, caller service, target service, and its dependencies.");
        }

        if (errorCount > 0 && issues.isEmpty()) {
            issues.add("Generic application error");
            rootCauses.add("Application error without a recognized signature.");
            actions.add("Review the first ERROR or Exception stack trace and correlate it with the deployment and request timeline.");
            steps.add("Start from the earliest ERROR line and inspect nearby stack traces and request IDs.");
        }

        if (warningCount > 0 && issues.isEmpty()) {
            issues.add("Warnings detected");
            rootCauses.add("Service degradation or configuration drift without a clear failure signal.");
            actions.add("Review WARN entries for degraded dependencies, retries, or configuration drift.");
            steps.add("Group WARN messages by type and compare their frequency before and during the incident.");
        }

        if (issues.isEmpty()) {
            issues.add("No obvious issue detected");
            rootCauses.add("The provided log window does not contain a recognized failure signature.");
            actions.add("If the service is still abnormal, provide a larger log window around the incident time.");
            steps.add("Collect logs from five minutes before and after the incident, plus metrics for CPU, memory, disk, and dependency latency.");
        }

        String severity = determineSeverity(errorCount, warningCount, normalized);
        String summary = buildSummary(severity, issues, errorCount, warningCount);
        String impactScope = determineImpactScope(normalized, issues);
        double confidence = determineConfidence(issues, errorCount, warningCount);
        boolean immediateActionRequired = isImmediateActionRequired(severity, issues);

        return new LogAnalysisResponse(
                severity,
                summary,
                List.copyOf(issues),
                List.copyOf(rootCauses),
                impactScope,
                confidence,
                immediateActionRequired,
                List.copyOf(actions),
                List.copyOf(steps),
                errorCount,
                warningCount,
                Instant.now(),
                "rule",
                null
        );
    }

    private static String determineSeverity(int errorCount, int warningCount, String normalizedLogText) {
        if (normalizedLogText.contains("outofmemoryerror") || normalizedLogText.contains("deadlock")) {
            return "CRITICAL";
        }
        if (errorCount > 0) {
            return "ERROR";
        }
        if (warningCount > 0) {
            return "WARN";
        }
        return "OK";
    }

    private static String buildSummary(String severity, List<String> issues, int errorCount, int warningCount) {
        return "Severity " + severity + ": detected " + errorCount + " error signals and "
                + warningCount + " warning signals. Primary finding: " + issues.getFirst() + ".";
    }

    private static String determineImpactScope(String normalizedLogText, List<String> issues) {
        if (normalizedLogText.contains("all nodes") || normalizedLogText.contains("cluster")) {
            return "cluster";
        }
        if (issues.stream().anyMatch(issue -> issue.contains("Database") || issue.contains("HTTP 5xx"))) {
            return "dependency";
        }
        if (issues.stream().anyMatch(issue -> issue.contains("JVM") || issue.contains("Disk"))) {
            return "single-service";
        }
        return "unknown";
    }

    private static double determineConfidence(List<String> issues, int errorCount, int warningCount) {
        boolean hasSpecificIssue = issues.stream().noneMatch(issue -> issue.equals("Generic application error")
                || issue.equals("Warnings detected") || issue.equals("No obvious issue detected"));
        if (hasSpecificIssue && errorCount > 0) {
            return 0.86;
        }
        if (hasSpecificIssue || errorCount > 0) {
            return 0.72;
        }
        if (warningCount > 0) {
            return 0.55;
        }
        return 0.35;
    }

    private static boolean isImmediateActionRequired(String severity, List<String> issues) {
        return severity.equals("CRITICAL")
                || issues.stream().anyMatch(issue -> issue.contains("Disk") || issue.contains("Database"));
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
