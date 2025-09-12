package com.example.infrastructure.services;

import com.example.application.services.StateService;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.services.ShapeOps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Initial in-memory ShapeOps implementation backed by StateService.
 *
 * NOTE: This mirrors existing behavior (immutable full-state copy on mutation). Optimization (index
 * maintenance) is minimal but isolates mutation pathways.
 */
public class InMemoryShapeOps implements ShapeOps {
  private final StateService stateService;
  // Simple index (id -> Shape) refreshed after each mutation. Could optimize diff updates later.
  private Map<String, Shape> index;

  public InMemoryShapeOps(StateService stateService) {
    this.stateService = stateService;
    rebuildIndex(stateService.getCurrentState());
  }

  private void rebuildIndex(DrawingState state) {
    this.index = new ConcurrentHashMap<>();
    for (Shape s : state.getShapes()) {
      index.put(s.getId(), s);
    }
  }

  @Override
  public DrawingState state() {
    return stateService.getCurrentState();
  }

  @Override
  public Optional<Shape> findById(String id) {
    if (id == null)
      return Optional.empty();
    Shape s = index.get(id);
    return Optional.ofNullable(s);
  }

  @Override
  public List<Shape> listAll() {
    return state().getShapes();
  }

  @Override
  public DrawingState add(Shape shape) {
    if (shape == null)
      return state();
    DrawingState newState = state().withAddedShape(shape);
    stateService.updateState(newState);
    index.put(shape.getId(), shape);
    return newState;
  }

  @Override
  public DrawingState replace(String id, Shape replacement) {
    if (id == null || replacement == null)
      return state();
    DrawingState current = state();
    List<Shape> shapes = current.getShapes();
    for (int i = 0; i < shapes.size(); i++) {
      if (id.equals(shapes.get(i).getId())) {
        DrawingState newState = current.withReplacedShape(i, replacement);
        stateService.updateState(newState);
        rebuildIndex(newState); // simple approach
        return newState;
      }
    }
    return current; // not found
  }

  @Override
  public DrawingState batchReplace(List<Shape> replacements) {
    if (replacements == null || replacements.isEmpty())
      return state();
    DrawingState current = state();
    DrawingState working = current;
    Set<String> ids = replacements.stream().map(Shape::getId).collect(Collectors.toSet());

    // Build map for quick fetch
    Map<String, Shape> replMap = new HashMap<>();
    for (Shape s : replacements)
      replMap.put(s.getId(), s);

    List<Shape> shapes = working.getShapes();
    for (int i = 0; i < shapes.size(); i++) {
      Shape existing = shapes.get(i);
      if (ids.contains(existing.getId())) {
        working = working.withReplacedShape(i, replMap.get(existing.getId()));
      }
    }
    if (working != current) {
      stateService.updateState(working);
      rebuildIndex(working);
    }
    return working;
  }

  @Override
  public DrawingState delete(String id) {
    if (id == null)
      return state();
    DrawingState current = state();
    List<Shape> shapes = current.getShapes();
    for (int i = 0; i < shapes.size(); i++) {
      if (id.equals(shapes.get(i).getId())) {
        DrawingState newState = current.withRemovedShape(i);
        stateService.updateState(newState);
        rebuildIndex(newState);
        return newState;
      }
    }
    return current;
  }

  @Override
  public DrawingState clearAll() {
    DrawingState current = state();
    if (current.getShapes().isEmpty()) {
      return current; // idempotent
    }
    DrawingState newState = current.withShapes(Collections.emptyList());
    stateService.updateState(newState);
    rebuildIndex(newState);
    return newState;
  }
}
