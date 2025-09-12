package com.example.domain.tools.impl;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.application.usecase.UseCaseFacade; // Phase A1 central mutation

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for creating circles in the drawing
 */
public class CreateCircleTool implements IAgentTool {

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
    Map<String, Object> color = new HashMap<>();
    color.put("type", "object");
    Map<String, Object> colorProps = new HashMap<>();
    colorProps.put("r", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("g", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("b", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("a", Map.of("type", "integer", "minimum", 0, "maximum", 255, "default", 255));
    color.put("properties", colorProps);
    properties.put("strokeColor", color);

    // Fill color (optional)
    properties.put("fillColor", color);

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
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
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
    Map<String, Object> strokeColorMap = getObjectMap(parameters, "strokeColor", false);
    if (strokeColorMap != null)
      styleBuilder.strokeColor(extractColor(strokeColorMap));

    // Set fill color if provided
    Map<String, Object> fillColorMap = getObjectMap(parameters, "fillColor", false);
    if (fillColorMap != null)
      styleBuilder.fillColor(extractColor(fillColorMap));
    else
      styleBuilder.fillColor(new Color(200, 200, 200, 255));

    // Set stroke weight if provided
    Number strokeWeight = getNumber(parameters, "strokeWeight", false);
    if (strokeWeight != null)
      styleBuilder.strokeWeight(strokeWeight.floatValue());

    // Set fill enabled
    Boolean filled = getBoolean(parameters, "filled");
    if (filled != null) {
      if (filled)
        styleBuilder.hasFill(true);
      else
        styleBuilder.noFill();
    }

    Style style = styleBuilder.build();

    Circle circle = new Circle(center, radius.floatValue(), style);
    // Centralized mutation via facade helper
    return UseCaseFacade.addShape(state, circle);
  }

  private Color extractColor(Map<String, Object> map) {
    Number r = getNumber(map, "r", false);
    if (r == null)
      r = 0;
    Number g = getNumber(map, "g", false);
    if (g == null)
      g = 0;
    Number b = getNumber(map, "b", false);
    if (b == null)
      b = 0;
    Number a = getNumber(map, "a", false);
    if (a == null)
      a = 255;
    return new Color(r.intValue(), g.intValue(), b.intValue(), a.intValue());
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

  private Boolean getBoolean(Map<String, Object> src, String key) {
    Object v = src.get(key);
    if (v == null)
      return null;
    if (v instanceof Boolean)
      return (Boolean) v;
    throw new IllegalArgumentException("Field '" + key + "' must be boolean");
  }
}
