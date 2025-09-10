package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.serialization.YamlUtils;

import java.util.Map;
import java.util.Objects;

/**
 * Triangle shape implementation 
 * Represents a triangular shape with three vertices
 */
public final class Triangle extends Shape {
    private final Position p1;
    private final Position p2;
    private final Position p3;

    public Triangle(
            String id,
            Position position,
            Position p1,
            Position p2,
            Position p3,
            Style style,
            boolean visible,
            int layerOrder) {
        super(id, position, style, visible, layerOrder);
        this.p1 = Objects.requireNonNull(p1, "Point 1 cannot be null");
        this.p2 = Objects.requireNonNull(p2, "Point 2 cannot be null");
        this.p3 = Objects.requireNonNull(p3, "Point 3 cannot be null");
    }

    public Triangle(String id, Position position, Position p1, Position p2, Position p3, Style style) {
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
        this(generateId("triangle"), basePosition,
             basePosition, 
             new Position(basePosition.getX() + width, basePosition.getY()),
             new Position(basePosition.getX() + width / 2, basePosition.getY() - height),
             style);
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

    public float getArea() {
        // Using the cross product formula: Area = 0.5 * |AB x AC|
        float dx1 = p2.getX() - p1.getX();
        float dy1 = p2.getY() - p1.getY();
        float dx2 = p3.getX() - p1.getX();
        float dy2 = p3.getY() - p1.getY();
        return Math.abs(dx1 * dy2 - dx2 * dy1) / 2.0f;
    }

    public float getPerimeter() {
        return distanceBetweenPoints(p1, p2) + 
               distanceBetweenPoints(p2, p3) + 
               distanceBetweenPoints(p3, p1);
    }

    private float distanceBetweenPoints(Position a, Position b) {
        float dx = a.getX() - b.getX();
        float dy = a.getY() - b.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
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
        float denominator = (p2.getY() - p3.getY()) * (p1.getX() - p3.getX()) + 
                           (p3.getX() - p2.getX()) * (p1.getY() - p3.getY());
        
        if (Math.abs(denominator) < 0.000001f) {
            return false; // Degenerate triangle
        }
        
        float a = ((p2.getY() - p3.getY()) * (point.getX() - p3.getX()) + 
                  (p3.getX() - p2.getX()) * (point.getY() - p3.getY())) / denominator;
        float b = ((p3.getY() - p1.getY()) * (point.getX() - p3.getX()) + 
                  (p1.getX() - p3.getX()) * (point.getY() - p3.getY())) / denominator;
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
    protected Shape createWithProperties(
            String id, Position position, Style style, boolean visible, int layerOrder) {
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
            pg.triangle(p1.getX(), p1.getY(), 
                       p2.getX(), p2.getY(), 
                       p3.getX(), p3.getY());
        }
    }

    // Check if this triangle intersects with another triangle
    public boolean intersects(Triangle other) {
        return getBounds().overlaps(other.getBounds()) && 
               (this.contains(other.p1) || this.contains(other.p2) || this.contains(other.p3) ||
                other.contains(this.p1) || other.contains(this.p2) || other.contains(this.p3));
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Triangle)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Triangle triangle = (Triangle) obj;
        return Objects.equals(p1, triangle.p1) &&
               Objects.equals(p2, triangle.p2) &&
               Objects.equals(p3, triangle.p3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), p1, p2, p3);
    }

    @Override
    public String toString() {
        return String.format(
                "Triangle(id=%s, p1=%s, p2=%s, p3=%s, visible=%s)",
                id, p1, p2, p3, visible);
    }

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("id", id);
        map.put("position", position.toYamlMap());
        map.put("p1", p1.toYamlMap());
        map.put("p2", p2.toYamlMap());
        map.put("p3", p3.toYamlMap());
        map.put("style", style.toYamlMap());
        map.put("visible", visible);
        map.put("layerOrder", layerOrder);
        return map;
    }

    @Override
    public String getYamlType() {
        return "triangle";
    }

    public static Triangle fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "triangle");
        YamlUtils.validateRequiredFields(yamlMap, "id", "position", "p1", "p2", "p3");

        String id = YamlUtils.getRequiredString(yamlMap, "id");
        Position position = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "position"));
        Position p1 = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "p1"));
        Position p2 = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "p2"));
        Position p3 = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "p3"));
        Style style = YamlUtils.getMap(yamlMap, "style", null) != null
                        ? Style.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "style"))
                        : Style.DEFAULT;
        boolean visible = YamlUtils.getBoolean(yamlMap, "visible", true);
        int layerOrder = YamlUtils.getInt(yamlMap, "layerOrder", 0);

        return new Triangle(id, position, p1, p2, p3, style, visible, layerOrder);
    }
}