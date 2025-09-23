package com.example.domain.services;

import processing.core.PFont;

/**
 * Text wrapping strategy interface for different languages and text types.
 * Defines how text should be wrapped based on language-specific rules.
 */
public interface ITextWrapStrategy {
    /**
     * Wraps text into multiple lines based on maximum width and font.
     *
     * @param text The text to wrap
     * @param maxWidth Maximum width in pixels for each line
     * @param font The font used for text measurement
     * @return Array of wrapped text lines
     */
    String[] wrapText(String text, float maxWidth, PFont font);
    
    /**
     * Determines if this strategy can handle the given text.
     *
     * @param text The text to check
     * @return true if this strategy can handle the text
     */
    boolean canHandle(String text);
}