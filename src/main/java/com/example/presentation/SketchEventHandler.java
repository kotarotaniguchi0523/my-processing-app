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
        chatManager.setCurrentInput("");
        AiChatHelper.sendAndAppendAsync(aiAgentService, chatManager, text,
            () -> chatManager.flushQueueToUI());
        // クリック押下のフィードバック
        uiRenderer.setSendPressed(true);
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

    // Undo/Redo shortcuts: Ctrl+Z / Ctrl+Y
    if (sketch.keyPressed && (sketch.keyCode == PConstants.CONTROL || sketch.keyCode == 157)) { // 157 is right Ctrl on some systems
      if (sketch.key == 'z' || sketch.key == 'Z') {
        handleUndo();
        return;
      } else if (sketch.key == 'y' || sketch.key == 'Y') {
        handleRedo();
        return;
      }
    }

    // テーマ切替: 'T'
    if (sketch.key == 't' || sketch.key == 'T') {
      uiRenderer.toggleTheme();
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
    // Allow scrolling when the cursor is anywhere inside the chat card area,
    // not just the chat log box. This fixes the issue where scrolling over
    // the title or input field didn't move the chat.
    int chatTop = uiRenderer.getChatAreaY();
    int chatBottom = uiRenderer.getChatAreaY() + uiRenderer.getChatAreaHeight();

    if (sketch.mouseY >= chatTop && sketch.mouseY <= chatBottom) {
      float scrollAmount = event.getCount() * CHAT_SCROLL_SPEED; // down: positive
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
        chatManager.setCurrentInput("");
        // Command: /load_history or /history
        if (text.equalsIgnoreCase("/load_history") || text.equalsIgnoreCase("/history")) {
          if (aiAgentService != null) {
            boolean loaded = aiAgentService.loadHistoryOnDemand();
            if (loaded) {
              chatManager.addMessage("System: 履歴を読み込みました (.agent_history.json)");
            } else {
              chatManager.addMessage("System: 読み込む履歴がありません");
            }
          } else {
            chatManager.addMessage("System: AIサービスが無効のため履歴を読み込めません");
          }
        } else {
          AiChatHelper.sendAndAppendAsync(aiAgentService, chatManager, text,
            () -> chatManager.flushQueueToUI());
        }
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

  /**
   * Handle undo operation (Ctrl+Z).
   */
  private void handleUndo() {
    if (aiAgentService != null) {
      try {
        boolean undoPerformed = aiAgentService.getHistoryService().undo();
        if (undoPerformed) {
          chatManager.addMessage("System: Undo performed");
        } else {
          chatManager.addMessage("System: No more undo actions available");
        }
      } catch (Exception e) {
        chatManager.addMessage("System: Error performing undo - " + e.getMessage());
      }
    } else {
      chatManager.addMessage("System: Undo not available - AI service not initialized");
    }
  }

  /**
   * Handle redo operation (Ctrl+Y).
   */
  private void handleRedo() {
    if (aiAgentService != null) {
      try {
        boolean redoPerformed = aiAgentService.getHistoryService().redo();
        if (redoPerformed) {
          chatManager.addMessage("System: Redo performed");
        } else {
          chatManager.addMessage("System: No more redo actions available");
        }
      } catch (Exception e) {
        chatManager.addMessage("System: Error performing redo - " + e.getMessage());
      }
    } else {
      chatManager.addMessage("System: Redo not available - AI service not initialized");
    }
  }

  // AI trigger logic is centralized in AiChatHelper

}
