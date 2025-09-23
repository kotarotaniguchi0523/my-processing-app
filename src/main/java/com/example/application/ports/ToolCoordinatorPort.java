package com.example.application.ports;

import com.example.domain.tools.adk.ADKTool;
import com.example.domain.values.ToolExecutionResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port interface for coordinating tool execution following the Hexagonal Architecture pattern.
 * This interface defines the contract for tool orchestration required by the application layer,
 * enabling different implementations for tool execution strategies.
 * 
 * <p>Responsibilities:
 * - Define contracts for tool discovery and execution
 * - Provide abstractions for tool validation and coordination
 * - Support both synchronous and asynchronous tool execution
 * 
 * <p>Design Principles:
 * - Dependency Inversion: Application layer defines interface, infrastructure implements
 * - Interface Segregation: Focused on tool coordination operations only
 * - Single Responsibility: Solely responsible for tool orchestration
 */
public interface ToolCoordinatorPort {
    
    /**
     * Discovers and returns all available tools for the current context.
     * 
     * @return List of available tools with their metadata
     */
    List<ToolDescriptor> getAvailableTools();
    
    /**
     * Finds a specific tool by its name.
     * 
     * @param toolName The name of the tool to find
     * @return Optional containing the tool if found, empty otherwise
     * @throws IllegalArgumentException if toolName is null or empty
     */
    Optional<ADKTool> findToolByName(String toolName);
    
    /**
     * Validates tool parameters before execution.
     * 
     * @param toolName The name of the tool to validate
     * @param parameters The parameters to validate
     * @return Validation result indicating success or failure with details
     * @throws IllegalArgumentException if toolName is null or parameters is null
     */
    ToolValidationResult validateToolParameters(String toolName, Map<String, Object> parameters);
    
    /**
     * Executes a single tool with the given parameters.
     * 
     * @param toolName The name of the tool to execute
     * @param parameters The parameters for tool execution
     * @return CompletableFuture containing the execution result
     * @throws IllegalArgumentException if toolName is null or parameters is null
     */
    CompletableFuture<ToolExecutionResult> executeTool(String toolName, Map<String, Object> parameters);
    
    /**
     * Executes multiple tools in a coordinated sequence.
     * 
     * @param toolChain List of tool executions to perform in order
     * @return CompletableFuture containing the results of all executions
     * @throws IllegalArgumentException if toolChain is null or empty
     */
    CompletableFuture<List<ToolExecutionResult>> executeToolChain(List<ToolExecution> toolChain);
    
    /**
     * Registers a new tool dynamically.
     * 
     * @param tool The tool to register
     * @return true if registration was successful, false otherwise
     * @throws IllegalArgumentException if tool is null
     */
    boolean registerTool(ADKTool tool);
    
    /**
     * Unregisters a tool by name.
     * 
     * @param toolName The name of the tool to unregister
     * @return true if unregistration was successful, false if tool not found
     * @throws IllegalArgumentException if toolName is null or empty
     */
    boolean unregisterTool(String toolName);
    
    /**
     * Gets execution statistics for monitoring and debugging.
     * 
     * @return Map containing execution statistics by tool name
     */
    Map<String, ToolExecutionStats> getExecutionStatistics();
    
    /**
     * Represents metadata about an available tool.
     */
    class ToolDescriptor {
        private final String name;
        private final String description;
        private final Map<String, ParameterDescriptor> parameters;
        private final List<String> examples;
        private final boolean isEnabled;
        
        public ToolDescriptor(String name, String description, Map<String, ParameterDescriptor> parameters, 
                            List<String> examples, boolean isEnabled) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.examples = examples;
            this.isEnabled = isEnabled;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, ParameterDescriptor> getParameters() { return parameters; }
        public List<String> getExamples() { return examples; }
        public boolean isEnabled() { return isEnabled; }
    }
    
    /**
     * Represents a parameter descriptor for a tool.
     */
    class ParameterDescriptor {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final Object defaultValue;
        
        public ParameterDescriptor(String name, String type, String description, boolean required, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.defaultValue = defaultValue;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
        public Object getDefaultValue() { return defaultValue; }
    }
    
    /**
     * Result of tool parameter validation.
     */
    class ToolValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ToolValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
    
    /**
     * Represents a single tool execution in a chain.
     */
    class ToolExecution {
        private final String toolName;
        private final Map<String, Object> parameters;
        private final boolean continueOnFailure;
        
        public ToolExecution(String toolName, Map<String, Object> parameters, boolean continueOnFailure) {
            this.toolName = toolName;
            this.parameters = parameters;
            this.continueOnFailure = continueOnFailure;
        }
        
        public String getToolName() { return toolName; }
        public Map<String, Object> getParameters() { return parameters; }
        public boolean shouldContinueOnFailure() { return continueOnFailure; }
    }
    
    /**
     * Execution statistics for a tool.
     */
    class ToolExecutionStats {
        private final String toolName;
        private final long totalExecutions;
        private final long successfulExecutions;
        private final long failedExecutions;
        private final double averageExecutionTime;
        
        public ToolExecutionStats(String toolName, long totalExecutions, long successfulExecutions, 
                                long failedExecutions, double averageExecutionTime) {
            this.toolName = toolName;
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.averageExecutionTime = averageExecutionTime;
        }
        
        public String getToolName() { return toolName; }
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
    }
}