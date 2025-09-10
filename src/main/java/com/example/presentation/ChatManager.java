package com.example.presentation;

import com.example.usecase.UseCaseFacade;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChatManager handles all chat-related functionality including message management,
 * input handling, and chat display logic.
 * This class follows the Single Responsibility Principle by focusing solely on chat management.
 */
public class ChatManager {
    private static final Logger LOGGER = Logger.getLogger(ChatManager.class.getName());
    private static final int LINE_HEIGHT = 20;

    private final PApplet sketch;
    private final UseCaseFacade useCaseFacade;

    // Chat state
    private final List<String> chatMessages;
    private String currentInput = "";
    private int chatScrollOffset;
    private boolean inputFocused;

    public ChatManager(PApplet sketch, UseCaseFacade useCaseFacade) {
        this.sketch = sketch;
        this.useCaseFacade = useCaseFacade;
        this.chatMessages = new ArrayList<>();
        
        // Initialize with welcome message
        chatMessages.add("System: AI Agent ready for instructions");
    }

    public void addMessage(String message) {
        chatMessages.add(message);
    }


    public List<String> getWrappedMessageLines(float maxWidth) {
        List<String> allLines = new ArrayList<>();
        for (String message : chatMessages) {
            allLines.addAll(wrapForWidth(message, maxWidth));
        }
        return allLines;
    }

    public void adjustScrollOffset(float scrollAmount) {
        chatScrollOffset += scrollAmount;

        // Calculate max scroll based on total message height
        int totalMessagesHeight = 0;
        for (String msg : chatMessages) {
            String[] lines = msg.split("\n");
            totalMessagesHeight += lines.length * LINE_HEIGHT + 10; // Line height + spacing
        }

        // Boundary checks for scrolling (assuming chat log height is available)
        int maxScroll = Math.max(0, totalMessagesHeight - getChatLogHeight() + 10);
        chatScrollOffset = PApplet.constrain(chatScrollOffset, 0, maxScroll);
    }

    // Text wrapping logic - handles languages without spaces (e.g., Japanese)
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
                if (sketch.textWidth(test) > maxWidth) {
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
            if (sketch.textWidth(test) > maxWidth) {
                if (buf.length() > 0) {
                    out.add(buf.toString());
                    buf.setLength(0);
                }
                // If even a single char is wider than maxWidth, hard add to avoid infinite loop
                if (sketch.textWidth(String.valueOf(c)) > maxWidth) {
                    out.add(String.valueOf(c));
                } else {
                    buf.append(c);
                }
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            out.add(buf.toString());
        }
        return out;
    }

    // Cache for chat log height - set by UI renderer
    private int chatLogHeight = 200;

    public void setChatLogHeight(int height) {
        this.chatLogHeight = height;
    }

    private int getChatLogHeight() {
        return chatLogHeight;
    }

    // Input management
    public String getCurrentInput() {
        return currentInput;
    }

    public void setCurrentInput(String input) {
        this.currentInput = input;
    }

    public void addToCurrentInput(char c) {
        this.currentInput += c;
    }

    public boolean isInputFocused() {
        return inputFocused;
    }

    public void setInputFocused(boolean focused) {
        this.inputFocused = focused;
    }


    // Japanese input integration
    private boolean useJapaneseInput = false;
    private boolean japaneseInputVisible = false;

    public boolean isUsingJapaneseInput() {
        return useJapaneseInput;
    }


    public boolean isJapaneseInputVisible() {
        return japaneseInputVisible;
    }

    public void setJapaneseInputVisible(boolean visible) {
        this.japaneseInputVisible = visible;
    }
}