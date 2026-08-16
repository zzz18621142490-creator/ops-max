package com.example.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.model")
public class AiModelProperties {
    private boolean enabled;
    private String baseUrl = "https://api.deepseek.com/";
    private String apiKey = "";
    private String model = "deepseek-v4-flash";
    private String reasoningEffort = "medium";
    private boolean storeResponses;
    private boolean allowInsecureHttp;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(45);
    private int maxInputChars = 20_000;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && isTransportAllowed();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public boolean isStoreResponses() {
        return storeResponses;
    }

    public void setStoreResponses(boolean storeResponses) {
        this.storeResponses = storeResponses;
    }

    public boolean isAllowInsecureHttp() {
        return allowInsecureHttp;
    }

    public void setAllowInsecureHttp(boolean allowInsecureHttp) {
        this.allowInsecureHttp = allowInsecureHttp;
    }

    private boolean isTransportAllowed() {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme())
                || allowInsecureHttp
                || "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }
}
