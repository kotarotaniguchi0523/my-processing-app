package com.example.domain.values;

import com.example.domain.models.DrawingState;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the result of executing a tool
 */
public class ToolExecutionResult {
    
    private final String toolName;
    private final boolean success;
    private final String errorMessage;
    private final DrawingState resultState;
    private final Map<String, Object> metadata;
    private final Instant timestamp;
    
    private ToolExecutionResult(String toolName, boolean success, String errorMessage, 
                               DrawingState resultState, Map<String, Object> metadata) {
        this.toolName = toolName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.resultState = resultState;
        this.metadata = metadata;
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a successful tool execution result
     * @param toolName The name of the executed tool
     * @param resultState The resulting drawing state
     * @return A new successful result
     */
    public static ToolExecutionResult success(String toolName, DrawingState resultState) {
        return new ToolExecutionResult(toolName, true, null, resultState, null);
    }
    
    /**
     * Creates a successful tool execution result with metadata
     * @param toolName The name of the executed tool
     * @param resultState The resulting drawing state
     * @param metadata Additional metadata
     * @return A new successful result
     */
    public static ToolExecutionResult success(String toolName, DrawingState resultState, 
                                             Map<String, Object> metadata) {
        return new ToolExecutionResult(toolName, true, null, resultState, metadata);
    }
    
    /**
     * Creates a failed tool execution result
     * @param errorMessage The error message
     * @return A new error result
     */
    public static ToolExecutionResult error(String errorMessage) {
        return new ToolExecutionResult(null, false, errorMessage, null, null);
    }
    
    /**
     * Creates a failed tool execution result with tool name
     * @param toolName The name of the tool that failed
     * @param errorMessage The error message
     * @return A new error result
     */
    public static ToolExecutionResult error(String toolName, String errorMessage) {
        return new ToolExecutionResult(toolName, false, errorMessage, null, null);
    }

    /**
     * Creates a failed tool execution result (alias for error method)
     * @param errorMessage The error message
     * @return A new error result
     */
    public static ToolExecutionResult failure(String errorMessage) {
        return error(errorMessage);
    }

    /**
     * Creates a successful tool execution result with shape ID and message
     * @param shapeId The ID of the created/modified shape
     * @param message The success message
     * @return A new successful result
     */
    public static ToolExecutionResult success(String shapeId, String message) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("shapeId", shapeId);
        metadata.put("message", message);
        return new ToolExecutionResult(null, true, null, null, metadata);
    }
    
    // Getters
    public String getToolName() {
        return toolName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public DrawingState getResultState() {
        return resultState;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Checks if this result has an error message
     * @return true if there is an error message
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
    
    /**
     * Checks if this result has a result state
     * @return true if there is a result state
     */
    public boolean hasResultState() {
        return resultState != null;
    }
    
    /**
     * Gets a summary of the execution result
     * @return A summary string
     */
    public String getSummary() {
        if (success) {
            return String.format("Tool '%s' executed successfully", 
                toolName != null ? toolName : "unknown");
        } else {
            return String.format("Tool '%s' failed: %s", 
                toolName != null ? toolName : "unknown",
                errorMessage != null ? errorMessage : "Unknown error");
        }
    }
    
    @Override
    public String toString() {
        return String.format("ToolExecutionResult{tool='%s', success=%s, error='%s', timestamp=%s}",
            toolName, success, errorMessage, timestamp);
    }
}