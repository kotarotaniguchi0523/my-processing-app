package com.example.infrastructure.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIClientFactory and provider selection logic
 */
class AIClientFactoryTest {

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        APIKeyManager.clearCache();
        
        // Clear any existing system properties
        System.clearProperty("CLAUDE_API_KEY");
        System.clearProperty("ANTHROPIC_API_KEY");
        System.clearProperty("GEMINI_API_KEY");
        System.clearProperty("GOOGLE_API_KEY");
        System.clearProperty("OPENAI_API_KEY");
    }

    @AfterEach
    void tearDown() {
        // Clear cache after each test
        APIKeyManager.clearCache();
        
        // Clean up system properties
        System.clearProperty("CLAUDE_API_KEY");
        System.clearProperty("ANTHROPIC_API_KEY");
        System.clearProperty("GEMINI_API_KEY");
        System.clearProperty("GOOGLE_API_KEY");
        System.clearProperty("OPENAI_API_KEY");
    }

    @Test
    void createClient_Claude_ShouldThrowUnsupportedOperationException() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, "test-claude-key");
        
        // Execute & Verify
        assertThrows(UnsupportedOperationException.class, () -> 
            AIClientFactory.createClient(APIKeyManager.Provider.CLAUDE));
    }

    @Test
    void createClient_GeminiWithDefaultConfig_ShouldReturnOptimizedGeminiClient() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-gemini-key");
        
        // Execute
        AIClient client = AIClientFactory.createClient(APIKeyManager.Provider.GEMINI);
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
        assertEquals("Gemini (Google)", client.getProviderName());
        assertNotNull(client.getConfig());
    }

    @Test
    void createClient_OpenAI_ShouldThrowUnsupportedOperationException() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.OPENAI, "test-openai-key");
        
        // Execute & Verify
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> 
            AIClientFactory.createClient(APIKeyManager.Provider.OPENAI));
        
        assertEquals("OpenAI client not yet implemented", exception.getMessage());
    }

    @Test
    void createClient_GeminiWithCustomConfig_ShouldUseCustomConfig() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-gemini-key");
        AIClientConfig customConfig = AIClientConfig.builder()
            .timeout(Duration.ofSeconds(45))
            .maxRetries(2)
            .enableStreaming(true)
            .modelName("gemini-1.5-pro")
            .build();
        
        // Execute
        AIClient client = AIClientFactory.createClient(APIKeyManager.Provider.GEMINI, customConfig);
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
        assertEquals(customConfig, client.getConfig());
        assertEquals(Duration.ofSeconds(45), client.getConfig().getTimeout());
        assertEquals(2, client.getConfig().getMaxRetries());
        assertTrue(client.getConfig().isStreamingEnabled());
        assertEquals("gemini-1.5-pro", client.getConfig().getModelName());
    }

    @Test
    void createClient_NoApiKey_ShouldThrowIllegalStateException() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            AIClientFactory.createClient(APIKeyManager.Provider.GEMINI));
        
        assertTrue(exception.getMessage().contains("No API key found for GEMINI"));
    }

    @Test
    void createClient_UnsupportedProvider_ShouldThrowIllegalArgumentException() {
        // This test would require a mock enum value, but we can test with null
        // In real usage, this would be caught at compile time
        assertThrows(NullPointerException.class, () -> 
            AIClientFactory.createClient(null));
    }

    @Test
    void createDefaultClient_OnlyGeminiAvailable_ShouldReturnGeminiClient() {
        // Setup - Only Gemini has API key
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-gemini-key");
        
        // Execute
        AIClient client = AIClientFactory.createDefaultClient();
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
        assertEquals("Gemini (Google)", client.getProviderName());
    }

    @Test
    void createDefaultClient_OnlyOpenAIAvailable_ShouldSkipUnsupportedAndThrow() {
        // Setup - Only OpenAI has API key (but not implemented)
        APIKeyManager.setApiKey(APIKeyManager.Provider.OPENAI, "test-openai-key");
        
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            AIClientFactory.createDefaultClient());
        
        assertTrue(exception.getMessage().contains("No AI provider API keys found"));
    }

    @Test
    void createDefaultClient_NoApiKeysAvailable_ShouldThrowIllegalStateException() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            AIClientFactory.createDefaultClient());
        
        assertEquals("No AI provider API keys found. Please configure at least one API key in environment variables or .env file", 
                     exception.getMessage());
    }

    @Test
    void createDefaultClient_WithCustomConfig_ShouldApplyCustomConfig() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-gemini-key");
        AIClientConfig customConfig = AIClientConfig.builder()
            .timeout(Duration.ofMinutes(3))
            .maxRetries(7)
            .enableStreaming(true)
            .build();
        
        // Execute
        AIClient client = AIClientFactory.createDefaultClient(customConfig);
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
        assertEquals(customConfig, client.getConfig());
        assertEquals(Duration.ofMinutes(3), client.getConfig().getTimeout());
        assertEquals(7, client.getConfig().getMaxRetries());
        assertTrue(client.getConfig().isStreamingEnabled());
    }

    @Test
    void createApplicationClient_ShouldReturnClientWithOptimizedConfig() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-gemini-key");
        
        // Execute
        AIClient client = AIClientFactory.createApplicationClient();
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
        
        // Verify optimized configuration
        AIClientConfig config = client.getConfig();
        assertEquals(Duration.ofSeconds(60), config.getTimeout());
        assertEquals(3, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(2), config.getRetryDelay());
        assertTrue(config.isLoggingEnabled());
        assertFalse(config.isStreamingEnabled()); // Disabled by default for app
    }

    @Test
    void createApplicationClient_NoProvidersAvailable_ShouldThrowException() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            AIClientFactory.createApplicationClient());
        
        assertTrue(exception.getMessage().contains("No AI provider API keys found"));
    }

    @Test
    void createClient_AlternativeApiKeyNames_ShouldWork() {
        // Test GOOGLE_API_KEY for Gemini
        System.setProperty("GOOGLE_API_KEY", "google-key");
        AIClient geminiClient = AIClientFactory.createClient(APIKeyManager.Provider.GEMINI);
        assertTrue(geminiClient instanceof OptimizedGeminiClient);
    }

    @Test
    void createDefaultClient_ProviderImplementationSkipping_ShouldWorkCorrectly() {
        // Setup scenario where first provider is unimplemented
        // OpenAI first, then Gemini
        APIKeyManager.setApiKey(APIKeyManager.Provider.OPENAI, "openai-key");
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "gemini-key");
        
        // Execute - should skip OpenAI (unimplemented) and use Gemini
        AIClient client = AIClientFactory.createDefaultClient();
        
        // Verify
        assertNotNull(client);
        assertTrue(client instanceof OptimizedGeminiClient);
    }

    @Test
    void factory_ConfigurationIsolation_ShouldNotAffectOtherClients() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "test-key");
        
        // Create clients with different configs
        AIClientConfig config1 = AIClientConfig.builder().timeout(Duration.ofSeconds(10)).build();
        AIClientConfig config2 = AIClientConfig.builder().timeout(Duration.ofSeconds(20)).build();
        
        AIClient client1 = AIClientFactory.createClient(APIKeyManager.Provider.GEMINI, config1);
        AIClient client2 = AIClientFactory.createClient(APIKeyManager.Provider.GEMINI, config2);
        
        // Verify configurations are independent
        assertEquals(Duration.ofSeconds(10), client1.getConfig().getTimeout());
        assertEquals(Duration.ofSeconds(20), client2.getConfig().getTimeout());
        assertNotSame(client1.getConfig(), client2.getConfig());
    }
}