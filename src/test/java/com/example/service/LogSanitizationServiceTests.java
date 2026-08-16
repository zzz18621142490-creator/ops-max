package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogSanitizationServiceTests {
    private final LogSanitizationService service = new LogSanitizationService();

    @Test
    void redactsSecretsAndPersonalDataWhileKeepingLogContext() {
        String rawLog = """
                ERROR database login failed password=super-secret
                Authorization: Bearer abcdefghijklmnopqrstuvwxyz
                jdbc:mysql://db-user:db-pass@192.168.10.24:3306/orders
                contact=ops@example.com phone=13812345678
                """;

        String sanitized = service.sanitize(rawLog);

        assertThat(sanitized)
                .contains("ERROR database login failed")
                .contains("password=[REDACTED]")
                .contains("Bearer [REDACTED]")
                .contains("jdbc:mysql://[REDACTED]@[REDACTED_IP]:3306/orders")
                .contains("[REDACTED_EMAIL]")
                .contains("[REDACTED_PHONE]")
                .doesNotContain("super-secret", "abcdefghijklmnopqrstuvwxyz", "db-user", "db-pass",
                        "192.168.10.24", "ops@example.com", "13812345678");
    }
}
