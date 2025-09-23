package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Ellipse;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.values.Color;
import static com.example.domain.tools.adk.ParameterExtractor.*;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for creating ellipses. Works directly with ADK's LlmAgent function calling.
 */
public class CreateEllipseADKTool extends BaseADKTool {

  private static final Logger LOGGER = Logger.getLogger(CreateEllipseADKTool.class.getName());

  @Override
  public String getName() {
    return "create_ellipse";
  }

  @Override
  public Map<String, Object> getSchema() {
    Map<String, Object> schema = new java.util.HashMap<>();
    schema.put("name", "create_ellipse");
    schema.put("description", "Creates an ellipse shape");
    return schema;
  }

  @Override
  public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
    try {
      LOGGER.info("Executing create_ellipse with parameters: " + parameters);

      // Extract center position - support both flat and nested position formats
      Number x = getNumber(parameters, "x");
      Number y = getNumber(parameters, "y");

      // If flat parameters not found, try nested position/center object
      if (x == null || y == null) {
        Map<String, Object> position = getMap(parameters, "position");
        if (position == null) {
          position = getMap(parameters, "center");
        }
        if (position != null) {
          x = getNumber(position, "x");
          y = getNumber(position, "y");
        }
      }

      if (x == null || y == null) {
        return ToolResult.failure(state,
            "Position x and y coordinates are required (either as flat x,y or position/center:{x,y})");
      }

      // Extract radii
      Number radiusX = getNumber(parameters, "radiusX");
      Number radiusY = getNumber(parameters, "radiusY");
      if (radiusX == null || radiusY == null) {
        return ToolResult.failure(state, "Both radiusX and radiusY are required");
      }

      // Validate radii are positive
      if (radiusX.floatValue() <= 0 || radiusY.floatValue() <= 0) {
        return ToolResult.failure(state, "Radii must be positive values");
      }

      // Create center position
      Position center = new Position(x.floatValue(), y.floatValue());

      // Build style using ParameterExtractor
      Style style = buildStyleFromParameters(parameters, new Color(100, 200, 255, 255), // Default
                                                                                        // cyan fill
          Color.BLACK, // Default black stroke
          2.0f, // Default stroke weight
          true // Default filled
      ).build();

      // Create the ellipse
      Ellipse ellipse = new Ellipse(center, radiusX.floatValue(), radiusY.floatValue(), style);

      // Add to state directly
      DrawingState newState = state.withAddedShape(ellipse);

      String message = String.format("Created ellipse at (%.0f, %.0f) with radii %.0fx%.0f",
          x.floatValue(), y.floatValue(), radiusX.floatValue(), radiusY.floatValue());

      LOGGER.info(message);
      return ToolResult.success(newState, message);

    } catch (Exception e) {
      LOGGER.severe("Failed to create ellipse: " + e.getMessage());
      return ToolResult.failure(state, "Failed to create ellipse: " + e.getMessage());
    }
  }

}
