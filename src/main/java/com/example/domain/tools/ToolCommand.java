package com.example.domain.tools;

import com.example.domain.commands.DrawCommand;
import com.example.domain.models.DrawingState;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * ToolCommand is a DrawCommand wrapper around an IAgentTool. It delegates execution to the provided
 * tool with the given parameters.
 */
public class ToolCommand extends DrawCommand {
    private final IAgentTool tool;
    private final Map<String, Object> parameters;

    /**
     * Create a ToolCommand that will execute the given tool with parameters.
     *
     * @param tool the tool to execute (must not be null)
     * @param parameters parameters for the tool (must not be null)
     */
    public ToolCommand(IAgentTool tool, Map<String, Object> parameters) {
        super("Execute tool '" + (tool != null ? tool.name() : "unknown") + "'");
        this.tool = Objects.requireNonNull(tool, "tool cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters cannot be null");
    }

    @Override
    public DrawingState execute(DrawingState currentState) {
        if (currentState == null) {
            throw new IllegalArgumentException("Current state cannot be null");
        }
        return tool.execute(parameters, currentState);
    }

    @Override
    public CommandType getType() {
        // Heuristic mapping based on tool name; defaults to STYLE for unknown
        String name = tool.name().toLowerCase(Locale.ROOT);
        if (name.contains("create") || name.contains("add")) {
            return CommandType.DRAW;
        } else if (name.contains("modify") || name.contains("update") || name.contains("edit")) {
            return CommandType.MODIFY;
        } else if (name.contains("delete") || name.contains("remove")) {
            return CommandType.DELETE;
        }
        return CommandType.STYLE;
    }

    public IAgentTool getTool() {
        return tool;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format("ToolCommand(tool=%s, params=%s)", tool.name(), parameters);
    }
}
