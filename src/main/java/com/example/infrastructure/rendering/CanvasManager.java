package com.example.infrastructure.rendering;

import com.example.domain.models.DrawingState;
import com.example.domain.values.Color;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Manages canvas settings and operations.
 * Extracted from DrawingEngine to follow Single Responsibility Principle.
 * Handles:
 * - Canvas configuration and settings
 * - Background clearing operations
 * - Window title management
 */
public class CanvasManager {

    private static final Logger LOGGER = Logger.getLogger(CanvasManager.class.getName());

    private final PApplet sketch;
    private PGraphics canvas;

    public CanvasManager(PApplet sketch, PGraphics canvas) {
        this.sketch = Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
        this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
    }

    /**
     * Update the graphics context
     */
    public void setCanvas(PGraphics canvas) {
        this.canvas = Objects.requireNonNull(canvas, "PGraphics canvas cannot be null");
    }


    /**
     * Applies canvas settings from DrawingState
     *
     * @param drawingState the state containing canvas settings
     */
    public void applyCanvasSettings(DrawingState drawingState) {
        Objects.requireNonNull(drawingState, "DrawingState cannot be null");

        // Apply canvas size if specified (for validation)
        int width = drawingState.getIntSetting("width", canvas.width);
        int height = drawingState.getIntSetting("height", canvas.height);

        // Log if there's a mismatch (canvas size is immutable after setup)
        if (width != canvas.width || height != canvas.height) {
            LOGGER.fine(String.format(
                "Canvas size mismatch - requested: %dx%d, actual: %dx%d",
                width, height, canvas.width, canvas.height
            ));
        }

        // Apply window title if available
        String title = drawingState.getStringSetting("title", null);
        if (title != null) {
            updateWindowTitle(title);
        }
    }

    /**
     * Update the window title
     */
    private void updateWindowTitle(String title) {
        try {
            if (sketch != null && sketch.getSurface() != null) {
                sketch.getSurface().setTitle(title);
                LOGGER.fine("Window title updated to: " + title);
            }
        } catch (Exception e) {
            // No display surface (e.g., headless tests) - safely ignore
            LOGGER.finest("Could not update window title (no display surface): " + e.getMessage());
        }
    }

    /**
     * Clears the canvas with the background color from DrawingState
     *
     * @param drawingState the state containing background color
     */
    public void clearCanvas(DrawingState drawingState) {
        Objects.requireNonNull(drawingState, "DrawingState cannot be null");

        String backgroundColorHex = drawingState.getStringSetting("backgroundColor", "#FFFFFF");
        clearCanvasWithHexColor(backgroundColorHex);
    }

    /**
     * Clear canvas with a hex color string
     */
    private void clearCanvasWithHexColor(String hexColor) {
        try {
            Color backgroundColor = Color.fromHex(hexColor);
            clearCanvas(backgroundColor);
        } catch (Exception e) {
            // Fallback to white background if color parsing fails
            LOGGER.fine("Failed to parse background color '" + hexColor +
                       "', using white: " + e.getMessage());
            canvas.background(255, 255, 255);
        }
    }

    /**
     * Clears the entire canvas with specified color
     *
     * @param color the background color
     */
    public void clearCanvas(Color color) {
        Objects.requireNonNull(color, "Color cannot be null");
        canvas.background(
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            color.getAlpha()
        );
    }
}