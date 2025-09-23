package com.example.presentation;

import processing.core.PApplet;

/**
 * Utility class for common UI rendering operations. Centralizes shared drawing functions to reduce
 * code duplication across UI components.
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
  public static void drawShadow(PApplet p, float x, float y, float w, float h, float r, int layers,
      float alpha) {
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
}
