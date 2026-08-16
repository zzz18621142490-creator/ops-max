package com.example.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "model_call_audit")
public class ModelCallAuditEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private long durationMs;

    @Column(length = 800)
    private String errorSummary;

    @Column(nullable = false)
    private Instant createdAt;

    protected ModelCallAuditEntity() {
    }

    public ModelCallAuditEntity(String id, String serviceName, String environment, String modelName,
                                String status, long durationMs, String errorSummary, Instant createdAt) {
        this.id = id;
        this.serviceName = serviceName;
        this.environment = environment;
        this.modelName = modelName;
        this.status = status;
        this.durationMs = durationMs;
        this.errorSummary = errorSummary;
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

    public String getModelName() {
        return modelName;
    }

    public String getStatus() {
        return status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
