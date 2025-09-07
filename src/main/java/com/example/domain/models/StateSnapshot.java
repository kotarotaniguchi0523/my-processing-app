package com.example.domain.models;

import java.util.Objects;

/**
 * StateSnapshot represents a point-in-time snapshot of the drawing state Used for undo/redo
 * functionality and state history management
 */
public final class StateSnapshot {
    private final String id;
    private final DrawingState state;
    private final String description;
    private final long timestamp;
    private final String operation;

    public StateSnapshot(String id, DrawingState state, String description, String operation) {
        this.id = Objects.requireNonNull(id, "Snapshot ID cannot be null");
        this.state = Objects.requireNonNull(state, "DrawingState cannot be null");
        this.description = description != null ? description : "";
        this.operation = operation != null ? operation : "unknown";
        this.timestamp = System.currentTimeMillis();
    }

    public StateSnapshot(DrawingState state, String description, String operation) {
        this(generateId(), state, description, operation);
    }

    public StateSnapshot(DrawingState state, String description) {
        this(generateId(), state, description, "manual");
    }

    public StateSnapshot(DrawingState state) {
        this(generateId(), state, "State snapshot", "manual");
    }

    private static String generateId() {
        return "snapshot_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    public String getId() {
        return id;
    }

    public DrawingState getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public String getOperation() {
        return operation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Create a new snapshot with modified description
    public StateSnapshot withDescription(String newDescription) {
        return new StateSnapshot(id, state, newDescription, operation);
    }

    // Create a new snapshot with modified operation
    public StateSnapshot withOperation(String newOperation) {
        return new StateSnapshot(id, state, description, newOperation);
    }

    // Check if this snapshot represents the same state as another
    public boolean hasSameState(StateSnapshot other) {
        return other != null && this.state.equals(other.state);
    }

    // Check if this snapshot is from the same operation
    public boolean hasSameOperation(StateSnapshot other) {
        return other != null && Objects.equals(this.operation, other.operation);
    }

    // Get time difference from another snapshot in milliseconds
    public long getTimeDifference(StateSnapshot other) {
        return other != null ? Math.abs(this.timestamp - other.timestamp) : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StateSnapshot)) {
            return false;
        }
        StateSnapshot snapshot = (StateSnapshot) obj;
        return timestamp == snapshot.timestamp
                && Objects.equals(id, snapshot.id)
                && Objects.equals(state, snapshot.state)
                && Objects.equals(description, snapshot.description)
                && Objects.equals(operation, snapshot.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, state, description, operation, timestamp);
    }

    @Override
    public String toString() {
        return String.format(
                "StateSnapshot(id=%s, operation=%s, description='%s', timestamp=%d)",
                id, operation, description, timestamp);
    }

    // Factory methods for common snapshot types
    public static StateSnapshot createInitial(DrawingState state) {
        return new StateSnapshot(state, "Initial state", "initialize");
    }

    public static StateSnapshot createFromDraw(DrawingState state, String shapeType) {
        return new StateSnapshot(state, "Drew " + shapeType, "draw");
    }

    public static StateSnapshot createFromDelete(DrawingState state, int deletedCount) {
        return new StateSnapshot(state, "Deleted " + deletedCount + " shape(s)", "delete");
    }

    public static StateSnapshot createFromModify(DrawingState state, String modification) {
        return new StateSnapshot(state, "Modified: " + modification, "modify");
    }

    public static StateSnapshot createFromClear(DrawingState state) {
        return new StateSnapshot(state, "Cleared canvas", "clear");
    }

    public static StateSnapshot createFromLoad(DrawingState state, String source) {
        return new StateSnapshot(state, "Loaded from " + source, "load");
    }

    public static StateSnapshot createFromSave(DrawingState state, String destination) {
        return new StateSnapshot(state, "Saved to " + destination, "save");
    }
}
