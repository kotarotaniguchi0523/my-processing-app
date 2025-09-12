package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.domain.services.ShapeOps;
import com.example.application.usecase.api.IChangeColor;
import com.example.application.usecase.dto.ChangeColorInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Shape;
import com.example.domain.values.Style;

/** UseCase: change shape colors (fill/stroke) */
public class ChangeColorUseCase implements IChangeColor {
  private final StateService stateService;
  private final ShapeOps shapeOps;

  public ChangeColorUseCase(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<String> execute(ChangeColorInput input) {
    Shape shape = shapeOps.findById(input.shapeId()).orElse(null);
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
    var newState = shapeOps.replace(shape.getId(), newShape);
    return UseCaseResult.ok(newState, shape.getId(), "style_changed");
  }
}
