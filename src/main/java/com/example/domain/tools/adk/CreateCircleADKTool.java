package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Circle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating circles.
 * Directly manipulates DrawingState without UseCase layer.
 */
public class CreateCircleADKTool implements ADKTool {

    private static final Logger LOGGER = Logger.getLogger(CreateCircleADKTool.class.getName());

    @Override
    public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
        try {
            LOGGER.info("Executing create_circle with parameters: " + parameters);

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

            // Extract radius using ParameterExtractor
            Number radius = getNumber(parameters, "radius");
            if (radius == null) {
                return ToolResult.failure(state, "Radius is required");
            }

            // Validate radius is positive
            if (radius.doubleValue() <= 0) {
                return ToolResult.failure(state, "Radius must be positive");
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
            String shapeId = "circle_" + UUID.randomUUID().toString().substring(0, 8);

            // Create the circle
            Circle circle = new Circle(shapeId, position, radius.floatValue(), style);

            // Add circle to state
            DrawingState newState = state.withAddedShape(circle);

            String message = String.format("Created circle at (%.1f, %.1f) with radius %.1f",
                x.floatValue(), y.floatValue(), radius.floatValue());

            return ToolResult.success(newState, shapeId, message);

        } catch (Exception e) {
            LOGGER.severe("Error creating circle: " + e.getMessage());
            return ToolResult.failure(state, "Failed to create circle: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
            "name", "createCircle",
            "description", "Create a circle shape on the canvas",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "x", Map.of("type", "number", "description", "X coordinate of center"),
                    "y", Map.of("type", "number", "description", "Y coordinate of center"),
                    "radius", Map.of("type", "number", "description", "Circle radius"),
                    "color", Map.of("type", "string", "description", "Fill color (hex, rgb, or name)"),
                    "strokeWeight", Map.of("type", "number", "description", "Stroke width (optional)"),
                    "filled", Map.of("type", "boolean", "description", "Whether to fill the circle")
                ),
                "required", java.util.List.of("x", "y", "radius")
            )
        );
    }
}