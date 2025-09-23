package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Objects;

/**
 * Rectangle shape implementation Represents a rectangular shape with position, width, and height
 */
public final class Rectangle extends Shape {
    private final float width;
    private final float height;

    public Rectangle(
            String id,
            Position position,
            float width,
            float height,
            Style style,
            boolean visible,
            int layerOrder) {
        super(id, position, style, visible, layerOrder);
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public Rectangle(String id, Position position, float width, float height, Style style) {
        this(id, position, width, height, style, true, 0);
    }

    public Rectangle(Position position, float width, float height, Style style) {
        this(generateId("rectangle"), position, width, height, style);
    }

    public Rectangle(Position position, float width, float height) {
        this(position, width, height, Style.DEFAULT);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Position getCenter() {
        return new Position(position.getX() + width / 2, position.getY() + height / 2);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.RECTANGLE;
    }

    @Override
    public Rectangle moveTo(Position newPosition) {
        return new Rectangle(id, newPosition, width, height, style, visible, layerOrder);
    }

    @Override
    public Rectangle withStyle(Style newStyle) {
        return new Rectangle(id, position, width, height, newStyle, visible, layerOrder);
    }

    @Override
    public boolean contains(Position point) {
        return point.getX() >= position.getX()
                && point.getX() <= position.getX() + width
                && point.getY() >= position.getY()
                && point.getY() <= position.getY() + height;
    }

    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(position.getX(), position.getY(), width, height);
    }

    @Override
    protected Shape createWithProperties(
            String id, Position position, Style style, boolean visible, int layerOrder) {
        return new Rectangle(id, position, width, height, style, visible, layerOrder);
    }

    @Override
    protected void performDraw(Object canvas) {
        // Drawing is handled by infrastructure layer renderers (RectangleRenderer)
        // This method is kept for backward compatibility but should not contain logic
        throw new UnsupportedOperationException("Drawing should be handled by RectangleRenderer in infrastructure layer");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rectangle)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Rectangle rectangle = (Rectangle) obj;
        return Float.compare(rectangle.width, width) == 0
                && Float.compare(rectangle.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), width, height);
    }

    @Override
    public String toString() {
        return String.format(
                "Rectangle(id=%s, position=%s, size=%.2fx%.2f, visible=%s)",
                id, position, width, height, visible);
    }

}
