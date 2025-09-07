package com.example.domain.commands;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.Objects;

/** Command to remove a shape from the drawing */
public final class RemoveShapeCommand extends DrawCommand {
    private final String shapeId;
    private final int shapeIndex;
    private Shape removedShape; // Store for undo

    public RemoveShapeCommand(String id, String shapeId, int shapeIndex, String description) {
        super(id, description);
        this.shapeId = Objects.requireNonNull(shapeId, "Shape ID cannot be null");
        this.shapeIndex = shapeIndex;
    }

    public RemoveShapeCommand(String shapeId, int shapeIndex) {
        super("Remove shape");
        this.shapeId = shapeId;
        this.shapeIndex = shapeIndex;
    }

    public RemoveShapeCommand(String shapeId) {
        this(shapeId, -1); // -1 means find by ID
    }

    @Override
    public DrawingState execute(DrawingState currentState) {
        var shapes = currentState.getShapes();

        // If index is specified and valid, use it
        if (shapeIndex >= 0 && shapeIndex < shapes.size()) {
            Shape shape = shapes.get(shapeIndex);
            if (shape.getId().equals(shapeId)) {
                removedShape = shape; // Store for undo
                return currentState.withRemovedShape(shapeIndex);
            }
        }

        // Otherwise, find by ID
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i).getId().equals(shapeId)) {
                removedShape = shapes.get(i); // Store for undo
                return currentState.withRemovedShape(i);
            }
        }

        return currentState; // Shape not found, return unchanged
    }

    @Override
    public DrawingState undo(DrawingState currentState) {
        if (removedShape == null) {
            throw new IllegalStateException("Cannot undo - no shape was removed");
        }

        // Try to restore at original index if possible
        var shapes = currentState.getShapes();
        if (shapeIndex >= 0 && shapeIndex <= shapes.size()) {
            shapes.add(shapeIndex, removedShape);
            return currentState.withShapes(shapes);
        } else {
            return currentState.withAddedShape(removedShape);
        }
    }

    @Override
    public boolean canExecute(DrawingState currentState) {
        if (!super.canExecute(currentState)) {
            return false;
        }

        var shapes = currentState.getShapes();

        // Check if index is valid
        if (shapeIndex >= 0) {
            return shapeIndex < shapes.size() && shapes.get(shapeIndex).getId().equals(shapeId);
        }

        // Check if shape exists by ID
        return shapes.stream().anyMatch(shape -> shape.getId().equals(shapeId));
    }

    @Override
    public CommandType getType() {
        return CommandType.DELETE;
    }

    @Override
    public CommandCost getCost() {
        return CommandCost.LOW;
    }

    public String getShapeId() {
        return shapeId;
    }

    public int getShapeIndex() {
        return shapeIndex;
    }

    public Shape getRemovedShape() {
        return removedShape;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RemoveShapeCommand)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        RemoveShapeCommand command = (RemoveShapeCommand) obj;
        return shapeIndex == command.shapeIndex && Objects.equals(shapeId, command.shapeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shapeId, shapeIndex);
    }

    @Override
    public String toString() {
        return String.format(
                "RemoveShapeCommand(id=%s, shapeId=%s, index=%d)", getId(), shapeId, shapeIndex);
    }
}
