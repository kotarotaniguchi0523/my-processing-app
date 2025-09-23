package com.example.presentation.diff;

import com.example.presentation.diff.DiffCalculator.DiffItem;
import processing.core.PGraphics;
import processing.core.PApplet;

/**
 * Renders diff items in the approval panel.
 * Single responsibility: Display diff items with appropriate styling.
 */
public class DiffItemRenderer {

    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_PADDING = 8;
    private static final int COLOR_ADD = 0xFF27AE60;
    private static final int COLOR_REMOVE = 0xFFE74C3C;
    private static final int COLOR_MODIFY = 0xFFF39C12;

    private final PApplet sketch;

    public DiffItemRenderer(PApplet sketch) {
        this.sketch = sketch;
    }

    /**
     * Draw a single diff item
     */
    public void drawItem(PGraphics pg, int x, int y, int width, DiffItem item) {
        int color = getColorForAction(item.getAction());
        String symbol = getSymbolForAction(item.getAction());

        pg.pushStyle();

        // Background
        pg.fill(color, 30);
        pg.noStroke();
        pg.rect(x, y, width - 10, ITEM_HEIGHT, 3);

        // Action indicator
        pg.fill(color);
        pg.textAlign(PApplet.LEFT, PApplet.CENTER);
        pg.text(symbol, x + ITEM_PADDING, y + ITEM_HEIGHT/2);

        // Description
        pg.fill(255, 200);
        String desc = truncateText(item.getDescription(), width - 50);
        pg.text(desc, x + ITEM_PADDING + 20, y + ITEM_HEIGHT/2);

        pg.popStyle();
    }

    /**
     * Draw multiple diff items with scrolling
     */
    public void drawItems(PGraphics pg, int x, int y, int width, int height,
                         java.util.List<DiffItem> items, int scrollOffset) {
        if (items == null || items.isEmpty()) {
            drawEmptyState(pg, x, y, width, height);
            return;
        }

        // Set clipping region
        pg.pushStyle();
        pg.clip(x, y, width, height);

        int itemY = y - scrollOffset;
        for (DiffItem item : items) {
            // Only draw visible items
            if (itemY + ITEM_HEIGHT > y && itemY < y + height) {
                drawItem(pg, x + 5, itemY, width, item);
            }
            itemY += ITEM_HEIGHT + 5;
        }

        pg.noClip();
        pg.popStyle();
    }

    /**
     * Draw empty state message
     */
    private void drawEmptyState(PGraphics pg, int x, int y, int width, int height) {
        pg.pushStyle();
        pg.fill(150);
        pg.textAlign(PApplet.CENTER, PApplet.CENTER);
        pg.text("No changes to approve", x + width/2, y + height/2);
        pg.popStyle();
    }

    /**
     * Get color for action type
     */
    private int getColorForAction(String action) {
        switch (action) {
            case "ADD":
                return COLOR_ADD;
            case "REMOVE":
                return COLOR_REMOVE;
            case "MODIFY":
                return COLOR_MODIFY;
            default:
                return 0xFF95A5A6;
        }
    }

    /**
     * Get symbol for action type
     */
    private String getSymbolForAction(String action) {
        switch (action) {
            case "ADD":
                return "+";
            case "REMOVE":
                return "-";
            case "MODIFY":
                return "~";
            default:
                return "?";
        }
    }

    /**
     * Truncate text if too long
     */
    private String truncateText(String text, int maxWidth) {
        if (text == null) {
            return "";
        }

        sketch.pushStyle();
        float textWidth = sketch.textWidth(text);

        if (textWidth <= maxWidth) {
            sketch.popStyle();
            return text;
        }

        // Binary search for the right length
        String ellipsis = "...";
        int low = 0;
        int high = text.length();

        while (low < high) {
            int mid = (low + high + 1) / 2;
            String truncated = text.substring(0, mid) + ellipsis;
            if (sketch.textWidth(truncated) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        sketch.popStyle();
        return text.substring(0, low) + ellipsis;
    }


    /**
     * Calculate total height needed for all items
     */
    public int calculateTotalHeight(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        return itemCount * ITEM_HEIGHT + (itemCount - 1) * 5;
    }
}