package com.example.presentation;

import processing.core.PApplet;
import com.example.domain.events.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * ChatManager handles all chat-related functionality including message management, input handling,
 * and chat display logic. This class follows the Single Responsibility Principle by focusing solely
 * on chat management.
 */
public class ChatManager implements IAIProcessingListener {
  private static final int DEFAULT_LINE_HEIGHT = 20;

  private final PApplet sketch;

  // Chat state (thread-safe)
  private final List<String> chatMessages;
  private String currentInput = "";
  private int chatScrollOffset;
  private boolean inputFocused;
  // Layout metrics (updated by UI renderer for exact calculations)
  private int layoutLineHeight = DEFAULT_LINE_HEIGHT;
  private int layoutBubblePadding = 14;
  private int layoutMessageSpacing = 12;
  private float layoutContentMaxWidth = 600f;

  // Icons for different message types
  private static final String AI_THINKING_PREFIX = "🤖 ";
  private static final String TOOL_PREFIX = "🔧 ";
  private static final String SUCCESS_PREFIX = "✅ ";
  private static final String ERROR_PREFIX = "❌ ";
  private static final String ANALYSIS_PREFIX = "🔍 ";

  // Thread-safe queue for UI updates
  private final Queue<String> uiMessageQueue = new ConcurrentLinkedQueue<>();
  private static final int MAX_MESSAGE_LENGTH = 200;
  private static final Pattern API_KEY_PATTERN = Pattern
      .compile("(sk-[a-zA-Z0-9]{20,}|key[=:]\s*[\"']?[a-zA-Z0-9]{20,})", Pattern.CASE_INSENSITIVE);

  public ChatManager(PApplet sketch) {
    this.sketch = sketch;
    this.chatMessages = new CopyOnWriteArrayList<>();

    // Initialize with welcome message
    chatMessages.add("System: AI Agent ready for instructions");
  }

  public void addMessage(String message) {
    // Use the thread-safe queue instead of directly adding to chatMessages
    // This ensures messages from background threads are properly handled
    uiMessageQueue.offer(message);
  }

  public List<String> getChatMessages() {
    return new ArrayList<>(chatMessages);
  }

  public List<String> getWrappedMessageLines(float maxWidth) {
    List<String> allLines = new ArrayList<>();
    for (String message : chatMessages) {
      allLines.addAll(wrapForWidth(stripPrefix(message), maxWidth));
    }
    return allLines;
  }

  public void adjustScrollOffset(float scrollAmount) {
    int totalHeight = computeTotalContentHeight();
    if (totalHeight > getChatLogHeight()) {
      chatScrollOffset += (int) scrollAmount;
      int maxScroll = Math.max(0, totalHeight - getChatLogHeight());
      chatScrollOffset = PApplet.constrain(chatScrollOffset, 0, maxScroll);
    } else {
      chatScrollOffset = 0;
    }
  }

  public int getChatScrollOffset() {
    return chatScrollOffset;
  }

  // Text wrapping logic - handles languages without spaces (e.g., Japanese)
  private List<String> wrapForWidth(String text, float maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return lines;
    }

    // Prefer space-based wrapping when spaces exist
    if (text.contains(" ")) {
      String current = "";
      for (String word : text.split(" ")) {
        String test = current.isEmpty() ? word : current + " " + word;
        if (sketch.textWidth(test) > maxWidth) {
          if (!current.isEmpty()) {
            lines.add(current);
            current = word;
          } else {
            // Single long word – fall back to char wrapping
            lines.addAll(charWrap(word, maxWidth));
            current = "";
          }
        } else {
          current = test;
        }
      }
      if (!current.isEmpty()) {
        lines.add(current);
      }
    } else {
      // No spaces – use character wrapping (common for Japanese)
      lines.addAll(charWrap(text, maxWidth));
    }

    return lines;
  }

  private List<String> charWrap(String text, float maxWidth) {
    List<String> out = new ArrayList<>();
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String test = buf.toString() + c;
      if (sketch.textWidth(test) > maxWidth) {
        if (buf.length() > 0) {
          out.add(buf.toString());
          buf.setLength(0);
        }
        // If even a single char is wider than maxWidth, hard add to avoid infinite loop
        if (sketch.textWidth(String.valueOf(c)) > maxWidth) {
          out.add(String.valueOf(c));
        } else {
          buf.append(c);
        }
      } else {
        buf.append(c);
      }
    }
    if (buf.length() > 0) {
      out.add(buf.toString());
    }
    return out;
  }

  // Cache for chat log height - set by UI renderer
  private int chatLogHeight = 200;

  public void setChatLogHeight(int height) {
    this.chatLogHeight = height;
  }

  private int getChatLogHeight() {
    return chatLogHeight;
  }

  /** Update layout metrics from the UI renderer to keep calculations consistent. */
  public void updateLayoutMetrics(int lineHeight, int bubblePadding, int messageSpacing,
      float contentMaxWidth) {
    this.layoutLineHeight = lineHeight > 0 ? lineHeight : DEFAULT_LINE_HEIGHT;
    this.layoutBubblePadding = Math.max(0, bubblePadding);
    this.layoutMessageSpacing = Math.max(0, messageSpacing);
    this.layoutContentMaxWidth = Math.max(10f, contentMaxWidth);
  }

  // Input management
  public String getCurrentInput() {
    return currentInput;
  }

  public void setCurrentInput(String input) {
    this.currentInput = input;
  }

  public void addToCurrentInput(char c) {
    this.currentInput += c;
  }

  public boolean isInputFocused() {
    return inputFocused;
  }

  public void setInputFocused(boolean focused) {
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

  // --- IAIProcessingListener Implementation ---

  @Override
  public void onProcessingStarted(ProcessingStartedEvent event) {
    addSystemMessage(AI_THINKING_PREFIX + "処理を開始しています...");
  }

  @Override
  public void onPromptAnalyzing(PromptAnalyzingEvent event) {
    addSystemMessage(ANALYSIS_PREFIX + event.getAnalysis());
  }

  @Override
  public void onToolSelecting(ToolSelectingEvent event) {
    String message = String.format("%sツール選択: %s", TOOL_PREFIX, event.getToolName());
    addSystemMessage(message);

    // Show reason
    if (event.getReason() != null && !event.getReason().isEmpty()) {
      addSystemMessage("  理由: " + event.getReason());
    }

    // Show parameters
    if (event.getParameters() != null && !event.getParameters().isEmpty()) {
      addSystemMessage("  パラメータ: " + formatParameters(event.getParameters()));
    }
  }

  @Override
  public void onToolExecuting(ToolExecutingEvent event) {
    String statusText =
        event.getStatus() instanceof ToolStatus ? ((ToolStatus) event.getStatus()).getDisplayName()
            : event.getStatus().toString();
    addSystemMessage(TOOL_PREFIX + event.getToolName() + " を" + statusText + "...");
  }

  @Override
  public void onProcessingCompleted(ProcessingCompletedEvent event) {
    addSystemMessage(SUCCESS_PREFIX + "処理完了");
  }

  @Override
  public void onProcessingFailed(ProcessingFailedEvent event) {
    addSystemMessage(ERROR_PREFIX + "エラー: " + event.getErrorMessage());
    if (event.getFailurePoint() != null) {
      addSystemMessage("  失敗箇所: " + event.getFailurePoint().getDisplayName());
    }
  }

  /**
   * Add a system message to the thread-safe queue. Messages will be processed in the UI thread via
   * flushQueueToUI().
   */
  private void addSystemMessage(String message) {
    // Mask sensitive data and truncate if needed
    String safeMessage = maskSensitiveData(message);
    String truncatedMessage = truncateMessage(safeMessage);

    // Add to thread-safe queue instead of directly to chatMessages
    uiMessageQueue.offer("System: " + truncatedMessage);
  }

  /**
   * Flush queued messages to UI (must be called from draw() thread).
   */
  public void flushQueueToUI() {
    String message;
    boolean hasNewMessages = false;

    while ((message = uiMessageQueue.poll()) != null) {
      chatMessages.add(message);
      hasNewMessages = true;
    }

    if (hasNewMessages) {
      adjustScrollForNewMessage();
    }
  }

  /**
   * Mask sensitive data like API keys.
   */
  private String maskSensitiveData(String message) {
    if (message == null)
      return "";

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
   * Truncate long messages for display.
   */
  private String truncateMessage(String message) {
    if (message == null)
      return "";
    if (message.length() <= MAX_MESSAGE_LENGTH)
      return message;
    return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
  }

  /**
   * Format parameters for display.
   */
  private String formatParameters(Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return "";
    }

    return params.entrySet().stream().map(e -> formatParameter(e.getKey(), e.getValue()))
        .collect(Collectors.joining(", "));
  }

  /**
   * Format a single parameter for display.
   */
  private String formatParameter(String key, Object value) {
    if (value instanceof Map) {
      // Handle nested objects (like position)
      @SuppressWarnings("unchecked")
      Map<String, Object> nested = (Map<String, Object>) value;
      String nestedStr = nested.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining(","));
      return key + "={" + nestedStr + "}";
    } else {
      return key + "=" + value;
    }
  }

  /**
   * Adjust scroll to show new message.
   */
  private void adjustScrollForNewMessage() {
    int totalHeight = computeTotalContentHeight();
    int viewHeight = getChatLogHeight();
    if (totalHeight > viewHeight) {
      chatScrollOffset = Math.max(0, totalHeight - viewHeight);
    } else {
      chatScrollOffset = 0;
    }
  }

  /** Compute exact total content height using current layout metrics. */
  private int computeTotalContentHeight() {
    int y = 10; // matches renderer's initial offset
    for (String message : chatMessages) {
      String displayText = stripPrefix(message);
      List<String> lines = wrapForWidth(displayText, layoutContentMaxWidth);
      int bubbleHeight = lines.size() * layoutLineHeight + layoutBubblePadding * 2;
      y += bubbleHeight + layoutMessageSpacing;
    }
    return y; // already includes top padding
  }

  /** Remove role prefix for wrapping consistency with renderer. */
  private String stripPrefix(String message) {
    if (message == null)
      return "";
    if (message.startsWith("User:"))
      return message.substring(5 + 1).trim();
    if (message.startsWith("Assistant:"))
      return message.substring(9 + 1).trim();
    if (message.startsWith("System:"))
      return message.substring(6 + 1).trim();
    return message;
  }
}
