package com.example.demo.application.export.input.api;

import com.example.demo.config.ExportApiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiHttpClientTest {

    @Test
    void shouldAggregatePaginatedContentAcrossAllPages() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        RetryTemplate retryTemplate = new RetryTemplate();
        ExportApiProperties properties = new ExportApiProperties();
        properties.setPageSize(2);

        ApiHttpClient client = new ApiHttpClient(restTemplate, retryTemplate, properties);

        when(restTemplate.getForObject(any(URI.class), eq(Map.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            String query = uri.getQuery() == null ? "" : uri.getQuery();

            if (query.contains("page=0")) {
                return pageResponse(
                        2,
                        false,
                        List.of(
                                new LinkedHashMap<>(Map.of("id", 1)),
                                new LinkedHashMap<>(Map.of("id", 2))
                        )
                );
            }
            if (query.contains("page=1")) {
                return pageResponse(
                        2,
                        true,
                        List.of(new LinkedHashMap<>(Map.of("id", 3)))
                );
            }

            throw new IllegalStateException("Pagina inesperada: " + uri);
        });

        Map<String, Object> response = client.fetchAllPages("http://localhost:8090/", "/riscos");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        assertEquals(3, content.size());
        assertEquals(List.of(1, 2, 3), content.stream().map(item -> ((Number) item.get("id")).intValue()).toList());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate, times(2)).getForObject(uriCaptor.capture(), eq(Map.class));
        List<String> requestedUrls = uriCaptor.getAllValues().stream().map(URI::toString).toList();
        assertTrue(requestedUrls.stream().anyMatch(url -> url.contains("page=0")));
        assertTrue(requestedUrls.stream().anyMatch(url -> url.contains("page=1")));
    }

    private Map<String, Object> pageResponse(int totalPages, boolean last, List<Map<String, Object>> content) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", content);
        page.put("totalPages", totalPages);
        page.put("last", last);
        return page;
    }
}
