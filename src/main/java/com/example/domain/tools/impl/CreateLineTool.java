package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.application.usecase.UseCaseFacade; // central mutation
import com.example.application.usecase.dto.UseCaseResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for creating lines in the drawing
 */
public class CreateLineTool implements IAgentTool {
  private UseCaseFacade facade;

  public CreateLineTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

  @Override
  public String name() {
    return "create_line";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Start position
    Map<String, Object> start = new HashMap<>();
    start.put("type", "object");
    Map<String, Object> startProps = new HashMap<>();
    startProps.put("x", Map.of("type", "number", "description", "X coordinate of start point"));
    startProps.put("y", Map.of("type", "number", "description", "Y coordinate of start point"));
    start.put("properties", startProps);
    start.put("required", new String[] {"x", "y"});
    properties.put("start", start);

    // End position
    Map<String, Object> end = new HashMap<>();
    end.put("type", "object");
    Map<String, Object> endProps = new HashMap<>();
    endProps.put("x", Map.of("type", "number", "description", "X coordinate of end point"));
    endProps.put("y", Map.of("type", "number", "description", "Y coordinate of end point"));
    end.put("properties", endProps);
    end.put("required", new String[] {"x", "y"});
    properties.put("end", end);

    // Stroke color (optional)
    Map<String, Object> color = new HashMap<>();
    color.put("type", "object");
    Map<String, Object> colorProps = new HashMap<>();
    colorProps.put("r", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("g", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("b", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    colorProps.put("a", Map.of("type", "integer", "minimum", 0, "maximum", 255, "default", 255));
    color.put("properties", colorProps);
    properties.put("strokeColor", color);

    // Stroke weight (optional)
    properties.put("strokeWeight",
        Map.of("type", "number", "description", "Width of the line", "minimum", 0.1, "default", 1));

    schema.put("properties", properties);
    schema.put("required", new String[] {"start", "end"});

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    // ---- 型安全な取得と検証 ----
    Map<String, Object> startMap = Parameters.obj(parameters, "start", true);
    Map<String, Object> endMap = Parameters.obj(parameters, "end", true);

    Number startX = Parameters.num(startMap, "x", true);
    Number startY = Parameters.num(startMap, "y", true);
    Number endX = Parameters.num(endMap, "x", true);
    Number endY = Parameters.num(endMap, "y", true);

    // Create positions
    Position startPos = new Position(startX.floatValue(), startY.floatValue());
    Position endPos = new Position(endX.floatValue(), endY.floatValue());

    // Build style from optional parameters
    Style.Builder styleBuilder = Style.builder()
        // 線は塗りつぶしなし
        .noFill().hasStroke(true);

    // strokeColor
    Map<String, Object> strokeColorMap = Parameters.obj(parameters, "strokeColor", false);
    if (strokeColorMap != null) {
      styleBuilder.strokeColor(extractColor(strokeColorMap));
    } else {
      styleBuilder.strokeColor(Color.BLACK);
    }

    // strokeWeight
    Number strokeWeight = Parameters.num(parameters, "strokeWeight", false);
    if (strokeWeight != null) {
      styleBuilder.strokeWeight(strokeWeight.floatValue());
    }

    Style style = styleBuilder.build();

    Line line = new Line(startPos, endPos, style);
    if (facade == null)
      throw new IllegalStateException("UseCaseFacade not injected");
    DrawingState newState = facade.addShape(line);
    return UseCaseResult.ok(newState, line.getId(), "created");
  }

  private Color extractColor(Map<String, Object> colorMap) {
    Number r = (Number) colorMap.getOrDefault("r", 0);
    Number g = (Number) colorMap.getOrDefault("g", 0);
    Number b = (Number) colorMap.getOrDefault("b", 0);
    Number a = (Number) colorMap.getOrDefault("a", 255);

    return new Color(r.intValue(), g.intValue(), b.intValue(), a.intValue());
  }

  // local helpers removed (centralized)
}
