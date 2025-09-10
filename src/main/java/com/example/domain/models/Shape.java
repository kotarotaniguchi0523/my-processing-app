package com.example.domain.models;

import com.example.domain.serialization.YamlSerializable;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Objects;

/**
 * Abstract base class for all shapes in the drawing system Provides common properties and behaviors
 * for geometric shapes
 */
public abstract class Shape implements YamlSerializable {
    protected final String id;
    protected final Position position;
    protected final Style style;
    protected final boolean visible;
    protected final int layerOrder;

    protected Shape(String id, Position position, Style style, boolean visible, int layerOrder) {
        this.id = Objects.requireNonNull(id, "Shape ID cannot be null");
        this.position = Objects.requireNonNull(position, "Position cannot be null");
        this.style = Objects.requireNonNull(style, "Style cannot be null");
        this.visible = visible;
        this.layerOrder = layerOrder;
    }

    protected Shape(String id, Position position, Style style) {
        this(id, position, style, true, 0);
    }

    // Abstract methods that concrete shapes must implement
    public abstract ShapeType getType();

    public abstract Shape moveTo(Position newPosition);

    public abstract Shape withStyle(Style newStyle);

    public abstract boolean contains(Position point);

    public abstract BoundingBox getBounds();

    // Template method for drawing - can be overridden by subclasses
    public void draw(Object canvas) {
        if (!visible) {
            return;
        }
        performDraw(canvas);
    }

    // Abstract method for actual drawing implementation
    protected abstract void performDraw(Object canvas);

    // Common getters
    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public Style getStyle() {
        return style;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getLayerOrder() {
        return layerOrder;
    }

    // Common transformation methods
    public Shape withVisibility(boolean visible) {
        if (this.visible == visible) {
            return this;
        }
        return createWithProperties(id, position, style, visible, layerOrder);
    }

    public Shape withLayerOrder(int layerOrder) {
        if (this.layerOrder == layerOrder) {
            return this;
        }
        return createWithProperties(id, position, style, visible, layerOrder);
    }

    public Shape withId(String newId) {
        if (Objects.equals(this.id, newId)) {
            return this;
        }
        return createWithProperties(newId, position, style, visible, layerOrder);
    }

    // Abstract method for creating new instances with different properties
    protected abstract Shape createWithProperties(
            String id, Position position, Style style, boolean visible, int layerOrder);

    // Distance calculation between two positions
    public double distanceTo(Position point) {
        float dx = position.getX() - point.getX();
        float dy = position.getY() - point.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Check if this shape overlaps with another shape's bounds
    public boolean overlaps(Shape other) {
        return getBounds().overlaps(other.getBounds());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Shape)) {
            return false;
        }
        Shape shape = (Shape) obj;
        return visible == shape.visible
                && layerOrder == shape.layerOrder
                && Objects.equals(id, shape.id)
                && Objects.equals(position, shape.position)
                && Objects.equals(style, shape.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position, style, visible, layerOrder);
    }

    @Override
    public String toString() {
        return String.format(
                "%s(id=%s, position=%s, visible=%s, layer=%d)",
                getClass().getSimpleName(), id, position, visible, layerOrder);
    }

    // Utility method for generating unique IDs
    protected static String generateId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    // Enum for shape types
    public enum ShapeType {
        CIRCLE("circle"),
        RECTANGLE("rectangle"),
        LINE("line"),
        TEXT("text"),
        POLYGON("polygon"),
        ELLIPSE("ellipse"),
        TRIANGLE("triangle"),
        CUSTOM("custom");

        private final String typeName;

        ShapeType(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    // BoundingBox inner class for shape bounds
    public static class BoundingBox {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public BoundingBox(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        public float getMinX() {
            return x;
        }

        public float getMinY() {
            return y;
        }

        public float getMaxX() {
            return x + width;
        }

        public float getMaxY() {
            return y + height;
        }

        public Position getCenter() {
            return new Position(x + width / 2, y + height / 2);
        }

        public boolean contains(Position point) {
            return point.getX() >= x
                    && point.getX() <= x + width
                    && point.getY() >= y
                    && point.getY() <= y + height;
        }

        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }

        public boolean overlaps(BoundingBox other) {
            return !(getMaxX() < other.getMinX()
                    || getMinX() > other.getMaxX()
                    || getMaxY() < other.getMinY()
                    || getMinY() > other.getMaxY());
        }

        public BoundingBox union(BoundingBox other) {
            float minX = Math.min(getMinX(), other.getMinX());
            float minY = Math.min(getMinY(), other.getMinY());
            float maxX = Math.max(getMaxX(), other.getMaxX());
            float maxY = Math.max(getMaxY(), other.getMaxY());
            return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BoundingBox)) {
                return false;
            }
            BoundingBox box = (BoundingBox) obj;
            return Float.compare(box.x, x) == 0
                    && Float.compare(box.y, y) == 0
                    && Float.compare(box.width, width) == 0
                    && Float.compare(box.height, height) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, width, height);
        }

        @Override
        public String toString() {
            return String.format(
                    "BoundingBox(x=%.2f, y=%.2f, w=%.2f, h=%.2f)", x, y, width, height);
        }
    }
}
