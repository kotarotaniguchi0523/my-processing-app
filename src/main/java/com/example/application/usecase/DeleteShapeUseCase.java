package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.domain.services.ShapeOps;
import com.example.application.usecase.api.IDeleteShape;
import com.example.application.usecase.dto.DeleteShapeInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;

/** Implementation of deleting a shape. */
class DeleteShapeUseCase implements IDeleteShape {
  private final StateService stateService;
  private final ShapeOps shapeOps;

  DeleteShapeUseCase(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<String> execute(DeleteShapeInput input) {
    Shape existing = shapeOps.findById(input.shapeId()).orElse(null);
    if (existing == null) {
      return UseCaseResult.fail(stateService.getCurrentState(), "shape not found");
    }
    var newState = shapeOps.delete(input.shapeId());
    return UseCaseResult.ok(newState, input.shapeId(), "deleted");
  }
}
