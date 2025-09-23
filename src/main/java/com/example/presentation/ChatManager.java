package com.example.presentation;

import processing.core.PApplet;
import com.example.application.services.StateChangeNotifier;
import com.example.domain.models.DrawingState;

import java.util.List;

/**
 * ChatManager handles all chat-related functionality including message management, input handling,
 * and chat display logic. This class follows the Single Responsibility Principle by focusing solely
 * on chat management. It also implements StateChangeListener to receive notifications about drawing
 * state changes.
 */
public class ChatManager implements StateChangeNotifier.StateChangeListener {

  // Extracted components
  private final MessageStorage messageStorage;
  private final ScrollManager scrollManager;
  private final TextFormatter textFormatter;

  // Input state
  private volatile String currentInput = "";
  private volatile boolean inputFocused;


  public ChatManager(PApplet sketch) {
    // Initialize extracted components
    this.messageStorage = new MessageStorage();
    this.scrollManager = new ScrollManager();
    this.textFormatter = new TextFormatter(sketch);
  }

  public void addMessage(String message) {
    // Use the thread-safe queue from MessageStorage
    messageStorage.queueMessage(message);
  }

  public List<String> getChatMessages() {
    return messageStorage.getChatMessages();
  }


  public void adjustScrollOffset(float scrollAmount) {
    int totalHeight = computeTotalContentHeight();
    scrollManager.adjustScrollOffset(scrollAmount, totalHeight);
  }

  public int getChatScrollOffset() {
    return scrollManager.getScrollOffset();
  }


  public void setChatLogHeight(int height) {
    scrollManager.setChatLogHeight(height);
  }


  /** Update layout metrics from the UI renderer to keep calculations consistent. */
  public void updateLayoutMetrics(int lineHeight, int bubblePadding, int messageSpacing,
      float contentMaxWidth) {
    // Update scroll manager metrics
    scrollManager.updateLayoutMetrics(lineHeight, bubblePadding, messageSpacing, contentMaxWidth);
  }

  // Input management (thread-safe)
  public synchronized String getCurrentInput() {
    return currentInput;
  }

  public synchronized void setCurrentInput(String input) {
    this.currentInput = input != null ? input : "";
  }

  public synchronized void addToCurrentInput(char c) {
    this.currentInput += c;
  }

  public synchronized boolean isInputFocused() {
    return inputFocused;
  }

  public synchronized void setInputFocused(boolean focused) {
    this.inputFocused = focused;
  }


  // Japanese input integration
  private boolean useJapaneseInput = false;
  private boolean japaneseInputVisible = false;

  public boolean isUsingJapaneseInput() {
    return useJapaneseInput;
  }


  public boolean isJapaneseInputVisible() {
    return japaneseInputVisible;
  }

  public void setJapaneseInputVisible(boolean visible) {
    this.japaneseInputVisible = visible;
  }



  /**
   * Flush queued messages to UI (must be called from draw() thread).
   */
  public void flushQueueToUI() {
    boolean hasNewMessages = messageStorage.flushQueueToStorage();

    if (hasNewMessages) {
      adjustScrollForNewMessage();
    }
  }


  /**
   * Adjust scroll to show new message.
   */
  private void adjustScrollForNewMessage() {
    int totalHeight = computeTotalContentHeight();
    scrollManager.autoScrollForNewContent(totalHeight);
  }

  /** Compute exact total content height using current layout metrics. */
  private int computeTotalContentHeight() {
    return scrollManager.calculateTotalHeight(messageStorage.getChatMessages(), textFormatter);
  }

  /**
   * Implementation of StateChangeListener interface. Called when the drawing state changes to
   * update the chat display.
   */
  @Override
  public void onStateChanged(DrawingState newState, String changeDescription) {
    // Add a system message to notify users about the state change
    addMessage("[System] Drawing updated: " + changeDescription + " (Shapes: "
        + newState.getShapes().size() + ")");
  }
}
