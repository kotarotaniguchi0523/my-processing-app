package com.example.infrastructure.rendering;

import com.example.domain.values.Color;
import com.example.domain.values.Style;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Abstract base class for shape renderers providing common functionality for style application and
 * error handling.
 */
public abstract class ShapeRenderer {

  protected final PGraphics canvas;

  /**
   * Creates a new ShapeRenderer with the specified graphics context
   *
   * @param canvas the PGraphics context to render to
   */
  protected ShapeRenderer(PGraphics canvas) {
    this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
  }

  /**
   * Applies a style to the current graphics context
   *
   * @param style the style to apply
   */
  protected void applyStyle(Style style) {
    Objects.requireNonNull(style, "Style cannot be null");

    // Apply fill
    if (style.hasFill()) {
      Color fillColor = style.getFillColor();
      canvas.fill(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
          fillColor.getAlpha());
    } else {
      canvas.noFill();
    }

    // Apply stroke
    if (style.hasStroke()) {
      Color strokeColor = style.getStrokeColor();
      canvas.stroke(strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(),
          strokeColor.getAlpha());
      canvas.strokeWeight(style.getStrokeWeight());
    } else {
      canvas.noStroke();
    }
  }

  /**
   * Custom exception for rendering errors
   */
  public static class RenderingException extends Exception {
    private static final long serialVersionUID = 1L;

    public RenderingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
