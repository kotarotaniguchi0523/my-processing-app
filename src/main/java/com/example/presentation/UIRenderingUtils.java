package com.example.presentation;

import processing.core.PApplet;
import processing.core.PConstants;

/**
 * Utility class for common UI rendering operations.
 * Centralizes shared drawing functions to reduce code duplication across UI components.
 */
public class UIRenderingUtils {

    /**
     * Draw a shadow effect behind a rectangular area.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param r Corner radius
     * @param layers Number of shadow layers (more = softer shadow)
     * @param alpha Maximum alpha value for the darkest layer
     */
    public static void drawShadow(PApplet p, float x, float y, float w, float h, float r, int layers, float alpha) {
        p.push();
        p.noStroke();

        for (int i = layers; i > 0; i--) {
            float offset = PApplet.map(i, 0, layers, 0, 10);
            float opacity = PApplet.map(i, 0, layers, 0, alpha);
            p.fill(0, opacity);
            p.rect(x + offset, y + offset, w, h, r);
        }

        p.pop();
    }

    /**
     * Draw a card-style rounded rectangle with default styling.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param r Corner radius
     */
    public static void drawCard(PApplet p, float x, float y, float w, float h, float r) {
        p.push();
        p.fill(255);
        p.noStroke();
        p.rect(x, y, w, h, r);
        p.pop();
    }

    /**
     * Draw a card with shadow effect.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param r Corner radius
     * @param shadowLayers Number of shadow layers
     * @param shadowAlpha Shadow opacity
     */
    public static void drawCardWithShadow(PApplet p, float x, float y, float w, float h, float r, int shadowLayers, float shadowAlpha) {
        drawShadow(p, x, y, w, h, r, shadowLayers, shadowAlpha);
        drawCard(p, x, y, w, h, r);
    }

    /**
     * Draw a button with hover effect.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param text Button text
     * @param isHovered Whether the button is being hovered
     * @param baseColor Base button color
     * @param hoverColor Hover button color
     * @param textColor Text color
     */
    public static void drawButton(PApplet p, float x, float y, float w, float h,
                                  String text, boolean isHovered,
                                  int baseColor, int hoverColor, int textColor) {
        p.push();

        // Draw button background
        if (isHovered) {
            p.fill(hoverColor);
        } else {
            p.fill(baseColor);
        }
        p.noStroke();
        p.rect(x, y, w, h, h/4);

        // Draw button text
        p.fill(textColor);
        p.textAlign(PConstants.CENTER, PConstants.CENTER);
        p.text(text, x + w/2, y + h/2);

        p.pop();
    }

    /**
     * Draw a simple border around a rectangular area.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param borderColor Border color
     * @param borderWeight Border thickness
     */
    public static void drawBorder(PApplet p, float x, float y, float w, float h, int borderColor, float borderWeight) {
        p.push();
        p.noFill();
        p.stroke(borderColor);
        p.strokeWeight(borderWeight);
        p.rect(x, y, w, h);
        p.pop();
    }

    /**
     * Draw a gradient background.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param topColor Color at the top
     * @param bottomColor Color at the bottom
     */
    public static void drawGradientBackground(PApplet p, float x, float y, float w, float h, int topColor, int bottomColor) {
        p.push();
        p.noStroke();

        for (int i = 0; i <= h; i++) {
            float inter = PApplet.map(i, 0, h, 0, 1);
            int c = p.lerpColor(topColor, bottomColor, inter);
            p.stroke(c);
            p.line(x, y + i, x + w, y + i);
        }

        p.pop();
    }

    /**
     * Draw a progress bar.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @param progress Progress value (0.0 to 1.0)
     * @param bgColor Background color
     * @param fillColor Fill color
     * @param borderColor Border color
     */
    public static void drawProgressBar(PApplet p, float x, float y, float w, float h,
                                       float progress, int bgColor, int fillColor, int borderColor) {
        p.push();

        // Background
        p.fill(bgColor);
        p.noStroke();
        p.rect(x, y, w, h, h/2);

        // Progress fill
        if (progress > 0) {
            p.fill(fillColor);
            p.rect(x, y, w * PApplet.constrain(progress, 0, 1), h, h/2);
        }

        // Border
        p.noFill();
        p.stroke(borderColor);
        p.strokeWeight(1);
        p.rect(x, y, w, h, h/2);

        p.pop();
    }

    /**
     * Draw a tooltip box.
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate
     * @param y Y coordinate
     * @param text Tooltip text
     * @param bgColor Background color
     * @param textColor Text color
     * @param padding Padding around text
     */
    public static void drawTooltip(PApplet p, float x, float y, String text,
                                   int bgColor, int textColor, float padding) {
        p.push();

        p.textAlign(PConstants.LEFT, PConstants.TOP);
        float textWidth = p.textWidth(text);
        float textHeight = p.textAscent() + p.textDescent();

        // Draw background with shadow
        drawShadow(p, x, y, textWidth + padding * 2, textHeight + padding * 2, 4, 3, 30);

        p.fill(bgColor);
        p.noStroke();
        p.rect(x, y, textWidth + padding * 2, textHeight + padding * 2, 4);

        // Draw text
        p.fill(textColor);
        p.text(text, x + padding, y + padding);

        p.pop();
    }

    /**
     * Check if mouse is over a rectangular area.
     *
     * @param p The PApplet instance
     * @param x X coordinate
     * @param y Y coordinate
     * @param w Width
     * @param h Height
     * @return true if mouse is over the area
     */
    public static boolean isMouseOver(PApplet p, float x, float y, float w, float h) {
        return p.mouseX >= x && p.mouseX <= x + w &&
               p.mouseY >= y && p.mouseY <= y + h;
    }

    /**
     * Draw a divider line.
     *
     * @param p The PApplet instance for drawing
     * @param x1 Start X coordinate
     * @param y1 Start Y coordinate
     * @param x2 End X coordinate
     * @param y2 End Y coordinate
     * @param color Line color
     * @param weight Line thickness
     */
    public static void drawDivider(PApplet p, float x1, float y1, float x2, float y2, int color, float weight) {
        p.push();
        p.stroke(color);
        p.strokeWeight(weight);
        p.line(x1, y1, x2, y2);
        p.pop();
    }

    /**
     * Draw a simple icon placeholder (circle with character).
     *
     * @param p The PApplet instance for drawing
     * @param x X coordinate (center)
     * @param y Y coordinate (center)
     * @param size Icon size
     * @param character Character to display
     * @param bgColor Background color
     * @param textColor Text color
     */
    public static void drawIcon(PApplet p, float x, float y, float size, char character, int bgColor, int textColor) {
        p.push();

        // Draw background circle
        p.fill(bgColor);
        p.noStroke();
        p.ellipse(x, y, size, size);

        // Draw character
        p.fill(textColor);
        p.textAlign(PConstants.CENTER, PConstants.CENTER);
        p.text(character, x, y);

        p.pop();
    }
}