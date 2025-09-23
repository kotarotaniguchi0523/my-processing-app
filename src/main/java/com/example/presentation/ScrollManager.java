package com.example.presentation;

import processing.core.PApplet;

import java.util.List;

/**
 * Manages scroll position and calculations for chat display.
 * Extracted from ChatManager to follow Single Responsibility Principle.
 * Handles:
 * - Scroll offset management
 * - Content height calculation
 * - Auto-scroll behavior
 */
public class ScrollManager {

    private volatile int chatScrollOffset;
    private int chatLogHeight = 200;

    // Layout metrics for content height calculation
    private int layoutLineHeight = 20;
    private int layoutBubblePadding = 14;
    private int layoutMessageSpacing = 12;
    private float layoutContentMaxWidth = 600f;

    public ScrollManager() {
        this.chatScrollOffset = 0;
    }

    /**
     * Adjust scroll offset by the given amount
     */
    public void adjustScrollOffset(float scrollAmount, int totalContentHeight) {
        if (totalContentHeight > chatLogHeight) {
            chatScrollOffset += (int) scrollAmount;
            int maxScroll = Math.max(0, totalContentHeight - chatLogHeight);
            chatScrollOffset = PApplet.constrain(chatScrollOffset, 0, maxScroll);
        } else {
            chatScrollOffset = 0;
        }
    }

    /**
     * Get current scroll offset
     */
    public int getScrollOffset() {
        return chatScrollOffset;
    }


    /**
     * Auto-scroll to show new content
     */
    public void autoScrollForNewContent(int totalContentHeight) {
        int viewHeight = chatLogHeight;
        if (totalContentHeight > viewHeight) {
            chatScrollOffset = Math.max(0, totalContentHeight - viewHeight);
        } else {
            chatScrollOffset = 0;
        }
    }

    /**
     * Set the chat log display height
     */
    public void setChatLogHeight(int height) {
        this.chatLogHeight = height;
    }

    /**
     * Get the chat log display height
     */
    public int getChatLogHeight() {
        return chatLogHeight;
    }

    /**
     * Update layout metrics for content calculations
     */
    public void updateLayoutMetrics(int lineHeight, int bubblePadding,
                                   int messageSpacing, float contentMaxWidth) {
        this.layoutLineHeight = lineHeight > 0 ? lineHeight : 20;
        this.layoutBubblePadding = Math.max(0, bubblePadding);
        this.layoutMessageSpacing = Math.max(0, messageSpacing);
        this.layoutContentMaxWidth = Math.max(10f, contentMaxWidth);
    }

    /**
     * Calculate total content height for a list of messages with wrapping
     */
    public int calculateTotalHeight(List<String> messages, TextFormatter formatter) {
        int y = 10; // initial offset
        for (String message : messages) {
            String displayText = formatter.stripPrefix(message);
            List<String> lines = formatter.wrapText(displayText, layoutContentMaxWidth);
            int bubbleHeight = lines.size() * layoutLineHeight + layoutBubblePadding * 2;
            y += bubbleHeight + layoutMessageSpacing;
        }
        return y;
    }


}