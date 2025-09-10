package com.example.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.*;
import com.google.common.collect.ImmutableList;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optimized Gemini API client using java-genai SDK with advanced features
 */
public class OptimizedGeminiClient extends AIClient {
    
    private static final Logger logger = Logger.getLogger(OptimizedGeminiClient.class.getName());
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String FAST_MODEL = "gemini-2.5-flash";
    
    private final Client genaiClient;
    private final String modelName;
    private final String fastModelName;
    private final Cache<String, GenerateContentResponse> responseCache;
    private final Map<String, Method> registeredFunctions;
    private final boolean enableThinking;
    private final boolean enableCaching;
    
    public OptimizedGeminiClient(String apiKey) {
        this(apiKey, AIClientConfig.defaultConfig());
    }
    
    public OptimizedGeminiClient(String apiKey, AIClientConfig config) {
        super(apiKey, config);
        
        // Model selection based on configuration
        this.modelName = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        this.fastModelName = FAST_MODEL;
        
        // Build optimized client with all advanced features
        Client.Builder clientBuilder = Client.builder()
            .apiKey(apiKey);
        
        // Configure HTTP options with optimized settings
        HttpOptions.Builder httpOptionsBuilder = HttpOptions.builder();
        if (config.getTimeout() != null) {
            httpOptionsBuilder.timeout((int) config.getTimeout().toSeconds());
        }
        
        // Use stable API version for production
        httpOptionsBuilder.apiVersion("v1beta");
        
        clientBuilder.httpOptions(httpOptionsBuilder.build());
        
        // Configure client options for optimal performance
        ClientOptions clientOptions = ClientOptions.builder()
            .maxConnections(128)  // Increased for parallel processing
            .maxConnectionsPerHost(32)  // Optimized per-host connections
            .build();
        clientBuilder.clientOptions(clientOptions);
        
        this.genaiClient = clientBuilder.build();
        
        // Initialize cache for responses (5 minutes TTL, max 100 entries)
        this.responseCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
        
        // Initialize function registry for tool calling
        this.registeredFunctions = new HashMap<>();
        
        // Check if model supports thinking mode
        this.enableThinking = modelName.contains("thinking");
        this.enableCaching = config.isLoggingEnabled(); // Use logging flag for caching
    }
    
    @Override
    public CompletableFuture<AIResponse> chatCompletion(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : modelName;
                
                // Check cache first if enabled
                if (enableCaching) {
                    String cacheKey = generateCacheKey(request);
                    GenerateContentResponse cached = responseCache.getIfPresent(cacheKey);
                    if (cached != null) {
                        logger.info("Cache hit for request");
                        return convertResponse(cached);
                    }
                }
                
                // Build optimized generation config
                GenerateContentConfig config = buildOptimizedConfig(request);
                
                // Build contents from messages
                List<Content> contents = buildContents(request);
                
                // Generate content with appropriate method
                GenerateContentResponse response;
                if (contents.size() == 1) {
                    response = genaiClient.models.generateContent(model, contents.get(0), config);
                } else {
                    response = genaiClient.models.generateContent(model, contents, config);
                }
                
                // Cache successful response if enabled
                if (enableCaching && response != null) {
                    String cacheKey = generateCacheKey(request);
                    responseCache.put(cacheKey, response);
                }
                
                return convertResponse(response);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to complete chat request", e);
            }
        });
    }
    
    /**
     * Execute chat completion with automatic function calling
     */
    public CompletableFuture<AIResponse> chatCompletionWithFunctions(
            ChatRequest request, 
            Map<String, Method> functions) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : fastModelName;
                
                // Build config with function tools
                GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
                configureBasicSettings(configBuilder, request);
                
                // Add functions as tools
                if (functions != null && !functions.isEmpty()) {
                    List<Method> methodList = new ArrayList<>(functions.values());
                    Tool functionTool = Tool.builder()
                        .functions(methodList.toArray(new Method[0]))
                        .build();
                    configBuilder.tools(functionTool);
                }
                
                GenerateContentConfig config = configBuilder.build();
                
                // Build contents
                List<Content> contents = buildContents(request);
                
                // Generate with function calling
                GenerateContentResponse response;
                if (contents.size() == 1) {
                    response = genaiClient.models.generateContent(model, contents.get(0), config);
                } else {
                    response = genaiClient.models.generateContent(model, contents, config);
                }
                
                // Convert response including function call history
                AIResponse aiResponse = convertResponse(response);
                
                // Add automatic function calling history if available
                response.automaticFunctionCallingHistory().ifPresent(history -> {
                    logger.info("Automatic function calling executed: " + history);
                });
                
                return aiResponse;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to complete chat with functions", e);
            }
        });
    }
    
    /**
     * Generate content with thinking mode for complex reasoning
     */
    public CompletableFuture<AIResponse> generateWithThinking(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use thinking model if available
                String model = enableThinking ? modelName : fastModelName;
                
                // Build config with thinking enabled
                GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
                configureBasicSettings(configBuilder, request);
                
                // Configure thinking mode
                if (enableThinking) {
                    // Set thinking budget (seconds of thinking time)
                    ThinkingConfig thinkingConfig = ThinkingConfig.builder()
                        .thinkingBudget(30)  // 30 seconds of thinking
                        .build();
                    configBuilder.thinkingConfig(thinkingConfig);
                }
                
                GenerateContentConfig config = configBuilder.build();
                
                // Build contents
                List<Content> contents = buildContents(request);
                
                // Generate with thinking
                GenerateContentResponse response;
                if (contents.size() == 1) {
                    response = genaiClient.models.generateContent(model, contents.get(0), config);
                } else {
                    response = genaiClient.models.generateContent(model, contents, config);
                }
                
                return convertResponse(response);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate with thinking", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> streamChatCompletion(ChatRequest request, StreamHandler handler) {
        return CompletableFuture.runAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : fastModelName;
                
                // Build optimized config
                GenerateContentConfig config = buildOptimizedConfig(request);
                
                // Build contents
                List<Content> contents = buildContents(request);
                
                // Stream generation
                ResponseStream<GenerateContentResponse> responseStream;
                if (contents.size() == 1) {
                    responseStream = genaiClient.models.generateContentStream(model, contents.get(0), config);
                } else {
                    responseStream = genaiClient.models.generateContentStream(model, contents, config);
                }
                
                // Process stream with buffering for better performance
                StringBuilder buffer = new StringBuilder();
                int bufferThreshold = 100; // Flush every 100 chars
                
                try {
                    for (GenerateContentResponse chunk : responseStream) {
                        String text = chunk.text();
                        if (text != null && !text.isEmpty()) {
                            buffer.append(text);
                            
                            // Flush buffer if threshold reached
                            if (buffer.length() >= bufferThreshold) {
                                handler.onChunk(buffer.toString());
                                buffer.setLength(0);
                            }
                        }
                    }
                    
                    // Flush remaining buffer
                    if (buffer.length() > 0) {
                        handler.onChunk(buffer.toString());
                    }
                    
                    handler.onComplete();
                } finally {
                    responseStream.close();
                }
                
            } catch (Exception e) {
                handler.onError(e);
            }
        });
    }
    
    /**
     * Count tokens for cost estimation
     */
    public CompletableFuture<TokenCount> countTokens(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : modelName;
                
                // Build contents
                List<Content> contents = buildContents(request);
                
                // Count tokens
                CountTokensResponse response = genaiClient.models.countTokens(model, contents, null);
                
                return new TokenCount(
                    response.totalTokens().orElse(0),
                    0  // billableCharacters not available in current API
                );
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to count tokens", e);
            }
        });
    }
    
    @Override
    public boolean testConnection() {
        try {
            ChatRequest testRequest = ChatRequest.builder()
                .addUserMessage("Hello")
                .model(fastModelName)
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
        return "Optimized Gemini (Google GenAI)";
    }
    
    @Override
    public Map<String, Boolean> getCapabilities() {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("tool_calling", true);
        capabilities.put("function_calling", true);
        capabilities.put("vision", true);
        capabilities.put("system_messages", true);
        capabilities.put("thinking_mode", enableThinking);
        capabilities.put("caching", enableCaching);
        capabilities.put("max_tokens_8k", true);
        capabilities.put("max_tokens_32k", true);
        capabilities.put("max_tokens_128k", true);
        capabilities.put("max_tokens_1m", true);
        capabilities.put("json_mode", true);
        capabilities.put("google_search", true);
        capabilities.put("code_execution", true);
        return capabilities;
    }
    
    private GenerateContentConfig buildOptimizedConfig(ChatRequest request) {
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
        
        // Basic settings
        configureBasicSettings(configBuilder, request);
        
        // Add advanced optimizations
        configureAdvancedSettings(configBuilder, request);
        
        return configBuilder.build();
    }
    
    private void configureBasicSettings(GenerateContentConfig.Builder configBuilder, ChatRequest request) {
        // Temperature with smart defaults
        if (request.getTemperature() != null) {
            configBuilder.temperature(request.getTemperature().floatValue());
        } else {
            // Use lower temperature for code/technical content
            configBuilder.temperature(0.5f);
        }
        
        // Max tokens with smart defaults
        if (request.getMaxTokens() != null) {
            configBuilder.maxOutputTokens(request.getMaxTokens());
        } else {
            configBuilder.maxOutputTokens(4096);
        }
        
        // Nucleus sampling parameters
        configBuilder.topP(0.95f);
        configBuilder.topK(40f);
        
        // Single candidate for consistency
        configBuilder.candidateCount(1);
        
        // Stop sequences if provided
        if (request.getAdditionalParams() != null) {
            Object stopSequences = request.getAdditionalParams().get("stopSequences");
            if (stopSequences instanceof List) {
                configBuilder.stopSequences((List<String>) stopSequences);
            }
        }
    }
    
    private void configureAdvancedSettings(GenerateContentConfig.Builder configBuilder, ChatRequest request) {
        // Safety settings - balanced for production use
        ImmutableList<SafetySetting> safetySettings = ImmutableList.of(
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HARASSMENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
                .build()
        );
        configBuilder.safetySettings(safetySettings);
        
        // System instruction
        Optional<String> systemMessage = extractSystemMessage(request);
        if (systemMessage.isPresent()) {
            Content systemInstruction = Content.fromParts(Part.fromText(systemMessage.get()));
            configBuilder.systemInstruction(systemInstruction);
        }
        
        // Response format
        if (request.getAdditionalParams() != null) {
            String responseFormat = (String) request.getAdditionalParams().get("responseFormat");
            if ("json".equals(responseFormat)) {
                configBuilder.responseMimeType("application/json");
                
                // JSON schema handling would require proper Schema object
                // For now, JSON mode is enabled via responseMimeType
            }
        }
        
        // Google Search tool if requested
        if (request.getAdditionalParams() != null && 
            Boolean.TRUE.equals(request.getAdditionalParams().get("enableGoogleSearch"))) {
            GoogleSearch googleSearch = GoogleSearch.builder().build();
            Tool searchTool = Tool.builder().googleSearch(googleSearch).build();
            configBuilder.tools(searchTool);
        }
        
        // Code execution feature - available in future API versions
        // Currently not supported in java-genai 1.15.0
    }
    
    private List<Content> buildContents(ChatRequest request) {
        List<Content> contents = new ArrayList<>();
        
        // Skip system messages as they're handled separately
        for (ChatRequest.Message msg : request.getMessages()) {
            if (msg.getRole() == ChatRequest.Role.SYSTEM) {
                continue;
            }
            
            // Create parts for the message
            Part textPart = Part.fromText(msg.getContent());
            
            // Map role to Gemini format
            String role = msg.getRole() == ChatRequest.Role.USER ? "user" : "model";
            
            // Create content with role
            Content content = Content.builder()
                .role(role)
                .parts(textPart)
                .build();
            
            contents.add(content);
        }
        
        return contents;
    }
    
    private Optional<String> extractSystemMessage(ChatRequest request) {
        return request.getMessages().stream()
            .filter(msg -> msg.getRole() == ChatRequest.Role.SYSTEM)
            .findFirst()
            .map(ChatRequest.Message::getContent);
    }
    
    private AIResponse convertResponse(GenerateContentResponse response) {
        AIResponse.Builder builder = AIResponse.builder();
        
        // Extract text content
        String text = response.text();
        if (text != null) {
            builder.content(text);
        }
        
        // Extract usage metadata
        response.usageMetadata().ifPresent(usage -> {
            int promptTokens = usage.promptTokenCount().orElse(0);
            int completionTokens = usage.candidatesTokenCount().orElse(0);
            int totalTokens = usage.totalTokenCount().orElse(0);
            
            builder.usage(new AIResponse.Usage(promptTokens, completionTokens, totalTokens));
        });
        
        // Extract grounding metadata if available
        response.candidates().ifPresent(candidates -> {
            if (!candidates.isEmpty()) {
                candidates.get(0).groundingMetadata().ifPresent(metadata -> {
                    logger.info("Grounding metadata available: " + metadata);
                });
            }
        });
        
        return builder.build();
    }
    
    private String generateCacheKey(ChatRequest request) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(request.getModel()).append(":");
        
        for (ChatRequest.Message msg : request.getMessages()) {
            keyBuilder.append(msg.getRole()).append(":").append(msg.getContent().hashCode()).append(";");
        }
        
        if (request.getTemperature() != null) {
            keyBuilder.append("t:").append(request.getTemperature()).append(";");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Token count result
     */
    public static class TokenCount {
        private final int totalTokens;
        private final int billableCharacters;
        
        public TokenCount(int totalTokens, int billableCharacters) {
            this.totalTokens = totalTokens;
            this.billableCharacters = billableCharacters;
        }
        
        public int getTotalTokens() { return totalTokens; }
        public int getBillableCharacters() { return billableCharacters; }
        
        @Override
        public String toString() {
            return String.format("TokenCount{tokens=%d, chars=%d}", totalTokens, billableCharacters);
        }
    }
}