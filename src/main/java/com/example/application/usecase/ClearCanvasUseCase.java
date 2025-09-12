package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.domain.services.ShapeOps;
import com.example.application.usecase.api.IClearCanvas;
import com.example.application.usecase.dto.UseCaseResult;

/** Implementation of clearing all shapes. */
class ClearCanvasUseCase implements IClearCanvas {
  private final ShapeOps shapeOps;

  ClearCanvasUseCase(StateService stateService, ShapeOps shapeOps) {
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<Integer> execute() {
    int count = shapeOps.listAll().size();
    if (count == 0) {
      return UseCaseResult.ok(shapeOps.state(), 0, "cleared"); // idempotent
    }
    var newState = shapeOps.clearAll();
    return UseCaseResult.ok(newState, count, "cleared");
  }
}
