package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.IDeleteShape;
import com.example.application.usecase.dto.DeleteShapeInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;

/** Implementation of deleting a shape. */
class DeleteShapeUseCase implements IDeleteShape {
  private final StateService stateService;

  DeleteShapeUseCase(StateService stateService) {
    this.stateService = stateService;
  }

  @Override
  public UseCaseResult<String> execute(DeleteShapeInput input) {
    Shape existing = stateService.findShapeById(input.shapeId());
    if (existing == null) {
      return UseCaseResult.fail(stateService.getCurrentState(), "shape not found");
    }
    stateService.removeShape(input.shapeId());
    return UseCaseResult.ok(stateService.getCurrentState(), input.shapeId(), "deleted");
  }
}
