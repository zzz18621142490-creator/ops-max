package com.example.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_history")
public class AnalysisHistoryEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false, length = 240)
    private String logPreview;

    @Lob
    @Column(nullable = false)
    private String resultJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected AnalysisHistoryEntity() {
    }

    public AnalysisHistoryEntity(String id, String serviceName, String environment, String logPreview,
                                 String resultJson, Instant createdAt) {
        this.id = id;
        this.serviceName = serviceName;
        this.environment = environment;
        this.logPreview = logPreview;
        this.resultJson = resultJson;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getLogPreview() {
        return logPreview;
    }

    public String getResultJson() {
        return resultJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
