package com.rinha.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.io.Serializable;
import java.math.BigDecimal;

@Serdeable
public record PaymentRequest(String correlationId, BigDecimal amount) implements Serializable {
}
