package com.example.demo.application.export.input.api;

public class ApiDataFetchException extends RuntimeException {

    private final String endpoint;
    private final String url;

    public ApiDataFetchException(String message, String endpoint, String url) {
        super(message);
        this.endpoint = endpoint;
        this.url = url;
    }

    public ApiDataFetchException(String message, String endpoint, String url, Throwable cause) {
        super(message, cause);
        this.endpoint = endpoint;
        this.url = url;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getUrl() {
        return url;
    }
}