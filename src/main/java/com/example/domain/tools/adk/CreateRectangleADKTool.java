package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Rectangle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating rectangles.
 * Directly manipulates DrawingState without UseCase layer.
 */
public class CreateRectangleADKTool implements ADKTool {

    private static final Logger LOGGER = Logger.getLogger(CreateRectangleADKTool.class.getName());

    @Override
    public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
        try {
            LOGGER.info("Executing create_rectangle with parameters: " + parameters);

            // Extract and validate x, y coordinates using ParameterExtractor
            Number x = getNumber(parameters, "x");
            Number y = getNumber(parameters, "y");
            if (x == null || y == null) {
                return ToolResult.failure(state, "Position x and y coordinates are required");
            }

            // Extract and validate dimensions using ParameterExtractor
            Number width = getNumber(parameters, "width");
            Number height = getNumber(parameters, "height");
            if (width == null || height == null) {
                return ToolResult.failure(state, "Width and height are required");
            }

            // Validate dimensions are positive
            if (width.doubleValue() <= 0 || height.doubleValue() <= 0) {
                return ToolResult.failure(state, "Width and height must be positive");
            }

            // Create position
            Position position = new Position(x.floatValue(), y.floatValue());

            // Build style using ParameterExtractor
            Style style = buildStyleFromParameters(
                parameters,
                Color.fromHex("#64C864"), // Default green fill
                Color.BLACK,               // Default black stroke
                2.0f,                      // Default stroke weight
                true                       // Default filled
            ).build();

            // Generate unique shape ID
            String shapeId = "rect_" + UUID.randomUUID().toString().substring(0, 8);

            // Create the rectangle
            Rectangle rectangle = new Rectangle(
                shapeId,
                position,
                width.floatValue(),
                height.floatValue(),
                style
            );

            // Add rectangle to state
            DrawingState newState = state.withAddedShape(rectangle);

            String message = String.format("Created rectangle at (%.1f, %.1f) with size %.1fx%.1f",
                x.floatValue(), y.floatValue(), width.floatValue(), height.floatValue());

            return ToolResult.success(newState, shapeId, message);

        } catch (Exception e) {
            LOGGER.severe("Error creating rectangle: " + e.getMessage());
            return ToolResult.failure(state, "Failed to create rectangle: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
            "name", "createRectangle",
            "description", "Create a rectangle shape on the canvas",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "x", Map.of("type", "number", "description", "X coordinate"),
                    "y", Map.of("type", "number", "description", "Y coordinate"),
                    "width", Map.of("type", "number", "description", "Rectangle width"),
                    "height", Map.of("type", "number", "description", "Rectangle height"),
                    "color", Map.of("type", "string", "description", "Fill color (hex, rgb, or name)"),
                    "strokeColor", Map.of("type", "string", "description", "Stroke color (optional)"),
                    "strokeWeight", Map.of("type", "number", "description", "Stroke width (optional)"),
                    "filled", Map.of("type", "boolean", "description", "Whether to fill the rectangle")
                ),
                "required", java.util.List.of("x", "y", "width", "height")
            )
        );
    }
}