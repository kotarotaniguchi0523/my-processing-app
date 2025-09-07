package com.example.agent.prompt;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single message in the conversation history
 */
public class ConversationMessage {
    
    /**
     * Role enumeration for different message types
     */
    public enum Role {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system"),
        TOOL_RESULT("tool_result");
        
        private final String value;
        
        Role(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    private final Role role;
    private final String content;
    private final Instant timestamp;
    private final String messageId;
    
    private ConversationMessage(Role role, String content) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.timestamp = Instant.now();
        this.messageId = generateMessageId();
    }
    
    /**
     * Creates a user message
     * @param content The message content
     * @return A new user message
     */
    public static ConversationMessage user(String content) {
        return new ConversationMessage(Role.USER, content);
    }
    
    /**
     * Creates an assistant message
     * @param content The message content
     * @return A new assistant message
     */
    public static ConversationMessage assistant(String content) {
        return new ConversationMessage(Role.ASSISTANT, content);
    }
    
    /**
     * Creates a system message
     * @param content The message content
     * @return A new system message
     */
    public static ConversationMessage system(String content) {
        return new ConversationMessage(Role.SYSTEM, content);
    }
    
    /**
     * Creates a tool result message
     * @param content The message content
     * @return A new tool result message
     */
    public static ConversationMessage toolResult(String content) {
        return new ConversationMessage(Role.TOOL_RESULT, content);
    }
    
    /**
     * Generates a unique message ID
     * @return A unique message ID
     */
    private String generateMessageId() {
        return String.format("%s_%d_%s", 
            role.getValue(), 
            timestamp.toEpochMilli(),
            Integer.toHexString(content.hashCode()));
    }
    
    // Getters
    public Role getRole() {
        return role;
    }
    
    public String getContent() {
        return content;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    /**
     * Checks if this is a user message
     * @return true if this is a user message
     */
    public boolean isUserMessage() {
        return role == Role.USER;
    }
    
    /**
     * Checks if this is an assistant message
     * @return true if this is an assistant message
     */
    public boolean isAssistantMessage() {
        return role == Role.ASSISTANT;
    }
    
    /**
     * Checks if this is a system message
     * @return true if this is a system message
     */
    public boolean isSystemMessage() {
        return role == Role.SYSTEM;
    }
    
    /**
     * Checks if this is a tool result message
     * @return true if this is a tool result message
     */
    public boolean isToolResultMessage() {
        return role == Role.TOOL_RESULT;
    }
    
    /**
     * Gets a truncated version of the content for display
     * @param maxLength Maximum length of the truncated content
     * @return Truncated content
     */
    public String getTruncatedContent(int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength - 3) + "...";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationMessage that = (ConversationMessage) o;
        return messageId.equals(that.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
    
    @Override
    public String toString() {
        return String.format("ConversationMessage{role=%s, content='%s', timestamp=%s}", 
            role, getTruncatedContent(50), timestamp);
    }
}