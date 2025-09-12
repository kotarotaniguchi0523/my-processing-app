package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.rendering.DrawingEngine;

/**
 * CanvasRenderer handles the drawing canvas area rendering.
 * Manages the canvas card, title, and shape rendering via DrawingEngine.
 */
public class CanvasRenderer {
    
    private static final int MARGIN = 12;
    private static final int CARD_RADIUS = 14;
    private static final int TITLE_Y_OFFSET = 12;
    private static final float SHADOW_ALPHA = 24f;
    
    private final PApplet sketch;
    private final ThemeManager themeManager;
    private final AIAgentService aiAgentService;
    private final DrawingEngine drawingEngine;
    private final DiffOverlayRenderer diffOverlayRenderer;
    
    private PFont font;
    
    public CanvasRenderer(PApplet sketch, ThemeManager themeManager, 
            AIAgentService aiAgentService, DiffOverlayRenderer diffOverlayRenderer) {
        this.sketch = sketch;
        this.themeManager = themeManager;
        this.aiAgentService = aiAgentService;
        this.drawingEngine = new DrawingEngine(sketch);
        this.diffOverlayRenderer = diffOverlayRenderer;
    }
    
    public void setFont(PFont font) {
        this.font = font;
    }
    
    /**
     * Draws the canvas area including shapes and diff overlays
     */
    public void drawCanvasArea(int canvasAreaY, int canvasAreaHeight) {
        sketch.pushStyle();
        try {
            // Canvas card with soft shadow
            drawCard(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight, CARD_RADIUS);

            // Canvas title
            sketch.fill(themeManager.getColorTextSecondary());
            if (font != null) {
                sketch.textFont(font);
            }
            sketch.textAlign(PApplet.LEFT, PApplet.TOP);
            sketch.text("Drawing Canvas", MARGIN + 14, canvasAreaY + TITLE_Y_OFFSET);

            // Render shapes using current state from AI agent
            if (aiAgentService != null) {
                try {
                    DrawingState current = aiAgentService.getCurrentState();
                    drawingEngine.renderState(current);
                    
                    // Handle diff overlay rendering
                    diffOverlayRenderer.updateDiff(current, canvasAreaY, canvasAreaHeight);
                    
                } catch (DrawingEngine.DrawingException ignored) {
                    // Ignore rendering errors to avoid UI crash
                }
            }
        } finally {
            sketch.popStyle();
        }
    }
    
    private void drawCard(float x, float y, float w, float h, float r) {
        drawShadow(x, y, w, h, r, 12, SHADOW_ALPHA);
        sketch.fill(themeManager.getColorSurface());
        sketch.stroke(themeManager.getColorBorder());
        sketch.strokeWeight(1);
        sketch.rect(x, y, w, h, r);
    }
    
    private void drawShadow(float x, float y, float w, float h, float r, int layers, float alpha) {
        sketch.pushStyle();
        try {
            for (int i = layers; i >= 1; i--) {
                float spread = i * 1.5f;
                int a = (int) (alpha * (i / (float) layers));
                sketch.noStroke();
                sketch.fill(0, 0, 0, a);
                sketch.rect(x + 0.5f * i, y + 0.8f * i, w, h, r + spread);
            }
        } finally {
            sketch.popStyle();
        }
    }
}