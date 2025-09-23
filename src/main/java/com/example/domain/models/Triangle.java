package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Objects;

/**
 * Triangle shape implementation Represents a triangular shape with three vertices
 */
public final class Triangle extends Shape {
  private final Position p1;
  private final Position p2;
  private final Position p3;

  public Triangle(String id, Position position, Position p1, Position p2, Position p3, Style style,
      boolean visible, int layerOrder) {
    super(id, position, style, visible, layerOrder);
    this.p1 = Objects.requireNonNull(p1, "Point 1 cannot be null");
    this.p2 = Objects.requireNonNull(p2, "Point 2 cannot be null");
    this.p3 = Objects.requireNonNull(p3, "Point 3 cannot be null");
  }

  public Triangle(String id, Position position, Position p1, Position p2, Position p3,
      Style style) {
    this(id, position, p1, p2, p3, style, true, 0);
  }

  public Triangle(Position position, Position p1, Position p2, Position p3, Style style) {
    this(generateId("triangle"), position, p1, p2, p3, style);
  }

  public Triangle(Position position, Position p1, Position p2, Position p3) {
    this(position, p1, p2, p3, Style.DEFAULT);
  }

  // Constructor for creating triangle from base position and width/height
  public Triangle(Position basePosition, float width, float height, Style style) {
    this(generateId("triangle"), basePosition, basePosition,
        new Position(basePosition.getX() + width, basePosition.getY()),
        new Position(basePosition.getX() + width / 2, basePosition.getY() - height), style);
  }

  public Triangle(Position basePosition, float width, float height) {
    this(basePosition, width, height, Style.DEFAULT);
  }

  public Position getP1() {
    return p1;
  }

  public Position getP2() {
    return p2;
  }

  public Position getP3() {
    return p3;
  }

  public Position getCentroid() {
    float centerX = (p1.getX() + p2.getX() + p3.getX()) / 3;
    float centerY = (p1.getY() + p2.getY() + p3.getY()) / 3;
    return new Position(centerX, centerY);
  }

  @Override
  public ShapeType getType() {
    return ShapeType.TRIANGLE;
  }

  @Override
  public Triangle moveTo(Position newPosition) {
    // Calculate the offset from current position to new position
    float offsetX = newPosition.getX() - position.getX();
    float offsetY = newPosition.getY() - position.getY();

    // Apply offset to all three points
    Position newP1 = new Position(p1.getX() + offsetX, p1.getY() + offsetY);
    Position newP2 = new Position(p2.getX() + offsetX, p2.getY() + offsetY);
    Position newP3 = new Position(p3.getX() + offsetX, p3.getY() + offsetY);

    return new Triangle(id, newPosition, newP1, newP2, newP3, style, visible, layerOrder);
  }

  @Override
  public Triangle withStyle(Style newStyle) {
    return new Triangle(id, position, p1, p2, p3, newStyle, visible, layerOrder);
  }

  public Triangle withPoints(Position newP1, Position newP2, Position newP3) {
    return new Triangle(id, position, newP1, newP2, newP3, style, visible, layerOrder);
  }

  @Override
  public boolean contains(Position point) {
    // Using barycentric coordinates to check if point is inside triangle
    float denominator = (p2.getY() - p3.getY()) * (p1.getX() - p3.getX())
        + (p3.getX() - p2.getX()) * (p1.getY() - p3.getY());

    if (Math.abs(denominator) < 0.000001f) {
      return false; // Degenerate triangle
    }

    float a = ((p2.getY() - p3.getY()) * (point.getX() - p3.getX())
        + (p3.getX() - p2.getX()) * (point.getY() - p3.getY())) / denominator;
    float b = ((p3.getY() - p1.getY()) * (point.getX() - p3.getX())
        + (p1.getX() - p3.getX()) * (point.getY() - p3.getY())) / denominator;
    float c = 1 - a - b;

    return a >= 0 && b >= 0 && c >= 0;
  }

  @Override
  public BoundingBox getBounds() {
    float minX = Math.min(Math.min(p1.getX(), p2.getX()), p3.getX());
    float maxX = Math.max(Math.max(p1.getX(), p2.getX()), p3.getX());
    float minY = Math.min(Math.min(p1.getY(), p2.getY()), p3.getY());
    float maxY = Math.max(Math.max(p1.getY(), p2.getY()), p3.getY());

    return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
  }

  @Override
  protected Shape createWithProperties(String id, Position position, Style style, boolean visible,
      int layerOrder) {
    return new Triangle(id, position, p1, p2, p3, style, visible, layerOrder);
  }

  @Override
  protected void performDraw(Object canvas) {
    // This would be implemented based on the specific rendering system
    // For now, it's a placeholder that would work with Processing
    if (canvas instanceof processing.core.PGraphics) {
      processing.core.PGraphics pg = (processing.core.PGraphics) canvas;

      // Apply style
      if (style.hasFill()) {
        pg.fill(style.getFillColor().toProcessingColor());
      } else {
        pg.noFill();
      }

      if (style.hasStroke()) {
        pg.stroke(style.getStrokeColor().toProcessingColor());
        pg.strokeWeight(style.getStrokeWeight());
      } else {
        pg.noStroke();
      }

      // Draw triangle using Processing's triangle function
      pg.triangle(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }
  }

  // Scale triangle from its centroid
  public Triangle scale(float scale) {
    Position centroid = getCentroid();

    Position scaledP1 = scalePointAroundCenter(p1, centroid, scale);
    Position scaledP2 = scalePointAroundCenter(p2, centroid, scale);
    Position scaledP3 = scalePointAroundCenter(p3, centroid, scale);

    return new Triangle(id, position, scaledP1, scaledP2, scaledP3, style, visible, layerOrder);
  }

  private Position scalePointAroundCenter(Position point, Position center, float scale) {
    float dx = point.getX() - center.getX();
    float dy = point.getY() - center.getY();
    return new Position(center.getX() + dx * scale, center.getY() + dy * scale);
  }


}
