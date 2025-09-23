package com.example.infrastructure.rendering;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Calculates bounding boxes for shapes and drawings.
 * Extracted from DrawingEngine to follow Single Responsibility Principle.
 * Handles:
 * - Individual shape bounds
 * - Combined bounds for multiple shapes
 * - Drawing state bounds calculation
 */
public class BoundsCalculator {

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

        // Calculate bounds from visible shapes only
        List<Shape> visibleShapes = drawingState.getShapes().stream()
            .filter(Shape::isVisible)
            .collect(Collectors.toList());

        if (visibleShapes.isEmpty()) {
            return null;
        }

        return calculateCombinedBounds(visibleShapes);
    }

    /**
     * Calculate combined bounds for a list of shapes
     *
     * @param shapes the list of shapes
     * @return the combined bounding box
     */
    public Shape.BoundingBox calculateCombinedBounds(List<Shape> shapes) {
        if (shapes == null || shapes.isEmpty()) {
            return null;
        }

        Shape.BoundingBox totalBounds = shapes.get(0).getBounds();
        for (int i = 1; i < shapes.size(); i++) {
            totalBounds = totalBounds.union(shapes.get(i).getBounds());
        }

        return totalBounds;
    }

}