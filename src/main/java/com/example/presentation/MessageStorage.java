package com.example.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe message storage and queue management. Extracted from ChatManager to follow Single
 * Responsibility Principle. Handles: - Thread-safe message storage - Message queue management -
 * Sensitive data masking - Message truncation
 */
public class MessageStorage {


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
   * Get a copy of all chat messages
   */
  public List<String> getChatMessages() {
    return new ArrayList<>(chatMessages);
  }


  /**
   * Flush queued messages to storage
   *
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


}
