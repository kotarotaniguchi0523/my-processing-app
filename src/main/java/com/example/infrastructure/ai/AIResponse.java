package com.example.infrastructure.ai;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from an AI service
 */
public class AIResponse {
    
    private final String content;
    private final String model;
    private final String id;
    private final Usage usage;
    private final List<ToolCall> toolCalls;
    private final Map<String, Object> metadata;
    
    private AIResponse(Builder builder) {
        this.content = builder.content;
        this.model = builder.model;
        this.id = builder.id;
        this.usage = builder.usage;
        this.toolCalls = builder.toolCalls;
        this.metadata = builder.metadata;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getContent() { return content; }
    public String getModel() { return model; }
    public String getId() { return id; }
    public Usage getUsage() { return usage; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
    
    /**
     * Represents token usage information
     */
    public static class Usage {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;
        
        public Usage(int promptTokens, int completionTokens, int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
        
        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public int getTotalTokens() { return totalTokens; }
    }
    
    /**
     * Represents a tool call in the response
     */
    public static class ToolCall {
        private final String id;
        private final String name;
        private final Map<String, Object> parameters;
        
        public ToolCall(String id, String name, Map<String, Object> parameters) {
            this.id = id;
            this.name = name;
            this.parameters = parameters;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public Map<String, Object> getParameters() { return parameters; }
    }
    
    /**
     * Builder for AIResponse
     */
    public static class Builder {
        private String content;
        private String model;
        private String id;
        private Usage usage;
        private List<ToolCall> toolCalls;
        private Map<String, Object> metadata;
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public AIResponse build() {
            return new AIResponse(this);
        }
    }
}