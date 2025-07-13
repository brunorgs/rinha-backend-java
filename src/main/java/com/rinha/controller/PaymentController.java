package com.rinha.controller;

import com.rinha.client.PaymentProcessor;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentSummary;
import com.rinha.dto.PaymentSummaryResponse;
import com.rinha.model.Payment;
import com.rinha.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessor defaultProcessor;
    private final PaymentProcessor fallbackProcessor;

    public PaymentController(PaymentRepository paymentRepository, PaymentProcessor defaultProcessor, PaymentProcessor fallbackProcessor) {
        this.paymentRepository = paymentRepository;
        this.defaultProcessor = defaultProcessor;
        this.fallbackProcessor = fallbackProcessor;
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest(UUID.randomUUID().toString(), request.amount(), ZonedDateTime.now().toInstant().toString());
        ResponseEntity<Object> response = defaultProcessor.processPayment(paymentRequest);

        System.out.println("Status " + response.getStatusCode());

        Payment paymentModel = paymentRequest.toModel();

        paymentRepository.save(paymentModel);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentsSummary(@RequestParam Instant from, @RequestParam Instant to) {

            PaymentSummaryResponse response = new PaymentSummaryResponse(
                    new PaymentSummary(43236L, new BigDecimal("415542345.98")),
                    new PaymentSummary(423545L, new BigDecimal("329347.34"))
            );

            return ResponseEntity.ok(response);
    }
}