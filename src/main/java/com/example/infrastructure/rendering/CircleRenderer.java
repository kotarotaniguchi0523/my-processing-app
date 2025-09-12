package com.example.infrastructure.rendering;

import com.example.domain.models.Circle;
import com.example.domain.models.Ellipse;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Renderer for circular shapes including circles and ellipses.
 * Handles all circular geometry rendering with proper style application.
 */
public class CircleRenderer extends ShapeRenderer {

    /**
     * Creates a new CircleRenderer with the specified graphics context
     *
     * @param canvas the PGraphics context to render to
     */
    public CircleRenderer(PGraphics canvas) {
        super(canvas);
    }

    /**
     * Renders a circle shape to the canvas
     *
     * @param circle the circle to render
     * @throws RenderingException if rendering fails
     */
    public void renderCircle(Circle circle) throws RenderingException {
        Objects.requireNonNull(circle, "Circle cannot be null");

        try {
            applyStyle(circle.getStyle());

            canvas.ellipse(
                    circle.getPosition().getX(),
                    circle.getPosition().getY(),
                    circle.getDiameter(),
                    circle.getDiameter());

        } catch (Exception e) {
            throw new RenderingException("Failed to render circle: " + circle.getId(), e);
        }
    }

    /**
     * Renders an ellipse shape to the canvas
     *
     * @param ellipse the ellipse to render
     * @throws RenderingException if rendering fails
     */
    public void renderEllipse(Ellipse ellipse) throws RenderingException {
        Objects.requireNonNull(ellipse, "Ellipse cannot be null");

        try {
            applyStyle(ellipse.getStyle());

            canvas.ellipse(
                    ellipse.getPosition().getX(),
                    ellipse.getPosition().getY(),
                    ellipse.getWidth(),
                    ellipse.getHeight());

        } catch (Exception e) {
            throw new RenderingException("Failed to render ellipse: " + ellipse.getId(), e);
        }
    }
}