package com.rinha.config;

import com.rinha.client.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class HttpClientConfig {

    @Value("${DEFAULT_URL:http://localhost:8001}")
    private String defaultUrl;
    @Value("${FALLBACK_URL:http://localhost:8002}")
    private String fallbackUrl;

    private static final Logger log = LoggerFactory.getLogger(HttpClientConfig.class);

    @Bean
    public PaymentProcessor defaultProcessor() {
        RestClient restClient = RestClient.builder().baseUrl(defaultUrl)
                .requestInterceptor((request, body, execution) -> {
                    logRequest(request, body);
                    var response = execution.execute(request, body);
                    logResponse(request, response);
                    return response;
                })
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

//        WebClient webClient = WebClient.builder().baseUrl(defaultUrl).build();
//        WebClientAdapter adapter = WebClientAdapter.create(webClient);
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(PaymentProcessor.class);
    }

    @Bean
    public PaymentProcessor fallbackProcessor() {
        RestClient restClient = RestClient.builder().baseUrl("https://api.github.com/").build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

//        WebClient webClient = WebClient.builder().baseUrl(fallbackUrl).build();
//        WebClientAdapter adapter = WebClientAdapter.create(webClient);
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(PaymentProcessor.class);
    }


    private void logRequest(HttpRequest request, byte[] body) {
        log.info("Request: {} {}", request.getMethod(), request.getURI());
//        logHeaders(request.getHeaders());
        if (body != null && body.length > 0) {
            log.info("Request body: {}", new String(body, StandardCharsets.UTF_8));
        }
    }

    private void logResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
        log.info("Response status: {}", response.getStatusCode());
//        logHeaders(response.getHeaders());
        byte[] responseBody = response.getBody().readAllBytes();
        if (responseBody.length > 0) {
            log.info("Response body: {}", new String(responseBody, StandardCharsets.UTF_8));
        }
    }
}
