package com.example.agent.core;

import com.example.agent.manager.ToolManager;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Agent is the base class for all AI agents in the system Provides core functionality for
 * reasoning, tool execution, and state management
 */
public abstract class Agent {

    protected final String name;
    protected final String description;
    protected final ToolManager toolManager;
    protected AgentContext context;
    protected final Map<String, Object> configuration;

    protected Agent(String name, String description, ToolManager toolManager) {
        this.name = Objects.requireNonNull(name, "Agent name cannot be null");
        this.description = Objects.requireNonNull(description, "Agent description cannot be null");
        this.toolManager = Objects.requireNonNull(toolManager, "ToolManager cannot be null");
        this.configuration = new HashMap<>();
    }

    // Abstract methods that concrete agents must implement

    /**
     * Process a user instruction and determine what actions to take
     *
     * @param instruction the user's instruction
     * @return the agent's response and any actions taken
     */
    public abstract AgentResponse processInstruction(String instruction);

    /** Initialize the agent with any specific setup requirements */
    protected abstract void initialize();

    /**
     * Analyze the current state and determine if any actions are needed
     *
     * @return analysis result with suggested actions
     */
    public abstract StateAnalysis analyzeCurrentState();

    // Core agent functionality

    /** Update the agent's context */
    public void updateContext(AgentContext newContext) {
        this.context = newContext;
    }

    /** Execute a tool with the given parameters */
    protected ToolResult executeTool(String toolName, Map<String, Object> parameters)
            throws ToolExecutionException {
        if (context == null) {
            throw new IllegalStateException("Agent context must be set before executing tools");
        }
        return toolManager.executeTool(toolName, parameters, context);
    }

    /** Execute multiple tools in sequence */
    protected List<ToolResult> executeTools(List<ToolExecution> executions) {
        List<ToolResult> results = new java.util.ArrayList<>();

        for (ToolExecution execution : executions) {
            try {
                ToolResult result = executeTool(execution.toolName, execution.parameters);
                results.add(result);

                // Update context with new state if the tool modified it
                if (result.hasNewState()) {
                    context = context.withCurrentState(result.getNewState());
                }

                // Stop execution if a tool failed and is marked as critical
                if (result.isError() && execution.critical) {
                    break;
                }

            } catch (ToolExecutionException e) {
                ToolResult errorResult = e.toToolResult();
                results.add(errorResult);

                // Stop execution if this was a critical tool
                if (execution.critical) {
                    break;
                }
            }
        }

        return results;
    }

    /** Get available tools for this agent */
    public List<String> getAvailableTools() {
        return new java.util.ArrayList<>(toolManager.getRegisteredToolNames());
    }

    /** Check if a tool is available */
    public boolean hasToolAvailable(String toolName) {
        return toolManager.isToolRegistered(toolName);
    }

    /** Get tool information */
    public Map<String, Object> getToolInfo(String toolName) {
        return toolManager.getToolInfo(toolName);
    }

    // Configuration management

    /** Set a configuration value */
    public void setConfiguration(String key, Object value) {
        configuration.put(key, value);
    }

    /** Get a configuration value */
    public Object getConfiguration(String key) {
        return configuration.get(key);
    }

    /** Get a configuration value with default */
    @SuppressWarnings("unchecked")
    public <T> T getConfiguration(String key, T defaultValue) {
        Object value = configuration.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /** Get string configuration value */
    public String getStringConfiguration(String key, String defaultValue) {
        Object value = configuration.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    /** Get boolean configuration value */
    public boolean getBooleanConfiguration(String key, boolean defaultValue) {
        Object value = configuration.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    /** Get integer configuration value */
    public int getIntConfiguration(String key, int defaultValue) {
        Object value = configuration.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    // Utility methods

    /** Check if the agent has a current context */
    public boolean hasContext() {
        return context != null;
    }

    /** Check if the current context has a drawing state */
    public boolean hasCurrentState() {
        return hasContext() && context.hasCurrentState();
    }

    /** Check if the current context has state history */
    public boolean hasStateHistory() {
        return hasContext() && context.hasStateHistory();
    }

    /** Get current drawing state if available */
    public DrawingState getCurrentState() {
        return hasCurrentState() ? context.getCurrentState().get() : null;
    }

    /** Get state history if available */
    public StateHistory getStateHistory() {
        return hasStateHistory() ? context.getStateHistory().get() : null;
    }

    // Getters

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AgentContext getContext() {
        return context;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public Map<String, Object> getConfiguration() {
        return new HashMap<>(configuration);
    }

    @Override
    public String toString() {
        return String.format(
                "%s(name='%s', hasContext=%s, tools=%d)",
                getClass().getSimpleName(), name, hasContext(), getAvailableTools().size());
    }

    // Inner classes

    /** Represents a single tool execution request */
    public static class ToolExecution {
        public final String toolName;
        public final Map<String, Object> parameters;
        public final boolean critical; // If true, failure stops execution chain

        public ToolExecution(String toolName, Map<String, Object> parameters, boolean critical) {
            this.toolName = toolName;
            this.parameters = new HashMap<>(parameters);
            this.critical = critical;
        }

        public ToolExecution(String toolName, Map<String, Object> parameters) {
            this(toolName, parameters, false);
        }

        public static ToolExecution of(String toolName, Map<String, Object> parameters) {
            return new ToolExecution(toolName, parameters);
        }

        public static ToolExecution critical(String toolName, Map<String, Object> parameters) {
            return new ToolExecution(toolName, parameters, true);
        }

        @Override
        public String toString() {
            return String.format(
                    "ToolExecution(tool='%s', critical=%s, params=%d)",
                    toolName, critical, parameters.size());
        }
    }

    /** Represents the result of processing an instruction */
    public static class AgentResponse {
        private final String message;
        private final List<ToolResult> toolResults;
        private final Map<String, Object> metadata;
        private final boolean success;

        public AgentResponse(
                String message,
                List<ToolResult> toolResults,
                Map<String, Object> metadata,
                boolean success) {
            this.message = message;
            this.toolResults =
                    toolResults != null
                            ? new java.util.ArrayList<>(toolResults)
                            : new java.util.ArrayList<>();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.success = success;
        }

        public static AgentResponse success(String message) {
            return new AgentResponse(message, null, null, true);
        }

        public static AgentResponse success(String message, List<ToolResult> toolResults) {
            return new AgentResponse(message, toolResults, null, true);
        }

        public static AgentResponse error(String message) {
            return new AgentResponse(message, null, null, false);
        }

        public static AgentResponse error(String message, List<ToolResult> toolResults) {
            return new AgentResponse(message, toolResults, null, false);
        }

        // Getters
        public String getMessage() {
            return message;
        }

        public List<ToolResult> getToolResults() {
            return new java.util.ArrayList<>(toolResults);
        }

        public Map<String, Object> getMetadata() {
            return new HashMap<>(metadata);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean hasToolResults() {
            return !toolResults.isEmpty();
        }

        @Override
        public String toString() {
            return String.format(
                    "AgentResponse(success=%s, tools=%d, message='%s')",
                    success,
                    toolResults.size(),
                    message.length() > 50 ? message.substring(0, 50) + "..." : message);
        }
    }

    /** Represents the result of analyzing current state */
    public static class StateAnalysis {
        private final String summary;
        private final List<String> observations;
        private final List<String> suggestions;
        private final Map<String, Object> metrics;

        public StateAnalysis(
                String summary,
                List<String> observations,
                List<String> suggestions,
                Map<String, Object> metrics) {
            this.summary = summary;
            this.observations =
                    observations != null
                            ? new java.util.ArrayList<>(observations)
                            : new java.util.ArrayList<>();
            this.suggestions =
                    suggestions != null
                            ? new java.util.ArrayList<>(suggestions)
                            : new java.util.ArrayList<>();
            this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        }

        public static StateAnalysis create(String summary) {
            return new StateAnalysis(summary, null, null, null);
        }

        // Getters
        public String getSummary() {
            return summary;
        }

        public List<String> getObservations() {
            return new java.util.ArrayList<>(observations);
        }

        public List<String> getSuggestions() {
            return new java.util.ArrayList<>(suggestions);
        }

        public Map<String, Object> getMetrics() {
            return new HashMap<>(metrics);
        }

        public boolean hasObservations() {
            return !observations.isEmpty();
        }

        public boolean hasSuggestions() {
            return !suggestions.isEmpty();
        }

        @Override
        public String toString() {
            return String.format(
                    "StateAnalysis(observations=%d, suggestions=%d)",
                    observations.size(), suggestions.size());
        }
    }
}
