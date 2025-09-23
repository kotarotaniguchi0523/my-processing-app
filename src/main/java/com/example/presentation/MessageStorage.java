package com.example.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe message storage and queue management.
 * Extracted from ChatManager to follow Single Responsibility Principle.
 * Handles:
 * - Thread-safe message storage
 * - Message queue management
 * - Sensitive data masking
 * - Message truncation
 */
public class MessageStorage {

    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(sk-[a-zA-Z0-9]{20,}|key[=:]\\s*[\"']?[a-zA-Z0-9]{20,})",
        Pattern.CASE_INSENSITIVE
    );

    // Thread-safe collections
    private final List<String> chatMessages;
    private final Queue<String> uiMessageQueue;

    public MessageStorage() {
        this.chatMessages = new CopyOnWriteArrayList<>();
        this.uiMessageQueue = new ConcurrentLinkedQueue<>();

        // Initialize with welcome message
        chatMessages.add("System: AI Agent ready for instructions");
    }

    /**
     * Add a message to the thread-safe queue
     */
    public void queueMessage(String message) {
        uiMessageQueue.offer(message);
    }

    /**
     * Add a system message to the queue with safety processing
     */
    public void queueSystemMessage(String message) {
        String safeMessage = maskSensitiveData(message);
        String truncatedMessage = truncateMessage(safeMessage);
        uiMessageQueue.offer("System: " + truncatedMessage);
    }

    /**
     * Get a copy of all chat messages
     */
    public List<String> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }


    /**
     * Flush queued messages to storage
     * @return true if new messages were added
     */
    public boolean flushQueueToStorage() {
        String message;
        boolean hasNewMessages = false;

        while ((message = uiMessageQueue.poll()) != null) {
            chatMessages.add(message);
            hasNewMessages = true;
        }

        return hasNewMessages;
    }

    /**
     * Mask sensitive data like API keys
     */
    private String maskSensitiveData(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = API_KEY_PATTERN.matcher(message);
        StringBuffer masked = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group();
            String replacement;
            if (key.length() > 8) {
                replacement = key.substring(0, 4) + "****" + key.substring(key.length() - 4);
            } else {
                replacement = "****";
            }
            matcher.appendReplacement(masked, replacement);
        }
        matcher.appendTail(masked);

        return masked.toString();
    }

    /**
     * Truncate long messages for display
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

}