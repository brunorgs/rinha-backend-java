package com.rinha.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {
    private UUID id;
    private String correlationId;
    private BigDecimal amount;
    private boolean fallback;
    private Instant requestedAt;

    public Payment(String correlationId, BigDecimal amount) {
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public Payment(UUID id, String correlationId, BigDecimal amount) {
        this.id = id;
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public Payment() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Boolean getFallback() {
        return fallback;
    }

    public void setFallback(Boolean fallback) {
        this.fallback = fallback;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
}
