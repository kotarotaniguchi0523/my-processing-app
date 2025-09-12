package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.application.usecase.UseCaseFacade;
import com.example.application.usecase.dto.CreateRectangleInput;
import com.example.domain.models.DrawingState;
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

  private UseCaseFacade facade; // lazily injected to avoid circular init

  public CreateRectangleTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

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
    Map<String, Object> positionMap = Parameters.obj(parameters, "position", true);
    Number x = Parameters.num(positionMap, "x", true);
    Number y = Parameters.num(positionMap, "y", true);
    Number width = Parameters.num(parameters, "width", true);
    Number height = Parameters.num(parameters, "height", true);

    if (x == null || y == null || width == null || height == null) {
      throw new IllegalArgumentException("Position, width and height are required");
    }

    // Create position
    Position position = new Position(x.floatValue(), y.floatValue());

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder();

    // Set stroke color if provided
    Map<String, Object> strokeColorMap = Parameters.obj(parameters, "strokeColor", false);
    if (strokeColorMap != null) {
      styleBuilder.strokeColor(extractColor(strokeColorMap));
    }

    // Set fill color if provided
    Map<String, Object> fillColorMap = Parameters.obj(parameters, "fillColor", false);
    if (fillColorMap != null) {
      styleBuilder.fillColor(extractColor(fillColorMap));
    } else {
      styleBuilder.fillColor(new Color(150, 150, 150, 255));
    }

    // Set stroke weight if provided
    Number strokeWeight = Parameters.num(parameters, "strokeWeight", false);
    if (strokeWeight != null)
      styleBuilder.strokeWeight(strokeWeight.floatValue());

    // Set fill enabled
    Boolean filled = Parameters.bool(parameters, "filled");
    if (filled != null) {
      if (filled)
        styleBuilder.hasFill(true);
      else
        styleBuilder.noFill();
    }

    Style style = styleBuilder.build();

    if (facade == null) {
      throw new IllegalStateException("UseCaseFacade not injected");
    }
    var result = facade.createRectangle(new CreateRectangleInput(position.getX(), position.getY(),
        width.floatValue(), height.floatValue(), style.getFillColor(), style.getStrokeColor(),
        style.getStrokeWeight(), style.hasFill(), style.hasStroke()));
    return result.success() ? facade.getCurrentState() : state;
  }

  private Color extractColor(Map<String, Object> map) {
    Number r = Parameters.num(map, "r", false);
    if (r == null)
      r = 0;
    Number g = Parameters.num(map, "g", false);
    if (g == null)
      g = 0;
    Number b = Parameters.num(map, "b", false);
    if (b == null)
      b = 0;
    Number a = Parameters.num(map, "a", false);
    if (a == null)
      a = 255;
    return new Color(r.intValue(), g.intValue(), b.intValue(), a.intValue());
  }

  // local helpers removed (centralized in Parameters)
}
