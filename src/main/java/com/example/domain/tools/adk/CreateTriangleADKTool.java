package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Triangle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating triangles. Works directly with ADK's LlmAgent function calling.
 */
public class CreateTriangleADKTool extends BaseADKTool {

  private static final Logger LOGGER = Logger.getLogger(CreateTriangleADKTool.class.getName());

  @Override
  public String getName() {
    return "create_triangle";
  }

  @Override
  public Map<String, Object> getSchema() {
    Map<String, Object> schema = new java.util.HashMap<>();
    schema.put("name", "create_triangle");
    schema.put("description", "Creates a triangle shape");
    return schema;
  }

  @Override
  public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
    try {
      LOGGER.info("Executing create_triangle with parameters: " + parameters);

      // Extract three points
      Map<String, Object> p1Map = getMap(parameters, "p1");
      Map<String, Object> p2Map = getMap(parameters, "p2");
      Map<String, Object> p3Map = getMap(parameters, "p3");

      if (p1Map == null || p2Map == null || p3Map == null) {
        return ToolResult.failure(state, "All three points (p1, p2, p3) are required");
      }

      Number x1 = getNumber(p1Map, "x");
      Number y1 = getNumber(p1Map, "y");
      Number x2 = getNumber(p2Map, "x");
      Number y2 = getNumber(p2Map, "y");
      Number x3 = getNumber(p3Map, "x");
      Number y3 = getNumber(p3Map, "y");

      if (x1 == null || y1 == null || x2 == null || y2 == null || x3 == null || y3 == null) {
        return ToolResult.failure(state, "All points must have x and y coordinates");
      }

      // Create positions
      Position pos1 = new Position(x1.floatValue(), y1.floatValue());
      Position pos2 = new Position(x2.floatValue(), y2.floatValue());
      Position pos3 = new Position(x3.floatValue(), y3.floatValue());

      // Use first point as base position
      Position basePosition = pos1;

      // Build style using ParameterExtractor
      Style style = buildStyleFromParameters(parameters, new Color(255, 255, 100, 255), // Default
                                                                                        // yellow
                                                                                        // fill
          Color.BLACK, // Default black stroke
          2.0f, // Default stroke weight
          true // Default filled
      ).build();

      // Create the triangle
      Triangle triangle = new Triangle(basePosition, pos1, pos2, pos3, style);

      // Add to state directly
      DrawingState newState = state.withAddedShape(triangle);

      String message =
          String.format("Created triangle with vertices at (%.0f,%.0f), (%.0f,%.0f), (%.0f,%.0f)",
              x1.floatValue(), y1.floatValue(), x2.floatValue(), y2.floatValue(), x3.floatValue(),
              y3.floatValue());

      LOGGER.info(message);
      return ToolResult.success(newState, message);

    } catch (Exception e) {
      LOGGER.severe("Failed to create triangle: " + e.getMessage());
      return ToolResult.failure(state, "Failed to create triangle: " + e.getMessage());
    }
  }

}
