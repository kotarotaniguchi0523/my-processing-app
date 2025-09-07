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
     * Inner class for comprehensive parameter validation with JSON schema support
     */
    private static class ToolValidator {
        
        /**
         * Validates parameters against a tool's schema with comprehensive JSON schema validation
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
                
                return validateObject(parameters, schema, "");
                
            } catch (Exception e) {
                return ValidationResult.invalid("Schema validation error: " + e.getMessage());
            }
        }
        
        /**
         * Validates an object value against an object schema
         * @param value The value to validate (should be a Map for objects)
         * @param schema The schema to validate against
         * @param path The current validation path for error reporting
         * @return Validation result
         */
        private ValidationResult validateObject(Object value, Map<String, Object> schema, String path) {
            // Handle null values
            if (value == null) {
                return ValidationResult.valid();
            }
            
            // Check if the value is the expected type
            if (!"object".equals(schema.get("type"))) {
                return validateValue(value, schema, path);
            }
            
            if (!(value instanceof Map)) {
                return ValidationResult.invalid(buildPath(path) + "Expected object but got " + 
                    (value != null ? value.getClass().getSimpleName() : "null"));
            }
            
            Map<String, Object> objectValue = (Map<String, Object>) value;
            
            // Get properties schema
            Object propertiesObj = schema.get("properties");
            if (!(propertiesObj instanceof Map)) {
                return ValidationResult.valid(); // No properties to validate
            }
            
            Map<String, Object> properties = (Map<String, Object>) propertiesObj;
            
            // Check required fields
            ValidationResult requiredValidation = validateRequiredFields(objectValue, schema, path);
            if (!requiredValidation.isValid()) {
                return requiredValidation;
            }
            
            // Validate each property
            for (Map.Entry<String, Object> entry : objectValue.entrySet()) {
                String propertyName = entry.getKey();
                Object propertyValue = entry.getValue();
                String propertyPath = buildPath(path, propertyName);
                
                if (properties.containsKey(propertyName)) {
                    Map<String, Object> propertySchema = (Map<String, Object>) properties.get(propertyName);
                    ValidationResult propertyValidation = validateValue(propertyValue, propertySchema, propertyPath);
                    if (!propertyValidation.isValid()) {
                        return propertyValidation;
                    }
                } else {
                    // Check if additionalProperties is allowed
                    Object additionalProperties = schema.get("additionalProperties");
                    if (Boolean.FALSE.equals(additionalProperties)) {
                        return ValidationResult.invalid(propertyPath + "Additional property '" + 
                            propertyName + "' is not allowed");
                    }
                }
            }
            
            return ValidationResult.valid();
        }
        
        /**
         * Validates required fields according to the schema
         * @param objectValue The object value to check
         * @param schema The schema containing required field definitions
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateRequiredFields(Map<String, Object> objectValue, 
                                                       Map<String, Object> schema, String path) {
            Object requiredObj = schema.get("required");
            if (requiredObj instanceof java.util.List) {
                java.util.List<String> required = (java.util.List<String>) requiredObj;
                
                for (String field : required) {
                    if (!objectValue.containsKey(field)) {
                        return ValidationResult.invalid(buildPath(path) + "Missing required field: " + field);
                    }
                }
            }
            return ValidationResult.valid();
        }
        
        /**
         * Validates a value against its schema with comprehensive type checking
         * @param value The value to validate
         * @param schema The schema to validate against
         * @param path The current validation path for error reporting
         * @return Validation result
         */
        private ValidationResult validateValue(Object value, Map<String, Object> schema, String path) {
            if (value == null) {
                return ValidationResult.valid(); // Null values are handled by required field validation
            }
            
            Object typeObj = schema.get("type");
            if (!(typeObj instanceof String)) {
                return ValidationResult.valid(); // No type constraint
            }
            
            String type = (String) typeObj;
            
            switch (type) {
                case "string":
                    return validateString(value, schema, path);
                case "number":
                    return validateNumber(value, schema, path);
                case "integer":
                    return validateInteger(value, schema, path);
                case "boolean":
                    return validateBoolean(value, path);
                case "array":
                    return validateArray(value, schema, path);
                case "object":
                    return validateObject(value, schema, path);
                default:
                    return ValidationResult.valid(); // Unknown type, allow
            }
        }
        
        /**
         * Validates a string value with additional string constraints
         * @param value The value to validate
         * @param schema The schema containing string constraints
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateString(Object value, Map<String, Object> schema, String path) {
            if (!(value instanceof String)) {
                return ValidationResult.invalid(buildPath(path) + "Expected string but got " + 
                    value.getClass().getSimpleName() + " (" + value + ")");
            }
            
            String stringValue = (String) value;
            
            // Check enum values
            Object enumObj = schema.get("enum");
            if (enumObj instanceof java.util.List) {
                java.util.List<String> enumValues = (java.util.List<String>) enumObj;
                if (!enumValues.contains(stringValue)) {
                    return ValidationResult.invalid(buildPath(path) + "Value '" + stringValue + 
                        "' is not one of allowed values: " + enumValues);
                }
            }
            
            // Check minimum length
            Object minLengthObj = schema.get("minLength");
            if (minLengthObj instanceof Number) {
                int minLength = ((Number) minLengthObj).intValue();
                if (stringValue.length() < minLength) {
                    return ValidationResult.invalid(buildPath(path) + "String length " + 
                        stringValue.length() + " is less than minimum " + minLength);
                }
            }
            
            // Check maximum length
            Object maxLengthObj = schema.get("maxLength");
            if (maxLengthObj instanceof Number) {
                int maxLength = ((Number) maxLengthObj).intValue();
                if (stringValue.length() > maxLength) {
                    return ValidationResult.invalid(buildPath(path) + "String length " + 
                        stringValue.length() + " exceeds maximum " + maxLength);
                }
            }
            
            return ValidationResult.valid();
        }
        
        /**
         * Validates a number value with numerical constraints
         * @param value The value to validate
         * @param schema The schema containing number constraints
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateNumber(Object value, Map<String, Object> schema, String path) {
            if (!(value instanceof Number)) {
                return ValidationResult.invalid(buildPath(path) + "Expected number but got " + 
                    value.getClass().getSimpleName() + " (" + value + ")");
            }
            
            double numberValue = ((Number) value).doubleValue();
            
            // Check minimum
            Object minimumObj = schema.get("minimum");
            if (minimumObj instanceof Number) {
                double minimum = ((Number) minimumObj).doubleValue();
                if (numberValue < minimum) {
                    return ValidationResult.invalid(buildPath(path) + "Value " + numberValue + 
                        " is less than minimum " + minimum);
                }
            }
            
            // Check maximum
            Object maximumObj = schema.get("maximum");
            if (maximumObj instanceof Number) {
                double maximum = ((Number) maximumObj).doubleValue();
                if (numberValue > maximum) {
                    return ValidationResult.invalid(buildPath(path) + "Value " + numberValue + 
                        " exceeds maximum " + maximum);
                }
            }
            
            return ValidationResult.valid();
        }
        
        /**
         * Validates an integer value with integer-specific constraints
         * @param value The value to validate
         * @param schema The schema containing integer constraints
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateInteger(Object value, Map<String, Object> schema, String path) {
            if (!(value instanceof Number)) {
                return ValidationResult.invalid(buildPath(path) + "Expected integer but got " + 
                    value.getClass().getSimpleName() + " (" + value + ")");
            }
            
            Number numberValue = (Number) value;
            
            // Check if it's actually an integer (not floating point)
            if (numberValue instanceof Float || numberValue instanceof Double) {
                double doubleVal = numberValue.doubleValue();
                if (doubleVal != Math.floor(doubleVal)) {
                    return ValidationResult.invalid(buildPath(path) + "Expected integer but got " + 
                        "floating point number " + doubleVal);
                }
            }
            
            // Apply number constraints
            return validateNumber(value, schema, path);
        }
        
        /**
         * Validates a boolean value
         * @param value The value to validate
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateBoolean(Object value, String path) {
            if (!(value instanceof Boolean)) {
                return ValidationResult.invalid(buildPath(path) + "Expected boolean but got " + 
                    value.getClass().getSimpleName() + " (" + value + ")");
            }
            return ValidationResult.valid();
        }
        
        /**
         * Validates an array value with item schema validation
         * @param value The value to validate
         * @param schema The schema containing array constraints
         * @param path The current validation path
         * @return Validation result
         */
        private ValidationResult validateArray(Object value, Map<String, Object> schema, String path) {
            if (!(value instanceof java.util.List)) {
                return ValidationResult.invalid(buildPath(path) + "Expected array but got " + 
                    value.getClass().getSimpleName() + " (" + value + ")");
            }
            
            java.util.List<?> arrayValue = (java.util.List<?>) value;
            
            // Check minimum items
            Object minItemsObj = schema.get("minItems");
            if (minItemsObj instanceof Number) {
                int minItems = ((Number) minItemsObj).intValue();
                if (arrayValue.size() < minItems) {
                    return ValidationResult.invalid(buildPath(path) + "Array length " + 
                        arrayValue.size() + " is less than minimum " + minItems);
                }
            }
            
            // Check maximum items
            Object maxItemsObj = schema.get("maxItems");
            if (maxItemsObj instanceof Number) {
                int maxItems = ((Number) maxItemsObj).intValue();
                if (arrayValue.size() > maxItems) {
                    return ValidationResult.invalid(buildPath(path) + "Array length " + 
                        arrayValue.size() + " exceeds maximum " + maxItems);
                }
            }
            
            // Validate array items
            Object itemsObj = schema.get("items");
            if (itemsObj instanceof Map) {
                Map<String, Object> itemSchema = (Map<String, Object>) itemsObj;
                for (int i = 0; i < arrayValue.size(); i++) {
                    Object item = arrayValue.get(i);
                    String itemPath = buildPath(path, "[" + i + "]");
                    ValidationResult itemValidation = validateValue(item, itemSchema, itemPath);
                    if (!itemValidation.isValid()) {
                        return itemValidation;
                    }
                }
            }
            
            return ValidationResult.valid();
        }
        
        /**
         * Builds a validation path for error reporting
         * @param basePath The base path
         * @param property The property name to append
         * @return The complete path
         */
        private String buildPath(String basePath, String property) {
            if (basePath == null || basePath.isEmpty()) {
                return property.startsWith("[") ? property : property;
            }
            return basePath + (property.startsWith("[") ? property : "." + property);
        }
        
        /**
         * Builds a validation path with proper formatting
         * @param path The path to format
         * @return Formatted path for error messages
         */
        private String buildPath(String path) {
            if (path == null || path.isEmpty()) {
                return "";
            }
            return path + ": ";
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