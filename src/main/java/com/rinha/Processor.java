package com.rinha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.dto.StatusResponse;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Processor {

    @Value("${DEFAULT_URL:http://localhost:8001}")
    private String defaultUrl;
    @Value("${FALLBACK_URL:http://localhost:8002}")
    private String fallbackUrl;
    private final HikariDataSource dataSource;
    private final RedisTemplate<String, Payment> paymentTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AtomicBoolean shouldUseFallback = new AtomicBoolean(false);
    private final AtomicBoolean skipCalls = new AtomicBoolean(false);

    public Processor(HikariDataSource dataSource, RedisTemplate<String, Payment> redisTemplate, ObjectMapper objectMapper, HttpClient httpClient) {
        this.dataSource = dataSource;
        this.paymentTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    private Integer callService(Payment payment)  {

        payment.setRequestedAt(Instant.now());
        Integer status = callPayment(payment);

        if(status >= 500) {
            shouldUseFallback.set(true);
            status = callFallbackPayment(payment);
            if(status == 200) payment.setFallback(true);
            else if(status == 500) skipCalls.set(true);
        }

        return status;
    }

    private void insertPayment(Payment payment) {

        String query = "INSERT INTO payment (amount, requested_at, fallback) VALUES (?, ?, ?)";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setBigDecimal(1, payment.getAmount());
            preparedStatement.setTimestamp(2, Timestamp.from(payment.getRequestedAt()));
            preparedStatement.setBoolean(3, payment.getFallback());

            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(Payment payment) {

//        if(skipCalls.get()) {
//            paymentTemplate.convertAndSend("payments", payment);
//            return;
//        }

        int status = callService(payment);

        if(status == 200) {
            insertPayment(payment);
        } else {
            payment.setRequestedAt(null);
            payment.setFallback(false);
            paymentTemplate.convertAndSend("payments", payment);
        }
    }

//    @Scheduled(fixedRate = 5000)
    public void status() {

        StatusResponse statusDefault = callStatus();
        StatusResponse statusFallback = callStatusFallback();
        boolean f = statusDefault.failing();

        if(statusFallback.failing()) {
            f = false;
            skipCalls.set(true);
        }

        if(!statusDefault.failing() || !statusFallback.failing()) skipCalls.set(false);

        if(!statusDefault.failing() && !statusFallback.failing()) {
            f = statusDefault.minResponseTime() - statusFallback.minResponseTime() > 100;
        }

        shouldUseFallback.set(f);
    }

    private StatusResponse callStatus() {

        try {
            HttpGet request = new HttpGet(defaultUrl + "/payments/service-health");

            return httpClient.execute(request, classicHttpResponse ->
                    objectMapper.readValue(classicHttpResponse.getEntity().getContent().readAllBytes(), StatusResponse.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private StatusResponse callStatusFallback() {

        try {
            HttpGet request = new HttpGet(fallbackUrl + "/payments/service-health");

            return httpClient.execute(request, classicHttpResponse ->
                    objectMapper.readValue(classicHttpResponse.getEntity().getContent().readAllBytes(), StatusResponse.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Integer callPayment(Payment paymentRequest) {

        try {
            HttpPost request = new HttpPost(defaultUrl + "/payments");
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(paymentRequest)));
            request.setHeader("Content-type", "application/json");

            return httpClient.execute(request, HttpResponse::getCode);
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return 0;
    }

    private Integer callFallbackPayment(Payment paymentRequest) {

        try {
            HttpPost request = new HttpPost(fallbackUrl + "/payments");
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(paymentRequest)));
            request.setHeader("Content-type", "application/json");

            return httpClient.execute(request, HttpResponse::getCode);
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return 0;
    }
}
