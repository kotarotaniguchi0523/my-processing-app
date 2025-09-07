package com.example.domain.commands;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.ArrayList;
import java.util.List;

/** Command to clear all shapes from the canvas */
public final class ClearCanvasCommand extends DrawCommand {
  private List<Shape> clearedShapes; // Store for undo

  public ClearCanvasCommand(String id, String description) {
    super(id, description);
  }

  public ClearCanvasCommand() {
    super("Clear canvas");
  }

  @Override
  public DrawingState execute(DrawingState currentState) {
    clearedShapes = new ArrayList<>(currentState.getShapes()); // Store for undo
    return currentState.withShapes(new ArrayList<>());
  }

  @Override
  public DrawingState undo(DrawingState currentState) {
    if (clearedShapes == null) {
      throw new IllegalStateException("Cannot undo - no shapes were cleared");
    }

    return currentState.withShapes(new ArrayList<>(clearedShapes));
  }

  @Override
  public boolean canExecute(DrawingState currentState) {
    return super.canExecute(currentState);
  }

  @Override
  public CommandType getType() {
    return CommandType.CANVAS;
  }

  @Override
  public CommandCost getCost() {
    return CommandCost.MEDIUM;
  }

  @Override
  public CommandPriority getPriority() {
    return CommandPriority.HIGH; // Canvas operations are usually high priority
  }

  public List<Shape> getClearedShapes() {
    return clearedShapes != null ? new ArrayList<>(clearedShapes) : new ArrayList<>();
  }

  public int getClearedShapeCount() {
    return clearedShapes != null ? clearedShapes.size() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ClearCanvasCommand)) return false;
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return String.format(
        "ClearCanvasCommand(id=%s, clearedCount=%d)", getId(), getClearedShapeCount());
  }
}
