package com.example.agent.service;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;
import com.example.domain.tools.ToolRegistry;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ToolInvoker handles the invocation of tools with parameter validation and error handling
 */
public class ToolInvoker {
    
    private static final Logger logger = Logger.getLogger(ToolInvoker.class.getName());
    
    private final IToolRegistry toolRegistry;
    private final ParameterValidator validator;
    private final Gson gson;
    
    public ToolInvoker() {
        this(ToolRegistry.getInstance(), new DefaultParameterValidator(), new Gson());
    }

    public ToolInvoker(IToolRegistry toolRegistry, ParameterValidator validator, Gson gson) {
        this.toolRegistry = toolRegistry;
        this.validator = validator;
        this.gson = gson;
    }
    
    /**
     * Invokes a tool with the given parameters
     * @param toolName The name of the tool to invoke
     * @param parameters The parameters for the tool
     * @param currentState The current drawing state
     * @return The result of the tool execution
     */
    public ToolExecutionResult invokeTool(String toolName, Map<String, Object> parameters, 
                                         DrawingState currentState) {
        try {
            // Validate tool exists
            if (!toolRegistry.hasTool(toolName)) {
                return ToolExecutionResult.error(toolName, 
                    "Tool not found: " + toolName);
            }
            
            IAgentTool tool = toolRegistry.getTool(toolName);
            
            // Validate parameters against schema
            ValidationResult validation = validator.validateParameters(tool, parameters);
            if (!validation.isValid()) {
                return ToolExecutionResult.error(toolName, 
                    "Parameter validation failed: " + validation.getErrorMessage());
            }
            
            // Log tool invocation
            logger.info(String.format("Invoking tool '%s' with parameters: %s", 
                toolName, gson.toJson(parameters)));
            
            // Execute the tool
            DrawingState newState = tool.execute(
                parameters != null ? parameters : new HashMap<>(), 
                currentState
            );
            
            // Log successful execution
            logger.info(String.format("Tool '%s' executed successfully", toolName));
            
            // Create metadata for the result
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parametersUsed", parameters);
            metadata.put("executionTime", System.currentTimeMillis());
            
            return ToolExecutionResult.success(toolName, newState, metadata);
            
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid arguments for tool: " + toolName, e);
            return ToolExecutionResult.error(toolName, "Invalid arguments: " + e.getMessage());
            
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Tool execution failed: " + toolName, e);
            return ToolExecutionResult.error(toolName, "Execution failed: " + e.getMessage());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error invoking tool: " + toolName, e);
            return ToolExecutionResult.error(toolName, "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Invokes a tool with JSON string parameters
     * @param toolName The name of the tool to invoke
     * @param jsonParameters JSON string of parameters
     * @param currentState The current drawing state
     * @return The result of the tool execution
     */
    public ToolExecutionResult invokeToolWithJson(String toolName, String jsonParameters, 
                                                 DrawingState currentState) {
        try {
            Map<String, Object> parameters = gson.fromJson(jsonParameters, Map.class);
            return invokeTool(toolName, parameters, currentState);
        } catch (Exception e) {
            return ToolExecutionResult.error(toolName, 
                "Failed to parse JSON parameters: " + e.getMessage());
        }
    }
    
    /**
     * Validates if a tool can be invoked with given parameters
     * @param toolName The name of the tool
     * @param parameters The parameters to validate
     * @return true if the tool can be invoked
     */
    public boolean canInvokeTool(String toolName, Map<String, Object> parameters) {
        if (!toolRegistry.hasTool(toolName)) {
            return false;
        }
        
        IAgentTool tool = toolRegistry.getTool(toolName);
        ValidationResult validation = validator.validateParameters(tool, parameters);
        return validation.isValid();
    }

    /**
     * Validates parameters against a tool schema and returns detailed result
     * @param toolName tool name
     * @param parameters parameter map (may be null)
     * @return ValidationResult with error message if invalid
     */
    public ValidationResult validateToolParameters(String toolName, Map<String, Object> parameters) {
        if (!toolRegistry.hasTool(toolName)) {
            return ValidationResult.invalid("Tool not found: " + toolName);
        }

        IAgentTool tool = toolRegistry.getTool(toolName);
        return validator.validateParameters(tool, parameters);
    }
    
    /**
     * Gets the schema for a tool
     * @param toolName The name of the tool
     * @return The tool schema, or null if not found
     */
    public Map<String, Object> getToolSchema(String toolName) {
        if (!toolRegistry.hasTool(toolName)) {
            return null;
        }
        
        IAgentTool tool = toolRegistry.getTool(toolName);
        return tool.schema();
    }
    
    // 検証ロジックと結果クラスは DefaultParameterValidator / ValidationResult に移動済み。
}
