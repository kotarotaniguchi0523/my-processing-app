package com.example.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIClient abstract class
 */
class AIClientTest {

    @Mock
    private AIClient.StreamHandler mockStreamHandler;

    private TestableAIClient client;
    private AIClientConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = AIClientConfig.builder()
            .timeout(java.time.Duration.ofSeconds(10))
            .maxRetries(2)
            .enableLogging(true)
            .build();
        client = new TestableAIClient("test-api-key", config);
    }

    @Test
    void constructor_ValidApiKeyAndConfig_ShouldInitializeCorrectly() {
        AIClient testClient = new TestableAIClient("valid-key", config);
        assertEquals(config, testClient.getConfig());
    }

    @Test
    void constructor_NullApiKey_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            new TestableAIClient(null, config));
    }

    @Test
    void constructor_EmptyApiKey_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            new TestableAIClient("", config));
    }

    @Test
    void constructor_WhitespaceApiKey_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            new TestableAIClient("   ", config));
    }

    @Test
    void constructor_NullConfig_ShouldUseDefaultConfig() {
        AIClient testClient = new TestableAIClient("valid-key", null);
        assertNotNull(testClient.getConfig());
        assertEquals(java.time.Duration.ofSeconds(30), testClient.getConfig().getTimeout());
    }

    @Test
    void validateApiKey_ValidKey_ShouldReturnTrimmedKey() {
        String result = client.validateApiKey("  valid-key  ");
        assertEquals("valid-key", result);
    }

    @Test
    void validateApiKey_NullKey_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            client.validateApiKey(null));
    }

    @Test
    void validateApiKey_EmptyKey_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
            client.validateApiKey(""));
    }

    @Test
    void getConfig_ShouldReturnCorrectConfig() {
        assertEquals(config, client.getConfig());
    }

    @Test
    void chatCompletionSync_SuccessfulRequest_ShouldReturnResponse() throws AIClientException {
        // Setup
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Test message")
            .build();
        
        AIResponse expectedResponse = AIResponse.builder()
            .content("Test response")
            .build();
        
        client.setMockResponse(expectedResponse);
        
        // Execute
        AIResponse actualResponse = client.chatCompletionSync(request);
        
        // Verify
        assertNotNull(actualResponse);
        assertEquals("Test response", actualResponse.getContent());
    }

    @Test
    void chatCompletionSync_FailedRequest_ShouldThrowException() {
        // Setup
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Test message")
            .build();
        
        client.setMockException(new RuntimeException("API error"));
        
        // Execute & Verify
        AIClientException exception = assertThrows(AIClientException.class, () -> 
            client.chatCompletionSync(request));
        
        assertEquals("Failed to complete chat request", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void getProviderName_ShouldReturnImplementationSpecificName() {
        assertEquals("TestProvider", client.getProviderName());
    }

    @Test
    void getCapabilities_ShouldReturnImplementationSpecificCapabilities() {
        Map<String, Boolean> capabilities = client.getCapabilities();
        assertNotNull(capabilities);
        assertTrue(capabilities.containsKey("test_capability"));
        assertTrue(capabilities.get("test_capability"));
    }

    @Test
    void testConnection_SuccessfulConnection_ShouldReturnTrue() {
        client.setConnectionTestResult(true);
        assertTrue(client.testConnection());
    }

    @Test
    void testConnection_FailedConnection_ShouldReturnFalse() {
        client.setConnectionTestResult(false);
        assertFalse(client.testConnection());
    }

    /**
     * Concrete implementation of AIClient for testing purposes
     */
    private static class TestableAIClient extends AIClient {
        
        private AIResponse mockResponse;
        private RuntimeException mockException;
        private boolean connectionTestResult = true;
        
        protected TestableAIClient(String apiKey, AIClientConfig config) {
            super(apiKey, config);
        }
        
        public void setMockResponse(AIResponse response) {
            this.mockResponse = response;
        }
        
        public void setMockException(RuntimeException exception) {
            this.mockException = exception;
        }
        
        public void setConnectionTestResult(boolean result) {
            this.connectionTestResult = result;
        }
        
        @Override
        public CompletableFuture<AIResponse> chatCompletion(ChatRequest request) {
            if (mockException != null) {
                return CompletableFuture.failedFuture(mockException);
            }
            return CompletableFuture.completedFuture(mockResponse);
        }
        
        @Override
        public CompletableFuture<Void> streamChatCompletion(ChatRequest request, StreamHandler handler) {
            return CompletableFuture.runAsync(() -> {
                if (mockException != null) {
                    handler.onError(mockException);
                } else {
                    handler.onChunk("Test chunk");
                    handler.onComplete();
                }
            });
        }
        
        @Override
        public boolean testConnection() {
            return connectionTestResult;
        }
        
        @Override
        public String getProviderName() {
            return "TestProvider";
        }
        
        @Override
        public Map<String, Boolean> getCapabilities() {
            return Map.of("test_capability", true);
        }
        
        // Expose protected method for testing
        public String validateApiKey(String apiKey) {
            return super.validateApiKey(apiKey);
        }
    }
}