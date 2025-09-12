package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.domain.services.ShapeOps;
import com.example.application.usecase.api.ICreateRectangle;
import com.example.application.usecase.dto.CreateRectangleInput;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.models.Rectangle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

/** UseCase: Create a Rectangle */
public class CreateRectangleUseCase implements ICreateRectangle {
  private final StateService stateService;
  private final ShapeOps shapeOps; // mandatory

  public CreateRectangleUseCase(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
  }

  @Override
  public UseCaseResult<String> execute(CreateRectangleInput input) {
    if (input.width() <= 0 || input.height() <= 0) {
      return UseCaseResult.fail(stateService.getCurrentState(), "width/height must be positive");
    }
    Style style = Style.builder().fillColor(input.fillColor()).strokeColor(input.strokeColor())
        .strokeWeight(input.strokeWeight()).hasFill(input.hasFill()).hasStroke(input.hasStroke())
        .build();
    Rectangle rect =
        new Rectangle(new Position(input.x(), input.y()), input.width(), input.height(), style);
    var newState = shapeOps.add(rect);
    return UseCaseResult.ok(newState, rect.getId(), "created");
  }
}
