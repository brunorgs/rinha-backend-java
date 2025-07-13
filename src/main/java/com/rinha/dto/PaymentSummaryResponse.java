package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentSummaryResponse(@JsonProperty("default") PaymentSummary defaultSummary, PaymentSummary fallback) {
}
