package com.example.infrastructure.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Claude API client implementation
 */
public class ClaudeClient extends AIClient {
    
    private static final Logger logger = Logger.getLogger(ClaudeClient.class.getName());
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String DEFAULT_MODEL = "claude-3-haiku-20240307";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    
    public ClaudeClient(String apiKey) {
        this(apiKey, AIClientConfig.defaultConfig());
    }
    
    public ClaudeClient(String apiKey, AIClientConfig config) {
        super(apiKey, config);
        
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Configure OkHttp client with timeout and retry settings
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeout())
            .readTimeout(config.getTimeout())
            .writeTimeout(config.getTimeout());
        
        // Add logging interceptor if enabled
        if (config.isLoggingEnabled()) {
            clientBuilder.addInterceptor(new LoggingInterceptor());
        }
        
        // Add retry interceptor
        clientBuilder.addInterceptor(new RetryInterceptor(
            config.getMaxRetries(), 
            config.getRetryDelay()
        ));
        
        this.httpClient = clientBuilder.build();
    }
    
    @Override
    public CompletableFuture<AIResponse> chatCompletion(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = buildRequestBody(request);
                
                Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
                
                try (Response response = httpClient.newCall(httpRequest).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (!response.isSuccessful()) {
                        handleErrorResponse(response.code(), responseBody);
                    }
                    
                    return parseResponse(responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to complete chat request", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> streamChatCompletion(ChatRequest request, StreamHandler handler) {
        return CompletableFuture.runAsync(() -> {
            try {
                String jsonBody = buildRequestBody(request);
                JsonObject bodyJson = gson.fromJson(jsonBody, JsonObject.class);
                bodyJson.addProperty("stream", true);
                
                Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(RequestBody.create(gson.toJson(bodyJson), JSON_MEDIA_TYPE))
                    .build();
                
                httpClient.newCall(httpRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        handler.onError(e);
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            handler.onError(new AIClientException("Stream request failed: " + response.code()));
                            return;
                        }
                        
                        try (ResponseBody body = response.body()) {
                            if (body != null) {
                                String line;
                                var reader = body.source();
                                while (!reader.exhausted()) {
                                    line = reader.readUtf8Line();
                                    if (line != null && line.startsWith("data: ")) {
                                        String data = line.substring(6);
                                        if (!data.equals("[DONE]")) {
                                            handler.onChunk(data);
                                        }
                                    }
                                }
                            }
                            handler.onComplete();
                        } catch (Exception e) {
                            handler.onError(e);
                        }
                    }
                });
            } catch (Exception e) {
                handler.onError(e);
            }
        });
    }
    
    @Override
    public boolean testConnection() {
        try {
            ChatRequest testRequest = ChatRequest.builder()
                .addUserMessage("Hi")
                .model(DEFAULT_MODEL)
                .maxTokens(10)
                .build();
            
            AIResponse response = chatCompletionSync(testRequest);
            return response != null && response.getContent() != null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Connection test failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderName() {
        return "Claude (Anthropic)";
    }
    
    @Override
    public Map<String, Boolean> getCapabilities() {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("tool_calling", true);
        capabilities.put("vision", true);
        capabilities.put("system_messages", true);
        capabilities.put("max_tokens_4k", true);
        capabilities.put("max_tokens_100k", true);
        return capabilities;
    }
    
    private String buildRequestBody(ChatRequest request) {
        JsonObject body = new JsonObject();
        
        // Set model
        String model = request.getModel() != null ? request.getModel() : 
                       (config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL);
        body.addProperty("model", model);
        
        // Set messages
        JsonArray messages = new JsonArray();
        String systemMessage = null;
        
        for (ChatRequest.Message msg : request.getMessages()) {
            if (msg.getRole() == ChatRequest.Role.SYSTEM) {
                systemMessage = msg.getContent();
            } else {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole().getValue());
                msgObj.addProperty("content", msg.getContent());
                messages.add(msgObj);
            }
        }
        
        body.add("messages", messages);
        
        // Add system message if present
        if (systemMessage != null) {
            body.addProperty("system", systemMessage);
        }
        
        // Set parameters
        body.addProperty("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1000);
        
        if (request.getTemperature() != null) {
            body.addProperty("temperature", request.getTemperature());
        }
        
        return gson.toJson(body);
    }
    
    private AIResponse parseResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        
        AIResponse.Builder builder = AIResponse.builder()
            .id(json.has("id") ? json.get("id").getAsString() : null)
            .model(json.has("model") ? json.get("model").getAsString() : null);
        
        // Extract content
        if (json.has("content") && json.get("content").isJsonArray()) {
            JsonArray contentArray = json.getAsJsonArray("content");
            if (contentArray.size() > 0) {
                JsonObject firstContent = contentArray.get(0).getAsJsonObject();
                if (firstContent.has("text")) {
                    builder.content(firstContent.get("text").getAsString());
                }
            }
        }
        
        // Extract usage
        if (json.has("usage")) {
            JsonObject usage = json.getAsJsonObject("usage");
            builder.usage(new AIResponse.Usage(
                usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0,
                usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0,
                usage.has("input_tokens") && usage.has("output_tokens") ? 
                    usage.get("input_tokens").getAsInt() + usage.get("output_tokens").getAsInt() : 0
            ));
        }
        
        return builder.build();
    }
    
    private void handleErrorResponse(int statusCode, String responseBody) throws AIClientException {
        AIClientException.ErrorType errorType = AIClientException.ErrorType.UNKNOWN;
        String message = "API request failed with status " + statusCode;
        
        try {
            JsonObject error = gson.fromJson(responseBody, JsonObject.class);
            if (error.has("error")) {
                JsonObject errorObj = error.getAsJsonObject("error");
                if (errorObj.has("message")) {
                    message = errorObj.get("message").getAsString();
                }
                if (errorObj.has("type")) {
                    String type = errorObj.get("type").getAsString();
                    errorType = mapErrorType(type, statusCode);
                }
            }
        } catch (Exception e) {
            // Failed to parse error response
        }
        
        throw new AIClientException(message, errorType);
    }
    
    private AIClientException.ErrorType mapErrorType(String type, int statusCode) {
        if (statusCode == 401) return AIClientException.ErrorType.AUTHENTICATION;
        if (statusCode == 429) return AIClientException.ErrorType.RATE_LIMIT;
        if (statusCode >= 500) return AIClientException.ErrorType.SERVER_ERROR;
        if (statusCode == 400) return AIClientException.ErrorType.INVALID_REQUEST;
        if (type != null && type.contains("timeout")) return AIClientException.ErrorType.TIMEOUT;
        return AIClientException.ErrorType.UNKNOWN;
    }
    
    /**
     * Logging interceptor for HTTP requests/responses
     */
    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            
            logger.info(String.format("Sending request: %s %s", 
                request.method(), request.url()));
            
            long startTime = System.nanoTime();
            Response response = chain.proceed(request);
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            logger.info(String.format("Received response: %d in %dms", 
                response.code(), duration));
            
            return response;
        }
    }
    
    /**
     * Retry interceptor for handling transient failures
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final Duration retryDelay;
        
        RetryInterceptor(int maxRetries, Duration retryDelay) {
            this.maxRetries = maxRetries;
            this.retryDelay = retryDelay;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;
            
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    if (attempt > 0) {
                        logger.info("Retry attempt " + attempt + " of " + maxRetries);
                        Thread.sleep(retryDelay.toMillis() * attempt);
                    }
                    
                    response = chain.proceed(request);
                    
                    // Don't retry on client errors (4xx)
                    if (response.code() < 500 || attempt == maxRetries) {
                        return response;
                    }
                    
                    response.close();
                } catch (IOException e) {
                    lastException = e;
                    if (attempt == maxRetries) {
                        throw e;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }
            
            if (lastException != null) {
                throw lastException;
            }
            
            return response;
        }
    }
}