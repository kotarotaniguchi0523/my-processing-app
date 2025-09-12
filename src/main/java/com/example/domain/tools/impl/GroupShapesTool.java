package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.tools.IAgentTool;
import com.example.application.usecase.dto.GroupShapesInput;
import com.example.application.usecase.UseCaseFacade;

import java.util.*;

/**
 * Tool for grouping multiple shapes together Groups are stored as metadata in the DrawingState's
 * arrays
 */
public class GroupShapesTool implements IAgentTool {
  private UseCaseFacade facade;

  public GroupShapesTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

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
  public UseCaseResult<Map<String, Object>> execute(Map<String, Object> parameters,
      DrawingState state) {
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

    // Build target ids list
    List<String> groupedShapeIds;
    if (shapeIds != null) {
      groupedShapeIds = shapeIds;
    } else {
      // indices path (already validated numbers)
      if (indices == null || indices.isEmpty()) {
        return UseCaseResult.fail(state, "indices must not be empty");
      }
      List<String> byIndex = new ArrayList<>();
      List<com.example.domain.models.Shape> shapes = state.getShapes();
      for (Number n : indices) {
        int idx = n.intValue();
        if (idx < 0 || idx >= shapes.size()) {
          return UseCaseResult.fail(state, "index out of bounds: " + idx);
        }
        byIndex.add(shapes.get(idx).getId());
      }
      groupedShapeIds = byIndex;
    }

    if (facade == null) {
      // Fallback (should not happen once facade wiring is complete)
      return UseCaseResult.fail(state, "facade not initialized for grouping");
    }

    try {
      var result = facade.groupShapes(new GroupShapesInput(groupedShapeIds, groupName));
      if (result.success()) {
        return result; // already contains new state & payload
      }
      return UseCaseResult.fail(state, result.message());
    } catch (IllegalArgumentException ex) {
      return UseCaseResult.fail(state, ex.getMessage());
    }
  }
}
