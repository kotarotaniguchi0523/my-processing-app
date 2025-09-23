package com.example.presentation.diff;

import processing.core.PGraphics;
import processing.core.PApplet;

/**
 * Manages approval and rejection buttons.
 * Single responsibility: Handle button rendering and interaction.
 */
public class ApprovalButtonManager {

    private static final int BUTTON_HEIGHT = 35;
    private static final int BUTTON_MARGIN = 10;
    private static final int BUTTON_COLOR_APPROVE = 0xFF27AE60;
    private static final int BUTTON_COLOR_REJECT = 0xFFE74C3C;

    private final PApplet sketch;
    private final Runnable onApprove;
    private final Runnable onReject;

    // Button positions (calculated on layout)
    private int approveX, approveY, approveW;
    private int rejectX, rejectY, rejectW;
    private boolean layoutCalculated = false;

    public ApprovalButtonManager(PApplet sketch, Runnable onApprove, Runnable onReject) {
        this.sketch = sketch;
        this.onApprove = onApprove;
        this.onReject = onReject;
    }

    /**
     * Calculate button layout
     */
    public void calculateLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int buttonY = panelY + panelHeight - BUTTON_HEIGHT - BUTTON_MARGIN;

        // Calculate button widths
        approveW = measureButtonWidth("✓ Approve");
        rejectW = measureButtonWidth("✗ Reject");

        int totalWidth = approveW + rejectW + BUTTON_MARGIN;
        int startX = panelX + (panelWidth - totalWidth) / 2;

        approveX = startX;
        approveY = buttonY;

        rejectX = startX + approveW + BUTTON_MARGIN;
        rejectY = buttonY;

        layoutCalculated = true;
    }

    /**
     * Draw the approval buttons
     */
    public void draw(PGraphics pg) {
        if (!layoutCalculated) {
            return;
        }

        int mx = sketch.mouseX;
        int my = sketch.mouseY;

        boolean hoverApprove = isInButtonArea(mx, my, approveX, approveY, approveW, BUTTON_HEIGHT);
        boolean hoverReject = isInButtonArea(mx, my, rejectX, rejectY, rejectW, BUTTON_HEIGHT);

        drawButton(pg, approveX, approveY, approveW, BUTTON_HEIGHT,
                  "✓ Approve", BUTTON_COLOR_APPROVE, hoverApprove);

        drawButton(pg, rejectX, rejectY, rejectW, BUTTON_HEIGHT,
                  "✗ Reject", BUTTON_COLOR_REJECT, hoverReject);
    }

    /**
     * Draw a single button
     */
    private void drawButton(PGraphics pg, int x, int y, int w, int h,
                           String text, int color, boolean hover) {
        pg.pushStyle();

        // Button background
        if (hover) {
            pg.fill(color);
            pg.stroke(255, 100);
            pg.strokeWeight(2);
        } else {
            pg.fill(color, 200);
            pg.noStroke();
        }

        pg.rect(x, y, w, h, 5);

        // Button text
        pg.fill(255);
        pg.textAlign(PApplet.CENTER, PApplet.CENTER);
        pg.text(text, x + w/2, y + h/2);

        pg.popStyle();
    }

    /**
     * Handle mouse click
     */
    public boolean handleClick(int mx, int my) {
        if (!layoutCalculated) {
            return false;
        }

        if (isInButtonArea(mx, my, approveX, approveY, approveW, BUTTON_HEIGHT)) {
            if (onApprove != null) {
                onApprove.run();
            }
            return true;
        }

        if (isInButtonArea(mx, my, rejectX, rejectY, rejectW, BUTTON_HEIGHT)) {
            if (onReject != null) {
                onReject.run();
            }
            return true;
        }

        return false;
    }

    /**
     * Check if mouse is in button area
     */
    private boolean isInButtonArea(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /**
     * Measure button width based on text
     */
    private int measureButtonWidth(String label) {
        sketch.pushStyle();
        float textW = sketch.textWidth(label);
        sketch.popStyle();
        return (int)(textW + 40); // Add padding
    }

    /**
     * Get button area height for layout calculations
     */
    public int getButtonAreaHeight() {
        return BUTTON_HEIGHT + BUTTON_MARGIN * 2;
    }
}