package com.example.domain.commands;

import com.example.domain.models.DrawingState;

import java.util.Objects;

/**
 * Abstract base class for all drawing commands Implements the Command pattern for drawing
 * operations
 */
public abstract class DrawCommand {
    protected final String id;
    protected final String description;
    protected final long timestamp;

    protected DrawCommand(String id, String description) {
        this.id = Objects.requireNonNull(id, "Command ID cannot be null");
        this.description = description != null ? description : "";
        this.timestamp = System.currentTimeMillis();
    }

    protected DrawCommand(String description) {
        this(generateId(), description);
    }

    // Execute the command and return the new state
    public abstract DrawingState execute(DrawingState currentState);

    // Undo the command (returns the state that would reverse this command's effect)
    // Default implementation throws UnsupportedOperationException
    public DrawingState undo(DrawingState currentState) {
        throw new UnsupportedOperationException(
                "Undo not supported for " + getClass().getSimpleName());
    }

    // Check if this command can be undone
    public boolean isUndoable() {
        try {
            undo(null);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (Exception e) {
            return true; // If it fails for other reasons, it's probably undoable
        }
    }

    // Validate if the command can be executed on the given state
    public boolean canExecute(DrawingState currentState) {
        return currentState != null;
    }

    // Get command type for categorization
    public abstract CommandType getType();

    // Get estimated execution cost (for performance optimization)
    public CommandCost getCost() {
        return CommandCost.MEDIUM;
    }

    // Check if this command modifies the drawing state
    public boolean isModifying() {
        return true;
    }

    // Get command priority (for queuing systems)
    public CommandPriority getPriority() {
        return CommandPriority.NORMAL;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Generate unique command ID
    protected static String generateId() {
        return "cmd_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DrawCommand)) {
            return false;
        }
        DrawCommand command = (DrawCommand) obj;
        return timestamp == command.timestamp
                && Objects.equals(id, command.id)
                && Objects.equals(description, command.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, timestamp);
    }

    @Override
    public String toString() {
        return String.format(
                "%s(id=%s, description='%s', timestamp=%d)",
                getClass().getSimpleName(), id, description, timestamp);
    }

    // Enum for command types
    public enum CommandType {
        DRAW("draw"),
        MODIFY("modify"),
        DELETE("delete"),
        TRANSFORM("transform"),
        STYLE("style"),
        CANVAS("canvas"),
        COMPOSITE("composite");

        private final String typeName;

        CommandType(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }

        @Override
        public String toString() {
            return typeName;
        }
    }

    // Enum for command execution cost
    public enum CommandCost {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        VERY_HIGH(4);

        private final int costLevel;

        CommandCost(int costLevel) {
            this.costLevel = costLevel;
        }

        public int getCostLevel() {
            return costLevel;
        }

        public boolean isMoreExpensiveThan(CommandCost other) {
            return this.costLevel > other.costLevel;
        }
    }

    // Enum for command priority
    public enum CommandPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);

        private final int priorityLevel;

        CommandPriority(int priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public int getPriorityLevel() {
            return priorityLevel;
        }

        public boolean isHigherThan(CommandPriority other) {
            return this.priorityLevel > other.priorityLevel;
        }
    }
}
