package com.example.domain.models;

import com.example.domain.serialization.YamlUtils;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Map;
import java.util.Objects;

/** Circle shape implementation Represents a circular shape with center position and radius */
public final class Circle extends Shape {
    private final float radius;

    public Circle(
            String id,
            Position center,
            float radius,
            Style style,
            boolean visible,
            int layerOrder) {
        super(id, center, style, visible, layerOrder);
        this.radius = Math.max(0, radius);
    }

    public Circle(String id, Position center, float radius, Style style) {
        this(id, center, radius, style, true, 0);
    }

    public Circle(Position center, float radius, Style style) {
        this(generateId("circle"), center, radius, style);
    }

    public Circle(Position center, float radius) {
        this(center, radius, Style.DEFAULT);
    }

    public float getRadius() {
        return radius;
    }

    public Position getCenter() {
        return position;
    }

    public float getDiameter() {
        return radius * 2;
    }

    @Override
    public ShapeType getType() {
        return ShapeType.CIRCLE;
    }

    @Override
    public Circle moveTo(Position newPosition) {
        return new Circle(id, newPosition, radius, style, visible, layerOrder);
    }

    @Override
    public Circle withStyle(Style newStyle) {
        return new Circle(id, position, radius, newStyle, visible, layerOrder);
    }

    @Override
    public boolean contains(Position point) {
        float dx = position.getX() - point.getX();
        float dy = position.getY() - point.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance <= radius;
    }

    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(
                position.getX() - radius, position.getY() - radius, radius * 2, radius * 2);
    }

    @Override
    protected Shape createWithProperties(
            String id, Position position, Style style, boolean visible, int layerOrder) {
        return new Circle(id, position, radius, style, visible, layerOrder);
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

            // Draw circle
            pg.ellipse(position.getX(), position.getY(), getDiameter(), getDiameter());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Circle)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Circle circle = (Circle) obj;
        return Float.compare(circle.radius, radius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), radius);
    }

    @Override
    public String toString() {
        return String.format(
                "Circle(id=%s, center=%s, radius=%.2f, visible=%s)", id, position, radius, visible);
    }

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("id", id);
        map.put("position", position.toYamlMap());
        map.put("radius", radius);
        map.put("style", style.toYamlMap());
        map.put("visible", visible);
        map.put("layerOrder", layerOrder);
        return map;
    }

    @Override
    public String getYamlType() {
        return "circle";
    }

    public static Circle fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "circle");
        YamlUtils.validateRequiredFields(yamlMap, "id", "position", "radius");

        String id = YamlUtils.getRequiredString(yamlMap, "id");
        Position position = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "position"));
        float radius = YamlUtils.getRequiredFloat(yamlMap, "radius");
        Style style = YamlUtils.getMap(yamlMap, "style", null) != null
                        ? Style.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "style"))
                        : Style.DEFAULT;
        boolean visible = YamlUtils.getBoolean(yamlMap, "visible", true);
        int layerOrder = YamlUtils.getInt(yamlMap, "layerOrder", 0);

        return new Circle(id, position, radius, style, visible, layerOrder);
    }
}
