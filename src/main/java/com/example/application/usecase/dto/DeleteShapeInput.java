package com.example.application.usecase.dto;

/** Input for deleting a single shape. */
public record DeleteShapeInput(String shapeId) {
  public DeleteShapeInput {
    if (shapeId == null || shapeId.isBlank()) {
      throw new IllegalArgumentException("shapeId is required");
    }
  }
}
