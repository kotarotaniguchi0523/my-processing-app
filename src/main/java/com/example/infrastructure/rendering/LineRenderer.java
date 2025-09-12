package com.example.infrastructure.rendering;

import com.example.domain.models.Line;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Renderer for line shapes and linear elements.
 * Handles line geometry rendering with proper style application.
 */
public class LineRenderer extends ShapeRenderer {

    /**
     * Creates a new LineRenderer with the specified graphics context
     *
     * @param canvas the PGraphics context to render to
     */
    public LineRenderer(PGraphics canvas) {
        super(canvas);
    }

    /**
     * Renders a line shape to the canvas
     *
     * @param line the line to render
     * @throws RenderingException if rendering fails
     */
    public void renderLine(Line line) throws RenderingException {
        Objects.requireNonNull(line, "Line cannot be null");

        try {
            applyStyle(line.getStyle());

            canvas.line(
                    line.getStartPosition().getX(),
                    line.getStartPosition().getY(),
                    line.getEndPosition().getX(),
                    line.getEndPosition().getY());

        } catch (Exception e) {
            throw new RenderingException("Failed to render line: " + line.getId(), e);
        }
    }
}