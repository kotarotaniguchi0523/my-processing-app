package com.example.application.usecase.dto;

import java.util.List;

/** Input DTO for grouping shapes. */
public record GroupShapesInput(List<String> shapeIds, String groupName) {
  public GroupShapesInput {
    if (shapeIds == null || shapeIds.size() < 2) {
      throw new IllegalArgumentException("At least two shapeIds required");
    }
  }
}
