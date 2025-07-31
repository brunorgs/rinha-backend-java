package com.rinha.config;

import com.rinha.Processor;
import com.rinha.model.Payment;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class Config {

    @Value("${DB_URL:jdbc:postgresql://localhost:5432/rinha?currentSchema=public&user=postgres&password=postgres}")
    private String dbUrl;
    @Value("${LISTENER_THREADS:4}")
    private Integer listenerThreads;

    @Bean
    public CloseableHttpClient httpClient() {

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        return HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    @Bean
    public HikariDataSource hikariDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        return ds;
    }


    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("payments"));
        container.setTaskExecutor(Executors.newFixedThreadPool(listenerThreads));

        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(Processor receiver) {
        MessageListenerAdapter receiveMessage = new MessageListenerAdapter(receiver);
        receiveMessage.setSerializer(new Jackson2JsonRedisSerializer<>(Payment.class));
        return receiveMessage;
    }

    @Bean
    public RedisTemplate<String, Payment> paymentTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Payment> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Payment.class));
        return redisTemplate;
    }
}
