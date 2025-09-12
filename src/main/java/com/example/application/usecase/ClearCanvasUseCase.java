package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.IClearCanvas;
import com.example.application.usecase.dto.UseCaseResult;

/** Implementation of clearing all shapes. */
class ClearCanvasUseCase implements IClearCanvas {
  private final StateService stateService;

  ClearCanvasUseCase(StateService stateService) {
    this.stateService = stateService;
  }

  @Override
  public UseCaseResult<Integer> execute() {
    int count = stateService.getAllShapes().size();
    if (count == 0) {
      return UseCaseResult.ok(stateService.getCurrentState(), 0, "cleared"); // idempotent
    }
    stateService.clearAllShapes();
    return UseCaseResult.ok(stateService.getCurrentState(), count, "cleared");
  }
}
