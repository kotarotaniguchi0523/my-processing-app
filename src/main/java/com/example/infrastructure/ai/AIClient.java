package com.example.infrastructure.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for AI client implementations.
 * Provides common interface for interacting with various AI APIs (Claude, Gemini, etc.)
 */
public abstract class AIClient {
    
    protected final String apiKey;
    protected final AIClientConfig config;
    
    /**
     * Constructor for AIClient
     * @param apiKey API key for authentication
     * @param config Configuration for the client
     */
    protected AIClient(String apiKey, AIClientConfig config) {
        this.apiKey = validateApiKey(apiKey);
        this.config = config != null ? config : AIClientConfig.defaultConfig();
    }
    
    /**
     * Sends a chat completion request to the AI service
     * @param request The chat request containing messages and parameters
     * @return CompletableFuture with the AI response
     */
    public abstract CompletableFuture<AIResponse> chatCompletion(ChatRequest request);
    
    /**
     * Sends a synchronous chat completion request
     * @param request The chat request
     * @return The AI response
     * @throws AIClientException if the request fails
     */
    public AIResponse chatCompletionSync(ChatRequest request) throws AIClientException {
        try {
            return chatCompletion(request).get();
        } catch (Exception e) {
            throw new AIClientException("Failed to complete chat request", e);
        }
    }
    
    /**
     * Streams a chat completion response
     * @param request The chat request
     * @param handler Handler for processing streamed chunks
     * @return CompletableFuture that completes when streaming is done
     */
    public abstract CompletableFuture<Void> streamChatCompletion(
        ChatRequest request, 
        StreamHandler handler
    );
    
    /**
     * Tests the connection to the AI service
     * @return true if connection is successful
     */
    public abstract boolean testConnection();
    
    /**
     * Gets the name of the AI provider
     * @return Provider name (e.g., "Claude", "Gemini")
     */
    public abstract String getProviderName();
    
    /**
     * Gets the model capabilities
     * @return Map of capability names to their availability
     */
    public abstract Map<String, Boolean> getCapabilities();
    
    /**
     * Validates the API key format
     * @param apiKey The API key to validate
     * @return The validated API key
     * @throws IllegalArgumentException if API key is invalid
     */
    protected String validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        return apiKey.trim();
    }
    
    /**
     * Gets the current configuration
     * @return The client configuration
     */
    public AIClientConfig getConfig() {
        return config;
    }
    
    /**
     * Interface for handling streamed responses
     */
    public interface StreamHandler {
        /**
         * Called when a new chunk is received
         * @param chunk The response chunk
         */
        void onChunk(String chunk);
        
        /**
         * Called when an error occurs during streaming
         * @param error The error that occurred
         */
        void onError(Throwable error);
        
        /**
         * Called when streaming is complete
         */
        void onComplete();
    }
}