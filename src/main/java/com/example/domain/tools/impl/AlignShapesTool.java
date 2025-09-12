package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Position;
import com.example.application.usecase.UseCaseFacade; // central mutation
import com.example.application.usecase.dto.UseCaseResult;

import java.util.*;

/**
 * Tool for aligning multiple shapes
 */
public class AlignShapesTool implements IAgentTool {
  public enum AlignmentType {
    LEFT, RIGHT, CENTER_HORIZONTAL, TOP, BOTTOM, CENTER_VERTICAL, CENTER_BOTH
  }

  private UseCaseFacade facade;

  public AlignShapesTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

  @Override
  public String name() {
    return "align_shapes";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");
    Map<String, Object> properties = new HashMap<>();
    Map<String, Object> shapeIds = new HashMap<>();
    shapeIds.put("type", "array");
    shapeIds.put("items", Map.of("type", "string"));
    shapeIds.put("minItems", 2);
    shapeIds.put("description", "IDs of shapes to align");
    properties.put("shapeIds", shapeIds);
    Map<String, Object> indices = new HashMap<>();
    indices.put("type", "array");
    indices.put("items", Map.of("type", "integer", "minimum", 0));
    indices.put("minItems", 2);
    indices.put("description", "Indices of shapes to align");
    properties.put("indices", indices);
    properties.put("alignment",
        Map.of(
            "type", "string", "enum", new String[] {"left", "right", "center_horizontal", "top",
                "bottom", "center_vertical", "center_both"},
            "description", "How to align the shapes"));
    properties.put("referenceShapeId", Map.of("type", "string", "description",
        "ID of the shape to align others to (defaults to first shape)"));
    properties.put("referenceIndex", Map.of("type", "integer", "description",
        "Index of the shape to align others to", "minimum", 0));
    schema.put("properties", properties);
    schema.put("required", new String[] {"alignment"});
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeIds", "alignment"}),
        Map.of("required", new String[] {"indices", "alignment"})});
    return schema;
  }

  @Override
  public UseCaseResult<?> execute(Map<String, Object> parameters, DrawingState state) {
    Object shapeIdsRaw = parameters.get("shapeIds");
    Object indicesRaw = parameters.get("indices");
    String alignmentStr = (String) parameters.get("alignment");
    String referenceShapeId = (String) parameters.get("referenceShapeId");
    Number referenceIndex = (Number) parameters.get("referenceIndex");
    List<String> shapeIds = null;
    if (shapeIdsRaw != null) {
      if (!(shapeIdsRaw instanceof List))
        throw new IllegalArgumentException("shapeIds must be array");
      shapeIds = new ArrayList<>();
      for (Object o : (List<?>) shapeIdsRaw) {
        if (!(o instanceof String))
          throw new IllegalArgumentException("shapeIds elements must be string");
        shapeIds.add((String) o);
      }
    }
    List<Number> indices = null;
    if (indicesRaw != null) {
      if (!(indicesRaw instanceof List))
        throw new IllegalArgumentException("indices must be array");
      indices = new ArrayList<>();
      for (Object o : (List<?>) indicesRaw) {
        if (!(o instanceof Number))
          throw new IllegalArgumentException("indices elements must be number");
        indices.add((Number) o);
      }
    }
    if (shapeIds == null && indices == null)
      throw new IllegalArgumentException("Either shapeIds or indices must be provided");
    if (alignmentStr == null)
      throw new IllegalArgumentException("Alignment type is required");
    AlignmentType alignment;
    try {
      alignment = AlignmentType.valueOf(alignmentStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid alignment type: " + alignmentStr);
    }
    List<Shape> shapes = new ArrayList<>(state.getShapes());
    List<Integer> targetIndices = new ArrayList<>();
    List<Shape> targetShapes = new ArrayList<>();
    if (shapeIds != null) {
      for (String id : shapeIds) {
        boolean found = false;
        for (int i = 0; i < shapes.size(); i++) {
          if (shapes.get(i).getId().equals(id)) {
            targetIndices.add(i);
            targetShapes.add(shapes.get(i));
            found = true;
            break;
          }
        }
        if (!found)
          throw new IllegalArgumentException("Shape with ID '" + id + "' not found");
      }
    } else if (indices != null) {
      for (Number n : indices) {
        int idx = n.intValue();
        if (idx < 0 || idx >= shapes.size())
          throw new IllegalArgumentException(
              "Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
        targetIndices.add(idx);
        targetShapes.add(shapes.get(idx));
      }
    }
    if (targetShapes.size() < 2)
      throw new IllegalArgumentException("At least 2 shapes are required for alignment");
    Shape referenceShape;
    if (referenceShapeId != null) {
      referenceShape = targetShapes.stream().filter(s -> s.getId().equals(referenceShapeId))
          .findFirst().orElseThrow(() -> new IllegalArgumentException(
              "Reference shape with ID '" + referenceShapeId + "' not found in target shapes"));
    } else if (referenceIndex != null) {
      int refIdx = referenceIndex.intValue();
      if (refIdx < 0 || refIdx >= targetShapes.size())
        throw new IllegalArgumentException("Reference index out of bounds");
      referenceShape = targetShapes.get(refIdx);
    } else {
      referenceShape = targetShapes.get(0);
    }
    float referenceX = getReferenceX(referenceShape, alignment);
    float referenceY = getReferenceY(referenceShape, alignment);
    DrawingState currentState = state;
    if (facade == null)
      throw new IllegalStateException("UseCaseFacade not injected");
    for (Shape shape : targetShapes) {
      if (shape == referenceShape)
        continue;
      Position newPosition = calculateAlignedPosition(shape, alignment, referenceX, referenceY);
      if (!shape.getPosition().equals(newPosition)) {
        Shape alignedShape = shape.moveTo(newPosition);
        currentState = facade.replaceShape(shape.getId(), alignedShape);
      }
    }
    return UseCaseResult.ok(currentState, null, "aligned");
  }

  private float getReferenceX(Shape shape, AlignmentType alignment) {
    Shape.BoundingBox bounds = shape.getBounds();
    return switch (alignment) {
      case LEFT -> bounds.getMinX();
      case RIGHT -> bounds.getMaxX();
      case CENTER_HORIZONTAL, CENTER_BOTH -> bounds.getCenter().getX();
      default -> shape.getPosition().getX();
    };
  }

  private float getReferenceY(Shape shape, AlignmentType alignment) {
    Shape.BoundingBox bounds = shape.getBounds();
    return switch (alignment) {
      case TOP -> bounds.getMinY();
      case BOTTOM -> bounds.getMaxY();
      case CENTER_VERTICAL, CENTER_BOTH -> bounds.getCenter().getY();
      default -> shape.getPosition().getY();
    };
  }

  private Position calculateAlignedPosition(Shape shape, AlignmentType alignment, float referenceX,
      float referenceY) {
    Shape.BoundingBox bounds = shape.getBounds();
    Position currentPos = shape.getPosition();
    float newX = currentPos.getX();
    float newY = currentPos.getY();
    switch (alignment) {
      case LEFT -> newX = referenceX + (currentPos.getX() - bounds.getMinX());
      case RIGHT -> newX = referenceX - (bounds.getMaxX() - currentPos.getX());
      case CENTER_HORIZONTAL -> newX =
          referenceX - (bounds.getWidth() / 2) + (currentPos.getX() - bounds.getMinX());
      case TOP -> newY = referenceY + (currentPos.getY() - bounds.getMinY());
      case BOTTOM -> newY = referenceY - (bounds.getMaxY() - currentPos.getY());
      case CENTER_VERTICAL -> newY =
          referenceY - (bounds.getHeight() / 2) + (currentPos.getY() - bounds.getMinY());
      case CENTER_BOTH -> {
        newX = referenceX - (bounds.getWidth() / 2) + (currentPos.getX() - bounds.getMinX());
        newY = referenceY - (bounds.getHeight() / 2) + (currentPos.getY() - bounds.getMinY());
      }
    }
    return new Position(newX, newY);
  }
}
