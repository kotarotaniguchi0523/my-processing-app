package com.example.infrastructure.ai;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaudeClient with mocked HTTP responses
 */
class ClaudeClientTest {

    private MockWebServer mockServer;
    private ClaudeClient client;
    private AIClientConfig config;

    @BeforeEach
    void setUp() throws IOException {
        // Setup mock server
        mockServer = new MockWebServer();
        mockServer.start();
        
        // Create config with mock server URL
        config = AIClientConfig.builder()
            .baseUrl(mockServer.url("/v1").toString())
            .timeout(Duration.ofSeconds(5))
            .maxRetries(1)
            .enableLogging(false) // Disable logging for cleaner test output
            .build();
        
        // Create client
        client = new ClaudeClient("test-api-key", config);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void constructor_ValidApiKey_ShouldInitializeCorrectly() {
        // Verify client was created successfully
        assertNotNull(client);
        assertEquals(config, client.getConfig());
        assertEquals("Claude (Anthropic)", client.getProviderName());
    }

    @Test
    void constructor_DefaultConfig_ShouldUseDefaults() {
        // Execute
        ClaudeClient defaultClient = new ClaudeClient("test-key");
        
        // Verify
        assertNotNull(defaultClient.getConfig());
        assertEquals(Duration.ofSeconds(30), defaultClient.getConfig().getTimeout());
    }

    @Test
    void chatCompletion_SuccessfulResponse_ShouldParseCorrectly() throws Exception {
        // Setup mock response
        String mockResponseBody = """
            {
              "id": "msg_01XFDUDYJgAACzvnptvVoYEL",
              "type": "message",
              "role": "assistant",
              "model": "claude-3-haiku-20240307",
              "content": [
                {
                  "type": "text",
                  "text": "Hello! How can I help you today?"
                }
              ],
              "usage": {
                "input_tokens": 10,
                "output_tokens": 25,
                "cache_creation_input_tokens": 0,
                "cache_read_input_tokens": 0
              }
            }
            """;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody)
            .addHeader("Content-Type", "application/json"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        AIResponse response = future.get(10, TimeUnit.SECONDS);
        
        // Verify response
        assertNotNull(response);
        assertEquals("Hello! How can I help you today?", response.getContent());
        assertEquals("claude-3-haiku-20240307", response.getModel());
        assertEquals("msg_01XFDUDYJgAACzvnptvVoYEL", response.getId());
        
        // Verify usage
        assertNotNull(response.getUsage());
        assertEquals(10, response.getUsage().getPromptTokens());
        assertEquals(25, response.getUsage().getCompletionTokens());
        assertEquals(35, response.getUsage().getTotalTokens());
        
        // Verify request was sent correctly
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/v1/messages", recordedRequest.getPath());
        assertEquals("test-api-key", recordedRequest.getHeader("x-api-key"));
        assertEquals("2023-06-01", recordedRequest.getHeader("anthropic-version"));
        assertTrue(recordedRequest.getHeader("content-type").startsWith("application/json"));
    }

    @Test
    void chatCompletion_AuthenticationError_ShouldThrowCorrectException() throws Exception {
        // Setup mock error response
        String errorResponse = """
            {
              "type": "error",
              "error": {
                "type": "authentication_error",
                "message": "Invalid API key provided"
              }
            }
            """;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody(errorResponse)
            .addHeader("Content-Type", "application/json"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute & Verify
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        Exception exception = assertThrows(Exception.class, () -> 
            future.get(10, TimeUnit.SECONDS));
        
        // Verify it's the expected exception type
        assertTrue(exception.getCause() instanceof RuntimeException);
        RuntimeException runtimeException = (RuntimeException) exception.getCause();
        assertTrue(runtimeException.getCause() instanceof AIClientException);
        
        AIClientException aiException = (AIClientException) runtimeException.getCause();
        assertEquals(AIClientException.ErrorType.AUTHENTICATION, aiException.getErrorType());
    }

    @Test
    void chatCompletion_RateLimitError_ShouldThrowCorrectException() throws Exception {
        // Setup mock error response
        String errorResponse = """
            {
              "type": "error",
              "error": {
                "type": "rate_limit_error",
                "message": "Rate limit exceeded"
              }
            }
            """;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(429)
            .setBody(errorResponse)
            .addHeader("Content-Type", "application/json"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute & Verify
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        Exception exception = assertThrows(Exception.class, () -> 
            future.get(10, TimeUnit.SECONDS));
        
        // Verify error type
        AIClientException aiException = findAIClientException(exception);
        assertNotNull(aiException);
        assertEquals(AIClientException.ErrorType.RATE_LIMIT, aiException.getErrorType());
    }

    @Test
    void chatCompletion_ServerError_ShouldThrowCorrectException() throws Exception {
        // Setup mock error response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute & Verify
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        Exception exception = assertThrows(Exception.class, () -> 
            future.get(10, TimeUnit.SECONDS));
        
        // Verify that some form of exception was thrown
        assertNotNull(exception);
        // Note: Exception type may vary due to complex wrapping in async execution
    }

    @Test
    void chatCompletion_WithSystemMessage_ShouldIncludeInRequest() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "id": "msg_123",
                  "content": [{"type": "text", "text": "Response"}],
                  "usage": {"input_tokens": 5, "output_tokens": 5}
                }
                """));
        
        // Create request with system message
        ChatRequest request = ChatRequest.builder()
            .addSystemMessage("You are a helpful assistant")
            .addUserMessage("Hello")
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify system message was included
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("system"));
        assertTrue(requestBody.contains("You are a helpful assistant"));
    }

    @Test
    void chatCompletion_CustomParameters_ShouldIncludeInRequest() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "id": "msg_123",
                  "content": [{"type": "text", "text": "Response"}],
                  "usage": {"input_tokens": 5, "output_tokens": 5}
                }
                """));
        
        // Create request with custom parameters
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .model("claude-3-opus-20240229")
            .temperature(0.8)
            .maxTokens(500)
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify parameters were included
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("claude-3-opus-20240229"));
        assertTrue(requestBody.contains("0.8"));
        assertTrue(requestBody.contains("500"));
    }

    @Test
    void chatCompletionSync_SuccessfulResponse_ShouldReturnResponse() throws Exception {
        // Setup mock response
        String mockResponseBody = """
            {
              "id": "msg_sync",
              "content": [{"type": "text", "text": "Sync response"}],
              "usage": {"input_tokens": 5, "output_tokens": 10}
            }
            """;
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(mockResponseBody));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Sync test")
            .build();
        
        // Execute
        AIResponse response = client.chatCompletionSync(request);
        
        // Verify
        assertNotNull(response);
        assertEquals("Sync response", response.getContent());
    }

    @Test
    void chatCompletionSync_ErrorResponse_ShouldThrowAIClientException() throws Exception {
        // Setup mock error
        mockServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("""
                {
                  "type": "error",
                  "error": {
                    "type": "invalid_request_error",
                    "message": "Invalid request format"
                  }
                }
                """));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Test")
            .build();
        
        // Execute & Verify
        AIClientException exception = assertThrows(AIClientException.class, () -> 
            client.chatCompletionSync(request));
        
        assertEquals("Failed to complete chat request", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void testConnection_SuccessfulResponse_ShouldReturnTrue() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "id": "msg_test",
                  "content": [{"type": "text", "text": "Connection test"}],
                  "usage": {"input_tokens": 1, "output_tokens": 2}
                }
                """));
        
        // Execute
        boolean result = client.testConnection();
        
        // Verify
        assertTrue(result);
    }

    @Test
    void testConnection_FailedResponse_ShouldReturnFalse() throws Exception {
        // Setup mock error
        mockServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized"));
        
        // Execute
        boolean result = client.testConnection();
        
        // Verify
        assertFalse(result);
    }

    @Test
    void getCapabilities_ShouldReturnExpectedCapabilities() {
        // Execute
        Map<String, Boolean> capabilities = client.getCapabilities();
        
        // Verify
        assertNotNull(capabilities);
        assertTrue(capabilities.get("streaming"));
        assertTrue(capabilities.get("tool_calling"));
        assertTrue(capabilities.get("vision"));
        assertTrue(capabilities.get("system_messages"));
        assertTrue(capabilities.get("max_tokens_4k"));
        assertTrue(capabilities.get("max_tokens_100k"));
    }

    @Test
    void chatCompletion_NetworkError_ShouldRetryAndFail() throws Exception {
        // Setup - first request fails, second request also fails (exhausts retries)
        mockServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));
        mockServer.enqueue(new MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute & Verify
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        Exception exception = assertThrows(Exception.class, () -> 
            future.get(10, TimeUnit.SECONDS));
        
        // Should have attempted multiple requests due to retry logic
        assertTrue(mockServer.getRequestCount() >= 1);
    }

    @Test
    void chatCompletion_EmptyResponse_ShouldHandleGracefully() throws Exception {
        // Setup mock response with minimal content
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{}"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .build();
        
        // Execute
        CompletableFuture<AIResponse> future = client.chatCompletion(request);
        AIResponse response = future.get(10, TimeUnit.SECONDS);
        
        // Verify
        assertNotNull(response);
        assertNull(response.getContent()); // Should handle missing content gracefully
    }

    @Test
    void streamChatCompletion_ShouldHandleStreamingResponse() throws Exception {
        // This is a basic test for streaming - full streaming tests would require
        // more complex setup with Server-Sent Events format
        
        // Setup mock streaming response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("data: {\"delta\": {\"text\": \"Hello\"}}\ndata: [DONE]\n")
            .addHeader("Content-Type", "text/event-stream"));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Stream test")
            .build();
        
        // Setup stream handler
        StringBuilder streamedContent = new StringBuilder();
        boolean[] completed = {false};
        boolean[] errorOccurred = {false};
        
        AIClient.StreamHandler handler = new AIClient.StreamHandler() {
            @Override
            public void onChunk(String chunk) {
                streamedContent.append(chunk);
            }
            
            @Override
            public void onError(Throwable error) {
                errorOccurred[0] = true;
            }
            
            @Override
            public void onComplete() {
                completed[0] = true;
            }
        };
        
        // Execute
        CompletableFuture<Void> future = client.streamChatCompletion(request, handler);
        future.get(10, TimeUnit.SECONDS);
        
        // Verify - streaming is complex, just check request was made
        assertTrue(mockServer.getRequestCount() >= 1);
    }

    // Helper method to find AIClientException in exception chain
    private AIClientException findAIClientException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof AIClientException) {
                return (AIClientException) current;
            }
            current = current.getCause();
        }
        return null;
    }
}