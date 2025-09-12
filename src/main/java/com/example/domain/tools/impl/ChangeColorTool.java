package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Style;
import com.example.application.usecase.UseCaseFacade;
import com.example.application.usecase.dto.ChangeColorInput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for changing the color and style of shapes
 */
public class ChangeColorTool implements IAgentTool {

  // Feature flag removed: always facade
  private UseCaseFacade facade;

  public ChangeColorTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
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
    Map<String, Object> color = new HashMap<>();
    color.put("type", "object");
    Map<String, Object> colorProps = new HashMap<>();
    colorProps.put("r", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("g", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("b", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("a", Map.of("type", "integer", "minimum", 0, "maximum", 255, "default", 255));
    color.put("properties", colorProps);
    properties.put("strokeColor", color);

    // New fill color (optional)
    properties.put("fillColor", color);

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
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeId"}),
        Map.of("required", new String[] {"index"})});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    String shapeId = (String) parameters.get("shapeId");
    Number index = (Number) parameters.get("index");

    if (shapeId == null && index == null) {
      throw new IllegalArgumentException("Either shapeId or index must be provided");
    }

    List<Shape> shapes = state.getShapes();
    Shape targetShape = null;

    if (shapeId != null) {
      // Find shape by ID
      for (int i = 0; i < shapes.size(); i++) {
        if (shapes.get(i).getId().equals(shapeId)) {
          targetShape = shapes.get(i);
          break;
        }
      }

      if (targetShape == null) {
        throw new IllegalArgumentException("Shape with ID '" + shapeId + "' not found");
      }
    } else {
      // Get shape by index
      int idx = index.intValue();
      if (idx < 0 || idx >= shapes.size()) {
        throw new IllegalArgumentException(
            "Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
      }
      targetShape = shapes.get(idx);
      // index no longer needed after refactor
    }

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

    if (facade == null) {
      throw new IllegalStateException("UseCaseFacade not injected");
    }
    var result = facade.changeColor(new ChangeColorInput(targetShape.getId(),
        newStyle.getFillColor(), newStyle.getStrokeColor()));
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

  // local helpers removed (centralized)
}
