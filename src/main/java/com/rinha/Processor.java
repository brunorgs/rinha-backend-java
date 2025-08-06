package com.rinha;

import com.rinha.config.Config;
import com.rinha.dto.PaymentRequest;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableAsync
@Component
public class Processor {

    private final HikariDataSource dataSource;
    private final Config httpClientConfig;

    public Processor(HikariDataSource dataSource, Config httpClientConfig) {
        this.dataSource = dataSource;
        this.httpClientConfig = httpClientConfig;
    }

    @Bean
    public ExecutorService processPaymentExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Async("processPaymentExecutor")
    @Scheduled(fixedRate = 10)
    public void queuePop() {

        long start = System.currentTimeMillis();
        String query = "delete from queue qe where qe.id = (select id from queue q order by q.created_at asc limit 1 for update skip locked) returning qe.correlation_id, qe.amount;";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery()) {

            while(rs.next()) {

                Payment payment = new Payment(
                        rs.getString(1),
                        rs.getObject(2, BigDecimal.class)
                );

                int status = callService(payment);

                if(status == 200) {
                    insertPayment(payment, conn);
                }

                if(status == 500) {
                    queueInsert(new PaymentRequest(payment.getCorrelationId(), payment.getAmount(), null), conn);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("TIME " + (System.currentTimeMillis() - start));
    }

    private Integer callService(Payment payment)  {

        payment.setRequestedAt(Instant.now());
        Integer status = httpClientConfig.callPayment(new PaymentRequest(payment.getCorrelationId(), payment.getAmount(), payment.getRequestedAt().toString()), false);

        if(status >= 500) {
            status = httpClientConfig.callPayment(new PaymentRequest(payment.getCorrelationId(), payment.getAmount(), payment.getRequestedAt().toString()), true);
            if(status == 200) payment.setFallback(true);
        }

        return status;
    }

    private void queueInsert(PaymentRequest request, Connection conn) {

        String query = "INSERT INTO queue (correlation_id, amount) VALUES (?, ?)";

        try(PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, request.correlationId());
            preparedStatement.setBigDecimal(2, request.amount());

            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertPayment(Payment payment, Connection conn) {

        String query = "INSERT INTO payment (amount, requested_at, fallback) VALUES (?, ?, ?)";

        try(PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setBigDecimal(1, payment.getAmount());
            preparedStatement.setTimestamp(2, Timestamp.from(payment.getRequestedAt()));
            preparedStatement.setBoolean(3, payment.getFallback());

            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
