package com.rinha.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentSummary;
import com.rinha.dto.PaymentSummaryResponse;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

@RestController
public class PaymentController {

    private final HikariDataSource dataSource;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PaymentController(HikariDataSource dataSource, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/payments")
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest request) throws IOException {

        rabbitTemplate.convertAndSend("rinhaExchange", "payment", objectMapper.writeValueAsString(request));

        return ResponseEntity.status(HttpStatus.CREATED).build();
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
}