package com.rinha.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

@Serdeable
public record PaymentSummary(Long totalRequests, BigDecimal totalAmount) {
}
