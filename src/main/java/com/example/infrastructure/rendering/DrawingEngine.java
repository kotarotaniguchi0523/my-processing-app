package com.example.infrastructure.rendering;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.values.Color;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Comparator;
import java.util.Objects;

/**
 * DrawingEngine renders Shape objects using Processing graphics It handles layer management,
 * Z-order processing, and drawing state rendering
 */
public final class DrawingEngine {

  private PGraphics canvas;

  // Extracted components
  private final ShapeRenderingDispatcher shapeDispatcher;
  private final CanvasManager canvasManager;
  private final BoundsCalculator boundsCalculator;

  /**
   * Creates a new DrawingEngine with the specified Processing sketch
   *
   * @param sketch the PApplet instance to draw on
   */
  public DrawingEngine(PApplet sketch) {
    Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.canvas = sketch.g; // Default to main graphics context

    // Initialize extracted components
    this.shapeDispatcher = new ShapeRenderingDispatcher(canvas);
    this.canvasManager = new CanvasManager(sketch, canvas);
    this.boundsCalculator = new BoundsCalculator();
  }

  /**
   * Creates a new DrawingEngine with custom graphics context
   *
   * @param sketch the PApplet instance
   * @param canvas the PGraphics context to draw on
   */
  public DrawingEngine(PApplet sketch, PGraphics canvas) {
    Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");

    // Initialize extracted components
    this.shapeDispatcher = new ShapeRenderingDispatcher(canvas);
    this.canvasManager = new CanvasManager(sketch, canvas);
    this.boundsCalculator = new BoundsCalculator();
  }

  /**
   * Sets the graphics context for rendering
   *
   * @param canvas the PGraphics context to use
   */
  public void setCanvas(PGraphics canvas) {
    this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
    canvasManager.setCanvas(canvas);
    shapeDispatcher.updateCanvas(canvas);
  }


  /**
   * Gets the current graphics context
   *
   * @return the current PGraphics canvas
   */
  public PGraphics getCanvas() {
    return canvas;
  }

  /**
   * Renders a complete DrawingState to the canvas
   *
   * @param drawingState the state containing all drawing information
   * @throws DrawingException if rendering fails
   */
  public void renderState(DrawingState drawingState) throws DrawingException {
    Objects.requireNonNull(drawingState, "DrawingState cannot be null");

    try {
      // Apply canvas settings from the drawing state
      canvasManager.applyCanvasSettings(drawingState);

      // Clear the background
      canvasManager.clearCanvas(drawingState);

      // Render all shapes with proper layer ordering
      renderShapes(drawingState.getShapes());

    } catch (Exception e) {
      throw new DrawingException("Failed to render DrawingState", e);
    }
  }

  /**
   * Renders a list of shapes to the canvas with proper Z-ordering
   *
   * @param shapes the list of shapes to render
   * @throws DrawingException if rendering fails
   */
  public void renderShapes(List<Shape> shapes) throws DrawingException {
    Objects.requireNonNull(shapes, "Shapes list cannot be null");

    try {
      // Sort shapes by layer order (lower values drawn first, higher values on top)
      List<Shape> sortedShapes = shapes.stream().filter(Shape::isVisible)
          .sorted(Comparator.comparingInt(Shape::getLayerOrder)).collect(Collectors.toList());

      // Render each shape
      for (Shape shape : sortedShapes) {
        renderShape(shape);
      }

    } catch (Exception e) {
      throw new DrawingException("Failed to render shapes", e);
    }
  }

  /**
   * Renders a single shape to the canvas
   *
   * @param shape the shape to render
   * @throws DrawingException if rendering fails
   */
  public void renderShape(Shape shape) throws DrawingException {
    Objects.requireNonNull(shape, "Shape cannot be null");

    if (!shape.isVisible()) {
      return;
    }

    try {
      // Use the shape's built-in draw method
      shape.draw(canvas);

    } catch (ClassCastException e) {
      // Fallback to specific shape type rendering if shape.draw() fails
      shapeDispatcher.renderShapeByType(shape);
    } catch (Exception e) {
      throw new DrawingException("Failed to render shape: " + shape.getId(), e);
    }
  }




  /**
   * Clears the entire canvas with specified color
   *
   * @param color the background color
   */
  public void clearCanvas(Color color) {
    canvasManager.clearCanvas(color);
  }

  /**
   * Gets the bounds of all shapes in the drawing state
   *
   * @param drawingState the drawing state
   * @return the bounding box containing all shapes, or null if no shapes
   */
  public Shape.BoundingBox getDrawingBounds(DrawingState drawingState) {
    return boundsCalculator.getDrawingBounds(drawingState);
  }

  /** Custom exception for drawing errors */
  public static class DrawingException extends Exception {
    private static final long serialVersionUID = 1L;

    public DrawingException(String message) {
      super(message);
    }

    public DrawingException(String message, Throwable cause) {
      super(message, cause);
    }

    public DrawingException(Throwable cause) {
      super(cause);
    }
  }
}
