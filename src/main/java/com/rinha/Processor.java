package com.rinha;

import com.rinha.config.Config;
import com.rinha.dto.StatusResponse;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Processor {

    private final HikariDataSource dataSource;
    private final Config httpClientConfig;
    private final RedisTemplate<String, Payment> paymentTemplate;
    private final AtomicBoolean shouldUseFallback = new AtomicBoolean(false);
    private final AtomicBoolean skipCalls = new AtomicBoolean(false);

    public Processor(HikariDataSource dataSource, Config httpClientConfig, RedisTemplate<String, Payment> redisTemplate) {
        this.dataSource = dataSource;
        this.httpClientConfig = httpClientConfig;
        this.paymentTemplate = redisTemplate;
    }

    private Integer callService(Payment payment)  {

        payment.setRequestedAt(Instant.now());
        Integer status = httpClientConfig.callPayment(payment, false);

        if(status >= 500) {
            shouldUseFallback.set(true);
            payment.setRequestedAt(Instant.now());
            status = httpClientConfig.callPayment(payment, true);
            if(status == 200) payment.setFallback(true);
            if(status == 500) skipCalls.set(true);
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

    public void onMessage(Payment payment) {

        if(skipCalls.get()) {
            paymentTemplate.convertAndSend("payments", payment);
            return;
        }

        int status = callService(payment);

        if(status == 200) {
            insertPayment(payment);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void status() {

        StatusResponse statusDefault = httpClientConfig.callStatus(false);
        StatusResponse statusFallback = httpClientConfig.callStatus(true);
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
}
