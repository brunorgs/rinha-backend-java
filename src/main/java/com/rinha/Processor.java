package com.rinha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.logging.Logger;

@Component
public class Processor {

    @Value("${DEFAULT_URL:http://localhost:8001}")
    private String defaultUrl;
    @Value("${FALLBACK_URL:http://localhost:8002}")
    private String fallbackUrl;
    private final HikariDataSource dataSource;
    private final RedisTemplate<String, Payment> paymentTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient javaHttpClient;
    private static final Logger log = Logger.getLogger(Processor.class.getName());

    public Processor(HikariDataSource dataSource, RedisTemplate<String, Payment> redisTemplate, ObjectMapper objectMapper, HttpClient javaHttpClient) {
        this.dataSource = dataSource;
        this.paymentTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.javaHttpClient = javaHttpClient;
    }

    public void handleMessage(Payment payment) {

        Mono.fromSupplier(() -> callService(payment))
                .flatMap(status -> {
                    if(status == 200) {
                        insertPayment(payment);
                    }
                    else if (status == 500) {
                        payment.setRequestedAt(null);
                        payment.setFallback(false);
                        paymentTemplate.convertAndSend("payments", payment);
                    }
                    return Mono.justOrEmpty(status);
                })
                .block();
    }

    private int callService(Payment payment)  {

        payment.setRequestedAt(Instant.now());
        int status = callJavaPayment(payment);

        if(status == 500 || status == 0) {

            status = callFallbackPayment(payment);
            if(status == 200) payment.setFallback(true);
        }

        return status;
    }

    private void insertPayment(Payment payment) {

        String query = payment.getFallback() ? "INSERT INTO payment_fallback (amount, requested_at) VALUES (?, ?)"
        : "INSERT INTO payment (amount, requested_at) VALUES (?, ?)";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setBigDecimal(1, payment.getAmount());
            preparedStatement.setObject(2, payment.getRequestedAt().atOffset(ZoneOffset.UTC));

            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer callJavaPayment(Payment paymentRequest) {

        try {
            return javaHttpClient.send(getJavaRequest(defaultUrl, paymentRequest), java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return 500;
    }

    private Integer callFallbackPayment(Payment paymentRequest) {

        try {
            return javaHttpClient.send(getJavaRequest(fallbackUrl, paymentRequest), java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return 500;
    }

    private HttpRequest getJavaRequest(String url, Payment paymentRequest) throws IOException {

        return HttpRequest.newBuilder()
                .uri(URI.create(url + "/payments"))
                .header("Content-type", "application/json")
                .timeout(Duration.ofSeconds(1))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(paymentRequest)))
                .build();
    }
}
