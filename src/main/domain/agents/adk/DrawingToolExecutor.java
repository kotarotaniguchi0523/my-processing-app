package com.example.domain.agents.adk;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.adk.ADKTool;
import com.example.domain.tools.adk.ADKToolRegistry;

import java.util.*;
import java.util.logging.Logger;

/**
 * Executor for drawing tools.
 * Single responsibility: Execute drawing tools and collect results.
 */
public class DrawingToolExecutor {

    private static final Logger LOGGER = Logger.getLogger(DrawingToolExecutor.class.getName());
    private final ADKToolRegistry toolRegistry;

    public DrawingToolExecutor(ADKToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * Execute a list of tools with parameters and return the updated state
     */
    public ExecutionResult executeTools(List<String> tools, List<Map<String, Object>> params, DrawingState currentState) {
        if (tools == null || tools.isEmpty()) {
            LOGGER.warning("No tools to execute");
            return new ExecutionResult(currentState, new ArrayList<>(), 0, 0);
        }

        if (params == null || params.size() != tools.size()) {
            LOGGER.warning("Tool and parameter count mismatch");
            return new ExecutionResult(currentState, new ArrayList<>(), 0, tools.size());
        }

        DrawingState state = currentState;
        List<String> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < tools.size(); i++) {
            String toolName = tools.get(i);
            Map<String, Object> toolParams = params.get(i);

            LOGGER.info("Executing tool: " + toolName);

            if (toolParams == null) {
                LOGGER.warning("Null parameters for tool: " + toolName);
                results.add("Tool " + toolName + " failed: null parameters");
                failureCount++;
                continue;
            }

            ADKTool.ToolResult result = toolRegistry.executeTool(toolName, toolParams, state);

            if (result.isSuccess()) {
                state = result.getState();
                results.add("Tool " + toolName + " succeeded: " + result.getMessage());
                successCount++;
            } else {
                results.add("Tool " + toolName + " failed: " + result.getMessage());
                failureCount++;
            }
        }

        return new ExecutionResult(state, results, successCount, failureCount);
    }

    /**
     * Result of tool execution
     */
    public static class ExecutionResult {
        private final DrawingState state;
        private final List<String> results;
        private final int successCount;
        private final int failureCount;

        public ExecutionResult(DrawingState state, List<String> results, int successCount, int failureCount) {
            this.state = state;
            this.results = results;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public DrawingState getState() {
            return state;
        }

        public List<String> getResults() {
            return results;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public boolean isFullySuccessful() {
            return failureCount == 0 && successCount > 0;
        }

        public String getSummary() {
            return String.format("Executed %d tools: %d successful, %d failed",
                successCount + failureCount, successCount, failureCount);
        }
    }
}