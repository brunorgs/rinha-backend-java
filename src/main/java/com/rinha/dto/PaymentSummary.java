package com.rinha.dto;

import java.math.BigDecimal;

public record PaymentSummary(Long totalRequests, BigDecimal totalAmount) {
}
