package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Style;
import com.example.application.usecase.api.ToolUseCasePort;
import com.example.application.usecase.dto.ChangeColorInput;
import com.example.application.usecase.dto.UseCaseResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.example.domain.tools.support.ToolPreconditions.requirePort;
import static com.example.domain.tools.support.ToolStyleUtil.*;
import static com.example.domain.tools.support.ToolSchemas.commonColorSchema;
import static com.example.domain.tools.support.ToolSchemas.commonOneOfRequired;
import static com.example.domain.tools.support.ToolShapeLookup.findByIdOrIndex;

/**
 * Tool for changing the color and style of shapes
 */
public class ChangeColorTool implements IAgentTool {

  // Feature flag removed: always facade
  private ToolUseCasePort port;

  public ChangeColorTool withPort(ToolUseCasePort port) {
    this.port = port;
    return this;
  }

  @Override
  public String name() {
    return "change_color";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Shape ID to modify
    properties.put("shapeId", Map.of("type", "string", "description", "ID of the shape to modify"));

    // Alternative: modify by index
    properties.put("index", Map.of("type", "integer", "description",
        "Index of the shape to modify (0-based)", "minimum", 0));

    // New stroke color (optional)
    properties.put("strokeColor", commonColorSchema());
    properties.put("fillColor", commonColorSchema());

    // Stroke weight (optional)
    properties.put("strokeWeight",
        Map.of("type", "number", "description", "Width of the stroke", "minimum", 0));

    // Fill enabled (optional)
    properties.put("filled",
        Map.of("type", "boolean", "description", "Whether the shape should be filled"));

    // Opacity (optional)
    properties.put("opacity", Map.of("type", "number", "description",
        "Overall opacity (0.0 = transparent, 1.0 = opaque)", "minimum", 0.0, "maximum", 1.0));

    schema.put("properties", properties);
    // Must have either shapeId or index
    schema.put("oneOf", commonOneOfRequired(new String[] {"shapeId"}, new String[] {"index"}));

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    String shapeId = (String) parameters.get("shapeId");
    Number index = (Number) parameters.get("index");

    List<Shape> shapes = state.getShapes();
    Shape targetShape = findByIdOrIndex(shapes, shapeId, index, "change_color");

    // Build new style based on current style and modifications
    Style currentStyle = targetShape.getStyle();
    Style.Builder styleBuilder = Style.builder().strokeColor(currentStyle.getStrokeColor())
        .fillColor(currentStyle.getFillColor()).strokeWeight(currentStyle.getStrokeWeight());
    if (currentStyle.hasFill())
      styleBuilder.hasFill(true);
    else
      styleBuilder.noFill();
    if (!currentStyle.hasStroke())
      styleBuilder.noStroke();

    // Apply new stroke color if provided
    Map<String, Object> strokeColorMap = Parameters.obj(parameters, "strokeColor", false);
    if (strokeColorMap != null) {
      Color strokeColor = extractColor(strokeColorMap);
      styleBuilder.strokeColor(strokeColor);
    }

    // Apply new fill color if provided
    Map<String, Object> fillColorMap = Parameters.obj(parameters, "fillColor", false);
    if (fillColorMap != null) {
      Color fillColor = extractColor(fillColorMap);
      styleBuilder.fillColor(fillColor);
    }

    // Apply new stroke weight if provided
    Number strokeWeight = Parameters.num(parameters, "strokeWeight", false);
    if (strokeWeight != null)
      styleBuilder.strokeWeight(strokeWeight.floatValue());

    // Apply fill enabled if provided
    Boolean filled = Parameters.bool(parameters, "filled");
    if (filled != null) {
      if (filled)
        styleBuilder.hasFill(true);
      else
        styleBuilder.noFill();
    }

    // Apply opacity if provided
    // opacity removed (Style doesn't support it)

    Style newStyle = styleBuilder.build();

    requirePort(port);
    var result = port.changeColor(new ChangeColorInput(targetShape.getId(), newStyle.getFillColor(),
        newStyle.getStrokeColor()));
    if (result.success()) {
      return UseCaseResult.ok(result.newState(), result.payload(), result.message());
    }
    return UseCaseResult.fail(state, result.message());
  }

  // color extraction centralized in ToolStyleUtil
}
