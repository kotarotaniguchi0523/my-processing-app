package com.example.presentation;

import com.example.domain.models.DrawingState;
import com.example.presentation.diff.DiffCalculator;
import com.example.presentation.diff.DiffCalculator.DiffSummary;
import com.example.presentation.diff.ApprovalButtonManager;
import com.example.presentation.diff.DiffItemRenderer;
import processing.core.PGraphics;
import processing.core.PApplet;

/**
 * Renderer for diff approval dialog.
 * Refactored to use separate components for different responsibilities.
 */
public class DiffApprovalRenderer {

    private final PApplet sketch;
    private final DiffItemRenderer itemRenderer;
    private final ApprovalButtonManager buttonManager;

    // Panel dimensions
    private final int panelWidth = 400;
    private final int panelHeight = 500;

    // State
    private boolean visible = false;
    private DiffSummary diffSummary;
    private int scrollOffset = 0;

    // Callbacks
    private Runnable onApprove;
    private Runnable onReject;

    public DiffApprovalRenderer(PApplet sketch) {
        this.sketch = sketch;
        this.itemRenderer = new DiffItemRenderer(sketch);
        this.buttonManager = new ApprovalButtonManager(
            sketch,
            this::handleApprove,
            this::handleReject
        );
    }

    /**
     * Show the approval dialog with diff
     */
    public void show(DrawingState previous, DrawingState proposed,
                    Runnable onApprove, Runnable onReject) {
        this.onApprove = onApprove;
        this.onReject = onReject;
        this.diffSummary = DiffCalculator.computeDiff(previous, proposed);
        this.scrollOffset = 0;
        this.visible = true;

        // Calculate button layout
        int panelX = (sketch.width - panelWidth) / 2;
        int panelY = (sketch.height - panelHeight) / 2;
        buttonManager.calculateLayout(panelX, panelY, panelWidth, panelHeight);
    }

    /**
     * Hide the approval dialog
     */
    public void hide() {
        visible = false;
        diffSummary = null;
        scrollOffset = 0;
    }

    /**
     * Draw the diff approval panel
     */
    public void draw(PGraphics pg) {
        if (!visible || diffSummary == null) {
            return;
        }

        int panelX = (sketch.width - panelWidth) / 2;
        int panelY = (sketch.height - panelHeight) / 2;

        // Draw overlay background
        pg.fill(0, 180);
        pg.noStroke();
        pg.rect(0, 0, sketch.width, sketch.height);

        // Draw panel background
        drawPanel(pg, panelX, panelY, panelWidth, panelHeight);

        // Draw header
        drawHeader(pg, panelX, panelY);

        // Draw diff items
        int contentY = panelY + 80;
        int contentHeight = panelHeight - 80 - buttonManager.getButtonAreaHeight();
        itemRenderer.drawItems(
            pg,
            panelX + 10,
            contentY,
            panelWidth - 20,
            contentHeight,
            diffSummary.getItems(),
            scrollOffset
        );

        // Draw buttons
        buttonManager.draw(pg);
    }

    /**
     * Draw the panel background
     */
    private void drawPanel(PGraphics pg, int x, int y, int w, int h) {
        pg.pushStyle();
        pg.fill(30, 40, 50);
        pg.stroke(255, 50);
        pg.strokeWeight(1);
        pg.rect(x, y, w, h, 10);
        pg.popStyle();
    }

    /**
     * Draw the header section
     */
    private void drawHeader(PGraphics pg, int x, int y) {
        pg.pushStyle();

        // Title
        pg.fill(255);
        pg.textAlign(PApplet.CENTER, PApplet.TOP);
        pg.textSize(18);
        pg.text("Approve Changes", x + panelWidth/2, y + 15);

        // Summary
        if (diffSummary != null && diffSummary.hasChanges()) {
            pg.textSize(14);
            pg.fill(200);
            String summary = String.format(
                "%d added, %d removed, %d modified",
                diffSummary.getAdded(),
                diffSummary.getRemoved(),
                diffSummary.getModified()
            );
            pg.text(summary, x + panelWidth/2, y + 45);
        }

        pg.popStyle();
    }

    /**
     * Handle mouse click
     */
    public boolean handleMousePressed() {
        if (!visible) {
            return false;
        }

        return buttonManager.handleClick(sketch.mouseX, sketch.mouseY);
    }

    /**
     * Handle mouse wheel for scrolling
     */
    public void handleMouseWheel(float delta) {
        if (!visible || diffSummary == null) {
            return;
        }

        int totalHeight = itemRenderer.calculateTotalHeight(diffSummary.getItems().size());
        int viewHeight = panelHeight - 80 - buttonManager.getButtonAreaHeight();

        scrollOffset += delta * 20;
        scrollOffset = Math.max(0, scrollOffset);
        scrollOffset = Math.min(Math.max(0, totalHeight - viewHeight), scrollOffset);
    }

    /**
     * Handle approval
     */
    private void handleApprove() {
        if (onApprove != null) {
            onApprove.run();
        }
        hide();
    }

    /**
     * Handle rejection
     */
    private void handleReject() {
        if (onReject != null) {
            onReject.run();
        }
        hide();
    }

    /**
     * Check if the dialog is visible
     */
    public boolean isVisible() {
        return visible;
    }
}