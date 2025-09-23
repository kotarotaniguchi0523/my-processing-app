package com.example.infrastructure.rendering;

import com.example.domain.models.*;
import com.example.domain.models.Shape;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Dispatches shape rendering to type-specific renderers.
 * Extracted from DrawingEngine to follow Single Responsibility Principle.
 * Handles:
 * - Type checking and casting
 * - Renderer delegation
 * - Error handling for unsupported shapes
 */
public class ShapeRenderingDispatcher {

    private CircleRenderer circleRenderer;
    private RectangleRenderer rectangleRenderer;
    private LineRenderer lineRenderer;
    private TextRenderer textRenderer;

    public ShapeRenderingDispatcher(PGraphics canvas) {
        this.circleRenderer = new CircleRenderer(canvas);
        this.rectangleRenderer = new RectangleRenderer(canvas);
        this.lineRenderer = new LineRenderer(canvas);
        this.textRenderer = new TextRenderer(canvas);
    }

    /**
     * Update the canvas for all renderers
     */
    public void updateCanvas(PGraphics canvas) {
        this.circleRenderer = new CircleRenderer(canvas);
        this.rectangleRenderer = new RectangleRenderer(canvas);
        this.lineRenderer = new LineRenderer(canvas);
        this.textRenderer = new TextRenderer(canvas);
    }

    /**
     * Renders a shape by dispatching to type-specific methods
     *
     * @param shape the shape to render
     * @throws DrawingEngine.DrawingException if shape type is not supported
     */
    public void renderShapeByType(Shape shape) throws DrawingEngine.DrawingException {
        Objects.requireNonNull(shape, "Shape cannot be null");

        if (!shape.isVisible()) {
            return;
        }

        try {
            switch (shape.getType()) {
                case CIRCLE:
                    if (shape instanceof Circle) {
                        circleRenderer.renderCircle((Circle) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Circle but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                case RECTANGLE:
                    if (shape instanceof Rectangle) {
                        rectangleRenderer.renderRectangle((Rectangle) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Rectangle but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                case LINE:
                    if (shape instanceof Line) {
                        lineRenderer.renderLine((Line) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Line but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                case TEXT:
                    if (shape instanceof Text) {
                        textRenderer.renderText((Text) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Text but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                case TRIANGLE:
                    if (shape instanceof Triangle) {
                        rectangleRenderer.renderTriangle((Triangle) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Triangle but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                case ELLIPSE:
                    if (shape instanceof Ellipse) {
                        circleRenderer.renderEllipse((Ellipse) shape);
                    } else {
                        throw new DrawingEngine.DrawingException(
                            "Shape type mismatch: expected Ellipse but got " +
                            shape.getClass().getSimpleName());
                    }
                    break;

                default:
                    throw new DrawingEngine.DrawingException(
                        "Unsupported shape type: " + shape.getType());
            }
        } catch (ShapeRenderer.RenderingException e) {
            throw new DrawingEngine.DrawingException(e.getMessage(), e);
        }
    }
}