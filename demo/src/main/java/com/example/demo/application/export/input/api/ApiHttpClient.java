package com.example.demo.application.export.input.api;

import com.example.demo.config.ExportApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class ApiHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ApiHttpClient.class);

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final ExportApiProperties properties;

    public ApiHttpClient(
            RestTemplate exportApiRestTemplate,
            RetryTemplate exportApiRetryTemplate,
            ExportApiProperties properties
    ) {
        this.restTemplate = exportApiRestTemplate;
        this.retryTemplate = exportApiRetryTemplate;
        this.properties = properties;
    }

    public Map<String, Object> fetchPage(String baseUrl, String endpoint) {
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        URI uri = buildUri(baseUrl, normalizedEndpoint);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = retryTemplate.execute(context -> {
                int attempt = context.getRetryCount() + 1;
                log.debug("event=api_request endpoint={} attempt={} url={}", normalizedEndpoint, attempt, uri);

                try {
                    return restTemplate.getForObject(uri, Map.class);
                } catch (RuntimeException ex) {
                    log.warn(
                            "event=api_request_attempt_failed endpoint={} attempt={} url={} message={}",
                            normalizedEndpoint,
                            attempt,
                            uri,
                            ex.getMessage()
                    );
                    throw ex;
                }
            });

            if (response == null) {
                log.error("event=api_request_empty_response endpoint={} url={}", normalizedEndpoint, uri);
                throw new ApiDataFetchException(
                        "Resposta vazia ao consultar endpoint " + normalizedEndpoint,
                        normalizedEndpoint,
                        uri.toString()
                );
            }

            return response;
        } catch (ApiDataFetchException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            log.error(
                    "event=api_request_http_error endpoint={} url={} status={} message={}",
                    normalizedEndpoint,
                    uri,
                    statusCode,
                    ex.getStatusText(),
                    ex
            );
            throw new ApiDataFetchException(
                    "Falha HTTP ao consultar endpoint " + normalizedEndpoint + " (status=" + statusCode + ")",
                    normalizedEndpoint,
                    uri.toString(),
                    ex
            );
        } catch (RuntimeException ex) {
            log.error(
                    "event=api_request_failed endpoint={} url={} message={}",
                    normalizedEndpoint,
                    uri,
                    ex.getMessage(),
                    ex
            );
            throw new ApiDataFetchException(
                    "Falha ao consultar endpoint " + normalizedEndpoint + " apos tentativas de retry",
                    normalizedEndpoint,
                    uri.toString(),
                    ex
            );
        }
    }

    private URI buildUri(String baseUrl, String endpoint) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl, endpoint);
        return UriComponentsBuilder.fromUriString(normalizedBaseUrl)
                .path(endpoint)
                .queryParam("page", 0)
                .queryParam("size", properties.getPageSize())
                .build(true)
                .toUri();
    }

    private String normalizeBaseUrl(String baseUrl, String endpoint) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApiDataFetchException(
                    "Parametro baseUrl nao pode ser vazio",
                    endpoint,
                    String.valueOf(baseUrl)
            );
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }
}