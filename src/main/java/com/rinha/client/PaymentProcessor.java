package com.rinha.client;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.StatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface PaymentProcessor {

    @PostExchange("/payments")
    ResponseEntity<Object> processPayment(@RequestBody PaymentRequest paymentRequest);

    @GetExchange(value = "/payments/service-health")
    StatusResponse status();
}
