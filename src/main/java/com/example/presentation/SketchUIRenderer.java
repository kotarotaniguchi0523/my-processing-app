package com.example.presentation;


import processing.core.PApplet;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.List;

import com.example.application.services.AIAgentService;
import com.example.infrastructure.rendering.DrawingEngine;


/**
 * SketchUIRenderer handles all UI drawing responsibilities for the SimpleSketch application. This
 * class follows the Single Responsibility Principle by focusing solely on rendering UI components.
 */
public class SketchUIRenderer {

  // UI Layout constants
  private static final int MARGIN = 12; // 少し余白を増やす（モダンな空間設計）
  private static final int INNER_GAP_SMALL = 6;
  private static final int INPUT_HEIGHT = 36;
  private static final int LINE_HEIGHT = 22;
  private static final int MESSAGE_SPACING = 12;
  private static final int BUBBLE_PADDING = 14;
  // private static final int CHAT_LOG_INNER_PADDING = 32; // 未使用のため一旦無効化
  private static final int INPUT_TEXT_RIGHT_PADDING = 96;
  private static final int CHAT_INPUT_RIGHT_GAP = 78;
  private static final int SEND_BUTTON_WIDTH = 64;
  private static final int CURSOR_BLINK_PERIOD = 60;
  private static final int CURSOR_BLINK_ON_TICKS = 30;
  private static final String ELLIPSIS = "...";
  private static final int MIN_LINES_FOR_MULTILINE = 2;
  private static final float WARN_THRESHOLD_RATIO = 0.8f;
  private static final int MAX_MESSAGE_LENGTH = 500;
  private static final float SHADOW_ALPHA = 24f; // 影の濃さ
  private static final int CARD_RADIUS = 14;
  private static final int BUBBLE_RADIUS = 16;
  private static final int TITLE_Y_OFFSET = 12;

  private final PApplet sketch;
  private final ChatManager chatManager;
  private final AIAgentService aiAgentService;
  private final DrawingEngine drawingEngine;

  // UI Areas
  private int canvasAreaY;
  private int canvasAreaHeight;
  private int chatAreaY;
  private int chatAreaHeight;
  private int chatInputY;
  private int chatLogY;
  private int chatLogHeight;

  // UI Components
  private PFont font;
  private PFont smallFont;

  // Theme / Style
  private boolean darkMode = false;
  private int colorBgStart;
  private int colorBgEnd;
  private int colorSurface;
  private int colorSurfaceMuted;
  private int colorBorder;
  private int colorTextPrimary;
  private int colorTextSecondary;
  private int colorAccent;
  private int colorUserBubble;
  private int colorAssistantBubble;
  private int colorSystemBubble;
  private float sendHoverAnim = 0f; // 0..1
  private boolean sendPressed = false;

  public SketchUIRenderer(PApplet sketch, ChatManager chatManager, AIAgentService aiAgentService) {
    this.sketch = sketch;
    this.chatManager = chatManager;
    this.aiAgentService = aiAgentService;
    this.drawingEngine = new DrawingEngine(sketch);
    applyTheme();
  }

  public void setFonts(PFont font, PFont smallFont) {
    this.font = font;
    this.smallFont = smallFont;
  }

  public void calculateLayout() {
    canvasAreaY = MARGIN + 8; // ヘッダー余白風
    canvasAreaHeight = (int) ((sketch.height - MARGIN * 2) * 0.54); // 54% for canvas

    chatAreaY = canvasAreaY + canvasAreaHeight + MARGIN;
    chatAreaHeight = sketch.height - chatAreaY - MARGIN; // Rest for chat

    chatInputY = chatAreaY + chatAreaHeight - INPUT_HEIGHT - MARGIN;
    chatLogY = chatAreaY + MARGIN + 24; // タイトル分の余白
    chatLogHeight = chatAreaHeight - INPUT_HEIGHT - MARGIN * 2 - 24; // タイトル分を差し引き

    // Update chat manager with the calculated height
    chatManager.setChatLogHeight(chatLogHeight);

    // Update chat layout metrics for accurate wrapping/scrolling
    float chatLogWidth = (sketch.width - MARGIN * 2 - 10);
    float bubbleMaxWidth = chatLogWidth * 0.74f; // match drawChatLog()
    float bubbleContentMaxWidth = bubbleMaxWidth - BUBBLE_PADDING * 2f;
    chatManager.updateLayoutMetrics(LINE_HEIGHT, BUBBLE_PADDING, MESSAGE_SPACING,
        bubbleContentMaxWidth);
  }

  // ===== Theme =====
  public void toggleTheme() {
    darkMode = !darkMode;
    applyTheme();
  }

  private void applyTheme() {
    if (darkMode) {
      colorBgStart = sketch.color(18, 22, 28);
      colorBgEnd = sketch.color(28, 32, 38);
      colorSurface = sketch.color(34, 40, 48);
      colorSurfaceMuted = sketch.color(40, 46, 54);
      colorBorder = sketch.color(70, 80, 92);
      colorTextPrimary = sketch.color(235);
      colorTextSecondary = sketch.color(190);
      colorAccent = sketch.color(88, 142, 255);
      colorUserBubble = sketch.color(70, 74, 120);
      colorAssistantBubble = sketch.color(248);
      colorSystemBubble = sketch.color(210);
    } else {
      colorBgStart = sketch.color(245);
      colorBgEnd = sketch.color(232, 240, 255);
      colorSurface = sketch.color(255);
      colorSurfaceMuted = sketch.color(248, 249, 252);
      colorBorder = sketch.color(210);
      colorTextPrimary = sketch.color(28);
      colorTextSecondary = sketch.color(90);
      colorAccent = sketch.color(60, 110, 255);
      colorUserBubble = sketch.color(60, 60, 60);
      colorAssistantBubble = sketch.color(255);
      colorSystemBubble = sketch.color(230);
    }
  }

  // 背景グラデーション
  public void drawBackground() {
    sketch.pushStyle();
    try {
      for (int y = 0; y < sketch.height; y++) {
        float t = PApplet.map(y, 0, (float) sketch.height, 0, 1);
        int c = sketch.lerpColor(colorBgStart, colorBgEnd, t);
        sketch.stroke(c);
        sketch.line(0, y, sketch.width, y);
      }
      // ヘッダー風のタイトル
      if (font != null)
        sketch.textFont(font);
      sketch.fill(colorTextSecondary);
      sketch.textAlign(PApplet.LEFT, PApplet.TOP);
      sketch.text("AI Processing Studio", MARGIN + 2, 8);
    } finally {
      sketch.popStyle();
    }
  }

  public void drawCanvasArea() {
    sketch.pushStyle();
    try {
      // Canvas card with soft shadow
      drawCard(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight, CARD_RADIUS);



      // Canvas title
      sketch.fill(colorTextSecondary);
      if (font != null) {
        sketch.textFont(font);
      }
      sketch.textAlign(PApplet.LEFT, PApplet.TOP);
      sketch.text("Drawing Canvas", MARGIN + 14, canvasAreaY + TITLE_Y_OFFSET);

      // Render shapes using current state from AI agent
      if (aiAgentService != null) {
        try {
          drawingEngine.renderState(aiAgentService.getCurrentState());
        } catch (DrawingEngine.DrawingException ignored) {
          // ignore rendering errors to avoid UI crash
        }
      }
    } finally {
      sketch.popStyle();
    }
  }

  public void drawChatArea() {
    sketch.pushStyle();
    try {
      // Chat area card
      drawCard(MARGIN, chatAreaY, sketch.width - MARGIN * 2, chatAreaHeight, CARD_RADIUS);

      // Draw chat log and input
      drawChatLog();
      drawChatInput();

      // Chat area title - moved inside the chat area bounds
      sketch.fill(colorTextSecondary);
      if (font != null) {
        sketch.textFont(font);
      }
      sketch.textAlign(PApplet.LEFT, PApplet.TOP);
      sketch.text("Chat Interface", MARGIN + 18, chatAreaY + TITLE_Y_OFFSET);
    } finally {
      sketch.popStyle();
    }
  }

  public void drawChatLog() {
    sketch.pushStyle();
    try {
      // Chat log surface
      sketch.fill(colorSurfaceMuted);
      sketch.noStroke();
      sketch.rect(MARGIN + INNER_GAP_SMALL, chatLogY, sketch.width - MARGIN * 2 - 10, chatLogHeight,
          CARD_RADIUS);

      // Constrain drawing to chat log box
      sketch.clip(MARGIN + INNER_GAP_SMALL, chatLogY, sketch.width - MARGIN * 2 - 10,
          chatLogHeight);

      // Draw messages as bubbles
      if (smallFont != null) {
        sketch.textFont(smallFont);
      }

      // Get messages and apply scroll
      List<String> messages = chatManager.getChatMessages();
      int scrollOffset = chatManager.getChatScrollOffset();
      int y = chatLogY + 10 - scrollOffset;

      float bubbleMaxWidth = (sketch.width - MARGIN * 2 - 10) * 0.74f; // 74%
      int messageSpacing = MESSAGE_SPACING;
      int bubblePadding = BUBBLE_PADDING;
      int bubbleRadius = BUBBLE_RADIUS;

      for (String message : messages) {
        boolean isUser = message.startsWith("User:");
        boolean isAssistant = message.startsWith("Assistant:");
        boolean isSystem = message.startsWith("System:");

        // Remove prefix for display
        String displayText = message;
        if (isUser) {
          displayText = message.substring(6).trim();
        } else if (isAssistant) {
          displayText = message.substring(11).trim();
        } else if (isSystem) {
          displayText = message.substring(8).trim();
        }

        // Wrap text for bubble
        List<String> lines = wrapTextForBubble(displayText, bubbleMaxWidth - bubblePadding * 2);
        int bubbleHeight = lines.size() * LINE_HEIGHT + bubblePadding * 2;

        // Calculate bubble width based on content
        float bubbleWidth = 0;
        for (String line : lines) {
          bubbleWidth = Math.max(bubbleWidth, sketch.textWidth(line));
        }
        bubbleWidth += bubblePadding * 2;
        bubbleWidth = Math.min(bubbleWidth, bubbleMaxWidth);

        // Calculate bubble position
        float bubbleX;
        if (isUser) {
          // User messages on the right
          bubbleX = sketch.width - MARGIN - INNER_GAP_SMALL - bubbleWidth - 10;
        } else {
          // AI and system messages on the left
          bubbleX = MARGIN + INNER_GAP_SMALL + 10;
        }

        // Only draw if bubble is visible
        if (y + bubbleHeight > chatLogY && y < chatLogY + chatLogHeight) {
          // Bubble background with subtle shadow
          sketch.pushStyle();
          drawShadow(bubbleX, y, bubbleWidth, bubbleHeight, bubbleRadius, 6, SHADOW_ALPHA);
          if (isUser) {
            sketch.noStroke();
            sketch.fill(colorUserBubble);
          } else if (isSystem) {
            sketch.noStroke();
            sketch.fill(colorSystemBubble);
          } else {
            sketch.stroke(colorBorder);
            sketch.strokeWeight(1);
            sketch.fill(colorAssistantBubble);
          }
          sketch.rect(bubbleX, y, bubbleWidth, bubbleHeight, bubbleRadius);

          // Text
          if (isUser) {
            sketch.fill(255);
          } else {
            sketch.fill(darkMode ? 230 : 0);
          }
          sketch.textAlign(PApplet.LEFT, PApplet.TOP);
          int textY = y + bubblePadding;
          for (String line : lines) {
            sketch.text(line, bubbleX + bubblePadding, textY);
            textY += LINE_HEIGHT;
          }
          sketch.popStyle();
        }

        y += bubbleHeight + messageSpacing;
      }

      // Calculate total height for scroll
      int totalHeight = y + scrollOffset - chatLogY;

      // Draw scroll indicator if needed
      if (totalHeight > chatLogHeight) {
        drawScrollIndicator(totalHeight, scrollOffset);
      }

      sketch.noClip();
    } finally {
      sketch.popStyle();
    }
  }

  private List<String> wrapTextForBubble(String text, float maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return lines;
    }

    String[] words = text.split(" ");
    String currentLine = "";

    for (String word : words) {
      String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
      if (sketch.textWidth(testLine) > maxWidth) {
        if (!currentLine.isEmpty()) {
          lines.add(currentLine);
          currentLine = word;
        } else {
          // Single word too long - break it
          lines.add(word);
          currentLine = "";
        }
      } else {
        currentLine = testLine;
      }
    }

    if (!currentLine.isEmpty()) {
      lines.add(currentLine);
    }

    return lines;
  }

  private void drawScrollIndicator(int totalHeight, int scrollOffset) {
    sketch.pushStyle();
    try {
      // Draw scroll bar on the right side
      int scrollBarX = sketch.width - MARGIN - 15;
      int scrollBarWidth = 5;

      // Calculate scroll bar position and size
      float scrollRatio = (float) chatLogHeight / totalHeight;
      int scrollBarHeight = (int) (chatLogHeight * scrollRatio);
      scrollBarHeight = Math.max(20, scrollBarHeight); // Minimum height

      float scrollProgress = (float) scrollOffset / (totalHeight - chatLogHeight);
      scrollProgress = PApplet.constrain(scrollProgress, 0, 1);
      int scrollBarY = chatLogY + (int) ((chatLogHeight - scrollBarHeight) * scrollProgress);

      // Draw scroll track
      sketch.fill(230);
      sketch.noStroke();
      sketch.rect(scrollBarX, chatLogY, scrollBarWidth, chatLogHeight);

      // Draw scroll bar
      sketch.fill(180);
      sketch.rect(scrollBarX, scrollBarY, scrollBarWidth, scrollBarHeight);
    } finally {
      sketch.popStyle();
    }
  }

  public void drawChatInput() {
    String currentInput = chatManager.getCurrentInput();
    boolean inputFocused = chatManager.isInputFocused();
    sketch.pushStyle();
    try {
      // Input field background as surface
      sketch.fill(inputFocused ? colorSurface : colorSurfaceMuted);
      sketch.stroke(inputFocused ? colorAccent : colorBorder);
      sketch.strokeWeight(inputFocused ? 2 : 1);
      int inputLeft = MARGIN + INNER_GAP_SMALL;
      sketch.rect(inputLeft, chatInputY, sketch.width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP,
          INPUT_HEIGHT, CARD_RADIUS);

      // Send button
      int sendButtonLeft = sketch.width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH;
      boolean hover = isInSendButtonArea(sketch.mouseX, sketch.mouseY);
      sendHoverAnim = PApplet.lerp(sendHoverAnim, hover ? 1f : 0f, 0.2f);
      int btnFill = sketch.lerpColor(colorAccent, sketch.color(255), sendHoverAnim * 0.25f);
      if (sendPressed) {
        btnFill = sketch.lerpColor(btnFill, sketch.color(0), 0.15f);
      }
      drawShadow(sendButtonLeft, chatInputY, SEND_BUTTON_WIDTH, INPUT_HEIGHT, CARD_RADIUS, 8,
          SHADOW_ALPHA);
      sketch.fill(btnFill);
      sketch.noStroke();
      sketch.rect(sendButtonLeft, chatInputY, SEND_BUTTON_WIDTH, INPUT_HEIGHT, CARD_RADIUS);

      // Input text - handle multiline display
      sketch.fill(colorTextPrimary);
      if (smallFont != null) {
        sketch.textFont(smallFont);
      }
      // Center vertically to avoid visual misalignment
      sketch.textAlign(PApplet.LEFT, PApplet.CENTER);

      // Split input by newlines and display what fits
      String[] lines = currentInput.split("\n", -1);
      String displayText;

      if (lines.length >= MIN_LINES_FOR_MULTILINE) {
        // For multiline, show last line and indication of more lines
        displayText = ELLIPSIS + lines[lines.length - 1];
      } else {
        displayText = currentInput;
      }

      // Truncate if single line is too long
      if (sketch.textWidth(displayText) > sketch.width - MARGIN * 2 - INPUT_TEXT_RIGHT_PADDING) {
        while (sketch.textWidth(displayText + ELLIPSIS) > sketch.width - MARGIN * 2
            - INPUT_TEXT_RIGHT_PADDING && !displayText.isEmpty()) {
          displayText = displayText.substring(0, displayText.length() - 1);
        }
        displayText += ELLIPSIS;
      }

      sketch.text(displayText, MARGIN + 14, chatInputY + INPUT_HEIGHT / 2f);

      // Cursor
      if (inputFocused && sketch.frameCount % CURSOR_BLINK_PERIOD < CURSOR_BLINK_ON_TICKS) {
        sketch.stroke(0);
        float cursorX = MARGIN + 10 + sketch.textWidth(displayText);
        sketch.line(cursorX, chatInputY + 5, cursorX, chatInputY + INPUT_HEIGHT - 5);
      }

      // Send button text (correctly centered)
      sketch.fill(255);
      sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
      float sendCenterX = sendButtonLeft + SEND_BUTTON_WIDTH / 2f;
      sketch.text("Send", sendCenterX, chatInputY + INPUT_HEIGHT / 2f);

      // Character count - positioned on the right side
      sketch.fill(currentInput.length() > MAX_MESSAGE_LENGTH * WARN_THRESHOLD_RATIO
          ? sketch.color(255, 120, 120)
          : colorTextSecondary);
      if (smallFont != null) {
        sketch.textFont(smallFont);
      }
      sketch.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
      sketch.text(currentInput.length() + "/" + MAX_MESSAGE_LENGTH,
          sketch.width - MARGIN - SEND_BUTTON_WIDTH - 10, chatInputY - 2);

      // Japanese input indicator from InputManager
      drawInputIndicator();
    } finally {
      sketch.popStyle();
    }
  }


  public void drawUIBorders() {
    // セパレーターは控えめに（必要なら）
    sketch.pushStyle();
    try {
      sketch.stroke(colorBorder);
      sketch.strokeWeight(1);
      sketch.line(MARGIN, chatAreaY - MARGIN / 2, sketch.width - MARGIN, chatAreaY - MARGIN / 2);
    } finally {
      sketch.popStyle();
    }
  }



  private void drawInputIndicator() {
    boolean useJapaneseInput = chatManager.isUsingJapaneseInput();
    boolean inputFocused = chatManager.isInputFocused();
    sketch.pushStyle();
    try {
      if (smallFont != null) {
        sketch.textFont(smallFont);
      }
      if (useJapaneseInput && chatManager.isJapaneseInputVisible()) {
        sketch.fill(100, 150, 100);
        sketch.textAlign(PApplet.LEFT, PApplet.BOTTOM);
        sketch.text("[日本語入力中]", MARGIN + 15, chatInputY - 2);
      } else if (inputFocused) {
        sketch.fill(150);
        sketch.textAlign(PApplet.LEFT, PApplet.BOTTOM);
        sketch.text("[クリックで日本語入力]", MARGIN + 15, chatInputY - 2);
      }
    } finally {
      sketch.popStyle();
    }
  }

  // Getters for layout information needed by other components
  public boolean isInChatInputArea(int x, int y) {
    return x >= MARGIN + INNER_GAP_SMALL && x <= sketch.width - MARGIN - CHAT_INPUT_RIGHT_GAP
        && y >= chatInputY && y <= chatInputY + INPUT_HEIGHT;
  }

  public boolean isInSendButtonArea(int x, int y) {
    return x >= sketch.width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH
        && x <= sketch.width - MARGIN - INNER_GAP_SMALL && y >= chatInputY
        && y <= chatInputY + INPUT_HEIGHT;
  }

  public int getChatLogY() {
    return chatLogY;
  }

  public int getChatLogHeight() {
    return chatLogHeight;
  }

  public int getChatInputY() {
    return chatInputY;
  }

  public int getChatInputLeft() {
    return MARGIN + INNER_GAP_SMALL;
  }

  public int getChatInputWidth() {
    return sketch.width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP;
  }

  public int getInputHeight() {
    return INPUT_HEIGHT;
  }

  // ===== Helpers =====
  private void drawCard(float x, float y, float w, float h, float r) {
    drawShadow(x, y, w, h, r, 12, SHADOW_ALPHA);
    sketch.fill(colorSurface);
    sketch.stroke(colorBorder);
    sketch.strokeWeight(1);
    sketch.rect(x, y, w, h, r);
  }

  private void drawShadow(float x, float y, float w, float h, float r, int layers, float alpha) {
    sketch.pushStyle();
    try {
      for (int i = layers; i >= 1; i--) {
        float spread = i * 1.5f;
        int a = (int) (alpha * (i / (float) layers));
        sketch.noStroke();
        sketch.fill(0, 0, 0, a);
        sketch.rect(x + 0.5f * i, y + 0.8f * i, w, h, r + spread);
      }
    } finally {
      sketch.popStyle();
    }
  }

  public void setSendPressed(boolean pressed) {
    this.sendPressed = pressed;
  }
}
