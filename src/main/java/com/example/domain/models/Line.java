package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;
import java.util.Map;
import java.util.Objects;

/** Line shape implementation Represents a line segment from one point to another */
public final class Line extends Shape {
  private final Position endPosition;

  public Line(
      String id,
      Position startPosition,
      Position endPosition,
      Style style,
      boolean visible,
      int layerOrder) {
    super(id, startPosition, style, visible, layerOrder);
    this.endPosition = Objects.requireNonNull(endPosition, "End position cannot be null");
  }

  public Line(String id, Position startPosition, Position endPosition, Style style) {
    this(id, startPosition, endPosition, style, true, 0);
  }

  public Line(Position startPosition, Position endPosition, Style style) {
    this(generateId("line"), startPosition, endPosition, style);
  }

  public Line(Position startPosition, Position endPosition) {
    this(startPosition, endPosition, Style.NO_FILL);
  }

  public Position getStartPosition() {
    return position;
  }

  public Position getEndPosition() {
    return endPosition;
  }

  public float getLength() {
    float dx = endPosition.getX() - position.getX();
    float dy = endPosition.getY() - position.getY();
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  public Position getMidpoint() {
    return new Position(
        (position.getX() + endPosition.getX()) / 2, (position.getY() + endPosition.getY()) / 2);
  }

  public float getSlope() {
    float dx = endPosition.getX() - position.getX();
    if (Math.abs(dx) < 1e-6) {
      return Float.POSITIVE_INFINITY; // Vertical line
    }
    float dy = endPosition.getY() - position.getY();
    return dy / dx;
  }

  public float getAngle() {
    float dx = endPosition.getX() - position.getX();
    float dy = endPosition.getY() - position.getY();
    return (float) Math.atan2(dy, dx);
  }

  public boolean isHorizontal() {
    return Math.abs(endPosition.getY() - position.getY()) < 1e-6;
  }

  public boolean isVertical() {
    return Math.abs(endPosition.getX() - position.getX()) < 1e-6;
  }

  @Override
  public ShapeType getType() {
    return ShapeType.LINE;
  }

  @Override
  public Line moveTo(Position newPosition) {
    Position offset = newPosition.subtract(position);
    Position newEndPosition = endPosition.add(offset);
    return new Line(id, newPosition, newEndPosition, style, visible, layerOrder);
  }

  @Override
  public Line withStyle(Style newStyle) {
    return new Line(id, position, endPosition, newStyle, visible, layerOrder);
  }

  public Line withEndPosition(Position newEndPosition) {
    return new Line(id, position, newEndPosition, style, visible, layerOrder);
  }

  public Line withPositions(Position newStartPosition, Position newEndPosition) {
    return new Line(id, newStartPosition, newEndPosition, style, visible, layerOrder);
  }

  @Override
  public boolean contains(Position point) {
    // For lines, we check if the point is very close to the line segment
    return distanceToPoint(point) <= Math.max(1.0, style.getStrokeWeight() / 2.0);
  }

  // Calculate the shortest distance from a point to this line segment
  public double distanceToPoint(Position point) {
    float x0 = point.getX();
    float y0 = point.getY();
    float x1 = position.getX();
    float y1 = position.getY();
    float x2 = endPosition.getX();
    float y2 = endPosition.getY();

    float dx = x2 - x1;
    float dy = y2 - y1;

    if (Math.abs(dx) < 1e-6 && Math.abs(dy) < 1e-6) {
      // Line is a point
      return Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));
    }

    float t = ((x0 - x1) * dx + (y0 - y1) * dy) / (dx * dx + dy * dy);
    t = Math.max(0, Math.min(1, t)); // Clamp t to [0, 1] for line segment

    float closestX = x1 + t * dx;
    float closestY = y1 + t * dy;

    return Math.sqrt((x0 - closestX) * (x0 - closestX) + (y0 - closestY) * (y0 - closestY));
  }

  @Override
  public BoundingBox getBounds() {
    float minX = Math.min(position.getX(), endPosition.getX());
    float maxX = Math.max(position.getX(), endPosition.getX());
    float minY = Math.min(position.getY(), endPosition.getY());
    float maxY = Math.max(position.getY(), endPosition.getY());

    // Add stroke weight to bounds
    float strokePadding = style.hasStroke() ? style.getStrokeWeight() / 2 : 0;

    return new BoundingBox(
        minX - strokePadding,
        minY - strokePadding,
        (maxX - minX) + 2 * strokePadding,
        (maxY - minY) + 2 * strokePadding);
  }

  @Override
  protected Shape createWithProperties(
      String id, Position position, Style style, boolean visible, int layerOrder) {
    return new Line(id, position, endPosition, style, visible, layerOrder);
  }

  @Override
  protected void performDraw(Object canvas) {
    // This would be implemented based on the specific rendering system
    // For now, it's a placeholder that would work with Processing
    if (canvas instanceof processing.core.PGraphics) {
      processing.core.PGraphics pg = (processing.core.PGraphics) canvas;

      // Apply style (lines typically don't have fill)
      if (style.hasStroke()) {
        pg.stroke(style.getStrokeColor().toProcessingColor());
        pg.strokeWeight(style.getStrokeWeight());
      } else {
        pg.noStroke();
      }

      // Disable fill for lines
      pg.noFill();

      // Draw line
      pg.line(position.getX(), position.getY(), endPosition.getX(), endPosition.getY());
    }
  }

  // Check if this line intersects with another line
  public boolean intersects(Line other) {
    return getIntersectionPoint(other) != null;
  }

  // Get the intersection point with another line (null if no intersection)
  public Position getIntersectionPoint(Line other) {
    float x1 = position.getX();
    float y1 = position.getY();
    float x2 = endPosition.getX();
    float y2 = endPosition.getY();

    float x3 = other.position.getX();
    float y3 = other.position.getY();
    float x4 = other.endPosition.getX();
    float y4 = other.endPosition.getY();

    float denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
    if (Math.abs(denom) < 1e-10) {
      return null; // Lines are parallel
    }

    float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
    float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;

    // Check if intersection is within both line segments
    if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
      return new Position(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
    }

    return null; // No intersection within segments
  }

  // Get a point along the line at parameter t (0 = start, 1 = end)
  public Position getPointAt(float t) {
    t = Math.max(0, Math.min(1, t)); // Clamp to [0, 1]
    return new Position(
        position.getX() + t * (endPosition.getX() - position.getX()),
        position.getY() + t * (endPosition.getY() - position.getY()));
  }

  // Extend the line by a certain length from both ends
  public Line extend(float startExtension, float endExtension) {
    float length = getLength();
    if (length < 1e-6) {
      return this; // Can't extend a point
    }

    float dx = (endPosition.getX() - position.getX()) / length;
    float dy = (endPosition.getY() - position.getY()) / length;

    Position newStart =
        new Position(position.getX() - dx * startExtension, position.getY() - dy * startExtension);
    Position newEnd =
        new Position(
            endPosition.getX() + dx * endExtension, endPosition.getY() + dy * endExtension);

    return new Line(id, newStart, newEnd, style, visible, layerOrder);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Line)) return false;
    if (!super.equals(obj)) return false;
    Line line = (Line) obj;
    return Objects.equals(endPosition, line.endPosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), endPosition);
  }

  @Override
  public String toString() {
    return String.format(
        "Line(id=%s, start=%s, end=%s, length=%.2f, visible=%s)",
        id, position, endPosition, getLength(), visible);
  }

  // YAML Serialization
  @Override
  public Map<String, Object> toYamlMap() {
    Map<String, Object> map =
        com.example.domain.serialization.YamlUtils.createYamlMap(getYamlType());
    map.put("id", id);
    map.put("startPosition", position.toYamlMap());
    map.put("endPosition", endPosition.toYamlMap());
    map.put("style", style.toYamlMap());
    map.put("visible", visible);
    map.put("layerOrder", layerOrder);
    return map;
  }

  @Override
  public String getYamlType() {
    return "line";
  }

  public static Line fromYamlMap(Map<String, Object> yamlMap) {
    com.example.domain.serialization.YamlUtils.validateYamlType(yamlMap, "line");
    com.example.domain.serialization.YamlUtils.validateRequiredFields(
        yamlMap, "id", "startPosition", "endPosition");

    String id = com.example.domain.serialization.YamlUtils.getRequiredString(yamlMap, "id");
    Position startPosition =
        Position.fromYamlMap(
            com.example.domain.serialization.YamlUtils.getRequiredMap(yamlMap, "startPosition"));
    Position endPosition =
        Position.fromYamlMap(
            com.example.domain.serialization.YamlUtils.getRequiredMap(yamlMap, "endPosition"));
    Style style =
        com.example.domain.serialization.YamlUtils.getMap(yamlMap, "style", null) != null
            ? Style.fromYamlMap(
                com.example.domain.serialization.YamlUtils.getRequiredMap(yamlMap, "style"))
            : Style.NO_FILL;
    boolean visible =
        com.example.domain.serialization.YamlUtils.getBoolean(yamlMap, "visible", true);
    int layerOrder = com.example.domain.serialization.YamlUtils.getInt(yamlMap, "layerOrder", 0);

    return new Line(id, startPosition, endPosition, style, visible, layerOrder);
  }
}
