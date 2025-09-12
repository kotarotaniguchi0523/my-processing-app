package com.example.infrastructure.rendering;

import com.example.domain.models.Rectangle;
import com.example.domain.models.Triangle;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Renderer for rectangular and triangular shapes.
 * Handles polygon geometry rendering with proper style application.
 */
public class RectangleRenderer extends ShapeRenderer {

    /**
     * Creates a new RectangleRenderer with the specified graphics context
     *
     * @param canvas the PGraphics context to render to
     */
    public RectangleRenderer(PGraphics canvas) {
        super(canvas);
    }

    /**
     * Renders a rectangle shape to the canvas
     *
     * @param rectangle the rectangle to render
     * @throws RenderingException if rendering fails
     */
    public void renderRectangle(Rectangle rectangle) throws RenderingException {
        Objects.requireNonNull(rectangle, "Rectangle cannot be null");

        try {
            applyStyle(rectangle.getStyle());

            canvas.rect(
                    rectangle.getPosition().getX(),
                    rectangle.getPosition().getY(),
                    rectangle.getWidth(),
                    rectangle.getHeight());

        } catch (Exception e) {
            throw new RenderingException("Failed to render rectangle: " + rectangle.getId(), e);
        }
    }

    /**
     * Renders a triangle shape to the canvas
     *
     * @param triangle the triangle to render
     * @throws RenderingException if rendering fails
     */
    public void renderTriangle(Triangle triangle) throws RenderingException {
        Objects.requireNonNull(triangle, "Triangle cannot be null");

        try {
            applyStyle(triangle.getStyle());

            canvas.triangle(
                    triangle.getP1().getX(),
                    triangle.getP1().getY(),
                    triangle.getP2().getX(),
                    triangle.getP2().getY(),
                    triangle.getP3().getX(),
                    triangle.getP3().getY());

        } catch (Exception e) {
            throw new RenderingException("Failed to render triangle: " + triangle.getId(), e);
        }
    }
}