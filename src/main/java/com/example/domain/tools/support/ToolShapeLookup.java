package com.example.domain.tools.support;

import com.example.domain.models.Shape;
import java.util.List;

/** Common shape lookup (by id or index) pattern reused in multiple tools. */
public final class ToolShapeLookup {
  private ToolShapeLookup() {}

  public static Shape findByIdOrIndex(List<Shape> shapes, String id, Number index, String context) {
    if (id == null && index == null) {
      throw new IllegalArgumentException("Either shapeId or index must be provided");
    }
    if (id != null) {
      for (Shape s : shapes) if (s.getId().equals(id)) return s;
      throw new IllegalArgumentException("Shape with ID '" + id + "' not found");
    }
    int idx = index.intValue();
    if (idx < 0 || idx >= shapes.size()) {
      throw new IllegalArgumentException("Index " + idx + " is out of bounds. Valid range: 0-" + (shapes.size() - 1));
    }
    return shapes.get(idx);
  }
}
