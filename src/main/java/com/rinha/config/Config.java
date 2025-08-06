package com.rinha.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.dto.PaymentRequest;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Config {

    @Value("${DEFAULT_URL:http://localhost:8001}")
    private String defaultUrl;
    @Value("${FALLBACK_URL:http://localhost:8002}")
    private String fallbackUrl;

    private final ObjectMapper objectMapper;

    public Config(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CloseableHttpClient httpClient() {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        return HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    public Integer callPayment(PaymentRequest paymentRequest, boolean fallback) {

        String url = fallback ? fallbackUrl : defaultUrl;

        try (CloseableHttpClient httpClient = httpClient()) {
            HttpPost request = new HttpPost(url + "/payments");
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(paymentRequest)));
            request.setHeader("Content-type", "application/json");

            return httpClient.execute(request, new HttpClientResponseHandler<Integer>() {
                @Override
                public Integer handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {

                    if(classicHttpResponse.getCode() == 422) {
                        System.out.println(new String(classicHttpResponse.getEntity().getContent().readAllBytes()));
                    }

                    return classicHttpResponse.getCode();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Bean
    public HikariDataSource hikariDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/rinha?currentSchema=public&user=postgres&password=postgres");
//        ds.setMaximumPoolSize(18);
        return ds;
    }
}
