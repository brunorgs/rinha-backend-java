package com.rinha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.config.Config;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.StatusResponse;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
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

    private final HikariDataSource dataSource;
    private final Config httpClientConfig;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private AtomicBoolean shouldUseFallback = new AtomicBoolean(false);
    private AtomicBoolean skipCalls = new AtomicBoolean(false);

    public Processor(HikariDataSource dataSource, Config httpClientConfig, ObjectMapper objectMapper, RabbitTemplate rabbitTemplate) {
        this.dataSource = dataSource;
        this.httpClientConfig = httpClientConfig;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Integer callService(Payment payment)  {

        payment.setRequestedAt(Instant.now());
        Integer status = httpClientConfig.callPayment(new PaymentRequest(payment.getCorrelationId(), payment.getAmount(), payment.getRequestedAt().toString()), false);

        if(status >= 500) {
            shouldUseFallback.set(true);
            status = httpClientConfig.callPayment(new PaymentRequest(payment.getCorrelationId(), payment.getAmount(), payment.getRequestedAt().toString()), true);
            if(status == 200) payment.setFallback(true);
            if(status == 500) skipCalls.set(true);
        }

        return status;
    }

    public void insertPayment(Payment payment) {

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

    @RabbitListener(queues = "rinhaQueue", concurrency = "8")
    public void receive(@Payload Message message) throws IOException {

        long start = System.currentTimeMillis();
        if(skipCalls.get()) {
//            System.out.println("LOOP");
            rabbitTemplate.send("rinhaExchange", "payment", message);
            return;
        }
//        System.out.println(new String(message.getBody()));
        Payment payment = objectMapper.readValue(message.getBody(), Payment.class);

        long s= System.currentTimeMillis();
        int status = callService(payment);
//        System.out.println("TIME SERVICE " + (System.currentTimeMillis() - s));

//        System.out.println("STATUS " + status);

        if(status == 200) {
            s = System.currentTimeMillis();
            insertPayment(payment);
//            System.out.println("TIME INSERT " + (System.currentTimeMillis() - s));
        }

//        if(status != 200) System.out.println("STATUS " + status);
//        System.out.println("USE FALLBACK " + shouldUseFallback);
//        System.out.println("SKIP CALLS " + skipCalls);

//        System.out.println("TIME TOTAL " + (System.currentTimeMillis() - start));
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
