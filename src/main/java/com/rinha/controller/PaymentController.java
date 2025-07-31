package com.rinha.controller;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentSummary;
import com.rinha.dto.PaymentSummaryResponse;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

@RestController
public class PaymentController {

    private final HikariDataSource dataSource;
    private final RedisTemplate<String, Payment> paymentTemplate;

    public PaymentController(HikariDataSource dataSource, RedisTemplate<String, Payment> paymentTemplate) {
        this.dataSource = dataSource;
        this.paymentTemplate = paymentTemplate;
    }

    @PostMapping("/payments")
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest request) {

        paymentTemplate.convertAndSend("payments", request.toModel());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentsSummary(@RequestParam Instant from, @RequestParam Instant to) {
        return ResponseEntity.ok(getSummary(from, to));
    }

    private PaymentSummaryResponse getSummary(Instant from, Instant to) {

        String query = "select COUNT(*), SUM(amount), fallback from payment where requested_at >= ?::timestamp and requested_at < ?::timestamp group by fallback;";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setTimestamp(1, Timestamp.from(from));
            preparedStatement.setTimestamp(2, Timestamp.from(to));

            ResultSet resultSet = preparedStatement.executeQuery();

            PaymentSummary d = new PaymentSummary(0, BigDecimal.ZERO);
            PaymentSummary f = new PaymentSummary(0, BigDecimal.ZERO);

            while(resultSet.next()) {
                if (!resultSet.getBoolean(3)) {
                    d = new PaymentSummary(resultSet.getInt(1), resultSet.getObject(2, BigDecimal.class));
                } else {
                    f = new PaymentSummary(resultSet.getInt(1), resultSet.getObject(2, BigDecimal.class));
                }
            }

            PaymentSummaryResponse response = new PaymentSummaryResponse(d, f);

            resultSet.close();

            return response;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}