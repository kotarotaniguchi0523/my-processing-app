package com.example.infrastructure.text;

import com.example.domain.services.ITextWrapStrategy;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * English text wrapping strategy implementation.
 * Handles line breaking at word boundaries for natural English text flow.
 */
public class EnglishTextWrapper implements ITextWrapStrategy {
    private final PGraphics graphics;
    
    /**
     * Creates an EnglishTextWrapper with a PGraphics context for text measurement.
     * 
     * @param graphics Processing graphics context for text width calculation
     */
    public EnglishTextWrapper(PGraphics graphics) {
        this.graphics = graphics;
    }
    
    @Override
    public String[] wrapText(String text, float maxWidth, PFont font) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        
        // Set font for accurate measurement
        graphics.textFont(font);
        
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        String currentLine = "";
        
        for (String word : words) {
            // Handle newlines within words
            if (word.contains("\n")) {
                String[] parts = word.split("\n");
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        // Add current line and start new one
                        if (!currentLine.isEmpty()) {
                            lines.add(currentLine);
                            currentLine = "";
                        }
                    }
                    
                    // Process the part
                    String part = parts[i];
                    if (!part.isEmpty()) {
                        String testLine = currentLine.isEmpty() ? part : currentLine + " " + part;
                        float testWidth = graphics.textWidth(testLine);
                        
                        if (testWidth > maxWidth && !currentLine.isEmpty()) {
                            lines.add(currentLine);
                            currentLine = part;
                        } else {
                            currentLine = testLine;
                        }
                    }
                }
            } else {
                // Normal word without newlines
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                float testWidth = graphics.textWidth(testLine);
                
                if (testWidth > maxWidth) {
                    // Line would be too long with this word
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine);
                        currentLine = word;
                    } else {
                        // Single word is wider than max width - force it on its own line
                        lines.add(word);
                        currentLine = "";
                    }
                } else {
                    currentLine = testLine;
                }
            }
        }
        
        // Add remaining text
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }
        
        return lines.isEmpty() ? new String[]{""} : lines.toArray(new String[0]);
    }
    
    @Override
    public boolean canHandle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Handle English text (no Japanese characters)
        for (char c : text.toCharArray()) {
            if (isJapaneseChar(c)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if a character is a Japanese character (Hiragana, Katakana, or Kanji).
     * 
     * @param c Character to check
     * @return true if character is Japanese
     */
    private boolean isJapaneseChar(char c) {
        // Hiragana: U+3040 - U+309F
        // Katakana: U+30A0 - U+30FF
        // CJK Unified Ideographs (Kanji): U+4E00 - U+9FAF
        return (c >= 0x3040 && c <= 0x309F) ||  // Hiragana
               (c >= 0x30A0 && c <= 0x30FF) ||  // Katakana
               (c >= 0x4E00 && c <= 0x9FAF);    // Kanji
    }
}