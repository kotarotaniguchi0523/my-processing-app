package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.IGroupShapes;
import com.example.application.usecase.dto.GroupShapesInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.services.ShapeOps;

import java.util.*;

/** Implementation for grouping shapes by id: stores grouping metadata under arrays.shape_groups. */
class GroupShapesUseCase implements IGroupShapes {
  private final StateService stateService;
  private final ShapeOps shapeOps; // optional transitional

  GroupShapesUseCase(StateService stateService) {
    this.stateService = stateService;
    this.shapeOps = null;
  }

  GroupShapesUseCase(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<Map<String, Object>> execute(GroupShapesInput input) {
    List<String> ids = input.shapeIds();
    // Validate existence via ShapeOps or fallback linear scan
    DrawingState current = shapeOps != null ? shapeOps.state() : stateService.getCurrentState();
    List<Shape> shapes = current.getShapes();
    Set<String> existingIds = new HashSet<>();
    for (Shape s : shapes) existingIds.add(s.getId());
    for (String id : ids) {
      if (!existingIds.contains(id)) {
        return UseCaseResult.fail(current, "shape not found: " + id);
      }
    }
    String groupName = (input.groupName() == null || input.groupName().isBlank())
        ? "group_" + System.currentTimeMillis()
        : input.groupName();

    // Build new arrays map updating shape_groups
    Map<String, List<Object>> arrays = current.getArrays();
    List<Object> groups = arrays.getOrDefault("shape_groups", new ArrayList<>());
    Map<String, Object> newGroup = new HashMap<>();
    newGroup.put("name", groupName);
    newGroup.put("shapeIds", List.copyOf(ids));
    newGroup.put("created", new Date().toString());
    List<Object> updatedGroups = new ArrayList<>(groups);
    updatedGroups.add(newGroup);
    arrays.put("shape_groups", updatedGroups);

    DrawingState newState = new DrawingState(current.getSettings(), current.getShapes(), arrays, current.getLists());
    // Propagate state mutation through StateService for history if ShapeOps present (since ShapeOps currently owns mutating ops)
    if (shapeOps != null) {
      stateService.updateState(newState); // triggers history save & validation
    } else {
      stateService.updateState(newState);
    }
    Map<String, Object> payload = Map.of("groupName", groupName, "shapeIds", ids);
    return UseCaseResult.ok(newState, payload, "grouped");
  }
}
