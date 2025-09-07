package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.AbstractTool;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.models.StateHistory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GetStateTool allows the AI agent to retrieve information about the current drawing state Provides
 * access to shapes, canvas settings, and history information
 */
public class GetStateTool extends AbstractTool {

  public GetStateTool() {
    super(
        "get_state",
        "Get information about the current drawing state",
        "state",
        false, // does not require approval
        false); // not state modifying
  }

  @Override
  protected void defineParameters() {
    addParameter("includeShapes", Boolean.class, "Include detailed shape information", false, true);
    addParameter("includeSettings", Boolean.class, "Include canvas settings", false, true);
    addParameter(
        "includeHistory", Boolean.class, "Include state history information", false, false);
    addParameter(
        "shapeFilter",
        String.class,
        "Filter shapes by type (circle, rectangle, line, text)",
        false);
    addParameter("summary", Boolean.class, "Return only summary information", false, false);
  }

  @Override
  protected ToolResult executeInternal(Map<String, Object> parameters, AgentContext context)
      throws ToolExecutionException {

    DrawingState currentState = requireCurrentState(context);

    // Extract parameters
    boolean includeShapes = getOptionalParameter(parameters, "includeShapes", Boolean.class, true);
    boolean includeSettings =
        getOptionalParameter(parameters, "includeSettings", Boolean.class, true);
    boolean includeHistory =
        getOptionalParameter(parameters, "includeHistory", Boolean.class, false);
    String shapeFilter = getOptionalParameter(parameters, "shapeFilter", String.class, null);
    boolean summaryOnly = getOptionalParameter(parameters, "summary", Boolean.class, false);

    Map<String, Object> stateInfo = new HashMap<>();

    // Basic state information
    stateInfo.put("timestamp", currentState.getTimestamp());
    stateInfo.put("totalShapes", currentState.getShapes().size());

    if (summaryOnly) {
      return createSummaryResult(currentState, stateInfo);
    }

    // Include canvas settings
    if (includeSettings) {
      Map<String, Object> settings = new HashMap<>(currentState.getSettings());
      stateInfo.put("canvasSettings", settings);
      stateInfo.put("canvasWidth", currentState.getIntSetting("width", 800));
      stateInfo.put("canvasHeight", currentState.getIntSetting("height", 600));
      stateInfo.put("backgroundColor", currentState.getStringSetting("backgroundColor", "#FFFFFF"));
    }

    // Include shape information
    if (includeShapes) {
      List<Shape> shapes = currentState.getShapes();
      List<Map<String, Object>> shapeInfoList = new ArrayList<>();

      for (Shape shape : shapes) {
        if (shapeFilter == null || shape.getType().getTypeName().equalsIgnoreCase(shapeFilter)) {
          Map<String, Object> shapeInfo = createShapeInfo(shape);
          shapeInfoList.add(shapeInfo);
        }
      }

      stateInfo.put("shapes", shapeInfoList);

      // Add shape statistics
      Map<String, Integer> shapeTypeCount = new HashMap<>();
      for (Shape shape : shapes) {
        String type = shape.getType().getTypeName();
        shapeTypeCount.put(type, shapeTypeCount.getOrDefault(type, 0) + 1);
      }
      stateInfo.put("shapeTypeCount", shapeTypeCount);
    }

    // Include history information
    if (includeHistory && context.hasStateHistory()) {
      StateHistory history = context.getStateHistory().get();
      Map<String, Object> historyInfo = new HashMap<>();

      historyInfo.put("totalSnapshots", history.size());
      historyInfo.put("currentIndex", history.getCurrentIndex());
      historyInfo.put("canUndo", history.canUndo());
      historyInfo.put("canRedo", history.canRedo());
      historyInfo.put("maxHistorySize", history.getMaxHistorySize());

      if (!history.isEmpty()) {
        historyInfo.put(
            "currentSnapshotId",
            history.getCurrentSnapshot() != null ? history.getCurrentSnapshot().getId() : null);
      }

      stateInfo.put("history", historyInfo);
    }

    // Create result message
    String message =
        createStateMessage(currentState, shapeFilter, includeHistory && context.hasStateHistory());

    return ToolResult.success(message)
        .withData("state", stateInfo)
        .withData(
            "queryParameters",
            Map.of(
                "includeShapes", includeShapes,
                "includeSettings", includeSettings,
                "includeHistory", includeHistory,
                "shapeFilter", shapeFilter != null ? shapeFilter : "none"));
  }

  private ToolResult createSummaryResult(DrawingState currentState, Map<String, Object> stateInfo) {
    List<Shape> shapes = currentState.getShapes();

    // Count shapes by type
    Map<String, Integer> shapeTypeCount = new HashMap<>();
    for (Shape shape : shapes) {
      String type = shape.getType().getTypeName();
      shapeTypeCount.put(type, shapeTypeCount.getOrDefault(type, 0) + 1);
    }

    stateInfo.put("shapeTypeCount", shapeTypeCount);
    stateInfo.put("canvasWidth", currentState.getIntSetting("width", 800));
    stateInfo.put("canvasHeight", currentState.getIntSetting("height", 600));
    stateInfo.put("isEmpty", shapes.isEmpty());

    String message =
        String.format(
            "Canvas summary: %d shapes total (%s), canvas size: %dx%d",
            shapes.size(),
            shapeTypeCount.isEmpty() ? "empty" : formatShapeTypeCounts(shapeTypeCount),
            currentState.getIntSetting("width", 800),
            currentState.getIntSetting("height", 600));

    return ToolResult.success(message).withData("summary", stateInfo);
  }

  private Map<String, Object> createShapeInfo(Shape shape) {
    Map<String, Object> info = new HashMap<>();
    info.put("id", shape.getId());
    info.put("type", shape.getType().getTypeName());
    info.put(
        "position",
        Map.of(
            "x", shape.getPosition().getX(),
            "y", shape.getPosition().getY()));
    info.put("visible", shape.isVisible());
    info.put("layerOrder", shape.getLayerOrder());

    // Add shape-specific properties
    switch (shape.getType()) {
      case CIRCLE:
        if (shape instanceof com.example.domain.models.Circle) {
          com.example.domain.models.Circle circle = (com.example.domain.models.Circle) shape;
          info.put("radius", circle.getRadius());
          info.put("diameter", circle.getDiameter());
          info.put("area", circle.getArea());
        }
        break;
      case RECTANGLE:
        if (shape instanceof com.example.domain.models.Rectangle) {
          com.example.domain.models.Rectangle rect = (com.example.domain.models.Rectangle) shape;
          info.put("width", rect.getWidth());
          info.put("height", rect.getHeight());
          info.put("area", rect.getArea());
        }
        break;
      case LINE:
        if (shape instanceof com.example.domain.models.Line) {
          com.example.domain.models.Line line = (com.example.domain.models.Line) shape;
          info.put(
              "endPosition",
              Map.of(
                  "x", line.getEndPosition().getX(),
                  "y", line.getEndPosition().getY()));
          info.put("length", line.getLength());
        }
        break;
      case TEXT:
        if (shape instanceof com.example.domain.models.Text) {
          com.example.domain.models.Text text = (com.example.domain.models.Text) shape;
          info.put("content", text.getContent());
          info.put("fontSize", text.getFontSize());
        }
        break;
    }

    // Add style information
    Map<String, Object> styleInfo = new HashMap<>();
    styleInfo.put("fillColor", shape.getStyle().getFillColor().toHex());
    styleInfo.put("strokeColor", shape.getStyle().getStrokeColor().toHex());
    styleInfo.put("strokeWeight", shape.getStyle().getStrokeWeight());
    info.put("style", styleInfo);

    return info;
  }

  private String createStateMessage(
      DrawingState currentState, String shapeFilter, boolean hasHistory) {
    List<Shape> shapes = currentState.getShapes();

    StringBuilder message = new StringBuilder();

    if (shapeFilter != null) {
      long filteredCount =
          shapes.stream()
              .filter(s -> s.getType().getTypeName().equalsIgnoreCase(shapeFilter))
              .count();
      message.append(
          String.format(
              "Found %d %s shapes out of %d total shapes",
              filteredCount, shapeFilter, shapes.size()));
    } else {
      message.append(String.format("Current state contains %d shapes", shapes.size()));

      if (!shapes.isEmpty()) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (Shape shape : shapes) {
          String type = shape.getType().getTypeName();
          typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }
        message.append(" (").append(formatShapeTypeCounts(typeCounts)).append(")");
      }
    }

    message.append(
        String.format(
            ", canvas size: %dx%d",
            currentState.getIntSetting("width", 800), currentState.getIntSetting("height", 600)));

    if (hasHistory) {
      message.append(", history available");
    }

    return message.toString();
  }

  private String formatShapeTypeCounts(Map<String, Integer> typeCounts) {
    List<String> parts = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
      parts.add(entry.getValue() + " " + entry.getKey() + (entry.getValue() == 1 ? "" : "s"));
    }
    return String.join(", ", parts);
  }
}
