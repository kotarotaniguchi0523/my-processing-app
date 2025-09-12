package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.application.usecase.api.ToolUseCasePort;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Ellipse;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.HashMap;
import java.util.Map;
import static com.example.domain.tools.support.ToolPreconditions.requirePort;
import static com.example.domain.tools.support.ToolStyleUtil.*;
import static com.example.domain.tools.support.ToolSchemas.colorSchema;

/**
 * Tool for creating ellipses in the drawing
 */
public class CreateEllipseTool implements IAgentTool {

  private ToolUseCasePort port; // lazily injected to avoid circular init

  public CreateEllipseTool withPort(ToolUseCasePort port) {
    this.port = port;
    return this;
  }

  @Override
  public String name() {
    return "create_ellipse";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Center position
    Map<String, Object> center = new HashMap<>();
    center.put("type", "object");
    Map<String, Object> centerProps = new HashMap<>();
    centerProps.put("x", Map.of("type", "number", "description", "X coordinate of the center"));
    centerProps.put("y", Map.of("type", "number", "description", "Y coordinate of the center"));
    center.put("properties", centerProps);
    center.put("required", new String[] {"x", "y"});
    properties.put("center", center);

    // Horizontal radius
    properties.put("radiusX",
        Map.of("type", "number", "description", "Horizontal radius of the ellipse", "minimum", 1));

    // Vertical radius
    properties.put("radiusY",
        Map.of("type", "number", "description", "Vertical radius of the ellipse", "minimum", 1));

    // Color (optional)
    properties.put("strokeColor", colorSchema());
    properties.put("fillColor", colorSchema());

    // Stroke weight (optional)
    properties.put("strokeWeight",
        Map.of("type", "number", "description", "Width of the stroke", "minimum", 0, "default", 1));

    // Fill enabled (optional)
    properties.put("filled", Map.of("type", "boolean", "description",
        "Whether the ellipse should be filled", "default", true));

    schema.put("properties", properties);
    schema.put("required", new String[] {"center", "radiusX", "radiusY"});

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    Map<String, Object> centerMap = Parameters.obj(parameters, "center", true);
    Number x = Parameters.num(centerMap, "x", true);
    Number y = Parameters.num(centerMap, "y", true);
    Number radiusX = Parameters.num(parameters, "radiusX", true);
    Number radiusY = Parameters.num(parameters, "radiusY", true);

    if (x == null || y == null || radiusX == null || radiusY == null) {
      throw new IllegalArgumentException("Center position and radii are required");
    }

    // Create center position
    Position center = new Position(x.floatValue(), y.floatValue());

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder();

    // Set stroke color if provided
    applyOptionalStrokeFill(parameters, styleBuilder);
    if (styleBuilder.build().getFillColor() == null) {
      // default fill color when none specified
      styleBuilder.fillColor(new Color(150, 150, 150, 255));
    }

    Style style = styleBuilder.build();

    // Create the ellipse shape
    Ellipse ellipse = new Ellipse(center, radiusX.floatValue(), radiusY.floatValue(), style);

    // Use the generic addShape method
    requirePort(port);
    DrawingState newState = port.addShape(ellipse);
    
    if (newState != null) {
      return UseCaseResult.ok(newState, ellipse.getId(), 
          String.format("Ellipse created at (%.0f,%.0f) with radii %.0fx%.0f",
              x.floatValue(), y.floatValue(),
              radiusX.floatValue(), radiusY.floatValue()));
    }
    
    return UseCaseResult.fail(state, "Failed to create ellipse");
  }
}