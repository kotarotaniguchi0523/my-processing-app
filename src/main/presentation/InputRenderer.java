package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * InputRenderer handles input field and send button rendering. Manages input focus states,
 * animations, and Japanese input indicators.
 */
public class InputRenderer {

  private static final int MARGIN = 12;
  private static final int INNER_GAP_SMALL = 6;
  private static final int INPUT_HEIGHT = 36;
  private static final int INPUT_TEXT_RIGHT_PADDING = 96;
  private static final int CHAT_INPUT_RIGHT_GAP = 78;
  private static final int SEND_BUTTON_WIDTH = 64;
  private static final int CURSOR_BLINK_PERIOD = 60;
  private static final int CURSOR_BLINK_ON_TICKS = 30;
  // Consistent left paddings so text and cursor align properly
  private static final int INPUT_TEXT_LEFT_PADDING = 8; // distance from input rect left edge
  private static final int CURSOR_AFTER_TEXT_GAP = 1; // small gap to avoid visual overlap with last
                                                      // glyph
  private static final String ELLIPSIS = "...";
  private static final int MIN_LINES_FOR_MULTILINE = 2;
  private static final float WARN_THRESHOLD_RATIO = 0.8f;
  private static final int MAX_MESSAGE_LENGTH = 500;
  private static final float SHADOW_ALPHA = 24f;
  private static final int CARD_RADIUS = 14;

  private final PApplet sketch;
  private final ThemeManager themeManager;
  private final ChatManager chatManager;

  private PFont smallFont;
  private float sendHoverAnim = 0f; // 0..1
  private boolean sendPressed = false;

  public InputRenderer(PApplet sketch, ThemeManager themeManager, ChatManager chatManager) {
    this.sketch = sketch;
    this.themeManager = themeManager;
    this.chatManager = chatManager;
  }

  public void setFont(PFont smallFont) {
    this.smallFont = smallFont;
  }

  /**
   * Draws the chat input field and send button
   */
  public void drawChatInput(int chatInputY) {
    String currentInput = chatManager.getCurrentInput();
    boolean inputFocused = chatManager.isInputFocused();

    sketch.pushStyle();
    try {
      // Input field background as surface
      sketch.fill(
          inputFocused ? themeManager.getColorSurface() : themeManager.getColorSurfaceMuted());
      sketch.stroke(inputFocused ? themeManager.getColorAccent() : themeManager.getColorBorder());
      sketch.strokeWeight(inputFocused ? 2 : 1);
      int inputLeft = MARGIN + INNER_GAP_SMALL;
      sketch.rect(inputLeft, chatInputY, sketch.width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP,
          INPUT_HEIGHT, CARD_RADIUS);

      // Send button
      drawSendButton(chatInputY);

      // Input text - handle multiline display
      drawInputText(currentInput, inputFocused, chatInputY);

      // Cursor
      if (inputFocused && sketch.frameCount % CURSOR_BLINK_PERIOD < CURSOR_BLINK_ON_TICKS) {
        drawCursor(currentInput, chatInputY);
      }

      // Character count - positioned on the right side
      drawCharacterCount(currentInput, chatInputY);

      // Japanese input indicator from InputManager
      drawInputIndicator(chatInputY);

    } finally {
      sketch.popStyle();
    }
  }

  private void drawSendButton(int chatInputY) {
    int sendButtonLeft = sketch.width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH;
    boolean hover = isInSendButtonArea(sketch.mouseX, sketch.mouseY, chatInputY);

    sendHoverAnim = PApplet.lerp(sendHoverAnim, hover ? 1f : 0f, 0.2f);
    int btnFill =
        sketch.lerpColor(themeManager.getColorAccent(), sketch.color(255), sendHoverAnim * 0.25f);
    if (sendPressed) {
      btnFill = sketch.lerpColor(btnFill, sketch.color(0), 0.15f);
    }

    UIRenderingUtils.drawShadow(sketch, sendButtonLeft, chatInputY, SEND_BUTTON_WIDTH, INPUT_HEIGHT, CARD_RADIUS, 8,
        SHADOW_ALPHA);
    sketch.fill(btnFill);
    sketch.noStroke();
    sketch.rect(sendButtonLeft, chatInputY, SEND_BUTTON_WIDTH, INPUT_HEIGHT, CARD_RADIUS);

    // Send button text (correctly centered)
    sketch.fill(255);
    sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
    float sendCenterX = sendButtonLeft + SEND_BUTTON_WIDTH / 2f;
    sketch.text("Send", sendCenterX, chatInputY + INPUT_HEIGHT / 2f);
  }

  private void drawInputText(String currentInput, boolean inputFocused, int chatInputY) {
    sketch.fill(themeManager.getColorTextPrimary());
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

    int inputLeft = MARGIN + INNER_GAP_SMALL;
    sketch.text(displayText, inputLeft + INPUT_TEXT_LEFT_PADDING, chatInputY + INPUT_HEIGHT / 2f);
  }

  private void drawCursor(String currentInput, int chatInputY) {
    sketch.stroke(0);
    String displayText = getDisplayText(currentInput);
    int inputLeft = MARGIN + INNER_GAP_SMALL;
    float cursorX =
        inputLeft + INPUT_TEXT_LEFT_PADDING + sketch.textWidth(displayText) + CURSOR_AFTER_TEXT_GAP;
    sketch.line(cursorX, chatInputY + 5, cursorX, chatInputY + INPUT_HEIGHT - 5);
  }

  private void drawCharacterCount(String currentInput, int chatInputY) {
    sketch.fill(currentInput.length() > MAX_MESSAGE_LENGTH * WARN_THRESHOLD_RATIO
        ? sketch.color(255, 120, 120)
        : themeManager.getColorTextSecondary());
    if (smallFont != null) {
      sketch.textFont(smallFont);
    }
    sketch.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
    sketch.text(currentInput.length() + "/" + MAX_MESSAGE_LENGTH,
        sketch.width - MARGIN - SEND_BUTTON_WIDTH - 10, chatInputY - 2);
  }

  private void drawInputIndicator(int chatInputY) {
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
        int inputLeft = MARGIN + INNER_GAP_SMALL;
        sketch.text("[日本語入力中]", inputLeft + INPUT_TEXT_LEFT_PADDING, chatInputY - 2);
      } else if (inputFocused) {
        sketch.fill(150);
        sketch.textAlign(PApplet.LEFT, PApplet.BOTTOM);
        int inputLeft = MARGIN + INNER_GAP_SMALL;
        sketch.text("[クリックで日本語入力]", inputLeft + INPUT_TEXT_LEFT_PADDING, chatInputY - 2);
      }
    } finally {
      sketch.popStyle();
    }
  }

  private String getDisplayText(String currentInput) {
    String[] lines = currentInput.split("\n", -1);
    String displayText;

    if (lines.length >= MIN_LINES_FOR_MULTILINE) {
      displayText = ELLIPSIS + lines[lines.length - 1];
    } else {
      displayText = currentInput;
    }

    if (sketch.textWidth(displayText) > sketch.width - MARGIN * 2 - INPUT_TEXT_RIGHT_PADDING) {
      while (sketch.textWidth(displayText + ELLIPSIS) > sketch.width - MARGIN * 2
          - INPUT_TEXT_RIGHT_PADDING && !displayText.isEmpty()) {
        displayText = displayText.substring(0, displayText.length() - 1);
      }
      displayText += ELLIPSIS;
    }

    return displayText;
  }


  // Getters for layout information needed by other components
  public boolean isInChatInputArea(int x, int y, int chatInputY) {
    return x >= MARGIN + INNER_GAP_SMALL && x <= sketch.width - MARGIN - CHAT_INPUT_RIGHT_GAP
        && y >= chatInputY && y <= chatInputY + INPUT_HEIGHT;
  }

  public boolean isInSendButtonArea(int x, int y, int chatInputY) {
    return x >= sketch.width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH
        && x <= sketch.width - MARGIN - INNER_GAP_SMALL && y >= chatInputY
        && y <= chatInputY + INPUT_HEIGHT;
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

  public void setSendPressed(boolean pressed) {
    this.sendPressed = pressed;
  }
}
