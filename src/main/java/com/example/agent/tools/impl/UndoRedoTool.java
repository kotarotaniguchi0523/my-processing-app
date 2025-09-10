package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.Tool;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.agent.tools.ToolValidationResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge tool to perform undo/redo using AgentContext's StateHistory.
 * name: "undo_redo"
 * params: { operation: "undo" | "redo" }
 */
public class UndoRedoTool implements Tool {

    @Override
    public String getName() {
        return "undo_redo";
    }

    @Override
    public String getDescription() {
        return "Perform undo or redo and return the new drawing state";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("operation", "string: 'undo' or 'redo'");
        return params;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {
        if (context == null || !context.hasStateHistory()) {
            throw ToolExecutionException.stateError(getName(), "No state history available");
        }

        String op = getString(parameters, "operation");
        if (!"undo".equalsIgnoreCase(op) && !"redo".equalsIgnoreCase(op)) {
            throw ToolExecutionException.invalidParameters(getName(),
                    "operation must be 'undo' or 'redo'");
        }

        StateHistory history = context.getStateHistory().get();
        StateHistory nextHistory = "undo".equalsIgnoreCase(op) ? history.undo() : history.redo();
        DrawingState newState = nextHistory.getCurrentState();

        if (newState == null) {
            return ToolResult.error("No state available after " + op);
        }

        return ToolResult
                .success("Performed " + op, newState)
                .withData("operation", op)
                .withData("canUndo", nextHistory.canUndo())
                .withData("canRedo", nextHistory.canRedo())
                .withData("index", nextHistory.getCurrentIndex());
    }

    @Override
    public ToolValidationResult validate(Map<String, Object> parameters) {
        return ToolValidationResult.valid();
    }

    private String getString(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object v = params.get(key);
        return v instanceof String ? (String) v : null;
    }
}
