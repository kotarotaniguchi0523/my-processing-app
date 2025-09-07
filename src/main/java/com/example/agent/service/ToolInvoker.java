package com.example.agent.service;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
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
    
    private final ToolRegistry toolRegistry;
    private final ToolValidator validator;
    private final Gson gson;
    
    public ToolInvoker() {
        this.toolRegistry = ToolRegistry.getInstance();
        this.validator = new ToolValidator();
        this.gson = new Gson();
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
    
    /**
     * Inner class for parameter validation
     */
    private static class ToolValidator {
        
        /**
         * Validates parameters against a tool's schema
         * @param tool The tool to validate against
         * @param parameters The parameters to validate
         * @return Validation result
         */
        public ValidationResult validateParameters(IAgentTool tool, Map<String, Object> parameters) {
            try {
                Map<String, Object> schema = tool.schema();
                if (schema == null) {
                    return ValidationResult.valid();
                }
                
                // Get required properties
                Object propertiesObj = schema.get("properties");
                if (!(propertiesObj instanceof Map)) {
                    return ValidationResult.valid(); // No properties to validate
                }
                
                Map<String, Object> properties = (Map<String, Object>) propertiesObj;
                
                // Check required fields
                Object requiredObj = schema.get("required");
                if (requiredObj instanceof java.util.List) {
                    java.util.List<String> required = (java.util.List<String>) requiredObj;
                    
                    for (String field : required) {
                        if (parameters == null || !parameters.containsKey(field)) {
                            return ValidationResult.invalid("Missing required field: " + field);
                        }
                    }
                }
                
                // Validate parameter types (basic validation)
                if (parameters != null) {
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        if (properties.containsKey(key)) {
                            Map<String, Object> propertySchema = (Map<String, Object>) properties.get(key);
                            if (!validateType(value, propertySchema)) {
                                return ValidationResult.invalid(
                                    String.format("Invalid type for field '%s'", key));
                            }
                        }
                    }
                }
                
                return ValidationResult.valid();
                
            } catch (Exception e) {
                return ValidationResult.invalid("Validation error: " + e.getMessage());
            }
        }
        
        /**
         * Validates a value against a property schema
         * @param value The value to validate
         * @param propertySchema The property schema
         * @return true if valid
         */
        private boolean validateType(Object value, Map<String, Object> propertySchema) {
            Object typeObj = propertySchema.get("type");
            if (!(typeObj instanceof String)) {
                return true; // No type constraint
            }
            
            String type = (String) typeObj;
            
            switch (type) {
                case "string":
                    return value instanceof String;
                case "number":
                    return value instanceof Number;
                case "integer":
                    return value instanceof Integer || value instanceof Long;
                case "boolean":
                    return value instanceof Boolean;
                case "object":
                    return value instanceof Map;
                case "array":
                    return value instanceof java.util.List;
                default:
                    return true; // Unknown type, allow
            }
        }
    }
    
    /**
     * Represents the result of parameter validation
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}