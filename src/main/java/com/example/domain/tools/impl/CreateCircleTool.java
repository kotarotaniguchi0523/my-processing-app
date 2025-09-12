package com.example.domain.tools.impl;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.application.usecase.UseCaseFacade; // central mutation
import com.example.application.usecase.dto.UseCaseResult;

import java.util.HashMap;
import java.util.Map;
import static com.example.domain.tools.support.ToolPreconditions.requireFacade;
import static com.example.domain.tools.support.ToolStyleUtil.*;
import static com.example.domain.tools.support.ToolSchemas.colorSchema;

/**
 * Tool for creating circles in the drawing
 */
public class CreateCircleTool implements IAgentTool {
  // Inject facade to use instance helpers
  private UseCaseFacade facade;

  public CreateCircleTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

  @Override
  public String name() {
    return "create_circle";
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
    centerProps.put("x", Map.of("type", "number", "description", "X coordinate of center"));
    centerProps.put("y", Map.of("type", "number", "description", "Y coordinate of center"));
    center.put("properties", centerProps);
    center.put("required", new String[] {"x", "y"});
    properties.put("center", center);

    // Radius
    properties.put("radius",
        Map.of("type", "number", "description", "Radius of the circle", "minimum", 1));

    // Color (optional)
    properties.put("strokeColor", colorSchema());
    properties.put("fillColor", colorSchema());

    // Stroke weight (optional)
    properties.put("strokeWeight",
        Map.of("type", "number", "description", "Width of the stroke", "minimum", 0, "default", 1));

    // Fill enabled (optional)
    properties.put("filled", Map.of("type", "boolean", "description",
        "Whether the circle should be filled", "default", true));

    schema.put("properties", properties);
    schema.put("required", new String[] {"center", "radius"});

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    // Extract parameters
    Map<String, Object> centerMap = getObjectMap(parameters, "center", true);
    Number x = getNumber(centerMap, "x", true);
    Number y = getNumber(centerMap, "y", true);
    Number radius = getNumber(parameters, "radius", true);

    if (x == null || y == null || radius == null) {
      throw new IllegalArgumentException("Center position and radius are required");
    }

    // Create position and basic style
    Position center = new Position(x.floatValue(), y.floatValue());

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder();

    // Set stroke color if provided
    applyOptionalStrokeFill(parameters, styleBuilder);
    if (styleBuilder.build().getFillColor() == null) {
      styleBuilder.fillColor(new Color(200, 200, 200, 255));
    }

    Style style = styleBuilder.build();

    Circle circle = new Circle(center, radius.floatValue(), style);
    requireFacade(facade);
    // Centralized mutation via facade helper
    DrawingState newState = facade.addShape(circle);
    return UseCaseResult.ok(newState, circle.getId(), "created");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getObjectMap(Map<String, Object> src, String key, boolean required) {
    Object v = src.get(key);
    if (v == null) {
      if (required)
        throw new IllegalArgumentException("Missing object: " + key);
      return null;
    }
    if (v instanceof Map)
      return (Map<String, Object>) v;
    throw new IllegalArgumentException("Field '" + key + "' must be object");
  }

  private Number getNumber(Map<String, Object> src, String key, boolean required) {
    Object v = src.get(key);
    if (v == null) {
      if (required)
        throw new IllegalArgumentException("Missing number: " + key);
      return null;
    }
    if (v instanceof Number)
      return (Number) v;
    throw new IllegalArgumentException("Field '" + key + "' must be number");
  }

}
