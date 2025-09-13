package com.example.domain.values;

/**
 * Value object representing formatted message with line breaks.
 * Immutable container for formatted text display information.
 */
public record MessageFormat(
    String[] lines,
    int lineCount,
    float maxLineWidth
) {
    /**
     * Creates a MessageFormat from wrapped text lines.
     * 
     * @param lines Array of text lines
     * @param lineCount Number of lines
     * @param maxLineWidth Maximum width among all lines in pixels
     */
    public MessageFormat {
        // Validate lineCount matches lines array length
        if (lines != null && lineCount != lines.length) {
            throw new IllegalArgumentException(
                "Line count must match lines array length");
        }
    }
    
    /**
     * Gets the text at the specified line index.
     * 
     * @param index Line index
     * @return Text at the specified line
     */
    public String getLine(int index) {
        if (index < 0 || index >= lineCount) {
            throw new IndexOutOfBoundsException(
                "Line index out of bounds: " + index);
        }
        return lines[index];
    }
    
    /**
     * Checks if the message has multiple lines.
     * 
     * @return true if message has more than one line
     */
    public boolean isMultiLine() {
        return lineCount > 1;
    }
}