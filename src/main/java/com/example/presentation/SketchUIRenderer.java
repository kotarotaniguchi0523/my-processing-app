package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import com.example.application.services.AIAgentService;


/**
 * SketchUIRenderer handles all UI drawing responsibilities for the SimpleSketch application. This
 * class follows the Single Responsibility Principle by focusing solely on rendering UI components.
 */
public class SketchUIRenderer {

  // UI Layout constants
  private static final int MARGIN = 12;
  
  private final PApplet sketch;
  private final ChatManager chatManager;
  @SuppressWarnings("unused") // Used indirectly in CanvasRenderer constructor
  private final AIAgentService aiAgentService;
  
  // UI Areas
  private int canvasAreaY;
  private int canvasAreaHeight;
  private int chatAreaY;
  private int chatAreaHeight;
  private int chatInputY;
  private int chatLogY;
  private int chatLogHeight;

  // UI Components - removed unused font fields as they are only passed through to other renderers
  
  // Specialized renderers
  private final ThemeManager themeManager;
  private final BackgroundRenderer backgroundRenderer;
  private final CanvasRenderer canvasRenderer;
  private final ChatRenderer chatRenderer;
  private final InputRenderer inputRenderer;
  private final DiffOverlayRenderer diffOverlayRenderer;

  public SketchUIRenderer(PApplet sketch, ChatManager chatManager, AIAgentService aiAgentService) {
    this.sketch = sketch;
    this.chatManager = chatManager;
    this.aiAgentService = aiAgentService;
    
    // Initialize specialized renderers
    this.themeManager = new ThemeManager(sketch);
    this.backgroundRenderer = new BackgroundRenderer(sketch, themeManager);
    this.diffOverlayRenderer = new DiffOverlayRenderer(sketch, themeManager);
    this.canvasRenderer = new CanvasRenderer(sketch, themeManager, aiAgentService, diffOverlayRenderer);
    this.chatRenderer = new ChatRenderer(sketch, themeManager, chatManager);
    this.inputRenderer = new InputRenderer(sketch, themeManager, chatManager);
  }

  public void setFonts(PFont font, PFont smallFont) {
    // Delegate fonts directly to all renderers without storing locally
    backgroundRenderer.setFont(font);
    canvasRenderer.setFont(font);
    chatRenderer.setFonts(font, smallFont);
    inputRenderer.setFont(smallFont);
    diffOverlayRenderer.setFonts(font, smallFont);
  }

  public void calculateLayout() {
    canvasAreaY = MARGIN + 8; // ヘッダー余白風
    canvasAreaHeight = (int) ((sketch.height - MARGIN * 2) * 0.54); // 54% for canvas

    chatAreaY = canvasAreaY + canvasAreaHeight + MARGIN;
    chatAreaHeight = sketch.height - chatAreaY - MARGIN; // Rest for chat

    chatInputY = chatAreaY + chatAreaHeight - inputRenderer.getInputHeight() - MARGIN;
    chatLogY = chatAreaY + MARGIN + 24; // タイトル分の余白
    chatLogHeight = chatAreaHeight - inputRenderer.getInputHeight() - MARGIN * 2 - 24; // タイトル分を差し引き

    // Update chat manager with the calculated height
    chatManager.setChatLogHeight(chatLogHeight);

    // Update chat layout metrics for accurate wrapping/scrolling
    float chatLogWidth = (sketch.width - MARGIN * 2 - 10);
    float bubbleMaxWidth = chatLogWidth * 0.74f; // match drawChatLog()
    float bubbleContentMaxWidth = bubbleMaxWidth - 14 * 2f; // BUBBLE_PADDING
    chatManager.updateLayoutMetrics(22, 14, 12,  // LINE_HEIGHT, BUBBLE_PADDING, MESSAGE_SPACING
        bubbleContentMaxWidth);
  }

  // ===== Theme =====
  public void toggleTheme() {
    themeManager.toggleTheme();
  }

  public void drawBackground() {
    backgroundRenderer.drawBackground();
  }

  public void drawCanvasArea() {
    canvasRenderer.drawCanvasArea(canvasAreaY, canvasAreaHeight);
  }

  public void drawChatArea() {
    chatRenderer.drawChatArea(chatAreaY, chatAreaHeight, chatLogY, chatLogHeight);
    inputRenderer.drawChatInput(chatInputY);
  }

  public void drawChatLog() {
    chatRenderer.drawChatLog(chatLogY, chatLogHeight);
  }

  public void drawChatInput() {
    inputRenderer.drawChatInput(chatInputY);
  }


  public void drawUIBorders() {
    // セパレーターは控えめに（必要なら）
    sketch.pushStyle();
    try {
      sketch.stroke(themeManager.getColorBorder());
      sketch.strokeWeight(1);
      sketch.line(MARGIN, chatAreaY - MARGIN / 2, sketch.width - MARGIN, chatAreaY - MARGIN / 2);
    } finally {
      sketch.popStyle();
    }
  }

  // Getters for layout information needed by other components
  public boolean isInChatInputArea(int x, int y) {
    return inputRenderer.isInChatInputArea(x, y, chatInputY);
  }

  public boolean isInSendButtonArea(int x, int y) {
    return inputRenderer.isInSendButtonArea(x, y, chatInputY);
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
    return inputRenderer.getChatInputLeft();
  }

  public int getChatInputWidth() {
    return inputRenderer.getChatInputWidth();
  }

  public int getInputHeight() {
    return inputRenderer.getInputHeight();
  }


  public void setSendPressed(boolean pressed) {
    inputRenderer.setSendPressed(pressed);
  }
}
