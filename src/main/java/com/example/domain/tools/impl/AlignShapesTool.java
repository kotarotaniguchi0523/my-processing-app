package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Position;
import com.example.application.usecase.UseCaseFacade; // Phase A1 central mutation

import java.util.*;

/**
 * Tool for aligning multiple shapes
 */
public class AlignShapesTool implements IAgentTool {

  public enum AlignmentType {
    LEFT, RIGHT, CENTER_HORIZONTAL, TOP, BOTTOM, CENTER_VERTICAL, CENTER_BOTH
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

    // Shape IDs to align
    Map<String, Object> shapeIds = new HashMap<>();
    shapeIds.put("type", "array");
    shapeIds.put("items", Map.of("type", "string"));
    shapeIds.put("minItems", 2);
    shapeIds.put("description", "IDs of shapes to align");
    properties.put("shapeIds", shapeIds);

    // Alternative: indices to align
    Map<String, Object> indices = new HashMap<>();
    indices.put("type", "array");
    indices.put("items", Map.of("type", "integer", "minimum", 0));
    indices.put("minItems", 2);
    indices.put("description", "Indices of shapes to align");
    properties.put("indices", indices);

    // Alignment type
    properties.put("alignment",
        Map.of(
            "type", "string", "enum", new String[] {"left", "right", "center_horizontal", "top",
                "bottom", "center_vertical", "center_both"},
            "description", "How to align the shapes"));

    // Reference shape (optional - first shape is used by default)
    properties.put("referenceShapeId", Map.of("type", "string", "description",
        "ID of the shape to align others to (defaults to first shape)"));

    properties.put("referenceIndex", Map.of("type", "integer", "description",
        "Index of the shape to align others to", "minimum", 0));

    schema.put("properties", properties);
    // Must have alignment and either shapeIds or indices
    schema.put("required", new String[] {"alignment"});
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeIds", "alignment"}),
        Map.of("required", new String[] {"indices", "alignment"})});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    Object shapeIdsRaw = parameters.get("shapeIds");
    Object indicesRaw = parameters.get("indices");
    String alignmentStr = (String) parameters.get("alignment");
    String referenceShapeId = (String) parameters.get("referenceShapeId");
    Number referenceIndex = (Number) parameters.get("referenceIndex");

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

    if (alignmentStr == null) {
      throw new IllegalArgumentException("Alignment type is required");
    }

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
      // Find shapes by ID
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
        if (!found) {
          throw new IllegalArgumentException("Shape with ID '" + id + "' not found");
        }
      }
    } else {
      // Use indices
      if (indices == null) {
        throw new IllegalArgumentException("indices must not be null");
      }
      for (Number indexNum : indices) {
        int idx = indexNum.intValue();
        if (idx < 0 || idx >= shapes.size()) {
          throw new IllegalArgumentException(
              "Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
        }
        targetIndices.add(idx);
        targetShapes.add(shapes.get(idx));
      }
    }

    if (targetShapes.size() < 2) {
      throw new IllegalArgumentException("At least 2 shapes are required for alignment");
    }

    // Determine reference shape
    Shape referenceShape;
    if (referenceShapeId != null) {
      referenceShape = targetShapes.stream().filter(s -> s.getId().equals(referenceShapeId))
          .findFirst().orElseThrow(() -> new IllegalArgumentException(
              "Reference shape with ID '" + referenceShapeId + "' not found in target shapes"));
    } else if (referenceIndex != null) {
      int refIdx = referenceIndex.intValue();
      if (refIdx < 0 || refIdx >= targetShapes.size()) {
        throw new IllegalArgumentException("Reference index out of bounds");
      }
      referenceShape = targetShapes.get(refIdx);
    } else {
      // Use first shape as reference
      referenceShape = targetShapes.get(0);
    }

    // Calculate reference position based on alignment
    float referenceX = getReferenceX(referenceShape, alignment);
    float referenceY = getReferenceY(referenceShape, alignment);

    // Create new state by applying alignments
    DrawingState currentState = state;

    for (int i = 0; i < targetShapes.size(); i++) {
      Shape shape = targetShapes.get(i);

      if (shape == referenceShape) {
        continue; // Skip reference shape
      }

      Position newPosition = calculateAlignedPosition(shape, alignment, referenceX, referenceY);

      if (!shape.getPosition().equals(newPosition)) {
        Shape alignedShape = shape.moveTo(newPosition);
        currentState = UseCaseFacade.replaceShape(currentState, shape.getId(), alignedShape);
      }
    }

    return currentState;
  }

  private float getReferenceX(Shape shape, AlignmentType alignment) {
    Shape.BoundingBox bounds = shape.getBounds();
    switch (alignment) {
      case LEFT:
        return bounds.getMinX();
      case RIGHT:
        return bounds.getMaxX();
      case CENTER_HORIZONTAL:
      case CENTER_BOTH:
        return bounds.getCenter().getX();
      default:
        return shape.getPosition().getX();
    }
  }

  private float getReferenceY(Shape shape, AlignmentType alignment) {
    Shape.BoundingBox bounds = shape.getBounds();
    switch (alignment) {
      case TOP:
        return bounds.getMinY();
      case BOTTOM:
        return bounds.getMaxY();
      case CENTER_VERTICAL:
      case CENTER_BOTH:
        return bounds.getCenter().getY();
      default:
        return shape.getPosition().getY();
    }
  }

  private Position calculateAlignedPosition(Shape shape, AlignmentType alignment, float referenceX,
      float referenceY) {
    Shape.BoundingBox bounds = shape.getBounds();
    Position currentPos = shape.getPosition();
    float newX = currentPos.getX();
    float newY = currentPos.getY();

    switch (alignment) {
      case LEFT:
        newX = referenceX + (currentPos.getX() - bounds.getMinX());
        break;
      case RIGHT:
        newX = referenceX - (bounds.getMaxX() - currentPos.getX());
        break;
      case CENTER_HORIZONTAL:
        newX = referenceX - (bounds.getWidth() / 2) + (currentPos.getX() - bounds.getMinX());
        break;
      case TOP:
        newY = referenceY + (currentPos.getY() - bounds.getMinY());
        break;
      case BOTTOM:
        newY = referenceY - (bounds.getMaxY() - currentPos.getY());
        break;
      case CENTER_VERTICAL:
        newY = referenceY - (bounds.getHeight() / 2) + (currentPos.getY() - bounds.getMinY());
        break;
      case CENTER_BOTH:
        newX = referenceX - (bounds.getWidth() / 2) + (currentPos.getX() - bounds.getMinX());
        newY = referenceY - (bounds.getHeight() / 2) + (currentPos.getY() - bounds.getMinY());
        break;
    }

    return new Position(newX, newY);
  }
}
