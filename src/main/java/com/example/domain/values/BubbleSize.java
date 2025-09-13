package com.example.domain.values;

/**
 * Value object representing chat bubble dimensions.
 * Encapsulates width, height, and padding for bubble rendering.
 */
public record BubbleSize(
    float width,
    float height,
    float padding
) {
    /**
     * Default padding for chat bubbles.
     */
    public static final float DEFAULT_PADDING = 14f;
    
    /**
     * Creates a BubbleSize with default padding.
     * 
     * @param width Bubble content width in pixels
     * @param height Bubble content height in pixels
     */
    public BubbleSize(float width, float height) {
        this(width, height, DEFAULT_PADDING);
    }
    
    /**
     * Validates dimensions are positive.
     */
    public BubbleSize {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        if (padding < 0) {
            throw new IllegalArgumentException("Padding cannot be negative");
        }
    }
    
    /**
     * Gets the total width including padding.
     * 
     * @return Total width with padding on both sides
     */
    public float getTotalWidth() {
        return width + (padding * 2);
    }
    
    /**
     * Gets the total height including padding.
     * 
     * @return Total height with padding on top and bottom
     */
    public float getTotalHeight() {
        return height + (padding * 2);
    }
    
    /**
     * Creates a new BubbleSize with adjusted dimensions.
     * 
     * @param widthAdjustment Amount to add to width
     * @param heightAdjustment Amount to add to height
     * @return New BubbleSize with adjusted dimensions
     */
    public BubbleSize adjust(float widthAdjustment, float heightAdjustment) {
        return new BubbleSize(
            width + widthAdjustment,
            height + heightAdjustment,
            padding
        );
    }
    
    /**
     * Creates a new BubbleSize with different padding.
     * 
     * @param newPadding New padding value
     * @return New BubbleSize with updated padding
     */
    public BubbleSize withPadding(float newPadding) {
        return new BubbleSize(width, height, newPadding);
    }
}