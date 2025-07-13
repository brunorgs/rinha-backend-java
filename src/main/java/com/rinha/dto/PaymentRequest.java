package com.rinha.dto;

import com.rinha.model.Payment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequest(String correlationId, BigDecimal amount, String requestedAt) implements Serializable {

    public Payment toModel() {
        return new Payment(correlationId, amount, false, Instant.parse(requestedAt));
    }
}
