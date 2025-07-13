package com.rinha.controller;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentSummary;
import com.rinha.dto.PaymentSummaryResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Controller
public class PaymentController {

    @Post("/payment")
    public Mono<HttpResponse<Void>> processPayment(@Body PaymentRequest request) {
        return Mono.fromSupplier(() -> {
            return HttpResponse.status(HttpStatus.CREATED);
        });
    }

    @Get("/payments-summary")
    public Mono<HttpResponse<PaymentSummaryResponse>> getPaymentsSummary(@QueryValue Instant from, @QueryValue Instant to) {

        return Mono.fromSupplier(() -> {
            PaymentSummaryResponse response = new PaymentSummaryResponse(
                    new PaymentSummary(43236L, new BigDecimal("415542345.98")),
                    new PaymentSummary(423545L, new BigDecimal("329347.34"))
            );

            return HttpResponse.ok(response);
        });
    }
}