package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatRenderer handles chat log display and message bubble rendering.
 * Manages scrolling, message formatting, and bubble styling.
 */
public class ChatRenderer {
    
    private static final int MARGIN = 12;
    private static final int INNER_GAP_SMALL = 6;
    private static final int LINE_HEIGHT = 22;
    private static final int MESSAGE_SPACING = 12;
    private static final int BUBBLE_PADDING = 14;
    private static final int BUBBLE_RADIUS = 16;
    private static final int CARD_RADIUS = 14;
    private static final int TITLE_Y_OFFSET = 12;
    private static final float SHADOW_ALPHA = 24f;
    
    private final PApplet sketch;
    private final ThemeManager themeManager;
    private final ChatManager chatManager;
    
    private PFont font;
    private PFont smallFont;
    
    public ChatRenderer(PApplet sketch, ThemeManager themeManager, ChatManager chatManager) {
        this.sketch = sketch;
        this.themeManager = themeManager;
        this.chatManager = chatManager;
    }
    
    public void setFonts(PFont font, PFont smallFont) {
        this.font = font;
        this.smallFont = smallFont;
    }
    
    /**
     * Draws the complete chat area including title and chat log
     */
    public void drawChatArea(int chatAreaY, int chatAreaHeight, int chatLogY, int chatLogHeight) {
        sketch.pushStyle();
        try {
            // Chat area card
            drawCard(MARGIN, chatAreaY, sketch.width - MARGIN * 2, chatAreaHeight, CARD_RADIUS);

            // Chat area title
            sketch.fill(themeManager.getColorTextSecondary());
            if (font != null) {
                sketch.textFont(font);
            }
            sketch.textAlign(PApplet.LEFT, PApplet.TOP);
            sketch.text("Chat Interface", MARGIN + 18, chatAreaY + TITLE_Y_OFFSET);

            // Draw chat log
            drawChatLog(chatLogY, chatLogHeight);
            
        } finally {
            sketch.popStyle();
        }
    }
    
    public void drawChatLog(int chatLogY, int chatLogHeight) {
        sketch.pushStyle();
        try {
            // Chat log surface
            sketch.fill(themeManager.getColorSurfaceMuted());
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
                List<String> lines = wrapTextForBubble(displayText, bubbleMaxWidth - BUBBLE_PADDING * 2);
                int bubbleHeight = lines.size() * LINE_HEIGHT + BUBBLE_PADDING * 2;

                // Calculate bubble width based on content
                float bubbleWidth = 0;
                for (String line : lines) {
                    bubbleWidth = Math.max(bubbleWidth, sketch.textWidth(line));
                }
                bubbleWidth += BUBBLE_PADDING * 2;
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
                    drawMessageBubble(bubbleX, y, bubbleWidth, bubbleHeight, lines, isUser, isSystem, isAssistant);
                }

                y += bubbleHeight + MESSAGE_SPACING;
            }

            // Calculate total height for scroll
            int totalHeight = y + scrollOffset - chatLogY;

            // Draw scroll indicator if needed
            if (totalHeight > chatLogHeight) {
                drawScrollIndicator(totalHeight, scrollOffset, chatLogY, chatLogHeight);
            }

            sketch.noClip();
        } finally {
            sketch.popStyle();
        }
    }
    
    private void drawMessageBubble(float bubbleX, int y, float bubbleWidth, int bubbleHeight, 
            List<String> lines, boolean isUser, boolean isSystem, boolean isAssistant) {
        sketch.pushStyle();
        
        // Bubble background with subtle shadow
        drawShadow(bubbleX, y, bubbleWidth, bubbleHeight, BUBBLE_RADIUS, 6, SHADOW_ALPHA);
        
        if (isUser) {
            sketch.noStroke();
            sketch.fill(themeManager.getColorUserBubble());
        } else if (isSystem) {
            sketch.noStroke();
            sketch.fill(themeManager.getColorSystemBubble());
        } else {
            sketch.stroke(themeManager.getColorBorder());
            sketch.strokeWeight(1);
            sketch.fill(themeManager.getColorAssistantBubble());
        }
        sketch.rect(bubbleX, y, bubbleWidth, bubbleHeight, BUBBLE_RADIUS);

        // Text
        if (isUser) {
            sketch.fill(255);
        } else {
            sketch.fill(themeManager.isDarkMode() ? 230 : 0);
        }
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);
        int textY = y + BUBBLE_PADDING;
        for (String line : lines) {
            sketch.text(line, bubbleX + BUBBLE_PADDING, textY);
            textY += LINE_HEIGHT;
        }
        
        sketch.popStyle();
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
    
    private void drawScrollIndicator(int totalHeight, int scrollOffset, int chatLogY, int chatLogHeight) {
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
    
    private void drawCard(float x, float y, float w, float h, float r) {
        drawShadow(x, y, w, h, r, 12, SHADOW_ALPHA);
        sketch.fill(themeManager.getColorSurface());
        sketch.stroke(themeManager.getColorBorder());
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
}