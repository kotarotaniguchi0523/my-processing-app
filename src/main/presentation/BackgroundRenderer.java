package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * BackgroundRenderer handles background gradient and title drawing.
 * Manages the main application background visual elements.
 */
public class BackgroundRenderer {
    
    private static final int MARGIN = 12;
    
    private final PApplet sketch;
    private final ThemeManager themeManager;
    private PFont font;
    
    public BackgroundRenderer(PApplet sketch, ThemeManager themeManager) {
        this.sketch = sketch;
        this.themeManager = themeManager;
    }
    
    public void setFont(PFont font) {
        this.font = font;
    }
    
    /**
     * Draws the main background gradient and application title
     */
    public void drawBackground() {
        sketch.pushStyle();
        try {
            // Draw gradient background
            for (int y = 0; y < sketch.height; y++) {
                float t = PApplet.map(y, 0, (float) sketch.height, 0, 1);
                int c = sketch.lerpColor(themeManager.getColorBgStart(), themeManager.getColorBgEnd(), t);
                sketch.stroke(c);
                sketch.line(0, y, sketch.width, y);
            }
            
            // Draw application title (smaller to avoid overlap with canvas)
            if (font != null) {
                // Override font size locally to keep title compact
                sketch.textFont(font, 14);
            }
            sketch.fill(themeManager.getColorTextSecondary());
            sketch.textAlign(PApplet.LEFT, PApplet.TOP);
            // Slightly raise the title so it doesn't collide with canvas area
            sketch.text("AI Processing Studio", MARGIN + 2, MARGIN - 6);
            
        } finally {
            sketch.popStyle();
        }
    }
}
