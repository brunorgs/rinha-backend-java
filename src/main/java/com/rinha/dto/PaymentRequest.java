package com.rinha.dto;

import com.rinha.model.Payment;

import java.io.Serializable;
import java.math.BigDecimal;

public record PaymentRequest(String correlationId, BigDecimal amount, String requestedAt) implements Serializable {

    public Payment toModel() {
        return new Payment(correlationId, amount);
    }
}
