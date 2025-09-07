package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.AbstractTool;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import com.example.domain.models.StateSnapshot;

import java.util.Locale;
import java.util.Map;

/**
 * UndoRedoTool allows the AI agent to perform undo and redo operations Navigates through the state
 * history to previous or next states
 */
public class UndoRedoTool extends AbstractTool {

    public UndoRedoTool() {
        super(
                "undo_redo",
                "Perform undo or redo operations on the drawing canvas",
                "state",
                false, // does
                // not
                // require
                // approval
                // for
                // navigation
                true); // state modifying (changes current state)
    }

    @Override
    protected void defineParameters() {
        addParameter("operation", String.class, "Operation to perform: 'undo' or 'redo'", true);
        addParameter("steps", Integer.class, "Number of steps to undo/redo", false, 1);
        addParameter(
                "toSnapshotId",
                String.class,
                "Navigate directly to a specific snapshot by ID",
                false);
        addParameter(
                "toIndex", Integer.class, "Navigate directly to a specific history index", false);
    }

    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {

        // Check if state history is available
        if (!context.hasStateHistory()) {
            throw ToolExecutionException.stateError(
                    getName(), "State history is not available for undo/redo operations");
        }

        StateHistory currentHistory = context.getStateHistory().get();

        // Extract parameters
        String operation =
                getRequiredParameter(parameters, "operation", String.class)
                        .toLowerCase(Locale.ROOT);
        String toSnapshotId = getOptionalParameter(parameters, "toSnapshotId", String.class, null);
        Integer toIndex = getOptionalParameter(parameters, "toIndex", Integer.class, null);
        int steps = getOptionalParameter(parameters, "steps", Integer.class, 1);

        // Validate steps parameter
        if (steps < 1) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Steps must be a positive integer");
        }

        StateHistory newHistory;
        String actionMessage;

        // Handle direct navigation first
        if (toSnapshotId != null) {
            newHistory = currentHistory.goTo(toSnapshotId);
            if (newHistory.getCurrentIndex() == currentHistory.getCurrentIndex()) {
                return ToolResult.error(
                        "Snapshot with ID '" + toSnapshotId + "' not found in history");
            }
            actionMessage = String.format("Navigated to snapshot '%s'", toSnapshotId);
        } else if (toIndex != null) {
            if (toIndex < 0 || toIndex >= currentHistory.size()) {
                throw ToolExecutionException.invalidParameters(
                        getName(),
                        String.format(
                                "Index %d is out of range (0-%d)",
                                toIndex, currentHistory.size() - 1));
            }
            newHistory = currentHistory.goTo(toIndex);
            actionMessage = String.format("Navigated to history index %d", toIndex);
        } else {
            // Handle undo/redo operations
            switch (operation) {
                case "undo":
                    newHistory = performUndo(currentHistory, steps);
                    actionMessage = createUndoMessage(currentHistory, newHistory, steps);
                    break;
                case "redo":
                    newHistory = performRedo(currentHistory, steps);
                    actionMessage = createRedoMessage(currentHistory, newHistory, steps);
                    break;
                default:
                    throw ToolExecutionException.invalidParameters(
                            getName(), "Operation must be 'undo' or 'redo', got: " + operation);
            }
        }

        // Check if the operation actually changed anything
        if (newHistory.getCurrentIndex() == currentHistory.getCurrentIndex()) {
            String reason;
            if ("undo".equals(operation) && !currentHistory.canUndo()) {
                reason = "Nothing to undo";
            } else if ("redo".equals(operation) && !currentHistory.canRedo()) {
                reason = "Nothing to redo";
            } else {
                reason = "No change occurred";
            }
            return ToolResult.error(reason, "NO_CHANGE");
        }

        // Get the new current state
        DrawingState newState = newHistory.getCurrentState();
        if (newState == null) {
            throw ToolExecutionException.stateError(
                    getName(), "Unable to retrieve state after undo/redo operation");
        }

        return ToolResult.success(actionMessage, newState)
                .withData("operation", operation)
                .withData("newIndex", newHistory.getCurrentIndex())
                .withData("totalSnapshots", newHistory.size())
                .withData("canUndo", newHistory.canUndo())
                .withData("canRedo", newHistory.canRedo())
                .withData(
                        "currentSnapshotId",
                        newHistory.getCurrentSnapshot() != null
                                ? newHistory.getCurrentSnapshot().getId()
                                : null)
                .withData("shapesCount", newState.getShapes().size());
    }

    private StateHistory performUndo(StateHistory currentHistory, int steps)
            throws ToolExecutionException {
        StateHistory history = currentHistory;
        int actualSteps = 0;

        for (int i = 0; i < steps && history.canUndo(); i++) {
            history = history.undo();
            actualSteps++;
        }

        if (actualSteps == 0) {
            throw ToolExecutionException.stateError(
                    getName(), "Cannot undo: no previous states available");
        }

        return history;
    }

    private StateHistory performRedo(StateHistory currentHistory, int steps)
            throws ToolExecutionException {
        StateHistory history = currentHistory;
        int actualSteps = 0;

        for (int i = 0; i < steps && history.canRedo(); i++) {
            history = history.redo();
            actualSteps++;
        }

        if (actualSteps == 0) {
            throw ToolExecutionException.stateError(
                    getName(), "Cannot redo: no future states available");
        }

        return history;
    }

    private String createUndoMessage(
            StateHistory oldHistory, StateHistory newHistory, int requestedSteps) {
        int actualSteps = oldHistory.getCurrentIndex() - newHistory.getCurrentIndex();

        if (actualSteps == 1) {
            StateSnapshot currentSnapshot = newHistory.getCurrentSnapshot();
            String operation = currentSnapshot != null ? currentSnapshot.getOperation() : "unknown";
            return String.format("Undid 1 step, reverted to state: %s", operation);
        } else {
            return String.format("Undid %d steps (requested %d)", actualSteps, requestedSteps);
        }
    }

    private String createRedoMessage(
            StateHistory oldHistory, StateHistory newHistory, int requestedSteps) {
        int actualSteps = newHistory.getCurrentIndex() - oldHistory.getCurrentIndex();

        if (actualSteps == 1) {
            StateSnapshot currentSnapshot = newHistory.getCurrentSnapshot();
            String operation = currentSnapshot != null ? currentSnapshot.getOperation() : "unknown";
            return String.format("Redid 1 step, applied: %s", operation);
        } else {
            return String.format("Redid %d steps (requested %d)", actualSteps, requestedSteps);
        }
    }
}
