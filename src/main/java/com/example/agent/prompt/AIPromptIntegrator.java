package com.example.agent.prompt;

import com.example.infrastructure.ai.*;
import com.example.domain.tools.ToolRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Integrates the PromptBuilder with AI clients to facilitate tool-aware conversations.
 * Handles the conversion between prompt formats and AI client request/response formats.
 */
public class AIPromptIntegrator {
    
    private static final Logger logger = Logger.getLogger(AIPromptIntegrator.class.getName());
    
    private final AIClient aiClient;
    private final PromptBuilder promptBuilder;
    private final Gson gson;
    
    public AIPromptIntegrator(AIClient aiClient) {
        this(aiClient, new PromptBuilder());
    }
    
    public AIPromptIntegrator(AIClient aiClient, PromptBuilder promptBuilder) {
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
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
        
        // Set parameters
        requestBuilder.temperature(0.7);
        requestBuilder.maxTokens(2000);
        
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
        
        // Look for JSON blocks in the content
        String jsonPattern = "```json\\s*([\\s\\S]*?)\\s*```";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String jsonStr = matcher.group(1);
            try {
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                
                // Check if it's a tool call format
                if (json.has("tool") && json.has("parameters")) {
                    String toolName = json.get("tool").getAsString();
                    Map<String, Object> parameters = gson.fromJson(json.get("parameters"), Map.class);
                    
                    AIResponse.ToolCall toolCall = new AIResponse.ToolCall(
                        generateToolCallId(),
                        toolName,
                        parameters
                    );
                    toolCalls.add(toolCall);
                }
            } catch (Exception e) {
                logger.warning("Failed to parse potential tool call JSON: " + e.getMessage());
            }
        }
        
        return toolCalls;
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