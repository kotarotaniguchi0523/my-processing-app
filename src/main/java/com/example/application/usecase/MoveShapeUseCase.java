package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.domain.services.ShapeOps;
import com.example.application.usecase.api.IMoveShape;
import com.example.application.usecase.dto.MoveShapeInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;
import com.example.domain.values.Position;
import com.example.domain.models.DrawingState;

/** UseCase: move existing shape */
public class MoveShapeUseCase implements IMoveShape {
  private final StateService stateService;
  private final ShapeOps shapeOps;

  public MoveShapeUseCase(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<String> execute(MoveShapeInput input) {
    Shape shape = shapeOps.findById(input.shapeId()).orElse(null);
    if (shape == null) {
      return UseCaseResult.fail(stateService.getCurrentState(), "shape not found");
    }
    Shape moved = shape.moveTo(new Position(input.newX(), input.newY()));
    DrawingState newState = shapeOps.replace(shape.getId(), moved);
    return UseCaseResult.ok(newState, shape.getId(), "moved");
  }
}
