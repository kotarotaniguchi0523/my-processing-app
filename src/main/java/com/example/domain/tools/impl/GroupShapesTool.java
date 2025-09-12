package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;

import java.util.*;

/**
 * Tool for grouping multiple shapes together Groups are stored as metadata in the DrawingState's
 * arrays
 */
public class GroupShapesTool implements IAgentTool {

  @Override
  public String name() {
    return "group_shapes";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Shape IDs to group
    Map<String, Object> shapeIds = new HashMap<>();
    shapeIds.put("type", "array");
    shapeIds.put("items", Map.of("type", "string"));
    shapeIds.put("minItems", 2);
    shapeIds.put("description", "IDs of shapes to group together");
    properties.put("shapeIds", shapeIds);

    // Alternative: indices to group
    Map<String, Object> indices = new HashMap<>();
    indices.put("type", "array");
    indices.put("items", Map.of("type", "integer", "minimum", 0));
    indices.put("minItems", 2);
    indices.put("description", "Indices of shapes to group together");
    properties.put("indices", indices);

    // Group name (optional)
    properties.put("groupName", Map.of("type", "string", "description", "Name for the group"));

    schema.put("properties", properties);
    // Must have either shapeIds or indices
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeIds"}),
        Map.of("required", new String[] {"indices"})});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    Object shapeIdsRaw = parameters.get("shapeIds");
    Object indicesRaw = parameters.get("indices");
    String groupName = (String) parameters.get("groupName");

    List<String> shapeIds = null;
    if (shapeIdsRaw != null) {
      if (!(shapeIdsRaw instanceof List))
        throw new IllegalArgumentException("shapeIds must be array");
      @SuppressWarnings("unchecked")
      List<Object> tmp = (List<Object>) shapeIdsRaw;
      shapeIds = tmp.stream().map(o -> {
        if (!(o instanceof String))
          throw new IllegalArgumentException("shapeIds elements must be string");
        return (String) o;
      }).toList();
    }

    List<Number> indices = null;
    if (indicesRaw != null) {
      if (!(indicesRaw instanceof List))
        throw new IllegalArgumentException("indices must be array");
      @SuppressWarnings("unchecked")
      List<Object> tmp = (List<Object>) indicesRaw;
      indices = tmp.stream().map(o -> {
        if (!(o instanceof Number))
          throw new IllegalArgumentException("indices elements must be number");
        return (Number) o;
      }).toList();
    }

    if (shapeIds == null && indices == null) {
      throw new IllegalArgumentException("Either shapeIds or indices must be provided");
    }

    List<Shape> shapes = state.getShapes();
    List<String> groupedShapeIds = new ArrayList<>();

    if (shapeIds != null) {
      // Validate all shape IDs exist
      for (String id : shapeIds) {
        boolean found = false;
        for (Shape shape : shapes) {
          if (shape.getId().equals(id)) {
            groupedShapeIds.add(id);
            found = true;
            break;
          }
        }
        if (!found) {
          throw new IllegalArgumentException("Shape with ID '" + id + "' not found");
        }
      }
    } else {
      // Group by indices
      if (indices == null) {
        throw new IllegalArgumentException("indices must not be null");
      }
      for (Number indexNum : indices) {
        int idx = indexNum.intValue();
        if (idx < 0 || idx >= shapes.size()) {
          throw new IllegalArgumentException(
              "Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
        }
        groupedShapeIds.add(shapes.get(idx).getId());
      }
    }

    if (groupedShapeIds.size() < 2) {
      throw new IllegalArgumentException("At least 2 shapes are required to form a group");
    }

    // Generate group name if not provided
    if (groupName == null || groupName.isEmpty()) {
      groupName = "group_" + System.currentTimeMillis();
    }

    // Get current groups from state
    Map<String, List<Object>> arrays = new HashMap<>(state.getArrays());
    List<Object> groups = arrays.getOrDefault("shape_groups", new ArrayList<>());

    // Create new group
    Map<String, Object> newGroup = new HashMap<>();
    newGroup.put("name", groupName);
    newGroup.put("shapeIds", groupedShapeIds);
    newGroup.put("created", new Date().toString());

    // Add to groups list
    List<Object> updatedGroups = new ArrayList<>(groups);
    updatedGroups.add(newGroup);

    // Update arrays in state
    arrays.put("shape_groups", updatedGroups);

    // Create new state with updated groups
    return new DrawingState(state.getSettings(), state.getShapes(), arrays, state.getLists());
  }
}
