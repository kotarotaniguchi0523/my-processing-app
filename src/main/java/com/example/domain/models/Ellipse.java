package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.serialization.YamlUtils;

import java.util.Map;

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

      // Draw ellipse using Processing's ellipse function
      // ellipse(x, y, width, height)
      pg.ellipse(position.getX(), position.getY(), getWidth(), getHeight());
    }
  }

  // YAML Serialization
  @Override
  public Map<String, Object> toYamlMap() {
    Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
    map.put("id", id);
    map.put("center", position.toYamlMap());
    map.put("radiusX", radiusX);
    map.put("radiusY", radiusY);
    map.put("style", style.toYamlMap());
    map.put("visible", visible);
    map.put("layerOrder", layerOrder);
    return map;
  }

  @Override
  public String getYamlType() {
    return "ellipse";
  }

  public static Ellipse fromYamlMap(Map<String, Object> yamlMap) {
    YamlUtils.validateYamlType(yamlMap, "ellipse");
    YamlUtils.validateRequiredFields(yamlMap, "id", "center", "radiusX", "radiusY");

    String id = YamlUtils.getRequiredString(yamlMap, "id");
    Position center = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "center"));
    float radiusX = YamlUtils.getRequiredFloat(yamlMap, "radiusX");
    float radiusY = YamlUtils.getRequiredFloat(yamlMap, "radiusY");
    Style style = YamlUtils.getMap(yamlMap, "style", null) != null
        ? Style.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "style"))
        : Style.DEFAULT;
    boolean visible = YamlUtils.getBoolean(yamlMap, "visible", true);
    int layerOrder = YamlUtils.getInt(yamlMap, "layerOrder", 0);

    return new Ellipse(id, center, radiusX, radiusY, style, visible, layerOrder);
  }
}