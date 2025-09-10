package com.example.presentation;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.infrastructure.rendering.DrawingEngine;
import com.example.usecase.UseCaseFacade;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SketchUIRenderer handles all UI drawing responsibilities for the SimpleSketch application.
 * This class follows the Single Responsibility Principle by focusing solely on rendering UI components.
 */
public class SketchUIRenderer {
    private static final Logger LOGGER = Logger.getLogger(SketchUIRenderer.class.getName());

    // UI Layout constants
    private static final int MARGIN = 10;
    private static final int INNER_GAP_SMALL = 5;
    private static final int INPUT_HEIGHT = 30;
    private static final int LINE_HEIGHT = 20;
    private static final int CHAT_LOG_INNER_PADDING = 30;
    private static final int INPUT_TEXT_RIGHT_PADDING = 80;
    private static final int CHAT_INPUT_RIGHT_GAP = 70;
    private static final int SEND_BUTTON_WIDTH = 55;
    private static final int CURSOR_BLINK_PERIOD = 60;
    private static final int CURSOR_BLINK_ON_TICKS = 30;
    private static final String ELLIPSIS = "...";
    private static final int MIN_LINES_FOR_MULTILINE = 2;
    private static final float WARN_THRESHOLD_RATIO = 0.8f;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final PApplet sketch;
    private final ChatManager chatManager;
    private final DrawingEngine drawingEngine;
    private final UseCaseFacade useCaseFacade;

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

    public SketchUIRenderer(PApplet sketch, ChatManager chatManager, DrawingEngine drawingEngine, UseCaseFacade useCaseFacade) {
        this.sketch = sketch;
        this.chatManager = chatManager;
        this.drawingEngine = drawingEngine;
        this.useCaseFacade = useCaseFacade;
    }

    public void setFonts(PFont font, PFont smallFont) {
        this.font = font;
        this.smallFont = smallFont;
    }

    public void calculateLayout() {
        canvasAreaY = MARGIN;
        canvasAreaHeight = (int) ((sketch.height - MARGIN * 3) * 50 / 100.0); // 50% for canvas

        chatAreaY = canvasAreaY + canvasAreaHeight + MARGIN;
        chatAreaHeight = (int) ((sketch.height - MARGIN * 3) * 40 / 100.0); // 40% for chat

        statusAreaY = chatAreaY + chatAreaHeight + MARGIN;
        statusAreaHeight = sketch.height - statusAreaY - MARGIN;

        chatInputY = chatAreaY + chatAreaHeight - INPUT_HEIGHT - MARGIN;
        chatLogY = chatAreaY + MARGIN;
        chatLogHeight = chatAreaHeight - INPUT_HEIGHT - MARGIN * 2;

        // Update chat manager with the calculated height
        chatManager.setChatLogHeight(chatLogHeight);
    }

    public void drawCanvasArea() {
        // Canvas background
        sketch.fill(255);
        sketch.stroke(200);
        sketch.strokeWeight(1);
        sketch.rect(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight);

        // Render domain shapes (AI/state-driven), clipped to canvas area
        renderDomainShapesInCanvasArea();

        // Canvas title
        sketch.fill(100);
        if (font != null) {
            sketch.textFont(font);
        }
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);
        sketch.text("Drawing Canvas", MARGIN + 10, canvasAreaY + 10);
    }

    public void drawChatArea() {
        // Chat area background
        sketch.fill(250);
        sketch.stroke(200);
        sketch.strokeWeight(1);
        sketch.rect(MARGIN, chatAreaY, sketch.width - MARGIN * 2, chatAreaHeight);

        // Draw chat log and input
        drawChatLog();
        drawChatInput();

        // Chat area title
        sketch.fill(100);
        if (font != null) {
            sketch.textFont(font);
        }
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);
        sketch.text("Chat Interface", MARGIN + 10, chatAreaY + 5);
    }

    public void drawChatLog() {
        // Chat log background
        sketch.fill(255);
        sketch.stroke(180);
        sketch.rect(MARGIN + INNER_GAP_SMALL, chatLogY, sketch.width - MARGIN * 2 - 10, chatLogHeight);

        // Constrain drawing to chat log box
        sketch.clip(MARGIN + INNER_GAP_SMALL, chatLogY, sketch.width - MARGIN * 2 - 10, chatLogHeight);

        // Draw messages bottom-aligned for better readability
        if (smallFont != null) {
            sketch.textFont(smallFont);
        }
        sketch.fill(0);
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);

        int lineHeight = LINE_HEIGHT;
        float maxWidth = sketch.width - MARGIN * 2 - CHAT_LOG_INNER_PADDING;

        // Get wrapped lines from chat manager and draw them
        List<String> allLines = chatManager.getWrappedMessageLines(maxWidth);
        int totalHeight = allLines.size() * lineHeight;
        // Start so that the last line sits near the bottom with small padding
        int startY = chatLogY + chatLogHeight - totalHeight - 8;
        int y = startY; // may be above the top; clipping will handle overflow

        for (String line : allLines) {
            sketch.text(line, MARGIN + 10, y);
            y += lineHeight;
        }

        sketch.noClip();
    }

    public void drawChatInput() {
        String currentInput = chatManager.getCurrentInput();
        boolean inputFocused = chatManager.isInputFocused();

        // Input field background
        sketch.fill(inputFocused ? 255 : 245);
        sketch.stroke(inputFocused ? 100 : 180);
        sketch.strokeWeight(inputFocused ? 2 : 1);
        sketch.rect(MARGIN + INNER_GAP_SMALL, chatInputY, sketch.width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP, INPUT_HEIGHT);

        // Send button
        sketch.fill(inputFocused ? 220 : 200);
        sketch.stroke(150);
        sketch.strokeWeight(1);
        sketch.rect(sketch.width - MARGIN - INNER_GAP_SMALL - SEND_BUTTON_WIDTH, chatInputY, SEND_BUTTON_WIDTH, INPUT_HEIGHT);

        // Input text - handle multiline display
        sketch.fill(0);
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
            while (sketch.textWidth(displayText + ELLIPSIS) > sketch.width - MARGIN * 2 - INPUT_TEXT_RIGHT_PADDING
                    && !displayText.isEmpty()) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += ELLIPSIS;
        }

        sketch.text(displayText, MARGIN + 10, chatInputY + INPUT_HEIGHT / 2f);

        // Cursor
        if (inputFocused && sketch.frameCount % CURSOR_BLINK_PERIOD < CURSOR_BLINK_ON_TICKS) {
            sketch.stroke(0);
            sketch.line(MARGIN + 10 + sketch.textWidth(displayText), chatInputY + 5,
                    MARGIN + 10 + sketch.textWidth(displayText), chatInputY + INPUT_HEIGHT - 5);
        }

        // Send button text
        sketch.fill(100);
        sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
        sketch.text("Send", sketch.width - MARGIN - 32, chatInputY + INPUT_HEIGHT / 2f);

        // Character count
        sketch.fill(currentInput.length() > MAX_MESSAGE_LENGTH * WARN_THRESHOLD_RATIO ? 200 : 150);
        if (smallFont != null) {
            sketch.textFont(smallFont);
        }
        sketch.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
        sketch.text(currentInput.length() + "/" + MAX_MESSAGE_LENGTH, sketch.width - MARGIN - 75, chatInputY - 2);

        // Japanese input indicator from InputManager
        drawInputIndicator();
    }

    public void drawStatusArea() {
        String agentStatus = "Ready"; // This could be passed from a status manager
        
        // Status area background
        sketch.fill(240);
        sketch.stroke(200);
        sketch.strokeWeight(1);
        sketch.rect(MARGIN, statusAreaY, sketch.width - MARGIN * 2, statusAreaHeight);

        // Agent status
        sketch.fill(100);
        if (smallFont != null) {
            sketch.textFont(smallFont);
        }
        sketch.textAlign(PApplet.LEFT, PApplet.CENTER);
        sketch.text("Agent Status: " + agentStatus, MARGIN + 10, statusAreaY + statusAreaHeight / 2);

        // Connection indicator
        sketch.fill("Ready".equals(agentStatus) ? sketch.color(0, 150, 0) : sketch.color(200, 150, 0));
        sketch.ellipse(sketch.width - MARGIN - 20, statusAreaY + statusAreaHeight / 2, 8, 8);
    }

    public void drawUIBorders() {
        // Draw separator lines
        sketch.stroke(180);
        sketch.strokeWeight(1);

        // Between canvas and chat
        sketch.line(MARGIN, chatAreaY - MARGIN / 2, sketch.width - MARGIN, chatAreaY - MARGIN / 2);

        // Between chat and status
        sketch.line(MARGIN, statusAreaY - MARGIN / 2, sketch.width - MARGIN, statusAreaY - MARGIN / 2);
    }

    public void drawApprovalOverlay(ApprovalUIManager approvalManager) {
        // Dim background
        sketch.fill(0, 0, 0, 150);
        sketch.noStroke();
        sketch.rect(0, 0, sketch.width, sketch.height);

        // Panel
        int panelW = (int) (sketch.width * 0.8f);
        int panelH = (int) (sketch.height * 0.7f);
        int panelX = (sketch.width - panelW) / 2;
        int panelY = (sketch.height - panelH) / 2;

        sketch.fill(255);
        sketch.stroke(50);
        sketch.strokeWeight(2);
        sketch.rect(panelX, panelY, panelW, panelH);

        int contentX = panelX + 20;
        int contentY = panelY + 20;
        int contentW = panelW - 40;
        int contentH = panelH - 80;

        // Title
        sketch.fill(0);
        if (font != null) {
            sketch.textFont(font);
        }
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);
        sketch.text("変更の承認", contentX, contentY);

        // Summary
        int summaryY = contentY + 30;
        if (smallFont != null) {
            sketch.textFont(smallFont);
        }
        sketch.text(approvalManager.getApprovalSummary(), contentX, summaryY);

        // Diff area (scrollable)
        int diffY = summaryY + 20;
        int diffH = contentH - 60;
        // Mask diff area
        sketch.clip(contentX, diffY, contentW, diffH);
        int lineY = diffY - approvalManager.getApprovalScrollOffset();
        String[] lines = approvalManager.getApprovalDiff().split("\n");
        for (String line : lines) {
            // Color lines by prefix
            if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@")) {
                sketch.fill(60);
            } else if (line.startsWith("+")) {
                sketch.fill(0, 120, 0);
            } else if (line.startsWith("-")) {
                sketch.fill(180, 0, 0);
            } else {
                sketch.fill(0);
            }
            sketch.text(line, contentX, lineY);
            lineY += 14;
        }
        sketch.noClip();

        // Buttons
        int approveBtnX = contentX + contentW - 100 - 110;
        int approveBtnY = panelY + panelH - 45;
        int rejectBtnX = contentX + contentW - 100;
        int rejectBtnY = panelY + panelH - 45;
        int buttonW = 100;
        int buttonH = 30;

        // Update button positions in approval manager
        approvalManager.setButtonPositions(approveBtnX, approveBtnY, rejectBtnX, rejectBtnY, buttonW, buttonH);

        // Approve
        sketch.fill(80, 170, 80);
        sketch.stroke(40, 120, 40);
        sketch.rect(approveBtnX, approveBtnY, buttonW, buttonH);
        sketch.fill(255);
        sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
        sketch.text("承認 (Y)", approveBtnX + buttonW / 2, approveBtnY + buttonH / 2);

        // Reject
        sketch.fill(200, 80, 80);
        sketch.stroke(150, 40, 40);
        sketch.rect(rejectBtnX, rejectBtnY, buttonW, buttonH);
        sketch.fill(255);
        sketch.text("拒否 (N)", rejectBtnX + buttonW / 2, rejectBtnY + buttonH / 2);
    }

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
            sketch.println("Rendering shapes: " + shapes.size() + " total");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Domain shapes count: " + shapes.size());
            }

            // Clip to the canvas viewport and render all shapes
            sketch.clip(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight);
            try {
                drawingEngine.renderShapes(shapes);
            } finally {
                sketch.noClip();
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Error rendering domain shapes: " + e.getMessage(), e);
            }
        }
    }

    private void drawInputIndicator() {
        boolean useJapaneseInput = chatManager.isUsingJapaneseInput();
        boolean inputFocused = chatManager.isInputFocused();
        
        if (useJapaneseInput && chatManager.isJapaneseInputVisible()) {
            sketch.fill(100, 150, 100);
            sketch.textAlign(PApplet.LEFT, PApplet.BOTTOM);
            sketch.text("[日本語入力中]", MARGIN + 10, chatInputY - 2);
        } else if (inputFocused) {
            sketch.fill(150);
            sketch.textAlign(PApplet.LEFT, PApplet.BOTTOM);
            sketch.text("[クリックで日本語入力]", MARGIN + 10, chatInputY - 2);
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

    public int getChatLogY() { return chatLogY; }
    public int getChatLogHeight() { return chatLogHeight; }
    public int getChatInputY() { return chatInputY; }
}