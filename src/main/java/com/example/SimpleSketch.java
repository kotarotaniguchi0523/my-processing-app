package com.example;

import com.example.application.ApplicationController;
import com.example.domain.models.Shape;
import com.example.events.EventType;
import com.example.events.UIEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import processing.core.PApplet;
import processing.core.PFont;

public class SimpleSketch extends PApplet {
  // UI Layout constants
  private static final int CANVAS_HEIGHT_RATIO = 60; // 60% for canvas
  private static final int CHAT_HEIGHT_RATIO = 35; // 35% for chat
  private static final int STATUS_HEIGHT_RATIO = 5; // 5% for status
  private static final int MARGIN = 10;
  private static final int INPUT_HEIGHT = 30;
  private static final int MAX_CHAT_MESSAGES = 100;
  private static final int MAX_MESSAGE_LENGTH = 500;

  // UI Areas
  private int canvasAreaY, canvasAreaHeight;
  private int chatAreaY, chatAreaHeight;
  private int statusAreaY, statusAreaHeight;
  private int chatInputY, chatLogY, chatLogHeight;

  // UI Components
  private PFont font, smallFont;
  private List<String> chatMessages;
  private String currentInput = "";
  private int chatScrollOffset = 0;
  private String agentStatus = "Ready";
  private boolean inputFocused = false;

  // Drawing Canvas State
  private boolean isDrawing = false;
  private List<DrawPoint> currentStroke;
  private List<List<DrawPoint>> allStrokes;

  // Application Integration
  private ApplicationController appController;

  // Simple drawing point class
  class DrawPoint {
    float x, y;

    DrawPoint(float x, float y) {
      this.x = x;
      this.y = y;
    }
  }

  public static void main(String[] args) {
    PApplet.main("com.example.SimpleSketch");
  }

  public void settings() {
    size(1000, 700);
  }

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

    // Calculate layout areas
    calculateLayout();

    println(
        "UI Layout initialized - Canvas: "
            + canvasAreaHeight
            + "px, Chat: "
            + chatAreaHeight
            + "px");
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
  }

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
      if (stroke.size() > 1) {
        for (int i = 1; i < stroke.size(); i++) {
          DrawPoint p1 = stroke.get(i - 1);
          DrawPoint p2 = stroke.get(i);
          line(p1.x, p1.y, p2.x, p2.y);
        }
      }
    }

    // Draw current stroke
    if (currentStroke.size() > 1) {
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

    // Draw chat log
    drawChatLog();

    // Draw input field
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
    rect(MARGIN + 5, chatLogY, width - MARGIN * 2 - 10, chatLogHeight);

    // Draw messages
    if (smallFont != null) textFont(smallFont);
    fill(0);
    textAlign(LEFT, TOP);

    int y = chatLogY + 5 - chatScrollOffset;
    int lineHeight = 16;

    for (String message : chatMessages) {
      if (y > chatLogY - lineHeight && y < chatLogY + chatLogHeight) {
        // Word wrap for long messages
        String[] words = message.split(" ");
        String line = "";
        int startY = y;

        for (String word : words) {
          String testLine = line + (line.isEmpty() ? "" : " ") + word;
          if (textWidth(testLine) > width - MARGIN * 2 - 30) {
            if (!line.isEmpty()) {
              text(line, MARGIN + 10, y);
              y += lineHeight;
              line = word;
            } else {
              // Single word too long, truncate
              text(word.substring(0, Math.min(word.length(), 50)) + "...", MARGIN + 10, y);
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
          if (textWidth(testLine) > width - MARGIN * 2 - 30) {
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
        if (!line.isEmpty()) y += lineHeight;
      }
    }
  }

  private void drawChatInput() {
    // Input field background
    fill(inputFocused ? 255 : 245);
    stroke(inputFocused ? 100 : 180);
    strokeWeight(inputFocused ? 2 : 1);
    rect(MARGIN + 5, chatInputY, width - MARGIN * 2 - 70, INPUT_HEIGHT);

    // Send button
    fill(inputFocused ? 220 : 200);
    stroke(150);
    strokeWeight(1);
    rect(width - MARGIN - 60, chatInputY, 55, INPUT_HEIGHT);

    // Input text - handle multiline display
    fill(0);
    if (smallFont != null) textFont(smallFont);
    textAlign(LEFT, TOP);
    
    // Split input by newlines and display what fits
    String[] lines = currentInput.split("\n", -1);
    String displayText = "";
    
    if (lines.length > 1) {
      // For multiline, show last line and indication of more lines
      displayText = "..." + lines[lines.length - 1];
    } else {
      displayText = currentInput;
    }
    
    // Truncate if single line is too long
    if (textWidth(displayText) > width - MARGIN * 2 - 80) {
      while (textWidth(displayText + "...") > width - MARGIN * 2 - 80 && displayText.length() > 0) {
        displayText = displayText.substring(0, displayText.length() - 1);
      }
      displayText += "...";
    }
    
    text(displayText, MARGIN + 10, chatInputY + 8);

    // Cursor
    if (inputFocused && frameCount % 60 < 30) {
      stroke(0);
      line(
          MARGIN + 10 + textWidth(displayText),
          chatInputY + 5,
          MARGIN + 10 + textWidth(displayText),
          chatInputY + INPUT_HEIGHT - 5);
    }

    // Send button text
    fill(100);
    textAlign(CENTER, CENTER);
    text("Send", width - MARGIN - 32, chatInputY + INPUT_HEIGHT / 2);

    // Character count
    fill(currentInput.length() > MAX_MESSAGE_LENGTH * 0.8 ? 200 : 150);
    if (smallFont != null) textFont(smallFont);
    textAlign(RIGHT, BOTTOM);
    text(currentInput.length() + "/" + MAX_MESSAGE_LENGTH, width - MARGIN - 75, chatInputY - 2);
  }

  private void drawStatusArea() {
    // Status area background
    fill(240);
    stroke(200);
    strokeWeight(1);
    rect(MARGIN, statusAreaY, width - MARGIN * 2, statusAreaHeight);

    // Agent status
    fill(100);
    if (smallFont != null) textFont(smallFont);
    textAlign(LEFT, CENTER);
    text("Agent Status: " + agentStatus, MARGIN + 10, statusAreaY + statusAreaHeight / 2);

    // Connection indicator
    fill(agentStatus.equals("Ready") ? color(0, 150, 0) : color(200, 150, 0));
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
  public void mousePressed() {
    if (isInCanvasArea(mouseX, mouseY)) {
      isDrawing = true;
      currentStroke = new ArrayList<>();
      currentStroke.add(new DrawPoint(mouseX, mouseY));
    } else if (isInChatInputArea(mouseX, mouseY)) {
      inputFocused = true;
    } else if (isInSendButtonArea(mouseX, mouseY)) {
      sendMessage();
    } else {
      inputFocused = false;
    }
  }

  public void mouseDragged() {
    if (isDrawing && isInCanvasArea(mouseX, mouseY)) {
      currentStroke.add(new DrawPoint(mouseX, mouseY));
    }
  }

  public void mouseReleased() {
    if (isDrawing && currentStroke.size() > 1) {
      allStrokes.add(new ArrayList<>(currentStroke));
      if (allStrokes.size() > 50) { // Limit number of strokes
        allStrokes.remove(0);
      }
    }
    isDrawing = false;
    currentStroke.clear();
  }

  public void keyPressed() {
    handleTextInput();
    handleGlobalShortcuts();
    handleUndoRedoShortcuts();
  }

  private void handleTextInput() {
    if (!inputFocused) {
      return;
    }

    if (key == ENTER || key == RETURN) {
      // Check if Shift is pressed for newline insertion
      if (keyEvent != null && keyEvent.isShiftDown()) {
        // Insert newline character when Shift+Enter is pressed
        if (currentInput.length() < MAX_MESSAGE_LENGTH) {
          currentInput += '\n';
        }
      } else {
        // Send message with just Enter (no Shift)
        if (!currentInput.trim().isEmpty()) {
          sendMessage();
        }
      }
    } else if (key == BACKSPACE) {
      if (currentInput.length() > 0) {
        currentInput = currentInput.substring(0, currentInput.length() - 1);
      }
    } else if (key >= 32 && key <= 126 || key >= 128) { // Printable characters including Unicode
      if (currentInput.length() < MAX_MESSAGE_LENGTH) {
        currentInput += key;
      }
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
    } else if (key == 26) { // Ctrl+Z (ASCII 26)
      handleUndo();
    } else if (key == 25) { // Ctrl+Y (ASCII 25)
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
  public void mouseWheel(processing.event.MouseEvent event) {
    // Check if mouse is over chat log area
    if (mouseX >= chatLogX && mouseX <= chatLogX + chatLogWidth &&
        mouseY >= chatLogY && mouseY <= chatLogY + chatLogHeight) {
      
      float scrollAmount = event.getCount() * 20; // Scroll speed
      chatScrollOffset += scrollAmount;
      
      // Calculate max scroll based on total message height
      int totalMessagesHeight = 0;
      for (ChatMessage msg : chatMessages) {
        String[] lines = msg.text.split("\n");
        totalMessagesHeight += lines.length * 20 + 10; // Line height + spacing
      }
      
      // Boundary checks for scrolling
      int maxScroll = max(0, totalMessagesHeight - chatLogHeight + 10);
      chatScrollOffset = constrain(chatScrollOffset, 0, maxScroll);
    }
  }

  private void sendMessage() {
    if (!currentInput.trim().isEmpty()) {
      String userInput = currentInput.trim();
      println("Sending message to ApplicationController: " + userInput);

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
    return x >= MARGIN
        && x <= width - MARGIN
        && y >= canvasAreaY
        && y <= canvasAreaY + canvasAreaHeight;
  }

  private boolean isInChatInputArea(int x, int y) {
    return x >= MARGIN + 5
        && x <= width - MARGIN - 70
        && y >= chatInputY
        && y <= chatInputY + INPUT_HEIGHT;
  }

  private boolean isInSendButtonArea(int x, int y) {
    return x >= width - MARGIN - 60
        && x <= width - MARGIN - 5
        && y >= chatInputY
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

        println("Domain shapes count: " + shapes.size());

        // Draw all shapes from the domain state
        for (Shape shape : shapes) {
          if (shape != null && shape.isVisible()) {
            drawDomainShape(shape);
          }
        }

        // Also redraw existing strokes from manual drawing
        stroke(0);
        strokeWeight(2);
        noFill();
        for (List<DrawPoint> stroke : allStrokes) {
          if (stroke.size() > 1) {
            for (int i = 1; i < stroke.size(); i++) {
              DrawPoint p1 = stroke.get(i - 1);
              DrawPoint p2 = stroke.get(i);
              line(p1.x, p1.y, p2.x, p2.y);
            }
          }
        }
      }
    } catch (Exception e) {
      println("Error updating canvas from state: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Draw a domain shape based on its type
  private void drawDomainShape(Shape shape) {
    try {
      // Ensure drawing is within canvas bounds
      if (shape.getPosition().getX() < MARGIN
          || shape.getPosition().getX() > width - MARGIN
          || shape.getPosition().getY() < canvasAreaY
          || shape.getPosition().getY() > canvasAreaY + canvasAreaHeight) {
        // Skip shapes outside canvas area
        return;
      }

      // Apply shape style before drawing
      if (shape.getStyle().hasFill()) {
        int fillColor = shape.getStyle().getFillColor().toProcessingColor();
        fill(fillColor);
      } else {
        noFill();
      }

      if (shape.getStyle().hasStroke()) {
        int strokeColor = shape.getStyle().getStrokeColor().toProcessingColor();
        stroke(strokeColor);
        strokeWeight(shape.getStyle().getStrokeWeight());
      } else {
        noStroke();
      }

      // Draw based on shape type
      switch (shape.getType()) {
        case CIRCLE:
          if (shape instanceof com.example.domain.models.Circle) {
            com.example.domain.models.Circle circle = (com.example.domain.models.Circle) shape;
            ellipse(
                circle.getCenter().getX(),
                circle.getCenter().getY(),
                circle.getDiameter(),
                circle.getDiameter());
          }
          break;

        case RECTANGLE:
          if (shape instanceof com.example.domain.models.Rectangle) {
            com.example.domain.models.Rectangle rect =
                (com.example.domain.models.Rectangle) shape;
            rect(
                rect.getPosition().getX(),
                rect.getPosition().getY(),
                rect.getWidth(),
                rect.getHeight());
          }
          break;

        case LINE:
          if (shape instanceof com.example.domain.models.Line) {
            com.example.domain.models.Line line = (com.example.domain.models.Line) shape;
            line(
                line.getStartPosition().getX(),
                line.getStartPosition().getY(),
                line.getEndPosition().getX(),
                line.getEndPosition().getY());
          }
          break;

        case TEXT:
          if (shape instanceof com.example.domain.models.Text) {
            com.example.domain.models.Text textShape = (com.example.domain.models.Text) shape;

            // Set font and size for text
            if (font != null) {
              textFont(font);
            }
            textSize(textShape.getFontSize());

            // Convert text alignment to Processing constants
            int hAlign = LEFT;
            switch (textShape.getHorizontalAlign()) {
              case CENTER:
                hAlign = CENTER;
                break;
              case RIGHT:
                hAlign = RIGHT;
                break;
              default:
                hAlign = LEFT;
                break;
            }

            int vAlign = TOP;
            switch (textShape.getVerticalAlign()) {
              case CENTER:
                vAlign = CENTER;
                break;
              case BOTTOM:
                vAlign = BOTTOM;
                break;
              default:
                vAlign = TOP;
                break;
            }

            textAlign(hAlign, vAlign);
            text(
                textShape.getContent(),
                textShape.getPosition().getX(),
                textShape.getPosition().getY());
          }
          break;

        default:
          // Handle any other shape types by trying to use the shape's own draw method
          // This uses the shape's built-in performDraw method if available
          try {
            shape.draw(g); // g is Processing's PGraphics object
          } catch (Exception e) {
            println(
                "Could not draw shape of type: "
                    + shape.getType()
                    + ", error: "
                    + e.getMessage());
          }
          break;
      }
    } catch (Exception e) {
      println("Error drawing shape: " + shape.getId() + ", error: " + e.getMessage());
    }
  }
}
