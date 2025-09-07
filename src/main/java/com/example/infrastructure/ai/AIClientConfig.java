package com.example.infrastructure.ai;

import java.time.Duration;

/**
 * Configuration class for AI client settings
 */
public class AIClientConfig {
    
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryDelay;
    private final boolean enableLogging;
    private final boolean enableStreaming;
    private final String baseUrl;
    private final String modelName;
    
    private AIClientConfig(Builder builder) {
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.retryDelay = builder.retryDelay;
        this.enableLogging = builder.enableLogging;
        this.enableStreaming = builder.enableStreaming;
        this.baseUrl = builder.baseUrl;
        this.modelName = builder.modelName;
    }
    
    public static AIClientConfig defaultConfig() {
        return new Builder().build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public Duration getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public Duration getRetryDelay() { return retryDelay; }
    public boolean isLoggingEnabled() { return enableLogging; }
    public boolean isStreamingEnabled() { return enableStreaming; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    
    /**
     * Builder for AIClientConfig
     */
    public static class Builder {
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private Duration retryDelay = Duration.ofSeconds(1);
        private boolean enableLogging = true;
        private boolean enableStreaming = false;
        private String baseUrl;
        private String modelName;
        
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries must be non-negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder retryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }
        
        public Builder enableLogging(boolean enable) {
            this.enableLogging = enable;
            return this;
        }
        
        public Builder enableStreaming(boolean enable) {
            this.enableStreaming = enable;
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }
        
        public AIClientConfig build() {
            return new AIClientConfig(this);
        }
    }
}