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
            
            // Draw application title
            if (font != null) {
                sketch.textFont(font);
            }
            sketch.fill(themeManager.getColorTextSecondary());
            sketch.textAlign(PApplet.LEFT, PApplet.TOP);
            sketch.text("AI Processing Studio", MARGIN + 2, 8);
            
        } finally {
            sketch.popStyle();
        }
    }
}