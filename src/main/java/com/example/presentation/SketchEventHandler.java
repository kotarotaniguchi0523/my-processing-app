package com.example.presentation;

import com.example.usecase.UseCaseFacade;
import processing.core.PApplet;
import processing.core.PConstants;

import java.awt.event.KeyEvent;

/**
 * SketchEventHandler handles all input events for the SimpleSketch application.
 * This class follows the Single Responsibility Principle by focusing solely on event handling.
 */
public class SketchEventHandler {
    private static final int CTRL_Z = 26;
    private static final int CTRL_Y = 25;
    private static final int ASCII_MIN_PRINTABLE = 32;
    private static final int ASCII_MAX_PRINTABLE = 126;
    private static final int MIN_EXTENDED_CODEPOINT = 128;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int CHAT_SCROLL_SPEED = 20;
    private static final int LINE_HEIGHT = 20;
    private static final int MARGIN = 10;
    private static final int INNER_GAP_SMALL = 5;

    private final PApplet sketch;
    private final SketchUIRenderer uiRenderer;
    private final ChatManager chatManager;
    private final ApprovalUIManager approvalManager;
    private final InputManager inputManager;
    private final UseCaseFacade useCaseFacade;

    // Drawing Canvas State
    private boolean shiftDown;

    public SketchEventHandler(PApplet sketch, SketchUIRenderer uiRenderer, ChatManager chatManager,
                             ApprovalUIManager approvalManager, InputManager inputManager,
                             UseCaseFacade useCaseFacade) {
        this.sketch = sketch;
        this.uiRenderer = uiRenderer;
        this.chatManager = chatManager;
        this.approvalManager = approvalManager;
        this.inputManager = inputManager;
        this.useCaseFacade = useCaseFacade;
    }

    public void handleMousePressed() {
        if (approvalManager.isApprovalPending()) {
            handleApprovalMousePressed();
            return; // block other interactions
        }

        if (uiRenderer.isInChatInputArea(sketch.mouseX, sketch.mouseY)) {
            chatManager.setInputFocused(true);
            inputManager.showJapaneseInput();
        } else if (uiRenderer.isInSendButtonArea(sketch.mouseX, sketch.mouseY)) {
            chatManager.sendMessage();
        } else {
            chatManager.setInputFocused(false);
            inputManager.hideJapaneseInput();
        }
    }

    public void handleMouseDragged() {
        // No freehand drawing; if approval shown, ignore interactions
    }

    public void handleMouseReleased() {
        // No freehand drawing
    }

    public void handleKeyPressed() {
        if (sketch.keyCode == PConstants.SHIFT) {
            shiftDown = true;
            return;
        }

        if (approvalManager.isApprovalPending()) {
            handleApprovalKeyPressed();
            return;
        }

        handleTextInput();
        handleGlobalShortcuts();
        handleUndoRedoShortcuts();
    }

    public void handleKeyReleased() {
        if (sketch.keyCode == PConstants.SHIFT) {
            shiftDown = false;
        }
    }

    public void handleMouseWheel(processing.event.MouseEvent event) {
        // Check if mouse is over chat log area
        if (sketch.mouseX >= MARGIN + INNER_GAP_SMALL && sketch.mouseX <= sketch.width - MARGIN - INNER_GAP_SMALL
                && sketch.mouseY >= uiRenderer.getChatLogY() && sketch.mouseY <= uiRenderer.getChatLogY() + uiRenderer.getChatLogHeight()) {

            float scrollAmount = event.getCount() * CHAT_SCROLL_SPEED; // Scroll speed
            chatManager.adjustScrollOffset(scrollAmount);
        }
    }

    private void handleApprovalMousePressed() {
        if (approvalManager.isInApproveButton(sketch.mouseX, sketch.mouseY)) {
            approvalManager.approve();
        } else if (approvalManager.isInRejectButton(sketch.mouseX, sketch.mouseY)) {
            approvalManager.reject();
        }
    }

    private void handleApprovalKeyPressed() {
        // simple scroll with arrow keys
        if (sketch.keyCode == KeyEvent.VK_UP) {
            approvalManager.scrollUp();
        } else if (sketch.keyCode == KeyEvent.VK_DOWN) {
            approvalManager.scrollDown();
        } else if (sketch.key == 'y' || sketch.key == 'Y') {
            approvalManager.approve();
        } else if (sketch.key == 'n' || sketch.key == 'N') {
            approvalManager.reject();
        }
    }

    private void handleTextInput() {
        if (approvalManager.isApprovalPending()) {
            return; // disable chat while awaiting approval
        }
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
            // Send message with just Enter (no Shift)
            if (!chatManager.getCurrentInput().isBlank()) {
                chatManager.sendMessage();
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
        return (key >= ASCII_MIN_PRINTABLE && key <= ASCII_MAX_PRINTABLE) || key >= MIN_EXTENDED_CODEPOINT;
    }

    private void handleGlobalShortcuts() {
        if (sketch.key == 'c' || sketch.key == 'C') {
            handleCanvasClear();
        }
    }

    private void handleCanvasClear() {
        // Clear canvas using use case
        var result = useCaseFacade.clearCanvas();
        if (result.success) {
            sketch.println("Canvas cleared");
        } else {
            sketch.println("Clear failed: " + result.message);
        }
    }

    private void handleUndoRedoShortcuts() {
        // Handle undo/redo shortcuts using ASCII control codes
        // Note: Processing CODED keys (arrows, function keys) are not needed currently
        if (sketch.key == CTRL_Z) { // Ctrl+Z (ASCII 26)
            handleUndo();
        } else if (sketch.key == CTRL_Y) { // Ctrl+Y (ASCII 25)
            handleRedo();
        }
    }

    private void handleUndo() {
        var result = useCaseFacade.undo();
        if (result.success) {
            sketch.println("Undo performed");
        } else {
            sketch.println("Undo failed: " + result.message);
        }
    }

    private void handleRedo() {
        var result = useCaseFacade.redo();
        if (result.success) {
            sketch.println("Redo performed");
        } else {
            sketch.println("Redo failed: " + result.message);
        }
    }

    // Getters for state
    public boolean isShiftDown() {
        return shiftDown;
    }
}