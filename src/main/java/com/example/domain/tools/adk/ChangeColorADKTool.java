package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for changing colors of existing shapes.
 * Directly manipulates DrawingState without UseCase layer.
 */
public class ChangeColorADKTool implements ADKTool {

    private static final Logger LOGGER = Logger.getLogger(ChangeColorADKTool.class.getName());

    @Override
    public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
        try {
            LOGGER.info("Executing change_color with parameters: " + parameters);

            // Extract shape ID using ParameterExtractor
            String shapeId = getString(parameters, "shapeId");
            if (shapeId == null || shapeId.trim().isEmpty()) {
                return ToolResult.failure(state, "Shape ID is required");
            }

            // Extract new color using ParameterExtractor
            String newColor = getString(parameters, "newColor");
            if (newColor == null || newColor.trim().isEmpty()) {
                return ToolResult.failure(state, "New color is required");
            }

            // Find the shape
            Shape targetShape = null;
            List<Shape> shapes = state.getShapes();
            for (Shape shape : shapes) {
                if (shape.getId().equals(shapeId)) {
                    targetShape = shape;
                    break;
                }
            }

            if (targetShape == null) {
                return ToolResult.failure(state, "Shape with ID '" + shapeId + "' not found");
            }

            // Parse the color using ParameterExtractor
            Color color = newColor.startsWith("#") ? parseColor(newColor) : getColorByName(newColor);
            if (color == null) {
                return ToolResult.failure(state, "Invalid color format: " + newColor);
            }

            // Create new shape with updated color
            Style oldStyle = targetShape.getStyle();
            Style newStyle = Style.builder()
                .fillColor(color)
                .strokeColor(oldStyle.getStrokeColor())
                .strokeWeight(oldStyle.getStrokeWeight())
                .hasFill(oldStyle.hasFill())
                .hasStroke(oldStyle.hasStroke())
                .build();

            Shape updatedShape = targetShape.withStyle(newStyle);

            // Create new state with updated shape
            List<Shape> newShapes = new ArrayList<>();
            for (Shape shape : shapes) {
                if (shape.getId().equals(shapeId)) {
                    newShapes.add(updatedShape);
                } else {
                    newShapes.add(shape);
                }
            }

            DrawingState newState = state.withShapes(newShapes);

            String message = String.format("Changed color of shape '%s' to %s", shapeId, newColor);
            return ToolResult.success(newState, shapeId, message);

        } catch (Exception e) {
            LOGGER.severe("Error changing color: " + e.getMessage());
            return ToolResult.failure(state, "Failed to change color: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
            "name", "changeColor",
            "description", "Change the color of an existing shape",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "shapeId", Map.of("type", "string", "description", "ID of the shape to change"),
                    "newColor", Map.of("type", "string", "description", "New color (hex, rgb, or name)")
                ),
                "required", java.util.List.of("shapeId", "newColor")
            )
        );
    }
}