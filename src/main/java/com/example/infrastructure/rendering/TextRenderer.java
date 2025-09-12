package com.example.infrastructure.rendering;

import com.example.domain.models.Text;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Renderer for text shapes with advanced alignment and typography support.
 * Handles text rendering with proper font sizing and alignment.
 */
public class TextRenderer extends ShapeRenderer {

    /**
     * Creates a new TextRenderer with the specified graphics context
     *
     * @param canvas the PGraphics context to render to
     */
    public TextRenderer(PGraphics canvas) {
        super(canvas);
    }

    /**
     * Renders a text shape to the canvas with proper alignment and sizing
     *
     * @param text the text to render
     * @throws RenderingException if rendering fails
     */
    public void renderText(Text text) throws RenderingException {
        Objects.requireNonNull(text, "Text cannot be null");

        try {
            applyStyle(text.getStyle());

            // Apply text-specific styling
            canvas.textSize(text.getFontSize());

            // Set horizontal alignment
            int hAlign = convertHorizontalAlignment(text.getHorizontalAlign());
            int vAlign = convertVerticalAlignment(text.getVerticalAlign());

            canvas.textAlign(hAlign, vAlign);
            canvas.text(text.getContent(), text.getPosition().getX(), text.getPosition().getY());

        } catch (Exception e) {
            throw new RenderingException("Failed to render text: " + text.getId(), e);
        }
    }

    /**
     * Converts domain horizontal alignment to Processing alignment constant
     *
     * @param align the domain horizontal alignment
     * @return the Processing alignment constant
     */
    private int convertHorizontalAlignment(com.example.domain.models.Text.TextAlign align) {
        switch (align) {
            case CENTER:
                return processing.core.PConstants.CENTER;
            case RIGHT:
                return processing.core.PConstants.RIGHT;
            case LEFT:
            default:
                return processing.core.PConstants.LEFT;
        }
    }

    /**
     * Converts domain vertical alignment to Processing alignment constant
     *
     * @param align the domain vertical alignment
     * @return the Processing alignment constant
     */
    private int convertVerticalAlignment(com.example.domain.models.Text.TextAlign align) {
        switch (align) {
            case CENTER:
                return processing.core.PConstants.CENTER;
            case BOTTOM:
                return processing.core.PConstants.BOTTOM;
            case TOP:
            default:
                return processing.core.PConstants.TOP;
        }
    }
}