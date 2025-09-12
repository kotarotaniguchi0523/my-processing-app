package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for deleting shapes from the drawing
 */
public class DeleteShapeTool implements IAgentTool {

  // Feature flag removed: always facade
  private com.example.application.usecase.api.ToolUseCasePort port;

  public DeleteShapeTool withPort(com.example.application.usecase.api.ToolUseCasePort port) {
    this.port = port;
    return this;
  }

  @Override
  public String name() {
    return "delete_shape";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Shape ID to delete
    properties.put("shapeId", Map.of("type", "string", "description", "ID of the shape to delete"));

    // Alternative: delete by index
    properties.put("index", Map.of("type", "integer", "description",
        "Index of the shape to delete (0-based)", "minimum", 0));

    schema.put("properties", properties);
    // At least one of shapeId or index is required
    schema.put("oneOf", com.example.domain.tools.support.ToolSchemas
        .commonOneOfRequired(new String[] {"shapeId"}, new String[] {"index"}));

    return schema;
  }

  @Override
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
    String shapeId = (String) parameters.get("shapeId");
    Number index = (Number) parameters.get("index");

    if (shapeId == null && index == null) {
      throw new IllegalArgumentException("Either shapeId or index must be provided");
    }

    List<Shape> shapes = state.getShapes();

    int targetIndex = -1;
    if (shapeId != null) {
      for (int i = 0; i < shapes.size(); i++) {
        if (shapes.get(i).getId().equals(shapeId)) {
          targetIndex = i;
          break;
        }
      }
      if (targetIndex < 0) {
        throw new IllegalArgumentException("Shape with ID '" + shapeId + "' not found");
      }
    } else {
      int idx = index.intValue();
      if (idx < 0 || idx >= shapes.size()) {
        throw new IllegalArgumentException(
            "Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
      }
      targetIndex = idx;
      shapeId = shapes.get(idx).getId();
    }

    if (port == null) {
      throw new IllegalStateException("ToolUseCasePort not injected");
    }
    var result =
        port.deleteShape(new com.example.application.usecase.dto.DeleteShapeInput(shapeId));
    if (result.success()) {
      return UseCaseResult.ok(result.newState(), result.payload(), result.message());
    }
    return UseCaseResult.fail(state, result.message());
  }
}
