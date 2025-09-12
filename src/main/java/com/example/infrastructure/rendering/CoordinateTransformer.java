package com.example.infrastructure.rendering;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.values.Position;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for coordinate transformations and spatial calculations.
 * Handles bounding box calculations, coordinate space transformations,
 * and spatial query operations on shapes.
 */
public class CoordinateTransformer {

    /**
     * Gets the bounding box of all visible shapes in a drawing state
     *
     * @param drawingState the drawing state containing shapes
     * @return the bounding box containing all visible shapes, or null if no visible shapes
     */
    public static Shape.BoundingBox getDrawingBounds(DrawingState drawingState) {
        Objects.requireNonNull(drawingState, "DrawingState cannot be null");

        List<Shape> visibleShapes = drawingState.getShapes().stream()
                .filter(Shape::isVisible)
                .collect(Collectors.toList());

        return calculateBounds(visibleShapes);
    }

    /**
     * Gets the bounding box of a list of shapes
     *
     * @param shapes the list of shapes
     * @return the bounding box containing all shapes, or null if list is empty
     */
    public static Shape.BoundingBox calculateBounds(List<Shape> shapes) {
        Objects.requireNonNull(shapes, "Shapes list cannot be null");

        if (shapes.isEmpty()) {
            return null;
        }

        Shape.BoundingBox totalBounds = shapes.get(0).getBounds();
        for (int i = 1; i < shapes.size(); i++) {
            totalBounds = totalBounds.union(shapes.get(i).getBounds());
        }

        return totalBounds;
    }

    /**
     * Translates a position by the given offset
     *
     * @param position the original position
     * @param offsetX the X offset
     * @param offsetY the Y offset
     * @return the translated position
     */
    public static Position translate(Position position, float offsetX, float offsetY) {
        Objects.requireNonNull(position, "Position cannot be null");
        return new Position(position.getX() + offsetX, position.getY() + offsetY);
    }

    /**
     * Scales a position around the origin
     *
     * @param position the original position
     * @param scaleX the X scale factor
     * @param scaleY the Y scale factor
     * @return the scaled position
     */
    public static Position scale(Position position, float scaleX, float scaleY) {
        Objects.requireNonNull(position, "Position cannot be null");
        return new Position(position.getX() * scaleX, position.getY() * scaleY);
    }

    /**
     * Calculates the distance between two positions
     *
     * @param pos1 the first position
     * @param pos2 the second position
     * @return the distance between the positions
     */
    public static float distance(Position pos1, Position pos2) {
        Objects.requireNonNull(pos1, "Position 1 cannot be null");
        Objects.requireNonNull(pos2, "Position 2 cannot be null");

        float dx = pos2.getX() - pos1.getX();
        float dy = pos2.getY() - pos1.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}