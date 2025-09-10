package com.example;

import com.example.usecase.UseCaseFacade;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.infrastructure.rendering.DrawingEngine;


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
  private static final int CANVAS_HEIGHT_RATIO = 50; // 50% for canvas (PRD)
  private static final int CHAT_HEIGHT_RATIO = 40; // 40% for chat (PRD)
  private static final int MARGIN = 10;
  private static final int INPUT_HEIGHT = 30;

  private static final int MAX_MESSAGE_LENGTH = 500;

  private static final String ELLIPSIS = "...";
  private static final int CTRL_Z = 26;
  private static final int CTRL_Y = 25;
  // Extracted UI constants to avoid magic numbers in conditions and layout
  private static final int CURSOR_BLINK_PERIOD = 60;
  private static final int CURSOR_BLINK_ON_TICKS = 30;
  private static final float WARN_THRESHOLD_RATIO = 0.8f;
  private static final int CHAT_SCROLL_SPEED = 20;
  private static final int CHAT_LOG_INNER_PADDING = 30;
  // Increase default line height for better readability
  private static final int LINE_HEIGHT = 20;
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
  // M3: Approval overlay state
  private boolean approvalPending;
  private String approvalSummary = "";
  private String approvalDiff = "";
  private int approvalScrollOffset;
  private int approveBtnX, approveBtnY, approveBtnW = 100, approveBtnH = 30;
  private int rejectBtnX, rejectBtnY, rejectBtnW = 100, rejectBtnH = 30;

  // Drawing Canvas State (manual drawing removed per PRD)
  private boolean shiftDown;

  // Application Integration
  private UseCaseFacade useCaseFacade;
  private DrawingEngine drawingEngine;
  private DrawingState pendingApprovalState;

  // Japanese IME Support
  private JTextField japaneseInputField;
  private boolean useJapaneseInput;

  // Simple drawing point class (removed: no freehand drawing)

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
    // Allow window resize per PRD
    try {
      if (getSurface() != null) {
        getSurface().setResizable(true);
      }
    } catch (Exception ignored) {
      // safe to ignore if surface not available
    }

    // Improve text readability: prefer Meiryo/Yu Gothic on Windows, with fallbacks
    try {
      font = chooseReadableFont(
          new String[] {"Meiryo UI", "Meiryo", "Yu Gothic UI", "Yu Gothic", "MS Gothic"}, 18);
      smallFont = chooseReadableFont(
          new String[] {"Meiryo UI", "Meiryo", "Yu Gothic UI", "Yu Gothic", "MS Gothic"}, 16);
      smooth(); // enable anti-aliasing for crisper text
    } catch (Exception e) {
      // フォント作成に失敗した場合のフォールバック
      textSize(18);
    }

    // Initialize chat system
    chatMessages = new ArrayList<>();
    chatMessages.add("System: AI Agent ready for instructions");

    // Initialize application controller (kept for compatibility but not used)
    // appController = new ApplicationController();
    // appController.setUICallback(new UICallbackImpl());
    // appController.start();

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
        // Match in-canvas font size for consistency
        japaneseInputField.setFont(new Font("Meiryo UI", Font.PLAIN, 16));

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

  // Try creating the first available font from candidates
  private PFont chooseReadableFont(String[] candidates, int size) {
    for (String name : candidates) {
      try {
        PFont f = createFont(name, size, true);
        if (f != null) {
          return f;
        }
      } catch (Exception ignored) {
        // try next candidate
      }
    }
    // Fallback to default
    return createFont("SansSerif", size, true);
  }

  // Wrap text to the given width. Handles languages without spaces (e.g., Japanese)
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
        if (textWidth(test) > maxWidth) {
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
      if (textWidth(test) > maxWidth) {
        if (buf.length() > 0) {
          out.add(buf.toString());
          buf.setLength(0);
        }
        // If even a single char is wider than maxWidth, hard add to avoid infinite loop
        if (textWidth(String.valueOf(c)) > maxWidth) {
          out.add(String.valueOf(c));
        } else {
          buf.append(c);
        }
      } else {
        buf.append(c);
      }
    }
    if (buf.length() > 0)
      out.add(buf.toString());
    return out;
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

    // M3: Draw approval overlay last (modal)
    if (approvalPending) {
      drawApprovalOverlay();
    }
  }

  private void drawCanvasArea() {
    // Canvas background
    fill(255);
    stroke(200);
    strokeWeight(1);
    rect(MARGIN, canvasAreaY, width - MARGIN * 2, canvasAreaHeight);

    // Render domain shapes (AI/state-driven), clipped to canvas area
    renderDomainShapesInCanvasArea();

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

    // Constrain drawing to chat log box
    clip(MARGIN + INNER_GAP_SMALL, chatLogY, width - MARGIN * 2 - 10, chatLogHeight);

    // Draw messages bottom-aligned for better readability
    if (smallFont != null) {
      textFont(smallFont);
    }
    fill(0);
    textAlign(LEFT, TOP);

    int lineHeight = LINE_HEIGHT;
    float maxWidth = width - MARGIN * 2 - CHAT_LOG_INNER_PADDING;

    // Prepare wrapped lines for all messages
    List<String> allLines = new ArrayList<>();
    for (String message : chatMessages) {
      allLines.addAll(wrapForWidth(message, maxWidth));
    }

    int totalHeight = allLines.size() * lineHeight;
    // Start so that the last line sits near the bottom with small padding
    int startY = chatLogY + chatLogHeight - totalHeight - 8;
    int y = startY; // may be above the top; clipping will handle overflow

    for (String line : allLines) {
      text(line, MARGIN + 10, y);
      y += lineHeight;
    }

    noClip();
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
    // Center vertically to avoid visual misalignment
    textAlign(LEFT, CENTER);

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

    text(displayText, MARGIN + 10, chatInputY + INPUT_HEIGHT / 2f);

    // Cursor
    if (inputFocused && frameCount % CURSOR_BLINK_PERIOD < CURSOR_BLINK_ON_TICKS) {
      stroke(0);
      line(MARGIN + 10 + textWidth(displayText), chatInputY + 5,
          MARGIN + 10 + textWidth(displayText), chatInputY + INPUT_HEIGHT - 5);
    }

    // Send button text
    fill(100);
    textAlign(CENTER, CENTER);
    text("Send", width - MARGIN - 32, chatInputY + INPUT_HEIGHT / 2f);

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
    if (approvalPending) {
      // Check overlay buttons
      if (isInRect(mouseX, mouseY, approveBtnX, approveBtnY, approveBtnW, approveBtnH)) {
        // Approve
        if (pendingApprovalState != null) {
          useCaseFacade.applyApprovedChanges(pendingApprovalState);
          approvalPending = false;
          pendingApprovalState = null;
        }
      } else if (isInRect(mouseX, mouseY, rejectBtnX, rejectBtnY, rejectBtnW, rejectBtnH)) {
        // Reject
        useCaseFacade.rejectChanges();
        approvalPending = false;
        pendingApprovalState = null;
      }
      return; // block other interactions
    }

    if (isInChatInputArea(mouseX, mouseY)) {
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
    // No freehand drawing; if approval shown, ignore interactions
  }

  @Override
  public void mouseReleased() {
    // No freehand drawing
  }

  @Override
  public void keyPressed() {
    if (keyCode == SHIFT) {
      shiftDown = true;
      return;
    }
    if (approvalPending) {
      // simple scroll with arrow keys
      if (keyCode == java.awt.event.KeyEvent.VK_UP) {
        approvalScrollOffset = Math.max(0, approvalScrollOffset - 20);
      } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
        approvalScrollOffset += 20;
      } else if (key == 'y' || key == 'Y') {
        if (pendingApprovalState != null) {
          useCaseFacade.applyApprovedChanges(pendingApprovalState);
          approvalPending = false;
          pendingApprovalState = null;
        }
      } else if (key == 'n' || key == 'N') {
        useCaseFacade.rejectChanges();
        approvalPending = false;
        pendingApprovalState = null;
      }
      return;
    }
    handleTextInput();
    handleGlobalShortcuts();
    handleUndoRedoShortcuts();
  }

  private void handleTextInput() {
    if (approvalPending) {
      return; // disable chat while awaiting approval
    }
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
    // Clear canvas using use case
    var result = useCaseFacade.clearCanvas();
    if (result.success) {
      println("Canvas cleared");
    } else {
      println("Clear failed: " + result.message);
    }
  }

  private void handleUndoRedoShortcuts() {
    // Handle undo/redo shortcuts using ASCII control codes
    // Note: Processing CODED keys (arrows, function keys) are not needed currently
    if (key == CTRL_Z) { // Ctrl+Z (ASCII 26)
      handleUndo();
    } else if (key == CTRL_Y) { // Ctrl+Y (ASCII 25)
      handleRedo();
    }
  }

  private void handleUndo() {
    var result = useCaseFacade.undo();
    if (result.success) {
      println("Undo performed");
    } else {
      println("Undo failed: " + result.message);
    }
  }

  private void handleRedo() {
    var result = useCaseFacade.redo();
    if (result.success) {
      println("Redo performed");
    } else {
      println("Redo failed: " + result.message);
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

      // Process user input through use case facade
      useCaseFacade.processUserInput(userInput);

      currentInput = "";
    }
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

  private boolean isInRect(int x, int y, int rx, int ry, int rw, int rh) {
    return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
  }

  private void drawApprovalOverlay() {
    // Dim background
    fill(0, 0, 0, 150);
    noStroke();
    rect(0, 0, width, height);

    // Panel
    int panelW = (int) (width * 0.8f);
    int panelH = (int) (height * 0.7f);
    int panelX = (width - panelW) / 2;
    int panelY = (height - panelH) / 2;

    fill(255);
    stroke(50);
    strokeWeight(2);
    rect(panelX, panelY, panelW, panelH);

    int contentX = panelX + 20;
    int contentY = panelY + 20;
    int contentW = panelW - 40;
    int contentH = panelH - 80;

    // Title
    fill(0);
    if (font != null)
      textFont(font);
    textAlign(LEFT, TOP);
    text("変更の承認", contentX, contentY);

    // Summary
    int summaryY = contentY + 30;
    if (smallFont != null)
      textFont(smallFont);
    text(approvalSummary, contentX, summaryY);

    // Diff area (scrollable)
    int diffY = summaryY + 20;
    int diffH = contentH - 60;
    // Mask diff area
    clip(contentX, diffY, contentW, diffH);
    int lineY = diffY - approvalScrollOffset;
    String[] lines = approvalDiff.split("\n");
    for (String line : lines) {
      // Color lines by prefix
      if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@")) {
        fill(60);
      } else if (line.startsWith("+")) {
        fill(0, 120, 0);
      } else if (line.startsWith("-")) {
        fill(180, 0, 0);
      } else {
        fill(0);
      }
      text(line, contentX, lineY);
      lineY += 14;
    }
    noClip();

    // Buttons
    approveBtnX = contentX + contentW - approveBtnW - 110;
    approveBtnY = panelY + panelH - 45;
    rejectBtnX = contentX + contentW - rejectBtnW;
    rejectBtnY = panelY + panelH - 45;

    // Approve
    fill(80, 170, 80);
    stroke(40, 120, 40);
    rect(approveBtnX, approveBtnY, approveBtnW, approveBtnH);
    fill(255);
    textAlign(CENTER, CENTER);
    text("承認 (Y)", approveBtnX + approveBtnW / 2, approveBtnY + approveBtnH / 2);

    // Reject
    fill(200, 80, 80);
    stroke(150, 40, 40);
    rect(rejectBtnX, rejectBtnY, rejectBtnW, rejectBtnH);
    fill(255);
    text("拒否 (N)", rejectBtnX + rejectBtnW / 2, rejectBtnY + rejectBtnH / 2);
  }



  // Update canvas to reflect current domain state
  private void renderDomainShapesInCanvasArea() {
    try {
      if (useCaseFacade == null || drawingEngine == null) {
        return;
      }

      DrawingState currentState = useCaseFacade.getCurrentState();
      if (currentState == null) {
        return;
      }

      List<Shape> shapes = currentState.getShapes();

      // Always log shape count for debugging
      println("Rendering shapes: " + shapes.size() + " total");
      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Domain shapes count: " + shapes.size());
      }

      // Clip to the canvas viewport and render all shapes
      clip(MARGIN, canvasAreaY, width - MARGIN * 2, canvasAreaHeight);
      try {
        drawingEngine.renderShapes(shapes);
      } finally {
        noClip();
      }
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error rendering domain shapes: " + e.getMessage(), e);
      }
    }
  }


  @Override
  public void exit() {
    // シャットダウン時の処理
    if (useCaseFacade != null) {
      useCaseFacade.shutdown();
    }
    super.exit();
  }
}
