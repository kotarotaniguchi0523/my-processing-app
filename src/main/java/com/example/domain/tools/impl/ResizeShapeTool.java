package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.domain.models.*;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Position;
import com.example.application.usecase.UseCaseFacade; // Phase A1 central mutation

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for resizing shapes
 */
public class ResizeShapeTool implements IAgentTool {

  @Override
  public String name() {
    return "resize_shape";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // Shape ID to resize
    properties.put("shapeId", Map.of("type", "string", "description", "ID of the shape to resize"));

    // Alternative: resize by index
    properties.put("index", Map.of("type", "integer", "description",
        "Index of the shape to resize (0-based)", "minimum", 0));

    // Scale factor (uniform scaling)
    properties.put("scale",
        Map.of("type", "number", "description",
            "Scale factor (1.0 = no change, 2.0 = double size, 0.5 = half size)", "minimum", 0.1,
            "maximum", 10.0));

    // New size (for rectangles)
    Map<String, Object> newSize = new HashMap<>();
    newSize.put("type", "object");
    Map<String, Object> sizeProps = new HashMap<>();
    sizeProps.put("width", Map.of("type", "number", "minimum", 1));
    sizeProps.put("height", Map.of("type", "number", "minimum", 1));
    newSize.put("properties", sizeProps);
    properties.put("newSize", newSize);

    // New radius (for circles)
    properties.put("newRadius",
        Map.of("type", "number", "description", "New radius for circles", "minimum", 1));

    schema.put("properties", properties);
    // Must have either shapeId or index
    schema.put("oneOf", new Map[] {Map.of("required", new String[] {"shapeId"}),
        Map.of("required", new String[] {"index"})});

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    String shapeId = (String) parameters.get("shapeId");
    Number index = (Number) parameters.get("index");
    Number scale = Parameters.num(parameters, "scale", false);
    Map<String, Object> newSizeMap = Parameters.obj(parameters, "newSize", false);
    Number newRadius = Parameters.num(parameters, "newRadius", false);

    if (shapeId == null && index == null) {
      throw new IllegalArgumentException("Either shapeId or index must be provided");
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

    // Resize based on shape type
    Shape resizedShape;

    if (targetShape instanceof Circle) {
      Circle circle = (Circle) targetShape;
      float newR;

      if (newRadius != null) {
        newR = newRadius.floatValue();
      } else if (scale != null) {
        newR = circle.getRadius() * scale.floatValue();
      } else {
        throw new IllegalArgumentException("For circles, provide either newRadius or scale");
      }

      resizedShape = new Circle(circle.getPosition(), newR, circle.getStyle());

    } else if (targetShape instanceof Rectangle) {
      Rectangle rect = (Rectangle) targetShape;
      float newWidth, newHeight;

      if (newSizeMap != null) {
        Number w = Parameters.num(newSizeMap, "width", false);
        Number h = Parameters.num(newSizeMap, "height", false);
        newWidth = w != null ? w.floatValue() : rect.getWidth();
        newHeight = h != null ? h.floatValue() : rect.getHeight();
      } else if (scale != null) {
        newWidth = rect.getWidth() * scale.floatValue();
        newHeight = rect.getHeight() * scale.floatValue();
      } else {
        throw new IllegalArgumentException("For rectangles, provide either newSize or scale");
      }

      resizedShape = new Rectangle(rect.getPosition(), newWidth, newHeight, rect.getStyle());

    } else if (targetShape instanceof Line) {
      Line line = (Line) targetShape;

      if (scale != null) {
        // Scale line from its start point
        Position start = line.getStartPosition();
        Position end = line.getEndPosition();
        float dx = end.getX() - start.getX();
        float dy = end.getY() - start.getY();

        Position newEnd = new Position(start.getX() + dx * scale.floatValue(),
            start.getY() + dy * scale.floatValue());

        resizedShape = new Line(start, newEnd, line.getStyle());
      } else {
        throw new IllegalArgumentException("For lines, provide scale factor");
      }

    } else if (targetShape instanceof Triangle) {
      Triangle triangle = (Triangle) targetShape;

      if (scale != null) {
        // Scale triangle from its centroid
        Position centroid = triangle.getCentroid();
        float s = scale.floatValue();

        Position newP1 = scalePoint(triangle.getP1(), centroid, s);
        Position newP2 = scalePoint(triangle.getP2(), centroid, s);
        Position newP3 = scalePoint(triangle.getP3(), centroid, s);

        resizedShape = new Triangle(triangle.getId(), triangle.getPosition(), newP1, newP2, newP3,
            triangle.getStyle(), triangle.isVisible(), triangle.getLayerOrder());
      } else {
        throw new IllegalArgumentException("For triangles, provide scale factor");
      }

    } else if (targetShape instanceof Text) {
      Text text = (Text) targetShape;

      if (scale != null) {
        // Scale font size
        float newFontSize = text.getFontSize() * scale.floatValue();
        resizedShape = text.withFontSize(newFontSize);
      } else {
        throw new IllegalArgumentException("For text, provide scale factor");
      }

    } else {
      throw new IllegalArgumentException(
          "Unsupported shape type for resizing: " + targetShape.getClass().getSimpleName());
    }


    // Replace shape directly (find index and immutable replace)
    // Centralized replacement
    return UseCaseFacade.replaceShape(state, targetShape.getId(), resizedShape);
  }

  private Position scalePoint(Position point, Position center, float scale) {
    float dx = point.getX() - center.getX();
    float dy = point.getY() - center.getY();
    return new Position(center.getX() + dx * scale, center.getY() + dy * scale);
  }

  // local helpers removed (centralized)
}
