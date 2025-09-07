package com.example;

import com.example.application.ApplicationController;
import com.example.domain.models.Shape;
import com.example.engine.DrawingEngine;
import com.example.events.EventType;
import com.example.events.UIEvent;

import processing.core.PApplet;
import processing.core.PFont;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SimpleSketch extends PApplet {
  private static final Logger LOGGER = Logger.getLogger(SimpleSketch.class.getName());

  // UI Layout constants
  private static final int CANVAS_HEIGHT_RATIO = 60; // 60% for canvas
  private static final int CHAT_HEIGHT_RATIO = 35; // 35% for chat
  private static final int MARGIN = 10;
  private static final int INPUT_HEIGHT = 30;
  private static final int MAX_CHAT_MESSAGES = 100;
  private static final int MAX_MESSAGE_LENGTH = 500;
  private static final int MIN_POINTS_IN_STROKE = 2;
  private static final int STROKE_HISTORY_LIMIT = 50;
  private static final String ELLIPSIS = "...";
  private static final int CTRL_Z = 26;
  private static final int CTRL_Y = 25;
  // Extracted UI constants to avoid magic numbers in conditions and layout
  private static final int CURSOR_BLINK_PERIOD = 60;
  private static final int CURSOR_BLINK_ON_TICKS = 30;
  private static final float WARN_THRESHOLD_RATIO = 0.8f;
  private static final int CHAT_SCROLL_SPEED = 20;
  private static final int WORD_TRUNCATE_LIMIT = 50;
  private static final int CHAT_LOG_INNER_PADDING = 30;
  private static final int LINE_HEIGHT = 16;
  private static final int INPUT_TEXT_RIGHT_PADDING = 80;
  private static final int CHAT_INPUT_RIGHT_GAP = 70;
  private static final int SEND_BUTTON_WIDTH = 55;
  private static final int INNER_GAP_SMALL = 5;
  private static final int MIN_LINES_FOR_MULTILINE = 2; // lines.length >= 2 means multiline
  private static final int ASCII_MIN_PRINTABLE = 32;
  private static final int ASCII_MAX_PRINTABLE = 126;
  private static final int MIN_EXTENDED_CODEPOINT = 128;

  // UI Areas
  private int canvasAreaY;
  private int canvasAreaHeight;
  private int chatAreaY;
  private int chatAreaHeight;
  private int statusAreaY;
  private int statusAreaHeight;
  private int chatInputY;
  private int chatLogY;
  private int chatLogHeight;

  // UI Components
  private PFont font;
  private PFont smallFont;
  private List<String> chatMessages;
  private String currentInput = "";
  private int chatScrollOffset;
  private String agentStatus = "Ready";
  private boolean inputFocused;

  // Drawing Canvas State
  private boolean isDrawing;
  private List<DrawPoint> currentStroke;
  private List<List<DrawPoint>> allStrokes;
  private boolean shiftDown;

    // Application Integration
    private ApplicationController appController;
    private DrawingEngine drawingEngine;

  // Japanese IME Support
  private JTextField japaneseInputField;
  private boolean useJapaneseInput;

  // Simple drawing point class
  class DrawPoint {
    float x;
    float y;

    DrawPoint(float x, float y) {
      this.x = x;
      this.y = y;
    }
  }

  public static void main(String[] args) {
    PApplet.main("com.example.SimpleSketch");
  }

  @Override
  public void settings() {
    size(1000, 700);
  }

  @Override
  public void setup() {
    background(240);

    // Initialize fonts
    try {
      font = createFont("MS Gothic", 16, true);
      smallFont = createFont("MS Gothic", 12, true);
    } catch (Exception e) {
      // フォント作成に失敗した場合のフォールバック
      textSize(16);
    }

    // Initialize chat system
    chatMessages = new ArrayList<>();
    chatMessages.add("System: AI Agent ready for instructions");

    // Initialize drawing system
    allStrokes = new ArrayList<>();
    currentStroke = new ArrayList<>();

        // Initialize application controller
        appController = new ApplicationController();
        appController.setUICallback(new UICallbackImpl());
        appController.start();

        // Initialize drawing engine for domain rendering
        drawingEngine = new DrawingEngine(this);

    // Calculate layout areas
    calculateLayout();

    // Initialize Japanese input field
    setupJapaneseInput();

    println("UI Layout initialized - Canvas: " + canvasAreaHeight + "px, Chat: " + chatAreaHeight
        + "px");
  }

  // Setup Japanese IME input field
  @SuppressWarnings("PMD.UnusedAssignment")
  private void setupJapaneseInput() {
    SwingUtilities.invokeLater(() -> {
      try {
        // Create Japanese input field
        japaneseInputField = new JTextField();
        japaneseInputField.setFont(new Font("MS Gothic", Font.PLAIN, 14));

        // Add action listener for Enter key
        japaneseInputField.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String text = japaneseInputField.getText();
            if (!text.isBlank()) {
              // Transfer text to currentInput and send
              currentInput = text;
              sendMessage();
              japaneseInputField.setText("");
              currentInput = "";
            }
          }
        });

        // Add focus listener
        japaneseInputField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            inputFocused = true;
            useJapaneseInput = true;
          }

          @Override
          public void focusLost(FocusEvent e) {
            // Keep focus status for a moment
            SwingUtilities.invokeLater(() -> {
              if (!japaneseInputField.hasFocus()) {
                useJapaneseInput = false;
                inputFocused = false;
              }
            });
          }
        });

        // Add key listener to sync text with currentInput
        japaneseInputField.addKeyListener(new java.awt.event.KeyAdapter() {
          @Override
          public void keyReleased(java.awt.event.KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
              currentInput = japaneseInputField.getText();
            }
          }
        });

        // Get the Processing window's frame
        Object nativeWindow = this.getSurface().getNative();
        javax.swing.JFrame frame = null;

        // Try to get the parent frame
        if (nativeWindow instanceof java.awt.Component) {
          java.awt.Component comp = (java.awt.Component) nativeWindow;
          java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(comp);
          if (window instanceof javax.swing.JFrame) {
            frame = (javax.swing.JFrame) window;
          }
        }

        if (frame != null) {
          // Use absolute positioning
          frame.setLayout(null);

          // Place the input field
          japaneseInputField.setBounds(MARGIN + INNER_GAP_SMALL, chatInputY,
              width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP, INPUT_HEIGHT);
          frame.add(japaneseInputField);

          // Initially hide the field - will show when user clicks on input area
          japaneseInputField.setVisible(false);
        } else {
          if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Could not find JFrame for Japanese input field");
          }
          useJapaneseInput = false;
        }
      } catch (Exception e) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
          LOGGER.log(Level.SEVERE, "Failed to setup Japanese input field: " + e.getMessage(), e);
        }
        useJapaneseInput = false;
      }
    });
  }

  private void calculateLayout() {
    canvasAreaY = MARGIN;
    canvasAreaHeight = (int) ((height - MARGIN * 3) * CANVAS_HEIGHT_RATIO / 100.0);

    chatAreaY = canvasAreaY + canvasAreaHeight + MARGIN;
    chatAreaHeight = (int) ((height - MARGIN * 3) * CHAT_HEIGHT_RATIO / 100.0);

    statusAreaY = chatAreaY + chatAreaHeight + MARGIN;
    statusAreaHeight = height - statusAreaY - MARGIN;

    chatInputY = chatAreaY + chatAreaHeight - INPUT_HEIGHT - MARGIN;
    chatLogY = chatAreaY + MARGIN;
    chatLogHeight = chatAreaHeight - INPUT_HEIGHT - MARGIN * 2;

    // Update Japanese input field position if it exists
    updateJapaneseInputPosition();
  }

  private void updateJapaneseInputPosition() {
    if (japaneseInputField != null) {
      SwingUtilities.invokeLater(() -> japaneseInputField.setBounds(MARGIN + INNER_GAP_SMALL,
          chatInputY, width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP, INPUT_HEIGHT));
    }
  }

  @Override
  public void draw() {
    background(245);

    // Draw UI areas
    drawCanvasArea();
    drawChatArea();
    drawStatusArea();

    // Draw UI borders for clarity
    drawUIBorders();
  }

  private void drawCanvasArea() {
    // Canvas background
    fill(255);
    stroke(200);
    strokeWeight(1);
    rect(MARGIN, canvasAreaY, width - MARGIN * 2, canvasAreaHeight);

    // Draw all completed strokes
    stroke(0);
    strokeWeight(2);
    for (List<DrawPoint> stroke : allStrokes) {
      if (stroke.size() >= MIN_POINTS_IN_STROKE) {
        for (int i = 1; i < stroke.size(); i++) {
          DrawPoint p1 = stroke.get(i - 1);
          DrawPoint p2 = stroke.get(i);
          line(p1.x, p1.y, p2.x, p2.y);
        }
      }
    }

    // Draw current stroke
    if (currentStroke.size() >= MIN_POINTS_IN_STROKE) {
      for (int i = 1; i < currentStroke.size(); i++) {
        DrawPoint p1 = currentStroke.get(i - 1);
        DrawPoint p2 = currentStroke.get(i);
        line(p1.x, p1.y, p2.x, p2.y);
      }
    }

    // Canvas title
    fill(100);
    if (font != null) {
      textFont(font);
    }
    textAlign(LEFT, TOP);
    text("Drawing Canvas", MARGIN + 10, canvasAreaY + 10);
  }

  private void drawChatArea() {
    // Chat area background
    fill(250);
    stroke(200);
    strokeWeight(1);
    rect(MARGIN, chatAreaY, width - MARGIN * 2, chatAreaHeight);

    // Draw chat log and input
    drawChatLog();
    drawChatInput();

    // Chat area title
    fill(100);
    if (font != null) {
      textFont(font);
    }
    textAlign(LEFT, TOP);
    text("Chat Interface", MARGIN + 10, chatAreaY + 5);
  }

  private void drawChatLog() {
    // Chat log background
    fill(255);
    stroke(180);
    rect(MARGIN + INNER_GAP_SMALL, chatLogY, width - MARGIN * 2 - 10, chatLogHeight);

    // Draw messages
    if (smallFont != null) {
      textFont(smallFont);
    }
    fill(0);
    textAlign(LEFT, TOP);

    int y = chatLogY + 5 - chatScrollOffset;
    int lineHeight = LINE_HEIGHT;

    for (String message : chatMessages) {
      if (y > chatLogY - lineHeight && y < chatLogY + chatLogHeight) {
        // Word wrap for long messages
        String[] words = message.split(" ");
        String line = "";

        for (String word : words) {
          String testLine = line + (line.isEmpty() ? "" : " ") + word;
          if (textWidth(testLine) > width - MARGIN * 2 - CHAT_LOG_INNER_PADDING) {
            if (!line.isEmpty()) {
              text(line, MARGIN + 10, y);
              y += lineHeight;
              line = word;
            } else {
              // Single word too long, truncate
              text(word.substring(0, Math.min(word.length(), WORD_TRUNCATE_LIMIT)) + ELLIPSIS,
                  MARGIN + 10, y);
              y += lineHeight;
              line = "";
            }
          } else {
            line = testLine;
          }
        }

        if (!line.isEmpty()) {
          text(line, MARGIN + 10, y);
          y += lineHeight;
        }
      } else {
        // Skip rendering but still count line height
        String[] words = message.split(" ");
        String line = "";
        for (String word : words) {
          String testLine = line + (line.isEmpty() ? "" : " ") + word;
          if (textWidth(testLine) > width - MARGIN * 2 - CHAT_LOG_INNER_PADDING) {
            if (!line.isEmpty()) {
              y += lineHeight;
              line = word;
            } else {
              y += lineHeight;
              line = "";
            }
          } else {
            line = testLine;
          }
        }
        if (!line.isEmpty()) {
          y += lineHeight;
        }
      }
    }
  }

  private void drawChatInput() {
    // Input field background
    fill(inputFocused ? 255 : 245);
    stroke(inputFocused ? 100 : 180);
    strokeWeight(inputFocused ? 2 : 1);
    rect(MARGIN + INNER_GAP_SMALL, chatInputY, width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP,
        INPUT_HEIGHT);

    // Send button
    fill(inputFocused ? 220 : 200);
    stroke(150);
    strokeWeight(1);
    rect(width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH, chatInputY, SEND_BUTTON_WIDTH,
        INPUT_HEIGHT);

    // Input text - handle multiline display
    fill(0);
    if (smallFont != null) {
      textFont(smallFont);
    }
    textAlign(LEFT, TOP);

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
    if (textWidth(displayText) > width - MARGIN * 2 - INPUT_TEXT_RIGHT_PADDING) {
      while (textWidth(displayText + ELLIPSIS) > width - MARGIN * 2 - INPUT_TEXT_RIGHT_PADDING
          && !displayText.isEmpty()) {
        displayText = displayText.substring(0, displayText.length() - 1);
      }
      displayText += ELLIPSIS;
    }

    text(displayText, MARGIN + 10, chatInputY + 8);

    // Cursor
    if (inputFocused && frameCount % CURSOR_BLINK_PERIOD < CURSOR_BLINK_ON_TICKS) {
      stroke(0);
      line(MARGIN + 10 + textWidth(displayText), chatInputY + 5,
          MARGIN + 10 + textWidth(displayText), chatInputY + INPUT_HEIGHT - 5);
    }

    // Send button text
    fill(100);
    textAlign(CENTER, CENTER);
    text("Send", width - MARGIN - 32, chatInputY + INPUT_HEIGHT / 2);

    // Character count
    fill(currentInput.length() > MAX_MESSAGE_LENGTH * WARN_THRESHOLD_RATIO ? 200 : 150);
    if (smallFont != null) {
      textFont(smallFont);
    }
    textAlign(RIGHT, BOTTOM);
    text(currentInput.length() + "/" + MAX_MESSAGE_LENGTH, width - MARGIN - 75, chatInputY - 2);

    // Japanese input indicator
    if (useJapaneseInput && japaneseInputField != null && japaneseInputField.isVisible()) {
      fill(100, 150, 100);
      textAlign(LEFT, BOTTOM);
      text("[日本語入力中]", MARGIN + 10, chatInputY - 2);
    } else if (inputFocused) {
      fill(150);
      textAlign(LEFT, BOTTOM);
      text("[クリックで日本語入力]", MARGIN + 10, chatInputY - 2);
    }
  }

  private void drawStatusArea() {
    // Status area background
    fill(240);
    stroke(200);
    strokeWeight(1);
    rect(MARGIN, statusAreaY, width - MARGIN * 2, statusAreaHeight);

    // Agent status
    fill(100);
    if (smallFont != null) {
      textFont(smallFont);
    }
    textAlign(LEFT, CENTER);
    text("Agent Status: " + agentStatus, MARGIN + 10, statusAreaY + statusAreaHeight / 2);

    // Connection indicator
    fill("Ready".equals(agentStatus) ? color(0, 150, 0) : color(200, 150, 0));
    ellipse(width - MARGIN - 20, statusAreaY + statusAreaHeight / 2, 8, 8);
  }

  private void drawUIBorders() {
    // Draw separator lines
    stroke(180);
    strokeWeight(1);

    // Between canvas and chat
    line(MARGIN, chatAreaY - MARGIN / 2, width - MARGIN, chatAreaY - MARGIN / 2);

    // Between chat and status
    line(MARGIN, statusAreaY - MARGIN / 2, width - MARGIN, statusAreaY - MARGIN / 2);
  }

  // Input handling
  @Override
  public void mousePressed() {
    if (isInCanvasArea(mouseX, mouseY)) {
      isDrawing = true;
      currentStroke = new ArrayList<>();
      currentStroke.add(new DrawPoint(mouseX, mouseY));
    } else if (isInChatInputArea(mouseX, mouseY)) {
      inputFocused = true;
      // Show Japanese input field when clicking on chat input area
      if (japaneseInputField != null) {
        SwingUtilities.invokeLater(() -> {
          japaneseInputField.setVisible(true);
          japaneseInputField.requestFocus();
          japaneseInputField.setText(currentInput);
        });
      }
    } else if (isInSendButtonArea(mouseX, mouseY)) {
      sendMessage();
    } else {
      inputFocused = false;
      // Hide Japanese input field when clicking elsewhere
      if (japaneseInputField != null && useJapaneseInput) {
        SwingUtilities.invokeLater(() -> japaneseInputField.setVisible(false));
      }
    }
  }

  @Override
  public void mouseDragged() {
    if (isDrawing && isInCanvasArea(mouseX, mouseY)) {
      currentStroke.add(new DrawPoint(mouseX, mouseY));
    }
  }

  @Override
  public void mouseReleased() {
    if (isDrawing && currentStroke.size() >= MIN_POINTS_IN_STROKE) {
      allStrokes.add(new ArrayList<>(currentStroke));
      if (allStrokes.size() > STROKE_HISTORY_LIMIT) {
        // Limit number of strokes
        allStrokes.remove(0);
      }
    }
    isDrawing = false;
    currentStroke.clear();
  }

  @Override
  public void keyPressed() {
    if (keyCode == SHIFT) {
      shiftDown = true;
      return;
    }
    handleTextInput();
    handleGlobalShortcuts();
    handleUndoRedoShortcuts();
  }

  private void handleTextInput() {
    if (!inputFocused) {
      return;
    }

    // Skip Processing's keyboard handling when using Japanese input field
    if (useJapaneseInput && japaneseInputField != null && japaneseInputField.isVisible()) {
      return;
    }

    if (key == ENTER || key == RETURN) {
      // Check if Shift is pressed for newline insertion
      if (shiftDown) {
        // Insert newline character when Shift+Enter is pressed
        if (currentInput.length() < MAX_MESSAGE_LENGTH) {
          currentInput += '\n';
        }
      } else {
        // Send message with just Enter (no Shift)
        if (!currentInput.isBlank()) {
          sendMessage();
        }
      }
    } else if (key == BACKSPACE) {
      if (!currentInput.isEmpty()) {
        currentInput = currentInput.substring(0, currentInput.length() - 1);
      }
    } else if ((key >= ASCII_MIN_PRINTABLE && key <= ASCII_MAX_PRINTABLE)
        || key >= MIN_EXTENDED_CODEPOINT) {
      // Printable characters including Unicode
      if (currentInput.length() < MAX_MESSAGE_LENGTH) {
        currentInput += key;
      }
    }
  }

  @Override
  public void keyReleased() {
    if (keyCode == SHIFT) {
      shiftDown = false;
    }
  }

  private void handleGlobalShortcuts() {
    if (key == 'c' || key == 'C') {
      handleCanvasClear();
    }
  }

  private void handleCanvasClear() {
    // Clear canvas - send event to ApplicationController
    UIEvent clearEvent = new UIEvent(EventType.UI_CANVAS_CLEARED, "canvas");
    if (appController != null) {
      appController.getEventDispatcher().dispatchUIEvent(clearEvent);
    }
    // Also clear strokes for immediate feedback
    allStrokes.clear();
    println("Canvas clear requested");
  }

  private void handleUndoRedoShortcuts() {
    if (key == CODED) {
      // For special key combinations, we'll use a simpler approach
      // Users can use keyboard shortcuts through the chat interface
      // TODO: Implement special key handling if needed
      return; // avoid EmptyBlock rule violation (no-op intended)
    } else if (key == CTRL_Z) { // Ctrl+Z (ASCII 26)
      handleUndo();
    } else if (key == CTRL_Y) { // Ctrl+Y (ASCII 25)
      handleRedo();
    }
  }

  private void handleUndo() {
    if (appController != null) {
      boolean undone = appController.getStateService().undo();
      if (undone) {
        println("Undo performed");
        updateCanvasFromState();
      }
    }
  }

  private void handleRedo() {
    if (appController != null) {
      boolean redone = appController.getStateService().redo();
      if (redone) {
        println("Redo performed");
        updateCanvasFromState();
      }
    }
  }

  // Handle mouse wheel scrolling for chat area
  @Override
  public void mouseWheel(processing.event.MouseEvent event) {
    // Check if mouse is over chat log area
    if (mouseX >= MARGIN + INNER_GAP_SMALL && mouseX <= width - MARGIN - INNER_GAP_SMALL
        && mouseY >= chatLogY && mouseY <= chatLogY + chatLogHeight) {

      float scrollAmount = event.getCount() * CHAT_SCROLL_SPEED; // Scroll speed
      chatScrollOffset += scrollAmount;

      // Calculate max scroll based on total message height
      int totalMessagesHeight = 0;
      for (String msg : chatMessages) {
        String[] lines = msg.split("\n");
        totalMessagesHeight += lines.length * LINE_HEIGHT + 10; // Line height + spacing
      }

      // Boundary checks for scrolling
      int maxScroll = max(0, totalMessagesHeight - chatLogHeight + 10);
      chatScrollOffset = constrain(chatScrollOffset, 0, maxScroll);
    }
  }

  private void sendMessage() {
    if (!currentInput.isBlank()) {
      String userInput = currentInput.trim();
      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Sending message to ApplicationController: " + userInput);
      }

      // Send UI event to ApplicationController
      UIEvent chatEvent = new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, userInput, "chat");
      if (appController != null) {
        appController.getEventDispatcher().dispatchUIEvent(chatEvent);
      }

      currentInput = "";
    }
  }

  // Helper methods for click detection
  private boolean isInCanvasArea(int x, int y) {
    return x >= MARGIN && x <= width - MARGIN && y >= canvasAreaY
        && y <= canvasAreaY + canvasAreaHeight;
  }

  private boolean isInChatInputArea(int x, int y) {
    return x >= MARGIN + INNER_GAP_SMALL && x <= width - MARGIN - CHAT_INPUT_RIGHT_GAP
        && y >= chatInputY && y <= chatInputY + INPUT_HEIGHT;
  }

  private boolean isInSendButtonArea(int x, int y) {
    return x >= width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH
        && x <= width - MARGIN - INNER_GAP_SMALL && y >= chatInputY
        && y <= chatInputY + INPUT_HEIGHT;
  }

  // UI Callback implementation for ApplicationController integration
  private class UICallbackImpl implements ApplicationController.UIUpdateCallback {
    @Override
    public void updateStatus(String status) {
      agentStatus = status;
    }

    @Override
    public void addChatMessage(String message) {
      chatMessages.add(message);
      // Maintain message limit
      while (chatMessages.size() > MAX_CHAT_MESSAGES) {
        chatMessages.remove(0);
      }
    }

    @Override
    public void refreshCanvas() {
      // Fetch shapes from the application controller and render them
      updateCanvasFromState();
    }

    @Override
    public void showError(String error) {
      chatMessages.add("ERROR: " + error);
    }
  }

    // Update canvas to reflect current domain state
    private void updateCanvasFromState() {
        try {
            if (appController != null && appController.getCurrentSession() != null) {
                List<Shape> shapes = appController.getCurrentSession().getAllShapes();

                // Clear the canvas area first (keep UI intact)
                fill(255);
                stroke(200);
                strokeWeight(1);
                rect(MARGIN, canvasAreaY, width - MARGIN * 2, canvasAreaHeight);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Domain shapes count: " + shapes.size());
                }

                // Draw domain shapes via DrawingEngine, limited to the canvas viewport
                if (drawingEngine != null) {
                    List<Shape> shapesInView = new ArrayList<>();
                    Shape.BoundingBox viewport =
                            new Shape.BoundingBox(
                                    MARGIN, canvasAreaY, width - MARGIN * 2, canvasAreaHeight);
                    for (Shape shape : shapes) {
                        if (shape != null && shape.isVisible()) {
                            try {
                                if (shape.getBounds().overlaps(viewport)) {
                                    shapesInView.add(shape);
                                }
                            } catch (Exception e) {
                                // If bounds are unavailable, draw it anyway to avoid missing shapes
                                shapesInView.add(shape);
                            }
                        }
                    }
                    drawingEngine.renderShapes(shapesInView);
                }

                // Also redraw existing strokes from manual drawing
                stroke(0);
                strokeWeight(2);
                noFill();
        for (List<DrawPoint> stroke : allStrokes) {
          if (stroke.size() >= MIN_POINTS_IN_STROKE) {
            for (int i = 1; i < stroke.size(); i++) {
              DrawPoint p1 = stroke.get(i - 1);
              DrawPoint p2 = stroke.get(i);
              line(p1.x, p1.y, p2.x, p2.y);
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error updating canvas from state: " + e.getMessage(), e);
      }
    }
  }

  
}
