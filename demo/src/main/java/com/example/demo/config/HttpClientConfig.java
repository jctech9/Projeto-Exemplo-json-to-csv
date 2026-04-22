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

import java.util.Map;

@Configuration
@EnableConfigurationProperties(ExportApiProperties.class)
public class HttpClientConfig {

    @Bean
    public RestTemplate exportApiRestTemplate(ExportApiProperties properties) {
        ExportApiProperties.Http http = properties.getHttp();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) http.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) http.getReadTimeout().toMillis());
        return new RestTemplate(requestFactory);
    }

    @Bean
    public RetryTemplate exportApiRetryTemplate(ExportApiProperties properties) {
        ExportApiProperties.Http http = properties.getHttp();
        ExportApiProperties.Retry retry = http.getRetry();

        // Erros transitórios: rede indisponível, 5xx e throttle (429).
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
            ResourceAccessException.class, true,
            HttpServerErrorException.class, true,
            UnknownHttpStatusCodeException.class, true,
            HttpClientErrorException.TooManyRequests.class, true
        );

        // "true" habilita varredura da causa raiz para não perder retries em exceções encapsuladas.
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retry.getMaxAttempts(),
                retryableExceptions,
                true
        );

        // Backoff exponencial reduz pressão no endpoint durante indisponibilidade temporária.
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retry.getInitialInterval().toMillis());
        backOffPolicy.setMaxInterval(retry.getMaxInterval().toMillis());
        backOffPolicy.setMultiplier(retry.getMultiplier());

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}