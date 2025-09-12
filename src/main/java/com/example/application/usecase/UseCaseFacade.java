package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.*;
import com.example.application.usecase.dto.*;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.services.ShapeOps;


public class UseCaseFacade implements com.example.application.usecase.api.ToolUseCasePort {
  private final ICreateRectangle createRectangleUseCase;
  private final IMoveShape moveShapeUseCase;
  private final IChangeColor changeColorUseCase;
  private final IDeleteShape deleteShapeUseCase;
  private final IClearCanvas clearCanvasUseCase;
  private final IGroupShapes groupShapesUseCase;
  private final StateService stateService;
  private final ShapeOps shapeOps;

  public UseCaseFacade(StateService stateService, ShapeOps shapeOps) {
    this.stateService = stateService;
    this.shapeOps = shapeOps;
    this.createRectangleUseCase = new CreateRectangleUseCase(stateService, shapeOps);
    this.moveShapeUseCase = new MoveShapeUseCase(stateService, shapeOps);
    this.changeColorUseCase = new ChangeColorUseCase(stateService, shapeOps);
    this.deleteShapeUseCase = new DeleteShapeUseCase(stateService, shapeOps);
    this.clearCanvasUseCase = new ClearCanvasUseCase(stateService, shapeOps); // enable clearAll()
    this.groupShapesUseCase = new GroupShapesUseCase(stateService, shapeOps);
  }

  public UseCaseResult<String> createRectangle(CreateRectangleInput input) {
    return createRectangleUseCase.execute(input);
  }

  public UseCaseResult<String> moveShape(MoveShapeInput input) {
    return moveShapeUseCase.execute(input);
  }

  public UseCaseResult<String> changeColor(ChangeColorInput input) {
    return changeColorUseCase.execute(input);
  }

  public UseCaseResult<String> deleteShape(DeleteShapeInput input) {
    return deleteShapeUseCase.execute(input);
  }

  public UseCaseResult<Integer> clearCanvas() {
    return clearCanvasUseCase.execute();
  }

  public UseCaseResult<java.util.Map<String, Object>> groupShapes(GroupShapesInput input) {
    return groupShapesUseCase.execute(input);
  }

  // Instance helpers (thin wrappers delegating to ShapeOps)
  public DrawingState addShape(Shape shape) {
    if (shape == null)
      return stateService.getCurrentState();
    return shapeOps.add(shape);
  }

  public DrawingState replaceShape(String shapeId, Shape newShape) {
    if (shapeId == null || newShape == null)
      return stateService.getCurrentState();
    return shapeOps.replace(shapeId, newShape);
  }

}
