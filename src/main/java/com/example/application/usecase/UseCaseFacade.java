package com.example.application.usecase;

import com.example.application.services.StateService;
import com.example.application.usecase.api.*;
import com.example.application.usecase.dto.*;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.List;

/**
 * Aggregates individual use cases behind a simple API. Later this will hide repositories /
 * transaction / history composition.
 */
public class UseCaseFacade {
  private final ICreateRectangle createRectangleUseCase;
  private final IMoveShape moveShapeUseCase;
  private final IChangeColor changeColorUseCase;
  private final IDeleteShape deleteShapeUseCase;
  private final IClearCanvas clearCanvasUseCase;
  private final StateService stateService;

  public UseCaseFacade(StateService stateService) {
    // In future: pass repositories or services
    this.stateService = stateService;
    this.createRectangleUseCase = new CreateRectangleUseCase(stateService);
    this.moveShapeUseCase = new MoveShapeUseCase(stateService);
    this.changeColorUseCase = new ChangeColorUseCase(stateService);
    this.deleteShapeUseCase = new DeleteShapeUseCase(stateService);
    this.clearCanvasUseCase = new ClearCanvasUseCase(stateService);
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

  /**
   * Temporary accessor until unified UseCaseResult provides newState in later steps. Tools should
   * call this after successful facade invocation to obtain the latest immutable DrawingState.
   */
  public DrawingState getCurrentState() {
    return stateService.getCurrentState();
  }

  // =============================================================
  // Phase A1: Centralized state mutation helpers (temporary static)
  // All direct structural modifications to DrawingState (add/replace/remove)
  // must happen only here or in dedicated UseCase classes. Tools call these
  // helpers instead of invoking withAddedShape / withReplacedShape directly.
  // Later phases will route through repositories + history.
  // =============================================================

  /** Append a shape immutably to the provided state. */
  public static DrawingState addShape(DrawingState state, Shape shape) {
    if (state == null || shape == null)
      return state; // defensive no-op
    return state.withAddedShape(shape);
  }

  /**
   * Replace a shape identified by id with a new instance. If not found, returns original state.
   */
  public static DrawingState replaceShape(DrawingState state, String shapeId, Shape newShape) {
    if (state == null || shapeId == null || newShape == null)
      return state;
    List<Shape> shapes = state.getShapes();
    for (int i = 0; i < shapes.size(); i++) {
      if (shapeId.equals(shapes.get(i).getId())) {
        return state.withReplacedShape(i, newShape);
      }
    }
    return state; // not found
  }

  /** Bulk replace a list of shapes (matched by id) returning the updated state. */
  public static DrawingState replaceShapes(DrawingState state, List<Shape> replacements) {
    if (state == null || replacements == null || replacements.isEmpty())
      return state;
    DrawingState current = state;
    for (Shape s : replacements) {
      current = replaceShape(current, s.getId(), s);
    }
    return current;
  }
}
