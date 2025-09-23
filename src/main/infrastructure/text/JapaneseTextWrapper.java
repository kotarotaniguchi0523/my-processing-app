package com.example.infrastructure.text;

import com.example.domain.services.ITextWrapStrategy;
import processing.core.PFont;
import processing.core.PGraphics;
import java.util.ArrayList;
import java.util.List;

/**
 * Japanese text wrapping strategy implementation.
 * Handles line breaking at appropriate Japanese text boundaries including
 * punctuation marks and particles for natural reading flow.
 */
public class JapaneseTextWrapper implements ITextWrapStrategy {
    private static final String PUNCTUATION = "。、！？」）】』";
    private static final String OPENING_BRACKETS = "「（【『";
    private static final String PARTICLES = "のをにがでとはもやからまでよりへ";
    private static final int IDEAL_LENGTH = 10;
    private static final int MAX_LENGTH = 15;
    
    private final PGraphics graphics;
    
    /**
     * Creates a JapaneseTextWrapper with a PGraphics context for text measurement.
     * 
     * @param graphics Processing graphics context for text width calculation
     */
    public JapaneseTextWrapper(PGraphics graphics) {
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
        String currentLine = "";
        int charCount = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char currentChar = text.charAt(i);
            String testLine = currentLine + currentChar;
            
            // Check if adding this character exceeds max width
            float testWidth = graphics.textWidth(testLine);
            
            if (testWidth > maxWidth || shouldBreakLine(text, i, charCount)) {
                // Don't break if current line is empty (avoid infinite loop)
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = String.valueOf(currentChar);
                    charCount = 1;
                } else {
                    // Force add character if line is empty
                    currentLine = testLine;
                    charCount++;
                }
            } else {
                currentLine = testLine;
                charCount++;
            }
            
            // Force line break at newline characters
            if (currentChar == '\n') {
                if (!currentLine.isEmpty()) {
                    // Remove the newline character from the line
                    String lineWithoutNewline = currentLine.substring(0, currentLine.length() - 1);
                    if (!lineWithoutNewline.isEmpty()) {
                        lines.add(lineWithoutNewline);
                    }
                }
                currentLine = "";
                charCount = 0;
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
        
        // Check if text contains Japanese characters (Hiragana, Katakana, or Kanji)
        for (char c : text.toCharArray()) {
            if (isJapaneseChar(c)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if a line should be broken at the current position based on
     * Japanese text rules and character count.
     * 
     * @param text Full text being processed
     * @param position Current position in the text
     * @param lineCharCount Current line character count
     * @return true if line should be broken
     */
    private boolean shouldBreakLine(String text, int position, int lineCharCount) {
        // Force break at maximum length
        if (lineCharCount >= MAX_LENGTH) {
            return true;
        }
        
        // Don't break very short lines
        if (lineCharCount < IDEAL_LENGTH) {
            return false;
        }
        
        // Check for natural break points
        if (position < text.length() - 1) {
            char currentChar = text.charAt(position);
            char nextChar = text.charAt(position + 1);
            
            // Break after punctuation
            if (PUNCTUATION.indexOf(currentChar) != -1) {
                // Don't break if next char is closing bracket
                return OPENING_BRACKETS.indexOf(nextChar) == -1;
            }
            
            // Don't break before opening brackets
            if (OPENING_BRACKETS.indexOf(nextChar) != -1) {
                return false;
            }
            
            // Consider breaking after particles if line is getting long
            if (lineCharCount >= IDEAL_LENGTH) {
                // Check if current position is after a particle
                for (char particle : PARTICLES.toCharArray()) {
                    if (currentChar == particle) {
                        // Check if it's likely a particle (followed by non-particle)
                        if (PARTICLES.indexOf(nextChar) == -1) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
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