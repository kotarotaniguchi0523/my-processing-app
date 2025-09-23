package com.example.infrastructure.rendering;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Ellipse;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.models.Triangle;
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

  private final PApplet sketch;
  private PGraphics canvas;

  // Specialized renderers for different shape types
  private CircleRenderer circleRenderer;
  private RectangleRenderer rectangleRenderer;
  private LineRenderer lineRenderer;
  private TextRenderer textRenderer;

  /**
   * Creates a new DrawingEngine with the specified Processing sketch
   *
   * @param sketch the PApplet instance to draw on
   */
  public DrawingEngine(PApplet sketch) {
    this.sketch = Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.canvas = sketch.g; // Default to main graphics context
    initializeRenderers();
  }

  /**
   * Creates a new DrawingEngine with custom graphics context
   *
   * @param sketch the PApplet instance
   * @param canvas the PGraphics context to draw on
   */
  public DrawingEngine(PApplet sketch, PGraphics canvas) {
    this.sketch = Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
    initializeRenderers();
  }

  /**
   * Sets the graphics context for rendering
   *
   * @param canvas the PGraphics context to use
   */
  public void setCanvas(PGraphics canvas) {
    this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
    initializeRenderers(); // Re-initialize renderers with new canvas
  }

  /**
   * Initializes all specialized renderers with the current canvas
   */
  private void initializeRenderers() {
    this.circleRenderer = new CircleRenderer(canvas);
    this.rectangleRenderer = new RectangleRenderer(canvas);
    this.lineRenderer = new LineRenderer(canvas);
    this.textRenderer = new TextRenderer(canvas);
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
      applyCanvasSettings(drawingState);

      // Clear the background
      clearCanvas(drawingState);

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
      renderShapeByType(shape);
    } catch (Exception e) {
      throw new DrawingException("Failed to render shape: " + shape.getId(), e);
    }
  }

  /**
   * Renders a shape by dispatching to type-specific methods
   *
   * @param shape the shape to render
   * @throws DrawingException if shape type is not supported
   */
  private void renderShapeByType(Shape shape) throws DrawingException {
    try {
      switch (shape.getType()) {
        case CIRCLE:
          if (shape instanceof Circle) {
            circleRenderer.renderCircle((Circle) shape);
          } else {
            throw new DrawingException(
                "Shape type mismatch: expected Circle but got " + shape.getClass().getSimpleName());
          }
          break;

        case RECTANGLE:
          if (shape instanceof Rectangle) {
            rectangleRenderer.renderRectangle((Rectangle) shape);
          } else {
            throw new DrawingException("Shape type mismatch: expected Rectangle but got "
                + shape.getClass().getSimpleName());
          }
          break;

        case LINE:
          if (shape instanceof Line) {
            lineRenderer.renderLine((Line) shape);
          } else {
            throw new DrawingException(
                "Shape type mismatch: expected Line but got " + shape.getClass().getSimpleName());
          }
          break;

        case TEXT:
          if (shape instanceof Text) {
            textRenderer.renderText((Text) shape);
          } else {
            throw new DrawingException(
                "Shape type mismatch: expected Text but got " + shape.getClass().getSimpleName());
          }
          break;

        case TRIANGLE:
          if (shape instanceof Triangle) {
            rectangleRenderer.renderTriangle((Triangle) shape);
          } else {
            throw new DrawingException("Shape type mismatch: expected Triangle but got "
                + shape.getClass().getSimpleName());
          }
          break;

        case ELLIPSE:
          if (shape instanceof Ellipse) {
            circleRenderer.renderEllipse((Ellipse) shape);
          } else {
            throw new DrawingException("Shape type mismatch: expected Ellipse but got "
                + shape.getClass().getSimpleName());
          }
          break;

        default:
          throw new DrawingException("Unsupported shape type: " + shape.getType());
      }
    } catch (ShapeRenderer.RenderingException e) {
      throw new DrawingException(e.getMessage(), e);
    }
  }


  /**
   * Applies canvas settings from DrawingState
   *
   * @param drawingState the state containing canvas settings
   */
  private void applyCanvasSettings(DrawingState drawingState) {
    // Apply canvas size if specified
    // Read canvas size settings if needed in the future (immutable in Processing after setup)
    drawingState.getIntSetting("width", canvas.width);
    drawingState.getIntSetting("height", canvas.height);

    // Note: Canvas size cannot be changed after creation in Processing
    // This is mainly for validation or future enhancement

    // Apply other canvas-wide settings as needed
    String title = drawingState.getStringSetting("title", null);
    if (title != null) {
      // Update window title when running on a display surface
      try {
        if (sketch != null && sketch.getSurface() != null) {
          sketch.getSurface().setTitle(title);
        }
      } catch (Exception ignored) {
        // No display surface (e.g., headless tests) - safely ignore
      }
    }
  }

  /**
   * Clears the canvas with the background color from DrawingState
   *
   * @param drawingState the state containing background color
   */
  private void clearCanvas(DrawingState drawingState) {
    String backgroundColorHex = drawingState.getStringSetting("backgroundColor", "#FFFFFF");

    try {
      Color backgroundColor = Color.fromHex(backgroundColorHex);
      canvas.background(backgroundColor.getRed(), backgroundColor.getGreen(),
          backgroundColor.getBlue());
    } catch (Exception e) {
      // Fallback to white background if color parsing fails
      canvas.background(255, 255, 255);
    }
  }

  /**
   * Clears the entire canvas with specified color
   *
   * @param color the background color
   */
  public void clearCanvas(Color color) {
    Objects.requireNonNull(color, "Color cannot be null");
    canvas.background(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
  }

  /**
   * Gets the bounds of all shapes in the drawing state
   *
   * @param drawingState the drawing state
   * @return the bounding box containing all shapes, or null if no shapes
   */
  public Shape.BoundingBox getDrawingBounds(DrawingState drawingState) {
    if (drawingState == null || drawingState.getShapes().isEmpty()) {
      return null;
    }

    // Calculate bounds from visible shapes
    List<Shape> visibleShapes =
        drawingState.getShapes().stream().filter(Shape::isVisible).collect(Collectors.toList());

    if (visibleShapes.isEmpty()) {
      return null;
    }

    Shape.BoundingBox totalBounds = visibleShapes.get(0).getBounds();
    for (int i = 1; i < visibleShapes.size(); i++) {
      totalBounds = totalBounds.union(visibleShapes.get(i).getBounds());
    }

    return totalBounds;
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
