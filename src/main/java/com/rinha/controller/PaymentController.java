package com.rinha.controller;

import com.rinha.client.PaymentProcessor;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentSummary;
import com.rinha.dto.PaymentSummaryResponse;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());

    private final PaymentProcessor defaultProcessor;
    private final PaymentProcessor fallbackProcessor;
    private final HikariDataSource dataSource;

    public PaymentController(PaymentProcessor defaultProcessor, PaymentProcessor fallbackProcessor, HikariDataSource dataSource) {
        this.defaultProcessor = defaultProcessor;
        this.fallbackProcessor = fallbackProcessor;
        this.dataSource = dataSource;
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

        insert(paymentModel);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public void insert(Payment payment) {

        String query = "INSERT INTO payment (id, correlation_id, amount, requested_at, fallback) VALUES (gen_random_uuid(), ?, ?, ?, ?)";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, payment.getCorrelationId());
            preparedStatement.setBigDecimal(2, payment.getAmount());
            preparedStatement.setTimestamp(3, Timestamp.from(payment.getRequestedAt()));
            preparedStatement.setBoolean(4, payment.getFallback());

            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentsSummary(@RequestParam Instant from, @RequestParam Instant to) {
        return ResponseEntity.ok(getSummary(from, to));
    }

    public PaymentSummaryResponse getSummary(Instant from, Instant to) {

        String query = "select COUNT(*), SUM(amount) from payment where requested_at between ?::timestamp and ?::timestamp group by fallback;";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setTimestamp(1, Timestamp.from(from));
            preparedStatement.setTimestamp(2, Timestamp.from(to));

            ResultSet resultSet = preparedStatement.executeQuery();

            boolean next = resultSet.next();

            PaymentSummary d;
            if(next) d = new PaymentSummary(resultSet.getInt(1), resultSet.getObject(2, BigDecimal.class));
            else d = new PaymentSummary(0, BigDecimal.ZERO);

            next = resultSet.next();

            PaymentSummary f;
            if(next) f = new PaymentSummary(resultSet.getInt(1), resultSet.getObject(2, BigDecimal.class));
            else f = new PaymentSummary(0, BigDecimal.ZERO);

            PaymentSummaryResponse response = new PaymentSummaryResponse(d, f);

            resultSet.close();

            return response;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @PostMapping("purge-payments")
    public ResponseEntity<String> purgePayments() {

        String query = "delete from payment;";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query);) {
            preparedStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        defaultProcessor.purgePayments();
        fallbackProcessor.purgePayments();

        return ResponseEntity.ok(" OK");
    }
}