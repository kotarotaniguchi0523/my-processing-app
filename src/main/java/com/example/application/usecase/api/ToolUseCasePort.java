package com.example.application.usecase.api;

import com.example.application.usecase.dto.*;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.Map;

/**
 * Abstraction (port) that tools depend on to execute application use cases without importing
 * concrete UseCaseFacade or infrastructure initializers.
 */
public interface ToolUseCasePort {
  // High-level use cases
  UseCaseResult<String> createRectangle(CreateRectangleInput input);

  UseCaseResult<String> moveShape(MoveShapeInput input);

  UseCaseResult<String> changeColor(ChangeColorInput input);

  UseCaseResult<String> deleteShape(DeleteShapeInput input);

  UseCaseResult<Integer> clearCanvas();

  UseCaseResult<Map<String, Object>> groupShapes(GroupShapesInput input);

  // Thin helpers currently exposed by facade (used by some tools)
  DrawingState addShape(Shape shape);

  DrawingState replaceShape(String shapeId, Shape newShape);
}
