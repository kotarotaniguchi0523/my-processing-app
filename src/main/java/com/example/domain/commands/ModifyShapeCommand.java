package com.example.domain.commands;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.Objects;
import java.util.function.Function;

/** Command to modify an existing shape in the drawing */
public final class ModifyShapeCommand extends DrawCommand {
  private final String shapeId;
  private final Function<Shape, Shape> modifier;
  private final String modificationDescription;
  private Shape originalShape; // Store for undo

  public ModifyShapeCommand(
      String id,
      String shapeId,
      Function<Shape, Shape> modifier,
      String modificationDescription,
      String description) {
    super(id, description);
    this.shapeId = Objects.requireNonNull(shapeId, "Shape ID cannot be null");
    this.modifier = Objects.requireNonNull(modifier, "Modifier function cannot be null");
    this.modificationDescription =
        modificationDescription != null ? modificationDescription : "modification";
  }

  public ModifyShapeCommand(
      String shapeId, Function<Shape, Shape> modifier, String modificationDescription) {
    super("Modify shape: " + modificationDescription);
    this.shapeId = shapeId;
    this.modifier = modifier;
    this.modificationDescription = modificationDescription;
  }

  @Override
  public DrawingState execute(DrawingState currentState) {
    var shapes = currentState.getShapes();

    for (int i = 0; i < shapes.size(); i++) {
      Shape shape = shapes.get(i);
      if (shape.getId().equals(shapeId)) {
        originalShape = shape; // Store for undo
        Shape modifiedShape = modifier.apply(shape);
        return currentState.withReplacedShape(i, modifiedShape);
      }
    }

    return currentState; // Shape not found, return unchanged
  }

  @Override
  public DrawingState undo(DrawingState currentState) {
    if (originalShape == null) {
      throw new IllegalStateException("Cannot undo - no shape was modified");
    }

    var shapes = currentState.getShapes();

    for (int i = 0; i < shapes.size(); i++) {
      if (shapes.get(i).getId().equals(shapeId)) {
        return currentState.withReplacedShape(i, originalShape);
      }
    }

    return currentState; // Shape not found, return unchanged
  }

  @Override
  public boolean canExecute(DrawingState currentState) {
    if (!super.canExecute(currentState)) {
      return false;
    }

    // Check if shape exists
    return currentState.getShapes().stream().anyMatch(shape -> shape.getId().equals(shapeId));
  }

  @Override
  public CommandType getType() {
    return CommandType.MODIFY;
  }

  @Override
  public CommandCost getCost() {
    return CommandCost.MEDIUM;
  }

  public String getShapeId() {
    return shapeId;
  }

  public String getModificationDescription() {
    return modificationDescription;
  }

  public Shape getOriginalShape() {
    return originalShape;
  }

  // Factory methods for common modifications
  public static ModifyShapeCommand moveShape(String shapeId, float deltaX, float deltaY) {
    return new ModifyShapeCommand(
        shapeId,
        shape ->
            shape.moveTo(
                shape.getPosition().add(new com.example.domain.values.Position(deltaX, deltaY))),
        "move by (" + deltaX + ", " + deltaY + ")");
  }

  public static ModifyShapeCommand changeVisibility(String shapeId, boolean visible) {
    return new ModifyShapeCommand(
        shapeId, shape -> shape.withVisibility(visible), visible ? "show" : "hide");
  }

  public static ModifyShapeCommand changeLayerOrder(String shapeId, int newLayer) {
    return new ModifyShapeCommand(
        shapeId, shape -> shape.withLayerOrder(newLayer), "change layer to " + newLayer);
  }

  public static ModifyShapeCommand changeStyle(
      String shapeId, com.example.domain.values.Style newStyle) {
    return new ModifyShapeCommand(shapeId, shape -> shape.withStyle(newStyle), "change style");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ModifyShapeCommand)) return false;
    if (!super.equals(obj)) return false;
    ModifyShapeCommand command = (ModifyShapeCommand) obj;
    return Objects.equals(shapeId, command.shapeId)
        && Objects.equals(modificationDescription, command.modificationDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), shapeId, modificationDescription);
  }

  @Override
  public String toString() {
    return String.format(
        "ModifyShapeCommand(id=%s, shapeId=%s, modification='%s')",
        getId(), shapeId, modificationDescription);
  }
}
