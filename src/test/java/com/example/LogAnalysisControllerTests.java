package com.example;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.example.model.LogAnalysisRequest;
import com.example.service.ModelCallAuditService;

@SpringBootTest(properties = "ai.model.enabled=false")
@AutoConfigureMockMvc
class LogAnalysisControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModelCallAuditService modelCallAuditService;

    @Test
    void analyzeLogDetectsTimeoutAndConnectionRefused() throws Exception {
        String body = """
                {
                  "serviceName": "order-service",
                  "environment": "dev",
                  "logText": "ERROR request timeout while calling payment-service; connection refused"
                }
                """;

        mockMvc.perform(post("/api/analyze-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(blankOrNullString())))
                .andExpect(jsonPath("$.serviceName").value("order-service"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.logPreview", containsString("payment-service")))
                .andExpect(jsonPath("$.result.severity").value("ERROR"))
                .andExpect(jsonPath("$.result.summary", containsString("Primary finding")))
                .andExpect(jsonPath("$.result.detectedIssues[0]").value("Connection refused"))
                .andExpect(jsonPath("$.result.detectedIssues[1]").value("Request timeout"))
                .andExpect(jsonPath("$.result.possibleRootCauses[0]", containsString("Downstream process")))
                .andExpect(jsonPath("$.result.impactScope").value("unknown"))
                .andExpect(jsonPath("$.result.confidence").value(0.86))
                .andExpect(jsonPath("$.result.immediateActionRequired").value(false))
                .andExpect(jsonPath("$.result.investigationSteps[0]", containsString("connectivity check")))
                .andExpect(jsonPath("$.result.errorCount").value(1))
                .andExpect(jsonPath("$.result.analysisSource").value("rule"));
    }

    @Test
    void analyzeLogDetectsDiskCapacityIssueAsImmediateAction() throws Exception {
        String body = """
                {
                  "serviceName": "log-service",
                  "environment": "prod",
                  "logText": "ERROR write failed: no space left on device"
                }
                """;

        mockMvc.perform(post("/api/analyze-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.severity").value("ERROR"))
                .andExpect(jsonPath("$.result.detectedIssues[0]").value("Disk capacity issue"))
                .andExpect(jsonPath("$.result.impactScope").value("single-service"))
                .andExpect(jsonPath("$.result.immediateActionRequired").value(true));
    }

    @Test
    void analysisHistoryReturnsSavedRecords() throws Exception {
        String body = """
                {
                  "serviceName": "auth-service",
                  "environment": "test",
                  "logText": "WARN token unauthorized 401"
                }
                """;

        String createdJson = mockMvc.perform(post("/api/analyze-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(createdJson, "$.id");

        mockMvc.perform(get("/api/analysis-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", not(blankOrNullString())));

        mockMvc.perform(get("/api/analysis-history/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.serviceName").value("auth-service"))
                .andExpect(jsonPath("$.result.detectedIssues[0]").value("Authentication or authorization failure"));
    }

    @Test
    void analyzeLogRejectsBlankLogText() throws Exception {
        mockMvc.perform(post("/api/analyze-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesOperationsConsole() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OpsPilot")))
                .andExpect(content().string(containsString("id=\"analysis-form\"")));
    }

    @Test
    void analyzeLogRedactsSensitiveDataBeforeSavingHistory() throws Exception {
        String body = """
                {
                  "serviceName": "secure-service",
                  "environment": "test",
                  "logText": "ERROR request failed password=secret-value Authorization: Bearer abcdefghijklmnop at 192.168.1.20"
                }
                """;

        mockMvc.perform(post("/api/analyze-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logPreview", containsString("password=[REDACTED]")))
                .andExpect(jsonPath("$.logPreview", containsString("Bearer [REDACTED]")))
                .andExpect(jsonPath("$.logPreview", containsString("[REDACTED_IP]")))
                .andExpect(jsonPath("$.logPreview", not(containsString("secret-value"))))
                .andExpect(jsonPath("$.logPreview", not(containsString("abcdefghijklmnop"))));
    }

    @Test
    void modelCallAuditEndpointReturnsSanitizedFailure() throws Exception {
        modelCallAuditService.recordFailure(
                new LogAnalysisRequest("audit-service", "test", "safe log"),
                "test-model",
                123,
                new IllegalStateException("401 Authorization: Bearer abcdefghijklmnop"));

        mockMvc.perform(get("/api/model-call-audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("audit-service"))
                .andExpect(jsonPath("$[0].modelName").value("test-model"))
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].durationMs").value(123))
                .andExpect(jsonPath("$[0].errorSummary", containsString("Bearer [REDACTED]")))
                .andExpect(jsonPath("$[0].errorSummary", not(containsString("abcdefghijklmnop"))));
    }

    @Test
    void analyzeLogFileUsesExistingSanitizationAndAnalysisPipeline() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payment.log",
                MediaType.TEXT_PLAIN_VALUE,
                "ERROR database timeout password=file-secret host=10.20.30.40".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/analyze-log-file")
                        .file(file)
                        .param("serviceName", "payment-service")
                        .param("environment", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("payment-service"))
                .andExpect(jsonPath("$.environment").value("test"))
                .andExpect(jsonPath("$.logPreview", containsString("password=[REDACTED]")))
                .andExpect(jsonPath("$.logPreview", containsString("[REDACTED_IP]")))
                .andExpect(jsonPath("$.logPreview", not(containsString("file-secret"))))
                .andExpect(jsonPath("$.result.analysisSource").value("rule"));
    }

    @Test
    void analyzeLogFileRejectsUnsupportedExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "events.csv", MediaType.TEXT_PLAIN_VALUE, "ERROR timeout".getBytes());

        mockMvc.perform(multipart("/api/analyze-log-file").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyzeLogFileRejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.log", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/analyze-log-file").file(file))
                .andExpect(status().isBadRequest());
    }
}
