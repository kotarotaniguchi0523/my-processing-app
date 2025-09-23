package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * DiffOverlayRenderer handles visualization of drawing state differences.
 * Shows added, removed, and modified shapes with GitHub-style highlighting.
 */
public class DiffOverlayRenderer {
    
    private static final long DIFF_OVERLAY_DURATION_MS = 3000L; // 3 seconds
    private static final int MARGIN = 12;
    
    private final PApplet sketch;
    // themeManager removed as unused - diff overlay uses hardcoded colors for consistency
    private PFont font;
    private PFont smallFont;
    
    // Diff overlay state
    private DrawingState lastRenderedState;
    private DiffSummary pendingDiffSummary;
    private long diffOverlayUntilMillis = 0L;
    
    public DiffOverlayRenderer(PApplet sketch, ThemeManager themeManager) {
        this.sketch = sketch;
        // themeManager not stored as diff overlay uses hardcoded GitHub-style colors
    }
    
    public void setFonts(PFont font, PFont smallFont) {
        this.font = font;
        this.smallFont = smallFont;
    }
    
    /**
     * Updates diff state and triggers overlay if changes detected
     */
    public void updateDiff(DrawingState currentState, int canvasAreaY, int canvasAreaHeight) {
        if (currentState != null) {
            if (lastRenderedState == null) {
                lastRenderedState = currentState;
            } else if (!currentState.equals(lastRenderedState)) {
                pendingDiffSummary = computeDiffSummary(lastRenderedState, currentState);
                diffOverlayUntilMillis = System.currentTimeMillis() + DIFF_OVERLAY_DURATION_MS;
                lastRenderedState = currentState;
            }
        }
        
        // Draw diff overlay if within time limit
        if (pendingDiffSummary != null && System.currentTimeMillis() < diffOverlayUntilMillis) {
            drawDiffOverlay(pendingDiffSummary, canvasAreaY, canvasAreaHeight);
        }
    }
    
    private void drawDiffOverlay(DiffSummary diff, int canvasAreaY, int canvasAreaHeight) {
        sketch.pushStyle();
        try {
            // Semi-transparent background overlay
            sketch.fill(0, 0, 0, 60);
            sketch.noStroke();
            sketch.rect(MARGIN, canvasAreaY, sketch.width - MARGIN * 2, canvasAreaHeight);
            
            // Highlight changed areas
            sketch.strokeWeight(3);
            
            // Added: Green highlight
            for (Shape.BoundingBox b : diff.added) {
                sketch.stroke(34, 197, 94, 200); // GitHub green
                sketch.fill(34, 197, 94, 40);
                drawHighlightBox(b);
            }

            // Removed: Red highlight
            for (Shape.BoundingBox b : diff.removed) {
                sketch.stroke(239, 68, 68, 200); // GitHub red
                sketch.fill(239, 68, 68, 40);
                drawHighlightBox(b);
            }

            // Modified: Orange highlight
            for (Shape.BoundingBox b : diff.modifiedNew) {
                sketch.stroke(251, 146, 60, 200); // GitHub orange
                sketch.fill(251, 146, 60, 40);
                drawHighlightBox(b);
            }
            
            // Show old positions with dashed lines
            sketch.strokeWeight(1);
            for (Shape.BoundingBox b : diff.modifiedOld) {
                drawDashedBox(b, sketch.color(156, 163, 175)); // GitHub gray
            }

            // Draw summary popup
            drawDiffPopup(diff, canvasAreaY);
            
        } finally {
            sketch.popStyle();
        }
    }
    
    private void drawHighlightBox(Shape.BoundingBox b) {
        if (b == null) return;
        float x = b.getX();
        float y = b.getY();
        float w = b.getWidth();
        float h = b.getHeight();
        sketch.rect(x - 2, y - 2, w + 4, h + 4, 4);
    }
    
    private void drawDashedBox(Shape.BoundingBox b, int color) {
        if (b == null) return;
        float x = b.getX();
        float y = b.getY(); 
        float w = b.getWidth();
        float h = b.getHeight();
        
        sketch.stroke(color);
        sketch.noFill();
        
        // Dash pattern
        float dashLength = 5;
        float gapLength = 5;
        
        // Top edge
        for (float i = x; i < x + w; i += dashLength + gapLength) {
            sketch.line(i, y, Math.min(i + dashLength, x + w), y);
        }
        // Bottom edge
        for (float i = x; i < x + w; i += dashLength + gapLength) {
            sketch.line(i, y + h, Math.min(i + dashLength, x + w), y + h);
        }
        // Left edge
        for (float i = y; i < y + h; i += dashLength + gapLength) {
            sketch.line(x, i, x, Math.min(i + dashLength, y + h));
        }
        // Right edge
        for (float i = y; i < y + h; i += dashLength + gapLength) {
            sketch.line(x + w, i, x + w, Math.min(i + dashLength, y + h));
        }
    }
    
    private void drawDiffPopup(DiffSummary diff, int canvasAreaY) {
        // GitHub-style diff summary popup
        int popupWidth = 280;
        int popupHeight = 100;
        int popupX = sketch.width - popupWidth - MARGIN - 20;
        int popupY = canvasAreaY + 20;
        
        // Popup shadow
        UIRenderingUtils.drawShadow(sketch, popupX, popupY, popupWidth, popupHeight, 8, 8, 40);
        
        // Popup background
        sketch.fill(255, 255, 255, 240);
        sketch.stroke(200);
        sketch.strokeWeight(1);
        sketch.rect(popupX, popupY, popupWidth, popupHeight, 8);
        
        // Title
        sketch.fill(33, 33, 33);
        sketch.textAlign(PApplet.LEFT, PApplet.TOP);
        if (font != null) sketch.textFont(font);
        sketch.text("Changes Applied", popupX + 15, popupY + 10);
        
        // Statistics
        if (smallFont != null) sketch.textFont(smallFont);
        int textY = popupY + 35;
        
        // Added
        if (!diff.added.isEmpty()) {
            sketch.fill(34, 197, 94);
            sketch.text("+ " + diff.added.size() + " added", popupX + 15, textY);
            textY += 20;
        }
        
        // Removed
        if (!diff.removed.isEmpty()) {
            sketch.fill(239, 68, 68);
            sketch.text("- " + diff.removed.size() + " removed", popupX + 15, textY);
            textY += 20;
        }
        
        // Modified
        if (!diff.modifiedNew.isEmpty()) {
            sketch.fill(251, 146, 60);
            sketch.text("~ " + diff.modifiedNew.size() + " modified", popupX + 15, textY);
        }
        
        // Progress bar for fade out effect
        float timeLeft = (diffOverlayUntilMillis - System.currentTimeMillis()) / (float)DIFF_OVERLAY_DURATION_MS;
        sketch.fill(34, 197, 94, 180);
        sketch.noStroke();
        sketch.rect(popupX, popupY + popupHeight - 3, popupWidth * timeLeft, 3, 0, 0, 4, 4);
    }
    
    
    private DiffSummary computeDiffSummary(DrawingState prev, DrawingState curr) {
        List<Shape> prevShapes = prev != null ? prev.getShapes() : Collections.emptyList();
        List<Shape> currShapes = curr != null ? curr.getShapes() : Collections.emptyList();

        Map<String, Shape> prevMap = new HashMap<>();
        for (Shape s : prevShapes) prevMap.put(s.getId(), s);
        Map<String, Shape> currMap = new HashMap<>();
        for (Shape s : currShapes) currMap.put(s.getId(), s);

        List<Shape.BoundingBox> added = new ArrayList<>();
        List<Shape.BoundingBox> removed = new ArrayList<>();
        List<Shape.BoundingBox> modifiedNew = new ArrayList<>();
        List<Shape.BoundingBox> modifiedOld = new ArrayList<>();

        // Find added shapes
        for (Shape s : currShapes) {
            if (!prevMap.containsKey(s.getId())) {
                Shape.BoundingBox b = safeBounds(s);
                if (b != null) added.add(b);
            }
        }
        
        // Find removed/modified shapes
        for (Shape sPrev : prevShapes) {
            Shape sCurr = currMap.get(sPrev.getId());
            if (sCurr == null) {
                Shape.BoundingBox b = safeBounds(sPrev);
                if (b != null) removed.add(b);
            } else if (!sPrev.equals(sCurr)) {
                Shape.BoundingBox bNew = safeBounds(sCurr);
                Shape.BoundingBox bOld = safeBounds(sPrev);
                if (bNew != null) modifiedNew.add(bNew);
                if (bOld != null) modifiedOld.add(bOld);
            }
        }

        return new DiffSummary(added, removed, modifiedNew, modifiedOld);
    }
    
    private Shape.BoundingBox safeBounds(Shape s) {
        try { 
            return s != null ? s.getBounds() : null; 
        } catch (Exception e) { 
            return null; 
        }
    }
    
    /**
     * Internal class to hold diff summary data
     */
    private static final class DiffSummary {
        final List<Shape.BoundingBox> added;
        final List<Shape.BoundingBox> removed;
        final List<Shape.BoundingBox> modifiedNew;
        final List<Shape.BoundingBox> modifiedOld;
        
        DiffSummary(List<Shape.BoundingBox> a, List<Shape.BoundingBox> r,
                List<Shape.BoundingBox> mn, List<Shape.BoundingBox> mo) {
            this.added = a; 
            this.removed = r; 
            this.modifiedNew = mn; 
            this.modifiedOld = mo;
        }
    }
}