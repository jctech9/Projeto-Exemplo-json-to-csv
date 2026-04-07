package com.example.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(ExportApiProperties.class)
public class HttpClientConfig {

    @Bean
    public RestTemplate exportApiRestTemplate(ExportApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getHttp().getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getHttp().getReadTimeout().toMillis());
        return new RestTemplate(requestFactory);
    }

    @Bean
    public RetryTemplate exportApiRetryTemplate(ExportApiProperties properties) {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(HttpServerErrorException.class, true);
        retryableExceptions.put(UnknownHttpStatusCodeException.class, true);
        retryableExceptions.put(HttpClientErrorException.TooManyRequests.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                properties.getHttp().getRetry().getMaxAttempts(),
                retryableExceptions,
                true
        );

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getHttp().getRetry().getInitialInterval().toMillis());
        backOffPolicy.setMaxInterval(properties.getHttp().getRetry().getMaxInterval().toMillis());
        backOffPolicy.setMultiplier(properties.getHttp().getRetry().getMultiplier());

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}