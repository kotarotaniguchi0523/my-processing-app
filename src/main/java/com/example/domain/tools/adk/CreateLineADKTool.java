package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
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

            // Extract line positions using utility
            Position[] positions = ADKToolUtils.extractLinePositions(parameters);
            if (positions == null) {
                return ToolResult.failure(state, "Start and end coordinates are required (either as flat x1,y1,x2,y2 or start/end:{x,y})");
            }

            Position startPos = positions[0];
            Position endPos = positions[1];

            // Validate positions are within bounds
            if (!ADKToolUtils.isPositionInStandardBounds(startPos)) {
                return ToolResult.failure(state, "Start position is outside reasonable bounds (±10000)");
            }
            if (!ADKToolUtils.isPositionInStandardBounds(endPos)) {
                return ToolResult.failure(state, "End position is outside reasonable bounds (±10000)");
            }

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

            // Generate unique shape ID using utility
            String shapeId = ADKToolUtils.generateShapeId("line");

            // Create the line
            Line line = new Line(shapeId, startPos, endPos, style);

            // Add line to state
            DrawingState newState = state.withAddedShape(line);

            String message = String.format(
                "Created line from (%.1f, %.1f) to (%.1f, %.1f)",
                startPos.getX(), startPos.getY(),
                endPos.getX(), endPos.getY()
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