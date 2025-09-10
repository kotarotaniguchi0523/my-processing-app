package com.example.agent.prompt;

import com.example.infrastructure.ai.*;
import com.example.domain.tools.IToolRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Integrates the PromptBuilder with AI clients to facilitate tool-aware conversations.
 * Handles the conversion between prompt formats and AI client request/response formats.
 */
public class AIPromptIntegrator {
    
    private static final Logger logger = Logger.getLogger(AIPromptIntegrator.class.getName());
    
    private final AIClient aiClient;
    private final PromptBuilder promptBuilder;
    private final IToolRegistry toolRegistry;
    private final Gson gson;
    
    public AIPromptIntegrator(AIClient aiClient) {
        this(aiClient, new PromptBuilder());
    }

    public AIPromptIntegrator(AIClient aiClient, PromptBuilder promptBuilder) {
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.toolRegistry = promptBuilder.getToolRegistry();
        this.gson = new Gson();
    }
    
    /**
     * Sends a user message to the AI and gets a response
     * @param userMessage The user's message
     * @return The AI's response
     */
    public CompletableFuture<AIResponse> sendMessage(String userMessage) {
        // Add user message to history
        promptBuilder.addUserMessage(userMessage);
        
        // Build chat request
        ChatRequest request = buildChatRequest();
        
        // Send to AI
        return aiClient.chatCompletion(request)
            .thenApply(response -> {
                // Add assistant response to history
                if (response.getContent() != null) {
                    promptBuilder.addAssistantResponse(response.getContent());
                }
                return response;
            });
    }
    
    /**
     * Processes a user message and handles tool calls if present
     * @param userMessage The user's message
     * @return ProcessedResponse containing the result and any tool calls
     */
    public CompletableFuture<ProcessedResponse> processMessage(String userMessage) {
        return sendMessage(userMessage)
            .thenApply(this::processAIResponse);
    }
    
    /**
     * Builds a ChatRequest from the current conversation state
     * @return The chat request
     */
    private ChatRequest buildChatRequest() {
        ChatRequest.Builder requestBuilder = ChatRequest.builder();
        
        // Add system prompt with tool schemas
        String systemPrompt = promptBuilder.buildSystemPrompt();
        requestBuilder.addSystemMessage(systemPrompt);
        
        // Add conversation history
        ConversationHistory history = promptBuilder.getConversationHistory();
        for (ConversationMessage msg : history.getMessages()) {
            switch (msg.getRole()) {
                case USER:
                    requestBuilder.addUserMessage(msg.getContent());
                    break;
                case ASSISTANT:
                    requestBuilder.addAssistantMessage(msg.getContent());
                    break;
                case SYSTEM:
                    requestBuilder.addSystemMessage(msg.getContent());
                    break;
                case TOOL_RESULT:
                    // Format tool results as user messages for context
                    requestBuilder.addUserMessage("[Tool Result] " + msg.getContent());
                    break;
            }
        }
        
        // Set parameters and prefer JSON responses for tool call extraction
        requestBuilder.temperature(0.7);
        requestBuilder.maxTokens(2000);
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("responseFormat", "json");
        requestBuilder.additionalParams(params);

        return requestBuilder.build();
    }
    
    /**
     * Processes an AI response to extract tool calls
     * @param response The AI response
     * @return Processed response with extracted information
     */
    private ProcessedResponse processAIResponse(AIResponse response) {
        ProcessedResponse processed = new ProcessedResponse();
        processed.setRawResponse(response);
        
        if (response.hasToolCalls()) {
            // Handle explicit tool calls from AI
            processed.setToolCalls(response.getToolCalls());
        } else if (response.getContent() != null) {
            // Try to extract tool calls from content
            List<AIResponse.ToolCall> extractedCalls = extractToolCallsFromContent(response.getContent());
            processed.setToolCalls(extractedCalls);
            processed.setTextResponse(response.getContent());
        }
        
        return processed;
    }
    
    /**
     * Attempts to extract tool calls from text content (JSON format)
     * @param content The response content
     * @return List of extracted tool calls
     */
    private List<AIResponse.ToolCall> extractToolCallsFromContent(String content) {
        List<AIResponse.ToolCall> toolCalls = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return toolCalls;
        }
        
        // Try multiple extraction strategies
        toolCalls.addAll(extractFromJsonCodeBlocks(content));
        toolCalls.addAll(extractFromInlineJson(content));
        toolCalls.addAll(extractFromXmlTags(content));
        
        // Log extraction results
        if (!toolCalls.isEmpty()) {
            logger.info(String.format("Extracted %d tool calls from AI response", toolCalls.size()));
        }
        
        return toolCalls;
    }
    
    /**
     * Extracts tool calls from JSON code blocks (```json ... ```)
     */
    private List<AIResponse.ToolCall> extractFromJsonCodeBlocks(String content) {
        List<AIResponse.ToolCall> toolCalls = new ArrayList<>();
        
        // Look for JSON blocks in the content
        String jsonPattern = "```(?:json)?\\s*([\\s\\S]*?)\\s*```";
        Pattern pattern = Pattern.compile(jsonPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String jsonStr = matcher.group(1).trim();
            toolCalls.addAll(parseToolCallsFromJson(jsonStr));
        }
        
        return toolCalls;
    }
    
    /**
     * Extracts tool calls from inline JSON (looking for standalone JSON objects)
     */
    private List<AIResponse.ToolCall> extractFromInlineJson(String content) {
        List<AIResponse.ToolCall> toolCalls = new ArrayList<>();
        
        // Look for JSON-like structures starting with { and containing "tool"
        String jsonPattern = "\\{[^{}]*\"tool\"[^{}]*\\}";
        Pattern pattern = Pattern.compile(jsonPattern);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String jsonStr = matcher.group();
            // Reuse common JSON parser that supports object/array
            List<AIResponse.ToolCall> parsed = parseToolCallsFromJson(jsonStr);
            if (parsed != null && !parsed.isEmpty()) {
                toolCalls.addAll(parsed);
            }
        }
        
        return toolCalls;
    }
    
    /**
     * Extracts tool calls from XML-style tags (some AIs use this format)
     */
    private List<AIResponse.ToolCall> extractFromXmlTags(String content) {
        List<AIResponse.ToolCall> toolCalls = new ArrayList<>();
        
        // Look for <tool_call>...</tool_call> patterns
        String xmlPattern = "<tool_call[^>]*>([\\s\\S]*?)</tool_call>";
        Pattern pattern = Pattern.compile(xmlPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String xmlContent = matcher.group(1).trim();
            toolCalls.addAll(parseToolCallsFromJson(xmlContent));
        }
        
        return toolCalls;
    }

    /**
     * Parses a JSON string (object or array) into ToolCall objects
     */
    private List<AIResponse.ToolCall> parseToolCallsFromJson(String jsonStr) {
        List<AIResponse.ToolCall> calls = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return calls;
        }

        try {
            var parsed = JsonParser.parseString(jsonStr);
            if (parsed.isJsonArray()) {
                var arr = parsed.getAsJsonArray();
                for (var el : arr) {
                    if (el.isJsonObject()) {
                        AIResponse.ToolCall call = parseToolCallObject(el.getAsJsonObject());
                        if (call != null) {
                            calls.add(call);
                        }
                    }
                }
            } else if (parsed.isJsonObject()) {
                AIResponse.ToolCall call = parseToolCallObject(parsed.getAsJsonObject());
                if (call != null) {
                    calls.add(call);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to parse tool calls JSON: " + e.getMessage());
        }
        return calls;
    }

    /** Parses a single tool call object */
    private AIResponse.ToolCall parseToolCallObject(JsonObject json) {
        try {
            // Check if it's a tool call format - support multiple field names
            String toolName = null;
            if (json.has("tool")) {
                toolName = json.get("tool").getAsString();
            } else if (json.has("name")) {
                toolName = json.get("name").getAsString();
            } else if (json.has("function")) {
                toolName = json.get("function").getAsString();
            }

            if (toolName == null || toolName.trim().isEmpty()) {
                return null;
            }

            // Extract parameters - support multiple field names
            Map<String, Object> parameters = new java.util.HashMap<>();
            if (json.has("parameters")) {
                parameters = gson.fromJson(json.get("parameters"), Map.class);
            } else if (json.has("args")) {
                parameters = gson.fromJson(json.get("args"), Map.class);
            } else if (json.has("arguments")) {
                parameters = gson.fromJson(json.get("arguments"), Map.class);
            }

            if (parameters == null) {
                parameters = new java.util.HashMap<>();
            }

            if (!toolRegistry.hasTool(toolName)) {
                logger.warning("Tool '" + toolName + "' not found in registry, skipping");
                return null;
            }

            return new AIResponse.ToolCall(
                generateToolCallId(),
                toolName,
                parameters
            );
        } catch (Exception e) {
            logger.warning("Error parsing tool call object: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates a unique tool call ID
     * @return A unique ID string
     */
    private String generateToolCallId() {
        return "tool_call_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    /**
     * Adds a tool execution result to the conversation
     * @param toolName The executed tool name
     * @param result The execution result
     */
    public void addToolResult(String toolName, String result) {
        promptBuilder.addToolResult(toolName, result);
    }
    
    /**
     * Clears the conversation history
     */
    public void clearConversation() {
        promptBuilder.clearHistory();
    }
    
    /**
     * Gets the current conversation history
     * @return The conversation history
     */
    public ConversationHistory getConversationHistory() {
        return promptBuilder.getConversationHistory();
    }
    
    /**
     * Gets the prompt builder
     * @return The prompt builder instance
     */
    public PromptBuilder getPromptBuilder() {
        return promptBuilder;
    }
    
    /**
     * Represents a processed AI response with extracted tool calls
     */
    public static class ProcessedResponse {
        private AIResponse rawResponse;
        private String textResponse;
        private List<AIResponse.ToolCall> toolCalls;
        
        public ProcessedResponse() {
            this.toolCalls = new ArrayList<>();
        }
        
        public AIResponse getRawResponse() {
            return rawResponse;
        }
        
        public void setRawResponse(AIResponse rawResponse) {
            this.rawResponse = rawResponse;
        }
        
        public String getTextResponse() {
            return textResponse;
        }
        
        public void setTextResponse(String textResponse) {
            this.textResponse = textResponse;
        }
        
        public List<AIResponse.ToolCall> getToolCalls() {
            return toolCalls;
        }
        
        public void setToolCalls(List<AIResponse.ToolCall> toolCalls) {
            this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
        }
        
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
        
        public boolean hasTextResponse() {
            return textResponse != null && !textResponse.trim().isEmpty();
        }
    }
}
