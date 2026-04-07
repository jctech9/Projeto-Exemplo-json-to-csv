package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "export.api")
public class ExportApiProperties {

    private int pageSize = 5000;
    private final Http http = new Http();

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Http getHttp() {
        return http;
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private final Retry retry = new Retry();

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Retry getRetry() {
            return retry;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private Duration initialInterval = Duration.ofMillis(400);
        private Duration maxInterval = Duration.ofSeconds(3);
        private double multiplier = 2.0;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(Duration initialInterval) {
            this.initialInterval = initialInterval;
        }

        public Duration getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(Duration maxInterval) {
            this.maxInterval = maxInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}