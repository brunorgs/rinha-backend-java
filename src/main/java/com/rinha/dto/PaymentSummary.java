package com.rinha.dto;

import java.math.BigDecimal;

public record PaymentSummary(Integer totalRequests, BigDecimal totalAmount) {
}
