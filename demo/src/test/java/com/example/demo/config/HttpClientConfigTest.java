package com.example.demo.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpClientConfigTest {

    @Test
    void shouldNotFollowRedirects() throws Exception {
        AtomicInteger redirectedTargetHits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add(
                    "Location",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/target"
            );
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> {
            redirectedTargetHits.incrementAndGet();
            byte[] body = "unexpected".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            ExportApiProperties properties = new ExportApiProperties();
            RestTemplate restTemplate = new HttpClientConfig().exportApiRestTemplate(properties);
            URI redirectUri = URI.create(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect"
            );

            ResponseEntity<String> response = restTemplate.getForEntity(redirectUri, String.class);

            assertEquals(302, response.getStatusCode().value());
            assertEquals(0, redirectedTargetHits.get());
        } finally {
            server.stop(0);
        }
    }
}
