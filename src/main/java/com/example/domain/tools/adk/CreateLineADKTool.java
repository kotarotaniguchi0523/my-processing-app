package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating lines.
 * Directly manipulates DrawingState without UseCase layer.
 */
public class CreateLineADKTool implements ADKTool {

    private static final Logger LOGGER = Logger.getLogger(CreateLineADKTool.class.getName());

    @Override
    public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
        try {
            LOGGER.info("Executing create_line with parameters: " + parameters);

            // Extract start and end coordinates - support both flat and nested position formats
            Number x1 = getNumber(parameters, "x1");
            Number y1 = getNumber(parameters, "y1");
            Number x2 = getNumber(parameters, "x2");
            Number y2 = getNumber(parameters, "y2");

            // If flat parameters not found, try nested start/end objects
            if (x1 == null || y1 == null) {
                Map<String, Object> start = getMap(parameters, "start");
                if (start != null) {
                    x1 = getNumber(start, "x");
                    y1 = getNumber(start, "y");
                }
            }

            if (x2 == null || y2 == null) {
                Map<String, Object> end = getMap(parameters, "end");
                if (end != null) {
                    x2 = getNumber(end, "x");
                    y2 = getNumber(end, "y");
                }
            }

            if (x1 == null || y1 == null || x2 == null || y2 == null) {
                return ToolResult.failure(state, "Start and end coordinates are required (either as flat x1,y1,x2,y2 or start/end:{x,y})");
            }

            // Create positions
            Position startPos = new Position(x1.floatValue(), y1.floatValue());
            Position endPos = new Position(x2.floatValue(), y2.floatValue());

            // Build style for line (lines have no fill, only stroke)
            Style.Builder styleBuilder = buildStyleFromParameters(
                parameters,
                null,        // No fill for lines
                Color.BLACK, // Default black stroke
                2.0f,        // Default stroke weight
                false        // Lines are not filled
            );

            // Override to ensure lines have no fill and do have stroke
            styleBuilder.noFill().hasStroke(true);

            Style style = styleBuilder.build();

            // Generate unique shape ID
            String shapeId = "line_" + UUID.randomUUID().toString().substring(0, 8);

            // Create the line
            Line line = new Line(shapeId, startPos, endPos, style);

            // Add line to state
            DrawingState newState = state.withAddedShape(line);

            String message = String.format(
                "Created line from (%.1f, %.1f) to (%.1f, %.1f)",
                x1.floatValue(), y1.floatValue(),
                x2.floatValue(), y2.floatValue()
            );

            return ToolResult.success(newState, shapeId, message);

        } catch (Exception e) {
            LOGGER.severe("Failed to create line: " + e.getMessage());
            return ToolResult.failure(state, "Failed to create line: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> getSchema() {
        return Map.of(
            "name", "createLine",
            "description", "Create a line on the canvas",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "x1", Map.of("type", "number", "description", "Start X coordinate"),
                    "y1", Map.of("type", "number", "description", "Start Y coordinate"),
                    "x2", Map.of("type", "number", "description", "End X coordinate"),
                    "y2", Map.of("type", "number", "description", "End Y coordinate"),
                    "color", Map.of("type", "string", "description", "Line color (hex, rgb, or name)"),
                    "strokeWeight", Map.of("type", "number", "description", "Line thickness")
                ),
                "required", java.util.List.of("x1", "y1", "x2", "y2")
            )
        );
    }
}