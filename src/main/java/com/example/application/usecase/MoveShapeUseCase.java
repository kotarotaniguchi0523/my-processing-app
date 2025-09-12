package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.IMoveShape;
import com.example.application.usecase.dto.MoveShapeInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;
import com.example.domain.values.Position;
import com.example.domain.models.DrawingState;

/** UseCase: move existing shape */
public class MoveShapeUseCase implements IMoveShape {
  private final StateService stateService;

  public MoveShapeUseCase(StateService stateService) {
    this.stateService = stateService;
  }

  @Override
  public UseCaseResult<String> execute(MoveShapeInput input) {
    Shape shape = stateService.findShapeById(input.shapeId());
    if (shape == null) {
      return UseCaseResult.fail(stateService.getCurrentState(), "shape not found");
    }
    Shape moved = shape.moveTo(new Position(input.newX(), input.newY()));
    // rebuild shapes list
    var current = stateService.getCurrentState();
    var newShapes = current.getShapes();
    for (int i = 0; i < newShapes.size(); i++) {
      if (newShapes.get(i).getId().equals(shape.getId())) {
        newShapes.set(i, moved);
        break;
      }
    }
    DrawingState updated = current.withShapes(newShapes);
    stateService.updateState(updated);
    return UseCaseResult.ok(stateService.getCurrentState(), shape.getId(), "moved");
  }
}
