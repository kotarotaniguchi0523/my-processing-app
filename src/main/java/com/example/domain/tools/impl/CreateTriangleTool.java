package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.application.usecase.api.ToolUseCasePort;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Triangle;
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
 * Tool for creating triangles in the drawing
 */
public class CreateTriangleTool implements IAgentTool {

  private ToolUseCasePort port; // lazily injected to avoid circular init

  public CreateTriangleTool withPort(ToolUseCasePort port) {
    this.port = port;
    return this;
  }

  @Override
  public String name() {
    return "create_triangle";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Three points for the triangle
    Map<String, Object> pointSchema = new HashMap<>();
    pointSchema.put("type", "object");
    Map<String, Object> pointProps = new HashMap<>();
    pointProps.put("x", Map.of("type", "number", "description", "X coordinate"));
    pointProps.put("y", Map.of("type", "number", "description", "Y coordinate"));
    pointSchema.put("properties", pointProps);
    pointSchema.put("required", new String[] {"x", "y"});

    properties.put("p1", Map.of(
        "type", "object",
        "description", "First vertex of the triangle",
        "properties", pointProps,
        "required", new String[] {"x", "y"}
    ));

    properties.put("p2", Map.of(
        "type", "object", 
        "description", "Second vertex of the triangle",
        "properties", pointProps,
        "required", new String[] {"x", "y"}
    ));

    properties.put("p3", Map.of(
        "type", "object",
        "description", "Third vertex of the triangle",
        "properties", pointProps,
        "required", new String[] {"x", "y"}
    ));

    // Color (optional)
    properties.put("strokeColor", colorSchema());
    properties.put("fillColor", colorSchema());

    // Stroke weight (optional)
    properties.put("strokeWeight",
        Map.of("type", "number", "description", "Width of the stroke", "minimum", 0, "default", 1));

    // Fill enabled (optional)
    properties.put("filled", Map.of("type", "boolean", "description",
        "Whether the triangle should be filled", "default", true));

    schema.put("properties", properties);
    schema.put("required", new String[] {"p1", "p2", "p3"});

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    // Extract three points
    Map<String, Object> p1Map = Parameters.obj(parameters, "p1", true);
    Map<String, Object> p2Map = Parameters.obj(parameters, "p2", true);
    Map<String, Object> p3Map = Parameters.obj(parameters, "p3", true);

    Number x1 = Parameters.num(p1Map, "x", true);
    Number y1 = Parameters.num(p1Map, "y", true);
    Number x2 = Parameters.num(p2Map, "x", true);
    Number y2 = Parameters.num(p2Map, "y", true);
    Number x3 = Parameters.num(p3Map, "x", true);
    Number y3 = Parameters.num(p3Map, "y", true);

    if (x1 == null || y1 == null || x2 == null || y2 == null || x3 == null || y3 == null) {
      throw new IllegalArgumentException("All three points (p1, p2, p3) with x and y coordinates are required");
    }

    // Create positions for the three vertices
    Position pos1 = new Position(x1.floatValue(), y1.floatValue());
    Position pos2 = new Position(x2.floatValue(), y2.floatValue());
    Position pos3 = new Position(x3.floatValue(), y3.floatValue());

    // Calculate base position (using the first point as reference)
    Position basePosition = pos1;

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder();

    // Set stroke color if provided
    applyOptionalStrokeFill(parameters, styleBuilder);
    if (styleBuilder.build().getFillColor() == null) {
      // default fill color when none specified
      styleBuilder.fillColor(new Color(150, 150, 150, 255));
    }

    Style style = styleBuilder.build();

    // Create the triangle shape
    Triangle triangle = new Triangle(basePosition, pos1, pos2, pos3, style);

    // Since we don't have a specific createTriangle method in ToolUseCasePort,
    // we'll use the generic addShape method
    requirePort(port);
    DrawingState newState = port.addShape(triangle);
    
    if (newState != null) {
      return UseCaseResult.ok(newState, triangle.getId(), 
          String.format("Triangle created with vertices at (%.0f,%.0f), (%.0f,%.0f), (%.0f,%.0f)",
              x1.floatValue(), y1.floatValue(),
              x2.floatValue(), y2.floatValue(),
              x3.floatValue(), y3.floatValue()));
    }
    
    return UseCaseResult.fail(state, "Failed to create triangle");
  }
}