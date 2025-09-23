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
        // Null safety checks
        if (parameters == null) {
            return ToolResult.failure(state, "Parameters cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("DrawingState cannot be null");
        }

        try {
            LOGGER.info("Executing create_rectangle with parameters: " + parameters);

            // Validate required parameters - support both flat and nested position formats
            boolean hasPosition = hasRequiredParameters(parameters, "x", "y") ||
                                (parameters.containsKey("position") && getMap(parameters, "position") != null);
            if (!hasPosition || !hasRequiredParameters(parameters, "width", "height")) {
                return ToolResult.failure(state, "Missing required parameters: position (x,y or position:{x,y}) and width, height");
            }

            // Extract coordinates - support both flat and nested position formats
            Number x = getNumber(parameters, "x");
            Number y = getNumber(parameters, "y");

            // If flat parameters not found, try nested position object
            if (x == null || y == null) {
                Map<String, Object> position = getMap(parameters, "position");
                if (position != null) {
                    x = getNumber(position, "x");
                    y = getNumber(position, "y");
                }
            }

            if (x == null || y == null) {
                return ToolResult.failure(state, "Position x and y coordinates are required (either as flat x,y or position:{x,y})");
            }

            // Extract and validate dimensions using ParameterExtractor
            Number width = getNumber(parameters, "width");
            Number height = getNumber(parameters, "height");
            if (width == null || height == null) {
                return ToolResult.failure(state, "Width and height are required and must be valid numbers");
            }

            // Validate dimensions are positive
            if (width.doubleValue() <= 0 || height.doubleValue() <= 0) {
                return ToolResult.failure(state, "Width and height must be positive values, got width=" + width + ", height=" + height);
            }

            // Validate coordinates are within reasonable bounds
            if (Math.abs(x.doubleValue()) > 10000 || Math.abs(y.doubleValue()) > 10000) {
                return ToolResult.failure(state, "Coordinates are outside reasonable bounds (±10000)");
            }

            // Validate dimensions are within reasonable bounds
            if (width.doubleValue() > 5000 || height.doubleValue() > 5000) {
                return ToolResult.failure(state, "Dimensions are too large (max 5000 pixels)");
            }

            // Create position
            Position position = new Position(x.floatValue(), y.floatValue());

            // Build style using ParameterExtractor with defensive programming
            Style style;
            try {
                Style.Builder styleBuilder = buildStyleFromParameters(
                    parameters,
                    Color.fromHex("#64C864"), // Default green fill
                    Color.BLACK,               // Default black stroke
                    2.0f,                      // Default stroke weight
                    true                       // Default filled
                );

                if (styleBuilder == null) {
                    return ToolResult.failure(state, "Failed to build style from parameters");
                }

                style = styleBuilder.build();
                if (style == null) {
                    return ToolResult.failure(state, "Failed to create style object");
                }
            } catch (Exception e) {
                LOGGER.warning("Error building style: " + e.getMessage());
                return ToolResult.failure(state, "Invalid style parameters: " + e.getMessage());
            }

            // Generate unique shape ID
            String shapeId = "rect_" + UUID.randomUUID().toString().substring(0, 8);

            // Create the rectangle with validation
            Rectangle rectangle;
            try {
                rectangle = new Rectangle(
                    shapeId,
                    position,
                    width.floatValue(),
                    height.floatValue(),
                    style
                );

            } catch (Exception e) {
                LOGGER.warning("Error creating rectangle: " + e.getMessage());
                return ToolResult.failure(state, "Failed to create rectangle: " + e.getMessage());
            }

            // Add rectangle to state with validation
            DrawingState newState;
            try {
                newState = state.withAddedShape(rectangle);
                if (newState == null) {
                    return ToolResult.failure(state, "Failed to add rectangle to drawing state");
                }
            } catch (Exception e) {
                LOGGER.warning("Error adding rectangle to state: " + e.getMessage());
                return ToolResult.failure(state, "Failed to update drawing state: " + e.getMessage());
            }

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