package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import java.util.Map;

/**
 * Interface for ADK-compatible tools.
 * Tools implementing this interface can be used directly with ADK's LlmAgent.
 * Note: FunctionDeclaration support removed - ADK 0.3 uses different tool declaration mechanism
 */
public interface ADKTool {

    /**
     * Execute the tool with the given parameters.
     *
     * @param parameters The parameters from the LLM
     * @param state The current drawing state
     * @return The result of executing the tool
     */
    ToolResult execute(Map<String, Object> parameters, DrawingState state);

    /**
     * Get the schema defining this tool's parameters.
     *
     * @return Map containing tool metadata and parameter definitions
     */
    Map<String, Object> getSchema();

    /**
     * Result of tool execution
     */
    class ToolResult {
        private final boolean success;
        private final DrawingState newState;
        private final String message;
        private final String shapeId;

        public ToolResult(boolean success, DrawingState newState, String message, String shapeId) {
            this.success = success;
            this.newState = newState;
            this.message = message;
            this.shapeId = shapeId;
        }

        public boolean isSuccess() { return success; }
        public DrawingState getNewState() { return newState; }
        public String getMessage() { return message; }
        public String getShapeId() { return shapeId; }

        public static ToolResult success(DrawingState newState, String shapeId, String message) {
            return new ToolResult(true, newState, message, shapeId);
        }

        public static ToolResult failure(DrawingState state, String message) {
            return new ToolResult(false, state, message, null);
        }
    }
}