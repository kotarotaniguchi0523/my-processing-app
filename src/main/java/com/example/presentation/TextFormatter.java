package com.example.presentation;

import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles text formatting and wrapping for chat display.
 * Extracted from ChatManager to follow Single Responsibility Principle.
 * Handles:
 * - Text wrapping for different languages
 * - Character-based wrapping for CJK languages
 * - Space-based wrapping for Western languages
 */
public class TextFormatter {

    private final PApplet sketch;

    public TextFormatter(PApplet sketch) {
        this.sketch = sketch;
    }

    /**
     * Wrap text to fit within specified width
     */
    public List<String> wrapText(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        // Prefer space-based wrapping when spaces exist
        if (text.contains(" ")) {
            lines = wrapByWords(text, maxWidth);
        } else {
            // No spaces - use character wrapping (common for Japanese/Chinese)
            lines = wrapByCharacters(text, maxWidth);
        }

        return lines;
    }

    /**
     * Wrap text by words (for languages with spaces)
     */
    private List<String> wrapByWords(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String current = "";

        for (String word : text.split(" ")) {
            String test = current.isEmpty() ? word : current + " " + word;

            if (sketch.textWidth(test) > maxWidth) {
                if (!current.isEmpty()) {
                    lines.add(current);
                    current = word;
                } else {
                    // Single long word - fall back to character wrapping
                    lines.addAll(wrapByCharacters(word, maxWidth));
                    current = "";
                }
            } else {
                current = test;
            }
        }

        if (!current.isEmpty()) {
            lines.add(current);
        }

        return lines;
    }

    /**
     * Wrap text by characters (for CJK languages without spaces)
     */
    private List<String> wrapByCharacters(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String test = buffer.toString() + c;

            if (sketch.textWidth(test) > maxWidth) {
                if (buffer.length() > 0) {
                    lines.add(buffer.toString());
                    buffer.setLength(0);
                }

                // If even a single char is wider than maxWidth, add it anyway
                if (sketch.textWidth(String.valueOf(c)) > maxWidth) {
                    lines.add(String.valueOf(c));
                } else {
                    buffer.append(c);
                }
            } else {
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            lines.add(buffer.toString());
        }

        return lines;
    }

    /**
     * Strip role prefix from message
     */
    public String stripPrefix(String message) {
        if (message == null) {
            return "";
        }

        if (message.startsWith("User:")) {
            return message.substring(5 + 1).trim();
        }
        if (message.startsWith("Assistant:")) {
            return message.substring(9 + 1).trim();
        }
        if (message.startsWith("System:")) {
            return message.substring(6 + 1).trim();
        }

        return message;
    }


}