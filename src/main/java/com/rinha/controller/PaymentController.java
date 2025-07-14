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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @PostMapping("/payments")
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest(UUID.randomUUID().toString(), request.amount(), ZonedDateTime.now().toInstant().toString());
        boolean fallback = false;

        try {
            defaultProcessor.processPayment(paymentRequest);
        } catch (Exception e) {
            fallbackProcessor.processPayment(paymentRequest);
            fallback = true;
        }

        Payment paymentModel = paymentRequest.toModel();
        paymentModel.setFallback(fallback);

        paymentRepository.save(paymentModel);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentsSummary(@RequestParam Instant from, @RequestParam Instant to) {

        List<Payment> payments = paymentRepository.findByRequestedAtBetween(from, to);

        Map<Boolean, List<Payment>> collect = payments.stream().collect(Collectors.groupingBy(Payment::getFallback));

        List<Payment> dPay = collect.getOrDefault(Boolean.FALSE, new ArrayList<>());
        List<Payment> fPay = collect.getOrDefault(Boolean.TRUE, new ArrayList<>());
        PaymentSummary defaultPayment = new PaymentSummary(dPay.size(), dPay.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        PaymentSummary fallbackPayment = new PaymentSummary(fPay.size(), fPay.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        PaymentSummaryResponse response = new PaymentSummaryResponse(defaultPayment, fallbackPayment);

        return ResponseEntity.ok(response);
    }

    @PostMapping("purge-payments")
    public ResponseEntity<String> purgePayments() {

        paymentRepository.deleteAll();
        defaultProcessor.purgePayments();
        fallbackProcessor.purgePayments();

        return ResponseEntity.ok(" OK");
    }
}