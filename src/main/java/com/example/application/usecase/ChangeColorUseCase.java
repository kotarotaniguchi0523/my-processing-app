package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.IChangeColor;
import com.example.application.usecase.dto.ChangeColorInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;
import com.example.domain.values.Style;
import com.example.domain.models.DrawingState;

/** UseCase: change shape colors (fill/stroke) */
public class ChangeColorUseCase implements IChangeColor {
  private final StateService stateService;

  public ChangeColorUseCase(StateService stateService) {
    this.stateService = stateService;
  }

  @Override
  public UseCaseResult<String> execute(ChangeColorInput input) {
    Shape shape = stateService.findShapeById(input.shapeId());
    if (shape == null) {
      return UseCaseResult.fail(stateService.getCurrentState(), "shape not found");
    }
    Style base = shape.getStyle();
    Style newStyle = Style.builder()
        .fillColor(input.fillColor() != null ? input.fillColor() : base.getFillColor())
        .strokeColor(input.strokeColor() != null ? input.strokeColor() : base.getStrokeColor())
        .strokeWeight(base.getStrokeWeight()).hasFill(base.hasFill()).hasStroke(base.hasStroke())
        .build();
    var newShape = shape.withStyle(newStyle);
    var current = stateService.getCurrentState();
    var newShapes = current.getShapes();
    for (int i = 0; i < newShapes.size(); i++) {
      if (newShapes.get(i).getId().equals(shape.getId())) {
        newShapes.set(i, newShape);
        break;
      }
    }
    DrawingState updated = current.withShapes(newShapes);
    stateService.updateState(updated);
    return UseCaseResult.ok(stateService.getCurrentState(), shape.getId(), "style_changed");
  }
}
