package com.example.domain.services;

import com.example.domain.values.ToolExecutionResult;
import com.example.domain.values.ValidationResult;
import com.example.domain.models.DrawingState;

import java.util.Map;

/**
 * Domain interface for tool invoker that handles the invocation of tools with parameter validation and error handling.
 */
public interface IToolInvoker {
    
    /**
     * Invokes a tool with the given parameters
     * @param toolName The name of the tool to invoke
     * @param parameters The parameters for the tool
     * @param currentState The current drawing state
     * @return The result of the tool execution
     */
    ToolExecutionResult invokeTool(String toolName, Map<String, Object> parameters, DrawingState currentState);
    
    /**
     * Invokes a tool with JSON string parameters
     * @param toolName The name of the tool to invoke
     * @param jsonParameters JSON string of parameters
     * @param currentState The current drawing state
     * @return The result of the tool execution
     */
    ToolExecutionResult invokeToolWithJson(String toolName, String jsonParameters, DrawingState currentState);
    
    /**
     * Validates if a tool can be invoked with given parameters
     * @param toolName The name of the tool
     * @param parameters The parameters to validate
     * @return true if the tool can be invoked
     */
    boolean canInvokeTool(String toolName, Map<String, Object> parameters);
    
    /**
     * Validates parameters against a tool schema and returns detailed result
     * @param toolName tool name
     * @param parameters parameter map (may be null)
     * @return ValidationResult with error message if invalid
     */
    ValidationResult validateToolParameters(String toolName, Map<String, Object> parameters);
    
    /**
     * Gets the schema for a tool
     * @param toolName The name of the tool
     * @return The tool schema, or null if not found
     */
    Map<String, Object> getToolSchema(String toolName);
}