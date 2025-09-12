package com.example.presentation;

import processing.core.PApplet;
import processing.core.PConstants;

import com.example.application.services.AIAgentService;


/**
 * SketchEventHandler handles all input events for the SimpleSketch application. This class follows
 * the Single Responsibility Principle by focusing solely on event handling.
 */
public class SketchEventHandler {

  private static final int ASCII_MIN_PRINTABLE = 32;
  private static final int ASCII_MAX_PRINTABLE = 126;
  private static final int MIN_EXTENDED_CODEPOINT = 128;
  private static final int MAX_MESSAGE_LENGTH = 500;
  private static final int CHAT_SCROLL_SPEED = 20;
  private static final int MARGIN = 10;
  private static final int INNER_GAP_SMALL = 5;

  private final PApplet sketch;
  private final SketchUIRenderer uiRenderer;
  private final ChatManager chatManager;
  private final InputManager inputManager;
  private final AIAgentService aiAgentService;

  // Drawing Canvas State
  private boolean shiftDown;

  public SketchEventHandler(PApplet sketch, SketchUIRenderer uiRenderer, ChatManager chatManager,
      InputManager inputManager, AIAgentService aiAgentService) {
    this.sketch = sketch;
    this.uiRenderer = uiRenderer;
    this.chatManager = chatManager;
    this.inputManager = inputManager;
    this.aiAgentService = aiAgentService;
  }

  public void handleMousePressed() {
    if (uiRenderer.isInChatInputArea(sketch.mouseX, sketch.mouseY)) {
      chatManager.setInputFocused(true);
      inputManager.showJapaneseInput();
    } else if (uiRenderer.isInSendButtonArea(sketch.mouseX, sketch.mouseY)) {
      if (!chatManager.getCurrentInput().isBlank()) {
        String text = chatManager.getCurrentInput();
        chatManager.addMessage("User: " + text);
        chatManager.setCurrentInput("");
        triggerAi(text);
      }
    } else {
      chatManager.setInputFocused(false);
      inputManager.hideJapaneseInput();
    }
  }


  public void handleKeyPressed() {
    if (sketch.keyCode == PConstants.SHIFT) {
      shiftDown = true;
      return;
    }

    handleTextInput();
  }

  public void handleKeyReleased() {
    if (sketch.keyCode == PConstants.SHIFT) {
      shiftDown = false;
    }
  }

  public void handleMouseWheel(processing.event.MouseEvent event) {
    // Check if mouse is over chat log area
    if (sketch.mouseX >= MARGIN + INNER_GAP_SMALL
        && sketch.mouseX <= sketch.width - MARGIN - INNER_GAP_SMALL
        && sketch.mouseY >= uiRenderer.getChatLogY()
        && sketch.mouseY <= uiRenderer.getChatLogY() + uiRenderer.getChatLogHeight()) {

      float scrollAmount = event.getCount() * CHAT_SCROLL_SPEED; // Scroll speed
      chatManager.adjustScrollOffset(scrollAmount);
    }
  }

  private void handleTextInput() {
    if (!chatManager.isInputFocused()) {
      return;
    }

    // Skip Processing's keyboard handling when using Japanese input field
    if (inputManager.isUsingJapaneseInput() && inputManager.isJapaneseInputVisible()) {
      return;
    }

    if (sketch.key == PConstants.ENTER || sketch.key == PConstants.RETURN) {
      handleEnterKey();
    } else if (sketch.key == PConstants.BACKSPACE) {
      handleBackspace();
    } else if (isPrintableCharacter(sketch.key)) {
      handlePrintableCharacter();
    }
  }

  private void handleEnterKey() {
    // Check if Shift is pressed for newline insertion
    if (shiftDown) {
      // Insert newline character when Shift+Enter is pressed
      String currentInput = chatManager.getCurrentInput();
      if (currentInput.length() < MAX_MESSAGE_LENGTH) {
        chatManager.addToCurrentInput('\n');
      }
    } else {
      if (!chatManager.getCurrentInput().isBlank()) {
        String text = chatManager.getCurrentInput();
        chatManager.addMessage("User: " + text);
        chatManager.setCurrentInput("");
        triggerAi(text);
      }
    }
  }

  private void handleBackspace() {
    String currentInput = chatManager.getCurrentInput();
    if (!currentInput.isEmpty()) {
      chatManager.setCurrentInput(currentInput.substring(0, currentInput.length() - 1));
    }
  }

  private void handlePrintableCharacter() {
    String currentInput = chatManager.getCurrentInput();
    if (currentInput.length() < MAX_MESSAGE_LENGTH) {
      chatManager.addToCurrentInput(sketch.key);
    }
  }

  private boolean isPrintableCharacter(char key) {
    return (key >= ASCII_MIN_PRINTABLE && key <= ASCII_MAX_PRINTABLE)
        || key >= MIN_EXTENDED_CODEPOINT;
  }

  private void triggerAi(String text) {
    new Thread(() -> {
      try {
        var reply = aiAgentService.handleUserPrompt(text);
        // Update chat on UI thread
        javax.swing.SwingUtilities.invokeLater(() -> {
          chatManager.addMessage("Assistant: " + reply.message);
        });
      } catch (Exception e) {
        javax.swing.SwingUtilities
            .invokeLater(() -> chatManager.addMessage("Assistant: (エラーが発生しました)"));
      }
    }, "AI-Call-Input").start();
  }


}
