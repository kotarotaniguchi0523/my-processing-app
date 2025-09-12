package com.example.domain.tools.impl;

import com.example.adapter.tools.Parameters;
import com.example.domain.models.*;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Position;
import com.example.application.usecase.UseCaseFacade; // central mutation
import com.example.application.usecase.dto.UseCaseResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for resizing shapes
 */
public class ResizeShapeTool implements IAgentTool {
  private UseCaseFacade facade;

  public ResizeShapeTool withFacade(UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

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
  public UseCaseResult<String> execute(Map<String, Object> parameters, DrawingState state) {
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
      for (Shape s : shapes) {
        if (s.getId().equals(shapeId)) {
          targetShape = s;
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

    if (targetShape instanceof Circle c) {
      float newR;

      if (newRadius != null) {
        newR = newRadius.floatValue();
      } else if (scale != null) {
        newR = c.getRadius() * scale.floatValue();
      } else {
        throw new IllegalArgumentException("For circles, provide either newRadius or scale");
      }

      resizedShape = new Circle(c.getPosition(), newR, c.getStyle());

    } else if (targetShape instanceof Rectangle r) {
      float newWidth, newHeight;

      if (newSizeMap != null) {
        Number w = Parameters.num(newSizeMap, "width", false);
        Number h = Parameters.num(newSizeMap, "height", false);
        newWidth = w != null ? w.floatValue() : r.getWidth();
        newHeight = h != null ? h.floatValue() : r.getHeight();
      } else if (scale != null) {
        newWidth = r.getWidth() * scale.floatValue();
        newHeight = r.getHeight() * scale.floatValue();
      } else {
        throw new IllegalArgumentException("For rectangles, provide either newSize or scale");
      }

      resizedShape = new Rectangle(r.getPosition(), newWidth, newHeight, r.getStyle());

    } else if (targetShape instanceof Line l) {
      if (scale != null) {
        // Scale line from its start point
        Position start = l.getStartPosition();
        Position end = l.getEndPosition();
        float dx = end.getX() - start.getX();
        float dy = end.getY() - start.getY();

        Position newEnd = new Position(start.getX() + dx * scale.floatValue(),
            start.getY() + dy * scale.floatValue());

        resizedShape = new Line(start, newEnd, l.getStyle());
      } else {
        throw new IllegalArgumentException("For lines, provide scale factor");
      }

    } else if (targetShape instanceof Triangle t) {
      if (scale != null) {
        // Scale triangle from its centroid
        Position centroid = t.getCentroid();
        float s = scale.floatValue();

        Position newP1 = scalePoint(t.getP1(), centroid, s);
        Position newP2 = scalePoint(t.getP2(), centroid, s);
        Position newP3 = scalePoint(t.getP3(), centroid, s);

        resizedShape = new Triangle(t.getId(), t.getPosition(), newP1, newP2, newP3, t.getStyle(),
            t.isVisible(), t.getLayerOrder());
      } else {
        throw new IllegalArgumentException("For triangles, provide scale factor");
      }

    } else if (targetShape instanceof Text txt) {
      if (scale != null) {
        // Scale font size
        float newFontSize = txt.getFontSize() * scale.floatValue();
        resizedShape = txt.withFontSize(newFontSize);
      } else {
        throw new IllegalArgumentException("For text, provide scale factor");
      }

    } else {
      throw new IllegalArgumentException(
          "Unsupported shape type for resizing: " + targetShape.getClass().getSimpleName());
    }

    if (facade == null)
      throw new IllegalStateException("UseCaseFacade not injected");
    // Centralized replacement
    DrawingState newState = facade.replaceShape(targetShape.getId(), resizedShape);
    return UseCaseResult.ok(newState, targetShape.getId(), "resized");
  }

  private Position scalePoint(Position point, Position center, float scale) {
    float dx = point.getX() - center.getX();
    float dy = point.getY() - center.getY();
    return new Position(center.getX() + dx * scale, center.getY() + dy * scale);
  }

  // local helpers removed (centralized)
}
