package com.rinha.client;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.StatusResponse;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface PaymentProcessor {

    @PostExchange("/payments")
    @Retry(name = "defaultPayment")
    ResponseEntity<Void> processPayment(@RequestBody PaymentRequest paymentRequest);

    @GetExchange(value = "/payments/service-health")
    StatusResponse status();

    @PostExchange(value = "/admin/purge-payments", headers = "X-Rinha-Token=123")
    ResponseEntity<Object> purgePayments();
}
