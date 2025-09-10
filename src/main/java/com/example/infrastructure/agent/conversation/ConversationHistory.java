package com.example.infrastructure.agent.conversation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the conversation history between user, assistant, and tool executions.
 * Provides methods for adding, retrieving, and managing conversation messages.
 */
public class ConversationHistory {
    
    private final List<ConversationMessage> messages;
    private final int maxMessages;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new conversation history with default settings
     */
    public ConversationHistory() {
        this(100); // Default max messages
    }
    
    /**
     * Creates a new conversation history with specified max messages
     * @param maxMessages Maximum number of messages to retain
     */
    public ConversationHistory(int maxMessages) {
        this.messages = new ArrayList<>();
        this.maxMessages = maxMessages;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Adds a message to the conversation history
     * @param message The message to add
     */
    public synchronized void addMessage(ConversationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        
        messages.add(message);
        
        // Trim history if it exceeds max messages
        if (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
    
    /**
     * Gets all messages in the conversation
     * @return Unmodifiable list of all messages
     */
    public List<ConversationMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }
    
    /**
     * Gets messages filtered by role
     * @param role The role to filter by
     * @return List of messages with the specified role
     */
    public List<ConversationMessage> getMessagesByRole(ConversationMessage.Role role) {
        return messages.stream()
            .filter(msg -> msg.getRole() == role)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the last N messages
     * @param n Number of messages to retrieve
     * @return List of the last N messages
     */
    public List<ConversationMessage> getLastMessages(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }
        
        int startIndex = Math.max(0, messages.size() - n);
        return messages.subList(startIndex, messages.size());
    }
    
    /**
     * Gets the last message in the conversation
     * @return The last message, or null if empty
     */
    public ConversationMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    /**
     * Gets the last user message
     * @return The last user message, or null if none exists
     */
    public ConversationMessage getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage msg = messages.get(i);
            if (msg.isUserMessage()) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * Gets the last assistant message
     * @return The last assistant message, or null if none exists
     */
    public ConversationMessage getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage msg = messages.get(i);
            if (msg.isAssistantMessage()) {
                return msg;
            }
        }
        return null;
    }
    
    /**
     * Clears all messages from the history
     */
    public synchronized void clear() {
        messages.clear();
        metadata.clear();
    }
    
    /**
     * Gets the number of messages in the history
     * @return The number of messages
     */
    public int size() {
        return messages.size();
    }
    
    /**
     * Checks if the history is empty
     * @return true if there are no messages
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }
    
    /**
     * Sets metadata for the conversation
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Gets metadata for the conversation
     * @param key The metadata key
     * @return The metadata value, or null if not set
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Removes metadata from the conversation
     * @param key The metadata key to remove
     * @return The removed value, or null if not present
     */
    public Object removeMetadata(String key) {
        return metadata.remove(key);
    }
    
    /**
     * Gets all metadata
     * @return Unmodifiable map of all metadata
     */
    public Map<String, Object> getAllMetadata() {
        return Collections.unmodifiableMap(new HashMap<>(metadata));
    }
    
    /**
     * Creates a summary of the conversation
     * @return A summary string
     */
    public String getSummary() {
        Map<ConversationMessage.Role, Long> roleCounts = messages.stream()
            .collect(Collectors.groupingBy(ConversationMessage::getRole, Collectors.counting()));
        
        return String.format("Conversation History: %d messages (User: %d, Assistant: %d, System: %d, Tool Results: %d)",
            messages.size(),
            roleCounts.getOrDefault(ConversationMessage.Role.USER, 0L),
            roleCounts.getOrDefault(ConversationMessage.Role.ASSISTANT, 0L),
            roleCounts.getOrDefault(ConversationMessage.Role.SYSTEM, 0L),
            roleCounts.getOrDefault(ConversationMessage.Role.TOOL_RESULT, 0L));
    }
    
    /**
     * Exports the conversation history as a formatted string
     * @return Formatted conversation history
     */
    public String export() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Conversation History ===\n");
        sb.append("Total Messages: ").append(messages.size()).append("\n");
        sb.append("Max Messages: ").append(maxMessages).append("\n\n");
        
        for (ConversationMessage msg : messages) {
            sb.append(String.format("[%s] %s: %s\n",
                msg.getTimestamp(),
                msg.getRole().getValue().toUpperCase(),
                msg.getContent()));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a context window of the most recent messages
     * @param windowSize The size of the context window
     * @return List of messages within the context window
     */
    public List<ConversationMessage> getContextWindow(int windowSize) {
        return getLastMessages(windowSize);
    }
    
    /**
     * Removes messages older than a certain index
     * @param keepLast Number of recent messages to keep
     */
    public synchronized void trimHistory(int keepLast) {
        if (keepLast <= 0 || messages.size() <= keepLast) {
            return;
        }
        
        int removeCount = messages.size() - keepLast;
        for (int i = 0; i < removeCount; i++) {
            messages.remove(0);
        }
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}