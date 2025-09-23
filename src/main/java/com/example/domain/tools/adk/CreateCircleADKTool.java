package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Circle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating circles. Directly manipulates DrawingState without UseCase
 * layer.
 */
public class CreateCircleADKTool implements ADKTool {

  private static final Logger LOGGER = Logger.getLogger(CreateCircleADKTool.class.getName());

  @Override
  public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
    try {
      LOGGER.info("Executing create_circle with parameters: " + parameters);

      // Extract position using utility
      Position position = ADKToolUtils.extractPosition(parameters);
      if (position == null) {
        return ToolResult.failure(state, ADKToolUtils.getPositionErrorMessage("Position"));
      }

      // Validate position is within bounds
      if (!ADKToolUtils.isPositionInStandardBounds(position)) {
        return ToolResult.failure(state, "Position is outside reasonable bounds (±10000)");
      }

      // Extract radius using ParameterExtractor
      Number radius = getNumber(parameters, "radius");
      if (radius == null) {
        return ToolResult.failure(state, "Radius is required");
      }

      // Validate radius
      if (!ADKToolUtils.isDimensionInStandardBounds(radius)) {
        return ToolResult.failure(state, ADKToolUtils.getDimensionErrorMessage("Radius", radius));
      }

      // Build style using ParameterExtractor
      Style style = buildStyleFromParameters(parameters, Color.fromHex("#64C864"), // Default green
                                                                                   // fill
          Color.BLACK, // Default black stroke
          2.0f, // Default stroke weight
          true // Default filled
      ).build();

      // Generate unique shape ID using utility
      String shapeId = ADKToolUtils.generateShapeId("circle");

      // Create the circle
      Circle circle = new Circle(shapeId, position, radius.floatValue(), style);

      // Add circle to state
      DrawingState newState = state.withAddedShape(circle);

      String message = String.format("Created circle at (%.1f, %.1f) with radius %.1f",
          position.getX(), position.getY(), radius.floatValue());

      return ToolResult.success(newState, message);

    } catch (Exception e) {
      LOGGER.severe("Error creating circle: " + e.getMessage());
      return ToolResult.failure(state, "Failed to create circle: " + e.getMessage());
    }
  }


  @Override
  public Map<String, Object> getSchema() {
    return Map.of("name", "createCircle", "description", "Create a circle shape on the canvas",
        "parameters",
        Map.of("type", "object", "properties",
            Map.of("x", Map.of("type", "number", "description", "X coordinate of center"), "y",
                Map.of("type", "number", "description", "Y coordinate of center"), "radius",
                Map.of("type", "number", "description", "Circle radius"), "color",
                Map.of("type", "string", "description", "Fill color (hex, rgb, or name)"),
                "strokeWeight", Map.of("type", "number", "description", "Stroke width (optional)"),
                "filled", Map.of("type", "boolean", "description", "Whether to fill the circle")),
            "required", java.util.List.of("x", "y", "radius")));
  }
}
