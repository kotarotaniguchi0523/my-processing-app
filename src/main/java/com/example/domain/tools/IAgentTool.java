package com.example.domain.tools;

import com.example.domain.models.DrawingState;
import java.util.Map;

/**
 * IAgentTool defines the contract for all agent tools used by the AI drawing agent.
 * Tools provide structured actions that the agent can perform with validated parameters
 * and consistent state management.
 */
public interface IAgentTool {

    /**
     * Get the unique name identifier for this tool
     *
     * @return the tool name (must be unique within the tool registry)
     */
    String name();

    /**
     * Get the JSON schema that defines the parameters this tool accepts
     * The schema follows JSON Schema specification for parameter validation
     *
     * @return JSON schema as a Map representing the parameter structure
     */
    Map<String, Object> schema();

    /**
     * Execute the tool with the provided parameters and current state
     * This method should be pure and not modify the input state directly
     *
     * @param parameters the parameters provided to the tool (must match schema)
     * @param state the current drawing state to operate on
     * @return a new DrawingState with the tool's modifications applied
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if execution fails
     */
    DrawingState execute(Map<String, Object> parameters, DrawingState state);
}