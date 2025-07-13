package com.rinha.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record PaymentSummaryResponse(PaymentSummary defaultSummary, PaymentSummary fallback) {
    public PaymentSummary getDefault() {
        return defaultSummary;
    }

    public PaymentSummary getFallback() {
        return fallback;
    }
}
