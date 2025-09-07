package com.example.domain.commands;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.Objects;

/** Command to add a shape to the drawing */
public final class AddShapeCommand extends DrawCommand {
  private final Shape shape;
  private final int insertIndex;

  public AddShapeCommand(String id, Shape shape, int insertIndex, String description) {
    super(id, description);
    this.shape = Objects.requireNonNull(shape, "Shape cannot be null");
    this.insertIndex = insertIndex;
  }

  public AddShapeCommand(Shape shape, int insertIndex) {
    super("Add " + shape.getType().getTypeName());
    this.shape = shape;
    this.insertIndex = insertIndex;
  }

  public AddShapeCommand(Shape shape) {
    this(shape, -1); // -1 means add to end
  }

  @Override
  public DrawingState execute(DrawingState currentState) {
    if (insertIndex < 0 || insertIndex >= currentState.getShapes().size()) {
      return currentState.withAddedShape(shape);
    } else {
      var shapes = currentState.getShapes();
      shapes.add(insertIndex, shape);
      return currentState.withShapes(shapes);
    }
  }

  @Override
  public DrawingState undo(DrawingState currentState) {
    var shapes = currentState.getShapes();

    // Find and remove the shape by ID
    for (int i = 0; i < shapes.size(); i++) {
      if (shapes.get(i).getId().equals(shape.getId())) {
        return currentState.withRemovedShape(i);
      }
    }

    return currentState; // Shape not found, return unchanged
  }

  @Override
  public boolean canExecute(DrawingState currentState) {
    return super.canExecute(currentState) && shape != null;
  }

  @Override
  public CommandType getType() {
    return CommandType.DRAW;
  }

  @Override
  public CommandCost getCost() {
    return CommandCost.LOW;
  }

  public Shape getShape() {
    return shape;
  }

  public int getInsertIndex() {
    return insertIndex;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AddShapeCommand)) return false;
    if (!super.equals(obj)) return false;
    AddShapeCommand command = (AddShapeCommand) obj;
    return insertIndex == command.insertIndex && Objects.equals(shape, command.shape);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), shape, insertIndex);
  }

  @Override
  public String toString() {
    return String.format(
        "AddShapeCommand(id=%s, shape=%s, index=%d)", getId(), shape.getType(), insertIndex);
  }
}
