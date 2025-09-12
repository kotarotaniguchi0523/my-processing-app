package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.application.usecase.api.ToolUseCasePort;
import com.example.application.usecase.dto.CreateRectangleInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.DrawingState;
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
 * Tool for creating rectangles in the drawing
 */
public class CreateRectangleTool implements IAgentTool {

  private ToolUseCasePort port; // lazily injected to avoid circular init

  public CreateRectangleTool withPort(ToolUseCasePort port) {
    this.port = port;
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
    properties.put("strokeColor", colorSchema());
    properties.put("fillColor", colorSchema());

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
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
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
    applyOptionalStrokeFill(parameters, styleBuilder);
    if (styleBuilder.build().getFillColor() == null) {
      // default fill color when none specified
      styleBuilder.fillColor(new Color(150, 150, 150, 255));
    }

    Style style = styleBuilder.build();

    requirePort(port);
    var result = port.createRectangle(new CreateRectangleInput(position.getX(), position.getY(),
        width.floatValue(), height.floatValue(), style.getFillColor(), style.getStrokeColor(),
        style.getStrokeWeight(), style.hasFill(), style.hasStroke()));
    if (result.success()) {
      return UseCaseResult.ok(result.newState(), result.payload(), result.message());
    }
    return UseCaseResult.fail(state, result.message());
  }

  // color & stroke helpers centralized in ToolStyleUtil
}
