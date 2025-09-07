package com.example.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a chat request to send to an AI service
 */
public class ChatRequest {
    
    private final List<Message> messages;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final Map<String, Object> additionalParams;
    
    private ChatRequest(Builder builder) {
        this.messages = new ArrayList<>(builder.messages);
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.additionalParams = builder.additionalParams;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public List<Message> getMessages() { return new ArrayList<>(messages); }
    public String getModel() { return model; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public Map<String, Object> getAdditionalParams() { return additionalParams; }
    
    /**
     * Represents a message in the chat
     */
    public static class Message {
        private final Role role;
        private final String content;
        
        public Message(Role role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public static Message user(String content) {
            return new Message(Role.USER, content);
        }
        
        public static Message assistant(String content) {
            return new Message(Role.ASSISTANT, content);
        }
        
        public static Message system(String content) {
            return new Message(Role.SYSTEM, content);
        }
        
        public Role getRole() { return role; }
        public String getContent() { return content; }
    }
    
    /**
     * Role enumeration for messages
     */
    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant");
        
        private final String value;
        
        Role(String value) {
            this.value = value;
        }
        
        public String getValue() { return value; }
    }
    
    /**
     * Builder for ChatRequest
     */
    public static class Builder {
        private List<Message> messages = new ArrayList<>();
        private String model;
        private Double temperature = 0.7;
        private Integer maxTokens = 1000;
        private Map<String, Object> additionalParams;
        
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }
        
        public Builder addUserMessage(String content) {
            return addMessage(Message.user(content));
        }
        
        public Builder addAssistantMessage(String content) {
            return addMessage(Message.assistant(content));
        }
        
        public Builder addSystemMessage(String content) {
            return addMessage(Message.system(content));
        }
        
        public Builder messages(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder temperature(double temperature) {
            if (temperature < 0 || temperature > 2) {
                throw new IllegalArgumentException("Temperature must be between 0 and 2");
            }
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("Max tokens must be positive");
            }
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder additionalParams(Map<String, Object> params) {
            this.additionalParams = params;
            return this;
        }
        
        public ChatRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalStateException("At least one message is required");
            }
            return new ChatRequest(this);
        }
    }
}