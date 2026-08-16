package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.config.AiModelProperties;
import com.example.model.LogAnalysisRequest;
import com.example.model.LogAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class OpenAiCompatibleLogAnalysisClientTests {
    @Test
    void sendsResponsesRequestAndConvertsModelJson() {
        AiModelProperties properties = new AiModelProperties();
        properties.setModel("gpt-5.5");
        properties.setReasoningEffort("medium");
        properties.setStoreResponses(false);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://model.test/");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiCompatibleLogAnalysisClient client = new OpenAiCompatibleLogAnalysisClient(
                properties, new ObjectMapper(), builder.build());

        server.expect(requestTo("http://model.test/responses"))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"model\":\"gpt-5.5\""),
                        org.hamcrest.Matchers.containsString("\"effort\":\"medium\""),
                        org.hamcrest.Matchers.containsString("\"store\":false"))))
                .andRespond(withSuccess("""
                        {
                          "output": [{
                            "type": "message",
                            "content": [{
                              "type": "output_text",
                              "text": "{\\\"severity\\\":\\\"CRITICAL\\\",\\\"summary\\\":\\\"Connection pool exhaustion is blocking database traffic.\\\",\\\"detectedIssues\\\":[\\\"Connection pool exhausted\\\"],\\\"possibleRootCauses\\\":[\\\"Slow queries retained all connections\\\"],\\\"impactScope\\\":\\\"dependency\\\",\\\"confidence\\\":0.92,\\\"immediateActionRequired\\\":true,\\\"recommendedActions\\\":[\\\"Increase pool capacity temporarily\\\"],\\\"investigationSteps\\\":[\\\"Inspect active database sessions\\\"]}"
                            }]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        LogAnalysisResponse ruleResult = new LogAnalysisResponse(
                "ERROR", "rule", List.of("Database access issue"), List.of("Database issue"),
                "dependency", 0.86, true, List.of("Check database"), List.of("Inspect metrics"),
                2, 1, Instant.now(), "rule", null);

        LogAnalysisResponse result = client.analyze(
                new LogAnalysisRequest("order-service", "prod", "ERROR connection pool exhausted"),
                ruleResult);

        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.analysisSource()).isEqualTo("llm");
        assertThat(result.modelName()).isEqualTo("gpt-5.5");
        assertThat(result.errorCount()).isEqualTo(2);
        assertThat(result.warningCount()).isEqualTo(1);
        server.verify();
    }
}
