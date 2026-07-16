package com.example.demo.application.export.input.api;

public class ApiDataFetchException extends RuntimeException {

    private final String endpoint;
    public ApiDataFetchException(String message, String endpoint) {
        super(message);
        this.endpoint = endpoint;
    }

    public ApiDataFetchException(String message, String endpoint, Throwable cause) {
        super(message, cause);
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
