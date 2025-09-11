package com.example.domain.tools.impl;

import com.example.domain.commands.AddShapeCommand;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Rectangle;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for creating rectangles in the drawing
 */
public class CreateRectangleTool implements IAgentTool {

  @Override
  public String name() {
    return "create_rectangle";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Position
    Map<String, Object> position = new HashMap<>();
    position.put("type", "object");
    Map<String, Object> posProps = new HashMap<>();
    posProps.put("x", Map.of("type", "number", "description", "X coordinate of top-left corner"));
    posProps.put("y", Map.of("type", "number", "description", "Y coordinate of top-left corner"));
    position.put("properties", posProps);
    position.put("required", new String[] {"x", "y"});
    properties.put("position", position);

    // Width and Height
    properties.put("width",
        Map.of("type", "number", "description", "Width of the rectangle", "minimum", 1));

    properties.put("height",
        Map.of("type", "number", "description", "Height of the rectangle", "minimum", 1));

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
        "Whether the rectangle should be filled", "default", true));

    schema.put("properties", properties);
    schema.put("required", new String[] {"position", "width", "height"});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    Map<String, Object> positionMap = getObjectMap(parameters, "position", true);
    Number x = getNumber(positionMap, "x", true);
    Number y = getNumber(positionMap, "y", true);
    Number width = getNumber(parameters, "width", true);
    Number height = getNumber(parameters, "height", true);

    if (x == null || y == null || width == null || height == null) {
      throw new IllegalArgumentException("Position, width and height are required");
    }

    // Create position
    Position position = new Position(x.floatValue(), y.floatValue());

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder();

    // Set stroke color if provided
    Map<String, Object> strokeColorMap = getObjectMap(parameters, "strokeColor", false);
    if (strokeColorMap != null) {
      styleBuilder.strokeColor(extractColor(strokeColorMap));
    }

    // Set fill color if provided
    Map<String, Object> fillColorMap = getObjectMap(parameters, "fillColor", false);
    if (fillColorMap != null) {
      styleBuilder.fillColor(extractColor(fillColorMap));
    } else {
      styleBuilder.fillColor(new Color(150, 150, 150, 255));
    }

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

    // Create the rectangle
    Rectangle rectangle = new Rectangle(position, width.floatValue(), height.floatValue(), style);

    // Create and execute the add command
    AddShapeCommand command = new AddShapeCommand(rectangle);
    return command.execute(state);
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

  // ---- small helpers ----
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
