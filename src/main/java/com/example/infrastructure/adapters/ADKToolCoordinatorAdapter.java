package com.example.infrastructure.adapters;

import com.example.application.ports.ToolCoordinatorPort;
import com.example.domain.tools.adk.ADKTool;
import com.example.domain.tools.adk.ADKToolRegistry;
import com.example.domain.tools.adk.ADKTool.ToolResult;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.models.DrawingState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Adapter implementation of ToolCoordinatorPort for ADK tool integration.
 * Bridges the application layer's tool coordination needs with the ADK infrastructure.
 *
 * This adapter follows the Hexagonal Architecture pattern by:
 * - Implementing the application port interface
 * - Adapting ADK-specific tools to the domain tool interface
 * - Managing tool execution statistics and validation
 */
public class ADKToolCoordinatorAdapter implements ToolCoordinatorPort {

    private static final Logger LOGGER = Logger.getLogger(ADKToolCoordinatorAdapter.class.getName());

    private final ADKToolRegistry toolRegistry;
    private final Map<String, ToolExecutionStats> executionStats;
    private final Map<String, ADKTool> registeredTools;
    private DrawingState currentState;

    public ADKToolCoordinatorAdapter() {
        this.toolRegistry = ADKToolRegistry.getInstance();
        this.executionStats = new ConcurrentHashMap<>();
        this.registeredTools = new ConcurrentHashMap<>();
        this.currentState = DrawingState.defaultState();

        // Register ADK tools
        registerADKTools();
    }

    /**
     * Sets the current drawing state for tool execution context.
     */
    public void setCurrentState(DrawingState state) {
        this.currentState = state != null ? state : DrawingState.defaultState();
    }

    private void registerADKTools() {
        for (ADKTool tool : toolRegistry.getAllTools()) {
            Map<String, Object> schema = tool.getSchema();
            String toolName = schema.get("name") != null ? schema.get("name").toString() : tool.getClass().getSimpleName();
            registeredTools.put(toolName, tool);
            LOGGER.info("Registered ADK tool: " + toolName);
        }
    }

    @Override
    public List<ToolDescriptor> getAvailableTools() {
        return registeredTools.entrySet().stream()
            .map(entry -> createToolDescriptor(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ADKTool> findToolByName(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        return Optional.ofNullable(registeredTools.get(toolName));
    }

    @Override
    public ToolValidationResult validateToolParameters(String toolName, Map<String, Object> parameters) {
        if (toolName == null) {
            throw new IllegalArgumentException("Tool name cannot be null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Optional<ADKTool> toolOpt = findToolByName(toolName);
        if (!toolOpt.isPresent()) {
            errors.add("Tool not found: " + toolName);
            return new ToolValidationResult(false, errors, warnings);
        }

        ADKTool tool = toolOpt.get();
        Map<String, Object> schema = tool.getSchema();

        // Basic validation - check if schema defines required parameters
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaParams = (Map<String, Object>) schema.get("parameters");
        if (schemaParams != null) {
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) schemaParams.get("required");
            if (required != null) {
                for (String requiredParam : required) {
                    if (!parameters.containsKey(requiredParam)) {
                        errors.add("Missing required parameter: " + requiredParam);
                    }
                }
            }
        }

        return new ToolValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public CompletableFuture<ToolExecutionResult> executeTool(String toolName, Map<String, Object> parameters) {
        if (toolName == null) {
            throw new IllegalArgumentException("Tool name cannot be null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                // Validate parameters first
                ToolValidationResult validation = validateToolParameters(toolName, parameters);
                if (!validation.isValid()) {
                    return ToolExecutionResult.failure("Validation failed: " + String.join(", ", validation.getErrors()));
                }

                // Find and execute tool
                Optional<ADKTool> toolOpt = findToolByName(toolName);
                if (!toolOpt.isPresent()) {
                    return ToolExecutionResult.failure("Tool not found: " + toolName);
                }

                ADKTool tool = toolOpt.get();

                // Execute the tool with current state
                ToolResult result = tool.execute(parameters, currentState);

                // Update statistics
                updateStatistics(toolName, true, System.currentTimeMillis() - startTime);

                // Update current state if successful
                if (result.isSuccess()) {
                    this.currentState = result.getNewState();
                    return ToolExecutionResult.success(result.getShapeId(), result.getMessage());
                } else {
                    return ToolExecutionResult.failure(result.getMessage());
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Tool execution failed: " + toolName, e);
                updateStatistics(toolName, false, System.currentTimeMillis() - startTime);
                return ToolExecutionResult.failure("Tool execution failed: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<ToolExecutionResult>> executeToolChain(List<ToolExecution> toolChain) {
        if (toolChain == null || toolChain.isEmpty()) {
            throw new IllegalArgumentException("Tool chain cannot be null or empty");
        }

        return CompletableFuture.supplyAsync(() -> {
            List<ToolExecutionResult> results = new ArrayList<>();

            for (ToolExecution execution : toolChain) {
                try {
                    CompletableFuture<ToolExecutionResult> future = executeTool(
                        execution.getToolName(),
                        execution.getParameters()
                    );

                    ToolExecutionResult result = future.get();
                    results.add(result);

                    if (!result.isSuccess() && !execution.shouldContinueOnFailure()) {
                        // Stop execution chain on failure if configured
                        break;
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Tool chain execution failed at: " + execution.getToolName(), e);
                    results.add(ToolExecutionResult.failure("Failed to execute: " + execution.getToolName()));

                    if (!execution.shouldContinueOnFailure()) {
                        break;
                    }
                }
            }

            return results;
        });
    }

    @Override
    public boolean registerTool(ADKTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }

        try {
            Map<String, Object> schema = tool.getSchema();
            String toolName = schema.get("name") != null ? schema.get("name").toString() : tool.getClass().getSimpleName();
            registeredTools.put(toolName, tool);
            LOGGER.info("Dynamically registered tool: " + toolName);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register tool", e);
            return false;
        }
    }

    @Override
    public boolean unregisterTool(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }

        ADKTool removed = registeredTools.remove(toolName);
        if (removed != null) {
            LOGGER.info("Unregistered tool: " + toolName);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, ToolExecutionStats> getExecutionStatistics() {
        return new HashMap<>(executionStats);
    }

    private ToolDescriptor createToolDescriptor(String toolName, ADKTool tool) {
        Map<String, Object> schema = tool.getSchema();
        String description = schema.get("description") != null ? schema.get("description").toString() : "";

        Map<String, ParameterDescriptor> params = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaParams = (Map<String, Object>) schema.get("parameters");
        if (schemaParams != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schemaParams.get("properties");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) schemaParams.get("required");

            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String paramName = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramSchema = (Map<String, Object>) entry.getValue();

                    String type = paramSchema.get("type") != null ? paramSchema.get("type").toString() : "object";
                    String paramDesc = paramSchema.get("description") != null ? paramSchema.get("description").toString() : "";
                    boolean isRequired = required != null && required.contains(paramName);
                    Object defaultValue = paramSchema.get("default");

                    params.put(paramName, new ParameterDescriptor(paramName, type, paramDesc, isRequired, defaultValue));
                }
            }
        }

        List<String> examples = new ArrayList<>(); // Could be populated from schema if available

        return new ToolDescriptor(toolName, description, params, examples, true);
    }

    private void updateStatistics(String toolName, boolean success, long executionTime) {
        executionStats.compute(toolName, (name, stats) -> {
            if (stats == null) {
                return new ToolExecutionStats(
                    toolName,
                    1,
                    success ? 1 : 0,
                    success ? 0 : 1,
                    executionTime
                );
            }

            long total = stats.getTotalExecutions() + 1;
            long successful = stats.getSuccessfulExecutions() + (success ? 1 : 0);
            long failed = stats.getFailedExecutions() + (success ? 0 : 1);
            double avgTime = (stats.getAverageExecutionTime() * stats.getTotalExecutions() + executionTime) / total;

            return new ToolExecutionStats(toolName, total, successful, failed, avgTime);
        });
    }
}