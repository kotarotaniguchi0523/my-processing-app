package com.example.domain.agent.tools;

import com.example.domain.agent.AgentContext;

import java.util.Map;

/**
 * Tool interface defines the contract for all AI agent tools Tools are actions that the AI agent
 * can perform to interact with the drawing system
 */
public interface Tool {

    /**
     * Get the unique identifier for this tool
     *
     * @return tool name/identifier
     */
    String getName();

    /**
     * Get a description of what this tool does
     *
     * @return human-readable description
     */
    String getDescription();

    /**
     * Get the parameters that this tool accepts
     *
     * @return map of parameter names to their descriptions
     */
    Map<String, String> getParameters();

    /**
     * Execute the tool with given parameters and context
     *
     * @param parameters the parameters for this tool execution
     * @param context the agent context containing the current state
     * @return the result of the tool execution
     * @throws ToolExecutionException if the tool execution fails
     */
    ToolResult execute(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException;

    /**
     * Validate that the provided parameters are valid for this tool
     *
     * @param parameters the parameters to validate
     * @return validation result containing any errors
     */
    ToolValidationResult validate(Map<String, Object> parameters);

    /**
     * Check if this tool requires approval before execution
     *
     * @return true if approval is required, false otherwise
     */
    default boolean requiresApproval() {
        return false;
    }

    /**
     * Check if this tool modifies the drawing state
     *
     * @return true if the tool modifies state, false otherwise
     */
    default boolean isStateModifying() {
        return false;
    }

    /**
     * Get the category of this tool (e.g., "drawing", "state", "navigation")
     *
     * @return tool category
     */
    default String getCategory() {
        return "general";
    }
}
