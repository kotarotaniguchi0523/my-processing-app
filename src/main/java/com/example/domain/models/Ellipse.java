package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;

/**
 * Ellipse shape implementation 
 * Represents an elliptical shape with center position and width/height radii
 */
public final class Ellipse extends Shape {
  private final float radiusX;  // Horizontal radius
  private final float radiusY;  // Vertical radius

  public Ellipse(String id, Position center, float radiusX, float radiusY, Style style,
      boolean visible, int layerOrder) {
    super(id, center, style, visible, layerOrder);
    if (radiusX <= 0 || radiusY <= 0) {
      throw new IllegalArgumentException("Radii must be positive");
    }
    this.radiusX = radiusX;
    this.radiusY = radiusY;
  }

  public Ellipse(String id, Position center, float radiusX, float radiusY, Style style) {
    this(id, center, radiusX, radiusY, style, true, 0);
  }

  public Ellipse(Position center, float radiusX, float radiusY, Style style) {
    this(generateId("ellipse"), center, radiusX, radiusY, style);
  }

  public Ellipse(Position center, float radiusX, float radiusY) {
    this(center, radiusX, radiusY, Style.DEFAULT);
  }

  // Create a circle (special case of ellipse)
  public static Ellipse createCircle(Position center, float radius, Style style) {
    return new Ellipse(center, radius, radius, style);
  }

  public float getRadiusX() {
    return radiusX;
  }

  public float getRadiusY() {
    return radiusY;
  }

  public float getWidth() {
    return radiusX * 2;
  }

  public float getHeight() {
    return radiusY * 2;
  }

  public boolean isCircle() {
    return Math.abs(radiusX - radiusY) < 0.001f;
  }

  @Override
  public ShapeType getType() {
    return ShapeType.ELLIPSE;
  }

  @Override
  public Ellipse moveTo(Position newPosition) {
    return new Ellipse(id, newPosition, radiusX, radiusY, style, visible, layerOrder);
  }

  @Override
  public Ellipse withStyle(Style newStyle) {
    return new Ellipse(id, position, radiusX, radiusY, newStyle, visible, layerOrder);
  }

  public Ellipse withRadii(float newRadiusX, float newRadiusY) {
    return new Ellipse(id, position, newRadiusX, newRadiusY, style, visible, layerOrder);
  }

  public Ellipse scale(float scaleX, float scaleY) {
    return new Ellipse(id, position, radiusX * scaleX, radiusY * scaleY, style, visible, layerOrder);
  }

  public Ellipse scale(float scale) {
    return scale(scale, scale);
  }

  @Override
  public boolean contains(Position point) {
    // Ellipse equation: ((x-h)^2 / a^2) + ((y-k)^2 / b^2) <= 1
    // where (h,k) is center, a is radiusX, b is radiusY
    float dx = point.getX() - position.getX();
    float dy = point.getY() - position.getY();
    float normalizedX = (dx * dx) / (radiusX * radiusX);
    float normalizedY = (dy * dy) / (radiusY * radiusY);
    return (normalizedX + normalizedY) <= 1.0f;
  }

  @Override
  public BoundingBox getBounds() {
    float minX = position.getX() - radiusX;
    float minY = position.getY() - radiusY;
    return new BoundingBox(minX, minY, getWidth(), getHeight());
  }

  @Override
  protected Shape createWithProperties(String id, Position position, Style style, boolean visible,
      int layerOrder) {
    return new Ellipse(id, position, radiusX, radiusY, style, visible, layerOrder);
  }

  @Override
  protected void performDraw(Object canvas) {
    // Drawing is handled by infrastructure layer renderers
    // This method is kept for backward compatibility but should not contain logic
    throw new UnsupportedOperationException("Drawing should be handled by infrastructure layer renderers");
  }

}