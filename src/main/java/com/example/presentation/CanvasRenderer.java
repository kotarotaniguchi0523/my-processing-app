package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.rendering.DrawingEngine;

import static com.example.presentation.UIRenderingUtils.*;

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
  private processing.core.PGraphics offscreen;
    
    private PFont font;
    
    public CanvasRenderer(PApplet sketch, ThemeManager themeManager, 
            AIAgentService aiAgentService) {
        this.sketch = sketch;
        this.themeManager = themeManager;
        this.aiAgentService = aiAgentService;
        this.drawingEngine = new DrawingEngine(sketch);
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
      drawShadow(sketch, MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight, CARD_RADIUS, 12, SHADOW_ALPHA);
      sketch.fill(themeManager.getColorSurface());
      sketch.stroke(themeManager.getColorBorder());
      sketch.strokeWeight(1);
      sketch.rect(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight, CARD_RADIUS);

            // Canvas title
            sketch.fill(themeManager.getColorTextSecondary());
            if (font != null) {
                sketch.textFont(font);
            }
            sketch.textAlign(PApplet.LEFT, PApplet.TOP);
            sketch.text("Drawing Canvas", MARGIN + 14, canvasAreaY + TITLE_Y_OFFSET);

      // Compute inner content area (below title)
      int contentPadding = 12;
      int titleReserve = 40; // room for title and spacing
      int contentX = MARGIN + contentPadding;
      int contentY = canvasAreaY + titleReserve;
      int contentW = sketch.width - MARGIN * 2 - contentPadding * 2;
      int contentH = canvasAreaHeight - titleReserve - contentPadding;
      if (contentW < 10 || contentH < 10) return;

      // Render shapes to offscreen buffer clipped to logical canvas size
      if (aiAgentService != null) {
        try {
          DrawingState current = aiAgentService.getCurrentState();
          int logicalW = Math.max(1, current.getIntSetting("width", 800));
          int logicalH = Math.max(1, current.getIntSetting("height", 600));

          // Recreate buffer if size changed
          if (offscreen == null || offscreen.width != logicalW || offscreen.height != logicalH) {
            offscreen = sketch.createGraphics(logicalW, logicalH);
          }

          offscreen.beginDraw();
          try {
            drawingEngine.setCanvas(offscreen);
            drawingEngine.renderState(current);
          } finally {
            offscreen.endDraw();
          }

          // Draw scaled buffer into content area (clipped by card)
          sketch.image(offscreen, contentX, contentY, contentW, contentH);

        } catch (DrawingEngine.DrawingException ignored) {
          // Ignore rendering errors to avoid UI crash
        }
      }
    } finally {
      sketch.popStyle();
    }
  }

}
