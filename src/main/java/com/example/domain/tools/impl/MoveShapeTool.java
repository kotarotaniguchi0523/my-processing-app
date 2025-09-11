package com.example.domain.tools.impl;

import com.example.domain.commands.ModifyShapeCommand;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for moving shapes to a new position
 */
public class MoveShapeTool implements IAgentTool {

  @Override
  public String name() {
    return "move_shape";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Shape ID to move
    properties.put("shapeId", Map.of("type", "string", "description", "ID of the shape to move"));

    // Alternative: move by index
    properties.put("index", Map.of("type", "integer", "description",
        "Index of the shape to move (0-based)", "minimum", 0));

    // New position
    Map<String, Object> newPosition = new HashMap<>();
    newPosition.put("type", "object");
    Map<String, Object> posProps = new HashMap<>();
    posProps.put("x", Map.of("type", "number", "description", "New X coordinate"));
    posProps.put("y", Map.of("type", "number", "description", "New Y coordinate"));
    newPosition.put("properties", posProps);
    newPosition.put("required", new String[] {"x", "y"});
    properties.put("newPosition", newPosition);

    // Optional: relative movement
    properties.put("relative", Map.of("type", "boolean", "description",
        "If true, move relative to current position", "default", false));

    schema.put("properties", properties);
    // Must have either shapeId or index, and newPosition
    schema.put("required", new String[] {"newPosition"});
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeId", "newPosition"}),
        Map.of("required", new String[] {"index", "newPosition"})});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    String shapeId = (String) parameters.get("shapeId");
    Number index = (Number) parameters.get("index");
    Map<String, Object> newPosMap = getObjectMap(parameters, "newPosition", true);
    Boolean relative = (Boolean) parameters.get("relative");

    if (shapeId == null && index == null) {
      throw new IllegalArgumentException("Either shapeId or index must be provided");
    }

    if (newPosMap == null) {
      throw new IllegalArgumentException("New position is required");
    }

    Number newX = getNumber(newPosMap, "x", true);
    Number newY = getNumber(newPosMap, "y", true);

    if (newX == null || newY == null) {
      throw new IllegalArgumentException("New position must have x and y coordinates");
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
    }

    // Calculate new position
    Position newPosition;
    if (Boolean.TRUE.equals(relative)) {
      // Move relative to current position
      Position currentPos = targetShape.getPosition();
      newPosition = new Position(currentPos.getX() + newX.floatValue(),
          currentPos.getY() + newY.floatValue());
    } else {
      // Absolute position
      newPosition = new Position(newX.floatValue(), newY.floatValue());
    }

    // Create modified shape with new position
    Shape modifiedShape = targetShape.moveTo(newPosition);

    // Create and execute modify command via modifier function
    ModifyShapeCommand command = new ModifyShapeCommand(targetShape.getId(), s -> modifiedShape,
        "move to (" + newPosition.getX() + "," + newPosition.getY() + ")");
    return command.execute(state);
  }

  // helpers
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
