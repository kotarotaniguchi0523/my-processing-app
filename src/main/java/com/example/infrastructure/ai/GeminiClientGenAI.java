package com.example.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.*;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Google Gemini API client implementation using java-genai SDK
 */
public class GeminiClientGenAI extends AIClient {
    
    private static final Logger logger = Logger.getLogger(GeminiClientGenAI.class.getName());
    private static final String DEFAULT_MODEL = "gemini-2.0-flash-exp";
    
    private final Client genaiClient;
    private final String modelName;
    
    public GeminiClientGenAI(String apiKey) {
        this(apiKey, AIClientConfig.defaultConfig());
    }
    
    public GeminiClientGenAI(String apiKey, AIClientConfig config) {
        super(apiKey, config);
        
        this.modelName = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        
        // Build the Google GenAI client
        Client.Builder clientBuilder = Client.builder()
            .apiKey(apiKey);
        
        // Configure timeout and other HTTP options if needed
        if (config.getTimeout() != null) {
            HttpOptions httpOptions = HttpOptions.builder()
                .timeout((int) config.getTimeout().toSeconds())
                .build();
            clientBuilder.httpOptions(httpOptions);
        }
        
        // Configure client options for connection pooling with default values
        ClientOptions clientOptions = ClientOptions.builder()
            .maxConnections(64)
            .maxConnectionsPerHost(16)
            .build();
        clientBuilder.clientOptions(clientOptions);
        
        this.genaiClient = clientBuilder.build();
    }
    
    @Override
    public CompletableFuture<AIResponse> chatCompletion(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : modelName;
                
                // Build contents from messages
                List<Content> contents = buildContents(request);
                
                // Build generation config
                GenerateContentConfig config = buildGenerationConfig(request);
                
                // Generate content
                GenerateContentResponse response;
                if (contents.size() == 1) {
                    // Single message - use simple API
                    response = genaiClient.models.generateContent(model, contents.get(0), config);
                } else {
                    // Multi-turn conversation
                    response = genaiClient.models.generateContent(model, contents, config);
                }
                
                // Convert response
                return convertResponse(response);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to complete chat request", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> streamChatCompletion(ChatRequest request, StreamHandler handler) {
        return CompletableFuture.runAsync(() -> {
            try {
                String model = request.getModel() != null ? request.getModel() : modelName;
                
                // Build contents from messages
                List<Content> contents = buildContents(request);
                
                // Build generation config
                GenerateContentConfig config = buildGenerationConfig(request);
                
                // Stream generation
                ResponseStream<GenerateContentResponse> responseStream;
                if (contents.size() == 1) {
                    // Single message
                    responseStream = genaiClient.models.generateContentStream(model, contents.get(0), config);
                } else {
                    // Multi-turn conversation
                    responseStream = genaiClient.models.generateContentStream(model, contents, config);
                }
                
                // Process stream
                try {
                    for (GenerateContentResponse chunk : responseStream) {
                        String text = chunk.text();
                        if (text != null && !text.isEmpty()) {
                            handler.onChunk(text);
                        }
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
    
    @Override
    public boolean testConnection() {
        try {
            ChatRequest testRequest = ChatRequest.builder()
                .addUserMessage("Hello")
                .model(modelName)
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
        return "Gemini (Google GenAI)";
    }
    
    @Override
    public Map<String, Boolean> getCapabilities() {
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("streaming", true);
        capabilities.put("tool_calling", true);
        capabilities.put("vision", true);
        capabilities.put("system_messages", true);
        capabilities.put("max_tokens_8k", true);
        capabilities.put("max_tokens_32k", true);
        capabilities.put("max_tokens_128k", true);
        capabilities.put("json_mode", true);
        return capabilities;
    }
    
    private List<Content> buildContents(ChatRequest request) {
        List<Content> contents = new ArrayList<>();
        
        for (ChatRequest.Message msg : request.getMessages()) {
            // Create parts for the message
            Part textPart = Part.fromText(msg.getContent());
            
            // Map role to Gemini format
            String role;
            switch (msg.getRole()) {
                case USER:
                    role = "user";
                    break;
                case ASSISTANT:
                    role = "model";
                    break;
                case SYSTEM:
                    // Gemini handles system messages as user messages with special formatting
                    role = "user";
                    textPart = Part.fromText("[System]: " + msg.getContent());
                    break;
                default:
                    role = "user";
            }
            
            // Create content with role
            Content content = Content.builder()
                .role(role)
                .parts(textPart)
                .build();
            
            contents.add(content);
        }
        
        return contents;
    }
    
    private GenerateContentConfig buildGenerationConfig(ChatRequest request) {
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
        
        // Set temperature
        if (request.getTemperature() != null) {
            configBuilder.temperature(request.getTemperature().floatValue());
        }
        
        // Set max tokens
        if (request.getMaxTokens() != null) {
            configBuilder.maxOutputTokens(request.getMaxTokens());
        }
        
        // Set top-p with default value
        configBuilder.topP(0.95f);
        
        // Set top-k
        configBuilder.topK(40f);
        
        // Set candidate count
        configBuilder.candidateCount(1);
        
        // Configure safety settings
        ImmutableList<SafetySetting> safetySettings = ImmutableList.of(
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
                .threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_HARASSMENT)
                .threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE)
                .build(),
            SafetySetting.builder()
                .category(HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                .threshold(HarmBlockThreshold.Known.BLOCK_MEDIUM_AND_ABOVE)
                .build()
        );
        configBuilder.safetySettings(safetySettings);
        
        // Handle system instruction if present
        Optional<String> systemMessage = extractSystemMessage(request);
        if (systemMessage.isPresent()) {
            Content systemInstruction = Content.fromParts(Part.fromText(systemMessage.get()));
            configBuilder.systemInstruction(systemInstruction);
        }
        
        // JSON mode can be enabled through additional params if needed
        // Check if JSON mode is requested in additional params
        if (request.getAdditionalParams() != null && 
            "json".equals(request.getAdditionalParams().get("responseFormat"))) {
            configBuilder.responseMimeType("application/json");
        }
        
        return configBuilder.build();
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
        
        // Extract usage metadata if available
        response.usageMetadata().ifPresent(usage -> {
            int promptTokens = usage.promptTokenCount().orElse(0);
            int completionTokens = usage.candidatesTokenCount().orElse(0);
            int totalTokens = usage.totalTokenCount().orElse(0);
            
            builder.usage(new AIResponse.Usage(promptTokens, completionTokens, totalTokens));
        });
        
        // Extract finish reason if needed
        // Note: finishReason may not be available in AIResponse.Builder
        // You may need to add this field to AIResponse if required
        
        return builder.build();
    }
}