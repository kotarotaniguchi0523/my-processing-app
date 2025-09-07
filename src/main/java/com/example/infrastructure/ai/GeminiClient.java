package com.example.infrastructure.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Google Gemini API client implementation
 */
public class GeminiClient extends AIClient {
    
    private static final Logger logger = Logger.getLogger(GeminiClient.class.getName());
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String DEFAULT_MODEL = "gemini-pro";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    
    public GeminiClient(String apiKey) {
        this(apiKey, AIClientConfig.defaultConfig());
    }
    
    public GeminiClient(String apiKey, AIClientConfig config) {
        super(apiKey, config);
        
        this.baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : DEFAULT_BASE_URL;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Configure OkHttp client
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
            .connectTimeout(config.getTimeout())
            .readTimeout(config.getTimeout())
            .writeTimeout(config.getTimeout());
        
        // Add logging if enabled
        if (config.isLoggingEnabled()) {
            clientBuilder.addInterceptor(chain -> {
                Request request = chain.request();
                logger.info("Sending request: " + request.method() + " " + request.url());
                
                long startTime = System.nanoTime();
                Response response = chain.proceed(request);
                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                
                logger.info(String.format("Received response: %d in %dms", 
                    response.code(), duration));
                
                return response;
            });
        }
        
        // Add retry logic
        clientBuilder.addInterceptor(new RetryInterceptor(
            config.getMaxRetries(), 
            config.getRetryDelay().toMillis()
        ));
        
        this.httpClient = clientBuilder.build();
    }
    
    @Override
    public CompletableFuture<AIResponse> chatCompletion(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : 
                              (config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL);
                
                String url = String.format("%s/models/%s:generateContent?key=%s", 
                    baseUrl, model, apiKey);
                
                String jsonBody = buildRequestBody(request);
                
                Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
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
                String model = request.getModel() != null ? request.getModel() : 
                              (config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL);
                
                String url = String.format("%s/models/%s:streamGenerateContent?key=%s&alt=sse", 
                    baseUrl, model, apiKey);
                
                String jsonBody = buildRequestBody(request);
                
                Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
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
                                        handler.onChunk(data);
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
                .addUserMessage("Hello")
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
        return "Gemini (Google)";
    }
    
    @Override
    public Map<String, Boolean> getCapabilities() {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("tool_calling", false); // Will be added in future
        capabilities.put("vision", true);
        capabilities.put("system_messages", false); // Gemini uses different approach
        capabilities.put("max_tokens_4k", true);
        capabilities.put("max_tokens_32k", true);
        return capabilities;
    }
    
    private String buildRequestBody(ChatRequest request) {
        JsonObject body = new JsonObject();
        
        // Build contents array (Gemini format)
        JsonArray contents = new JsonArray();
        
        for (ChatRequest.Message msg : request.getMessages()) {
            JsonObject content = new JsonObject();
            
            // Map roles
            String role = msg.getRole() == ChatRequest.Role.USER ? "user" : 
                         msg.getRole() == ChatRequest.Role.ASSISTANT ? "model" : "user";
            content.addProperty("role", role);
            
            // Add parts
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", msg.getContent());
            parts.add(textPart);
            content.add("parts", parts);
            
            contents.add(content);
        }
        
        body.add("contents", contents);
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        if (request.getTemperature() != null) {
            generationConfig.addProperty("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            generationConfig.addProperty("maxOutputTokens", request.getMaxTokens());
        }
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        
        body.add("generationConfig", generationConfig);
        
        // Add safety settings
        JsonArray safetySettings = new JsonArray();
        String[] categories = {
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.add(setting);
        }
        
        body.add("safetySettings", safetySettings);
        
        return gson.toJson(body);
    }
    
    private AIResponse parseResponse(String responseBody) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        
        AIResponse.Builder builder = AIResponse.builder();
        
        // Extract content from candidates
        if (json.has("candidates") && json.get("candidates").isJsonArray()) {
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            JsonObject part = parts.get(0).getAsJsonObject();
                            if (part.has("text")) {
                                builder.content(part.get("text").getAsString());
                            }
                        }
                    }
                }
            }
        }
        
        // Extract usage metadata if available
        if (json.has("usageMetadata")) {
            JsonObject usage = json.getAsJsonObject("usageMetadata");
            builder.usage(new AIResponse.Usage(
                usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0,
                usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0,
                usage.has("totalTokenCount") ? usage.get("totalTokenCount").getAsInt() : 0
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
                if (errorObj.has("code")) {
                    int code = errorObj.get("code").getAsInt();
                    errorType = mapErrorType(code, statusCode);
                }
            }
        } catch (Exception e) {
            // Failed to parse error response
        }
        
        throw new AIClientException(message, errorType);
    }
    
    private AIClientException.ErrorType mapErrorType(int code, int statusCode) {
        if (statusCode == 401 || statusCode == 403) return AIClientException.ErrorType.AUTHENTICATION;
        if (statusCode == 429) return AIClientException.ErrorType.RATE_LIMIT;
        if (statusCode >= 500) return AIClientException.ErrorType.SERVER_ERROR;
        if (statusCode == 400) return AIClientException.ErrorType.INVALID_REQUEST;
        return AIClientException.ErrorType.UNKNOWN;
    }
    
    /**
     * Retry interceptor for handling transient failures
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final long retryDelayMs;
        
        RetryInterceptor(int maxRetries, long retryDelayMs) {
            this.maxRetries = maxRetries;
            this.retryDelayMs = retryDelayMs;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;
            
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    if (attempt > 0) {
                        Thread.sleep(retryDelayMs * attempt);
                    }
                    
                    response = chain.proceed(request);
                    
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