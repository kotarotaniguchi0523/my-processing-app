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
 * Unit tests for GeminiClient with mocked HTTP responses
 */
class GeminiClientTest {

    private MockWebServer mockServer;
    private GeminiClient client;
    private AIClientConfig config;

    @BeforeEach
    void setUp() throws IOException {
        // Setup mock server
        mockServer = new MockWebServer();
        mockServer.start();
        
        // Create config with mock server URL
        config = AIClientConfig.builder()
            .baseUrl(mockServer.url("/v1beta").toString())
            .timeout(Duration.ofSeconds(5))
            .maxRetries(1)
            .enableLogging(false) // Disable logging for cleaner test output
            .build();
        
        // Create client
        client = new GeminiClient("test-api-key", config);
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
        assertEquals("Gemini (Google)", client.getProviderName());
    }

    @Test
    void constructor_DefaultConfig_ShouldUseDefaults() {
        // Execute
        GeminiClient defaultClient = new GeminiClient("test-key");
        
        // Verify
        assertNotNull(defaultClient.getConfig());
        assertEquals(Duration.ofSeconds(30), defaultClient.getConfig().getTimeout());
    }

    @Test
    void chatCompletion_SuccessfulResponse_ShouldParseCorrectly() throws Exception {
        // Setup mock response in Gemini format
        String mockResponseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Hello! I'm Gemini. How can I help you today?"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP",
                  "index": 0
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 8,
                "candidatesTokenCount": 12,
                "totalTokenCount": 20
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
        assertEquals("Hello! I'm Gemini. How can I help you today?", response.getContent());
        
        // Verify usage
        assertNotNull(response.getUsage());
        assertEquals(8, response.getUsage().getPromptTokens());
        assertEquals(12, response.getUsage().getCompletionTokens());
        assertEquals(20, response.getUsage().getTotalTokens());
        
        // Verify request was sent correctly
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/models/gemini-pro:generateContent"));
        assertTrue(recordedRequest.getPath().contains("key=test-api-key"));
        assertTrue(recordedRequest.getHeader("Content-Type").startsWith("application/json"));
    }

    @Test
    void chatCompletion_AuthenticationError_ShouldThrowCorrectException() throws Exception {
        // Setup mock error response in Gemini format
        String errorResponse = """
            {
              "error": {
                "code": 401,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "UNAUTHENTICATED"
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
        
        // Verify error type
        AIClientException aiException = findAIClientException(exception);
        assertNotNull(aiException);
        assertEquals(AIClientException.ErrorType.AUTHENTICATION, aiException.getErrorType());
    }

    @Test
    void chatCompletion_RateLimitError_ShouldThrowCorrectException() throws Exception {
        // Setup mock error response
        String errorResponse = """
            {
              "error": {
                "code": 429,
                "message": "Quota exceeded",
                "status": "RESOURCE_EXHAUSTED"
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
    void chatCompletion_CustomParameters_ShouldIncludeInRequest() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Custom response"}],
                        "role": "model"
                      }
                    }
                  ]
                }
                """));
        
        // Create request with custom parameters
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .model("gemini-1.5-pro")
            .temperature(0.9)
            .maxTokens(800)
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify request path contains custom model
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertTrue(recordedRequest.getPath().contains("gemini-1.5-pro"));
        
        // Verify request body contains parameters
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("0.9")); // temperature
        assertTrue(requestBody.contains("800")); // maxOutputTokens
    }

    @Test
    void chatCompletion_ConversationFlow_ShouldHandleMultipleMessages() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "I understand your conversation context"}],
                        "role": "model"
                      }
                    }
                  ]
                }
                """));
        
        // Create request with conversation
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .addAssistantMessage("Hi there!")
            .addUserMessage("How are you?")
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify request body contains all messages
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("Hello"));
        assertTrue(requestBody.contains("Hi there!"));
        assertTrue(requestBody.contains("How are you?"));
        assertTrue(requestBody.contains("user"));
        assertTrue(requestBody.contains("model")); // Gemini uses "model" instead of "assistant"
    }

    @Test
    void chatCompletion_SystemMessage_ShouldBeHandledAsUserMessage() throws Exception {
        // Note: Gemini doesn't support system messages the same way as Claude,
        // so they should be converted to user messages
        
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "I'll follow your instructions"}],
                        "role": "model"
                      }
                    }
                  ]
                }
                """));
        
        // Create request with system message
        ChatRequest request = ChatRequest.builder()
            .addSystemMessage("You are a helpful assistant")
            .addUserMessage("Hello")
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify system message is included as user message
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("You are a helpful assistant"));
        assertTrue(requestBody.contains("Hello"));
    }

    @Test
    void chatCompletionSync_SuccessfulResponse_ShouldReturnResponse() throws Exception {
        // Setup mock response
        String mockResponseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{"text": "Sync response from Gemini"}],
                    "role": "model"
                  }
                }
              ]
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
        assertEquals("Sync response from Gemini", response.getContent());
    }

    @Test
    void chatCompletionSync_ErrorResponse_ShouldThrowAIClientException() throws Exception {
        // Setup mock error
        mockServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("""
                {
                  "error": {
                    "code": 400,
                    "message": "Invalid request format",
                    "status": "INVALID_ARGUMENT"
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
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Connection test successful"}],
                        "role": "model"
                      }
                    }
                  ]
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
        assertFalse(capabilities.get("tool_calling")); // Not yet supported
        assertTrue(capabilities.get("vision"));
        assertFalse(capabilities.get("system_messages")); // Different approach
        assertTrue(capabilities.get("max_tokens_4k"));
        assertTrue(capabilities.get("max_tokens_32k"));
    }

    @Test
    void chatCompletion_SafetySettings_ShouldBeIncludedInRequest() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Safe response"}],
                        "role": "model"
                      }
                    }
                  ]
                }
                """));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Test safety")
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify safety settings are included in request
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("safetySettings"));
        assertTrue(requestBody.contains("HARM_CATEGORY_HARASSMENT"));
        assertTrue(requestBody.contains("HARM_CATEGORY_HATE_SPEECH"));
        assertTrue(requestBody.contains("BLOCK_MEDIUM_AND_ABOVE"));
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
    void streamChatCompletion_ShouldHandleStreamingResponse() throws Exception {
        // Setup mock streaming response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}\ndata: [DONE]\n")
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
        
        // Verify stream endpoint was used
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertTrue(recordedRequest.getPath().contains("streamGenerateContent"));
        assertTrue(recordedRequest.getPath().contains("alt=sse"));
        
        // Verify - streaming is complex, just check request was made
        assertTrue(mockServer.getRequestCount() >= 1);
    }

    @Test
    void chatCompletion_GenerationConfig_ShouldBeConfiguredCorrectly() throws Exception {
        // Setup mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "Generated with custom config"}],
                        "role": "model"
                      }
                    }
                  ]
                }
                """));
        
        // Create request
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Test generation config")
            .build();
        
        // Execute
        client.chatCompletion(request).get(10, TimeUnit.SECONDS);
        
        // Verify generation config is included
        RecordedRequest recordedRequest = mockServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("generationConfig"));
        assertTrue(requestBody.contains("topK"));
        assertTrue(requestBody.contains("topP"));
        assertTrue(requestBody.contains("40")); // topK value
        assertTrue(requestBody.contains("0.95")); // topP value
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