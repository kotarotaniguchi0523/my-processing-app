package com.example.infrastructure.ai;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Factory for creating AI client instances
 */
public class AIClientFactory {
    
    private static final Logger logger = Logger.getLogger(AIClientFactory.class.getName());
    
    /**
     * Creates an AI client for the specified provider using default configuration
     * @param provider The AI provider
     * @return The configured AI client
     */
    public static AIClient createClient(APIKeyManager.Provider provider) {
        return createClient(provider, AIClientConfig.defaultConfig());
    }
    
    /**
     * Creates an AI client for the specified provider with custom configuration
     * @param provider The AI provider
     * @param config Custom configuration
     * @return The configured AI client
     */
    public static AIClient createClient(APIKeyManager.Provider provider, AIClientConfig config) {
        String apiKey = APIKeyManager.getApiKey(provider);
        
        switch (provider) {
            case CLAUDE:
                logger.info("Creating Claude client");
                return new ClaudeClient(apiKey, config);
                
            case GEMINI:
                logger.info("Creating Gemini client");
                return new GeminiClient(apiKey, config);
                
            case OPENAI:
                logger.info("Creating OpenAI client");
                // OpenAI client would be implemented similarly
                throw new UnsupportedOperationException("OpenAI client not yet implemented");
                
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
    
    /**
     * Creates an AI client with the first available provider
     * @return The configured AI client
     * @throws IllegalStateException if no providers have API keys configured
     */
    public static AIClient createDefaultClient() {
        return createDefaultClient(AIClientConfig.defaultConfig());
    }
    
    /**
     * Creates an AI client with the first available provider and custom configuration
     * @param config Custom configuration
     * @return The configured AI client
     * @throws IllegalStateException if no providers have API keys configured
     */
    public static AIClient createDefaultClient(AIClientConfig config) {
        // Try providers in preference order
        APIKeyManager.Provider[] providers = {
            APIKeyManager.Provider.CLAUDE,
            APIKeyManager.Provider.GEMINI,
            APIKeyManager.Provider.OPENAI
        };
        
        for (APIKeyManager.Provider provider : providers) {
            if (APIKeyManager.hasApiKey(provider)) {
                logger.info("Using " + provider + " as default AI provider");
                try {
                    return createClient(provider, config);
                } catch (UnsupportedOperationException e) {
                    // Provider not implemented yet, try next
                    continue;
                }
            }
        }
        
        throw new IllegalStateException(
            "No AI provider API keys found. Please configure at least one API key in environment variables or .env file"
        );
    }
    
    /**
     * Creates a pre-configured client optimized for this application
     * @return The configured AI client
     */
    public static AIClient createApplicationClient() {
        AIClientConfig config = AIClientConfig.builder()
            .timeout(Duration.ofSeconds(60))
            .maxRetries(3)
            .retryDelay(Duration.ofSeconds(2))
            .enableLogging(true)
            .enableStreaming(false)  // Can be enabled later if needed
            .build();
        
        return createDefaultClient(config);
    }
}