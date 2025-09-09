package com.example.agent.service;

import com.example.agent.prompt.AIPromptIntegrator;
import com.example.agent.prompt.PromptBuilder;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;
import com.example.domain.tools.ToolRegistry;
import com.example.infrastructure.ai.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AgentService is the main orchestrator for AI-driven drawing operations.
 * It handles user input, AI communication, tool execution, and approval workflows.
 */
public class AgentService {
    
    private static final Logger logger = Logger.getLogger(AgentService.class.getName());
    
    private final AIClient aiClient;
    private final AIPromptIntegrator promptIntegrator;
    private final ToolInvoker toolInvoker;
    private final IToolRegistry toolRegistry;
    private final Gson gson;
    private DrawingState currentState;
    private final List<PendingAction> pendingActions;
    
    /**
     * Creates a new AgentService with the specified AI client
     * @param aiClient The AI client to use for communication
     */
    public AgentService(AIClient aiClient) {
        this(aiClient, new ToolInvoker(), ToolRegistry.getInstance(), new Gson());
    }

    /**
     * 依存を注入可能な拡張コンストラクタ（DIP対応）。
     */
    public AgentService(AIClient aiClient, ToolInvoker toolInvoker, IToolRegistry toolRegistry, Gson gson) {
        this.aiClient = aiClient;
        this.toolInvoker = toolInvoker;
        this.toolRegistry = toolRegistry;
        this.promptIntegrator = new AIPromptIntegrator(aiClient, new PromptBuilder(toolRegistry));
        this.gson = gson;
        this.currentState = DrawingState.empty();
        this.pendingActions = new ArrayList<>();
    }
    
    /**
     * Creates a new AgentService with default AI client
     */
    public AgentService() {
        this(AIClientFactory.createApplicationClient());
    }
    
    /**
     * Processes a user instruction asynchronously
     * @param instruction The user's instruction
     * @return CompletableFuture with the service response
     */
    public CompletableFuture<ServiceResponse> processInstruction(String instruction) {
        logger.info("Processing instruction: " + instruction);
        
        return promptIntegrator.processMessage(instruction)
            .thenCompose(this::handleAIResponse)
            .exceptionally(this::handleError);
    }
    
    /**
     * Processes a user instruction synchronously
     * @param instruction The user's instruction
     * @return The service response
     */
    public ServiceResponse processInstructionSync(String instruction) {
        try {
            return processInstruction(instruction).get();
        } catch (Exception e) {
            return handleError(e);
        }
    }
    
    /**
     * Handles the AI response and processes any tool calls
     * @param response The processed AI response
     * @return CompletableFuture with the service response
     */
    private CompletableFuture<ServiceResponse> handleAIResponse(AIPromptIntegrator.ProcessedResponse response) {
        ServiceResponse serviceResponse = new ServiceResponse();
        
        // Validate response is not null
        if (response == null) {
            logger.warning("Received null processed response from AI");
            return CompletableFuture.completedFuture(
                ServiceResponse.error("Received invalid response from AI"));
        }
        
        // Log response details for debugging
        logger.info(String.format("Processing AI response - has tool calls: %s, has text: %s", 
            response.hasToolCalls(), response.hasTextResponse()));
        
        if (response.hasToolCalls()) {
            // Validate tool calls before processing
            List<AIResponse.ToolCall> validToolCalls = validateToolCalls(response.getToolCalls());
            
            if (validToolCalls.isEmpty()) {
                logger.warning("No valid tool calls found in response");
                // Fall back to text response if available
                if (response.hasTextResponse()) {
                    serviceResponse.setTextResponse(response.getTextResponse());
                    serviceResponse.setSuccess(true);
                    return CompletableFuture.completedFuture(serviceResponse);
                } else {
                    return CompletableFuture.completedFuture(
                        ServiceResponse.error("Invalid tool calls and no text response"));
                }
            }
            
            // Process valid tool calls
            List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
            
            for (AIResponse.ToolCall toolCall : validToolCalls) {
                futures.add(executeToolAsync(toolCall));
            }
            
            // Wait for all tool executions to complete
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ToolExecutionResult> results = new ArrayList<>();
                    for (CompletableFuture<ToolExecutionResult> future : futures) {
                        try {
                            results.add(future.join());
                        } catch (CompletionException e) {
                            logger.log(Level.WARNING, "Tool execution failed", e);
                            String toolName = extractToolNameFromException(e);
                            results.add(ToolExecutionResult.error(toolName, e.getCause().getMessage()));
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Unexpected error during tool execution", e);
                            results.add(ToolExecutionResult.error("Unknown", "Unexpected error: " + e.getMessage()));
                        }
                    }
                    
                    serviceResponse.setToolExecutionResults(results);
                    serviceResponse.setRequiresApproval(checkIfApprovalRequired(results));
                    
                    // Check if any tools succeeded
                    boolean hasSuccess = results.stream().anyMatch(ToolExecutionResult::isSuccess);
                    serviceResponse.setSuccess(hasSuccess);
                    
                    // Include text response if available and some tools failed
                    if (response.hasTextResponse() && !hasSuccess) {
                        serviceResponse.setTextResponse(
                            "Tool execution failed. AI response: " + response.getTextResponse());
                    }
                    
                    // Add tool results to conversation with better formatting
                    addToolResultsToConversation(results);
                    
                    return serviceResponse;
                })
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Failed to process tool calls", throwable);
                    ServiceResponse errorResponse = new ServiceResponse();
                    errorResponse.setSuccess(false);
                    errorResponse.setErrorMessage("Failed to process tool calls: " + throwable.getMessage());
                    if (response.hasTextResponse()) {
                        errorResponse.setTextResponse(response.getTextResponse());
                    }
                    return errorResponse;
                });
                
        } else if (response.hasTextResponse()) {
            // Handle text-only response
            String textResponse = response.getTextResponse();
            logger.info("Processing text-only response: " + 
                (textResponse.length() > 100 ? textResponse.substring(0, 100) + "..." : textResponse));
            
            serviceResponse.setTextResponse(textResponse);
            serviceResponse.setSuccess(true);
            return CompletableFuture.completedFuture(serviceResponse);
            
        } else {
            // No valid response - this should be rare
            logger.warning("AI response contains neither tool calls nor text response");
            serviceResponse.setErrorMessage("AI provided an empty or malformed response");
            serviceResponse.setSuccess(false);
            return CompletableFuture.completedFuture(serviceResponse);
        }
    }
    
    /**
     * Validates a list of tool calls, filtering out invalid ones
     * @param toolCalls The tool calls to validate
     * @return List of valid tool calls
     */
    private List<AIResponse.ToolCall> validateToolCalls(List<AIResponse.ToolCall> toolCalls) {
        List<AIResponse.ToolCall> validCalls = new ArrayList<>();
        
        if (toolCalls == null) {
            return validCalls;
        }
        
        for (AIResponse.ToolCall toolCall : toolCalls) {
            if (isValidToolCall(toolCall)) {
                validCalls.add(toolCall);
            } else {
                logger.warning(String.format("Skipping invalid tool call: %s", 
                    toolCall != null ? toolCall.getName() : "null"));
            }
        }
        
        logger.info(String.format("Validated tool calls: %d valid out of %d total", 
            validCalls.size(), toolCalls.size()));
        
        return validCalls;
    }
    
    /**
     * Checks if a tool call is valid
     * @param toolCall The tool call to validate
     * @return true if valid
     */
    private boolean isValidToolCall(AIResponse.ToolCall toolCall) {
        if (toolCall == null) {
            return false;
        }
        
        if (toolCall.getName() == null || toolCall.getName().trim().isEmpty()) {
            return false;
        }
        
        if (!toolRegistry.hasTool(toolCall.getName())) {
            logger.warning("Tool not found in registry: " + toolCall.getName());
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    /**
     * Adds tool execution results to the conversation with better formatting
     * @param results The tool execution results
     */
    private void addToolResultsToConversation(List<ToolExecutionResult> results) {
        for (ToolExecutionResult result : results) {
            String toolName = result.getToolName() != null ? result.getToolName() : "unknown";
            String formattedResult;
            
            if (result.isSuccess()) {
                formattedResult = String.format("Tool '%s' executed successfully", toolName);
                if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                    formattedResult += " with metadata: " + gson.toJson(result.getMetadata());
                }
            } else {
                formattedResult = String.format("Tool '%s' failed: %s", toolName, 
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error");
            }
            
            promptIntegrator.addToolResult(toolName, formattedResult);
        }
    }
    
    /**
     * Attempts to extract tool name from completion exception
     * @param e The completion exception
     * @return Tool name if found, "unknown" otherwise
     */
    private String extractToolNameFromException(CompletionException e) {
        // Try to extract tool name from exception message or stack trace
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null) {
            // Look for tool name patterns in the message
            String message = cause.getMessage();
            if (message.contains("Tool '") && message.contains("'")) {
                int start = message.indexOf("Tool '") + 6;
                int end = message.indexOf("'", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }
        }
        return "unknown";
    }
    
    /**
     * Executes a tool call asynchronously
     * @param toolCall The tool call to execute
     * @return CompletableFuture with the execution result
     */
    private CompletableFuture<ToolExecutionResult> executeToolAsync(AIResponse.ToolCall toolCall) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return toolInvoker.invokeTool(toolCall.getName(), 
                    toolCall.getParameters(), currentState);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Tool execution failed: " + toolCall.getName(), e);
                return ToolExecutionResult.error(e.getMessage());
            }
        });
    }
    
    /**
     * Checks if the tool execution results require user approval
     * @param results The tool execution results
     * @return true if approval is required
     */
    private boolean checkIfApprovalRequired(List<ToolExecutionResult> results) {
        // Approval required if:
        // - Multiple shapes are being added/modified
        // - Canvas is being cleared
        // - Major state changes
        
        long significantChanges = results.stream()
            .filter(ToolExecutionResult::isSuccess)
            .filter(r -> isSignificantChange(r.getToolName()))
            .count();
        
        return significantChanges >= 3 || 
               results.stream().anyMatch(r -> r.getToolName().equals("clearCanvas"));
    }
    
    /**
     * Determines if a tool represents a significant change
     * @param toolName The name of the tool
     * @return true if the tool makes significant changes
     */
    private boolean isSignificantChange(String toolName) {
        return toolName.contains("create") || 
               toolName.contains("delete") || 
               toolName.contains("clear") ||
               toolName.contains("modify");
    }
    
    /**
     * Handles errors during processing
     * @param throwable The error that occurred
     * @return Service response with error information
     */
    private ServiceResponse handleError(Throwable throwable) {
        logger.log(Level.SEVERE, "Error processing instruction", throwable);
        
        ServiceResponse response = new ServiceResponse();
        response.setSuccess(false);
        response.setErrorMessage("Error: " + throwable.getMessage());
        
        // Try to recover with a helpful message
        String recoveryPrompt = "An error occurred: " + throwable.getMessage() + 
                               ". Please try rephrasing your request.";
        response.setTextResponse(recoveryPrompt);
        
        return response;
    }
    
    /**
     * Approves pending actions and executes them
     * @return The result of executing approved actions
     */
    public CompletableFuture<ServiceResponse> approvePendingActions() {
        if (pendingActions.isEmpty()) {
            ServiceResponse response = new ServiceResponse();
            response.setSuccess(true);
            response.setTextResponse("No pending actions to approve");
            return CompletableFuture.completedFuture(response);
        }
        
        List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
        
        for (PendingAction action : pendingActions) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    DrawingState newState = action.getTool().execute(
                        action.getParameters(), currentState);
                    currentState = newState;
                    return ToolExecutionResult.success(action.getToolName(), newState);
                } catch (Exception e) {
                    return ToolExecutionResult.error(e.getMessage());
                }
            }));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                ServiceResponse response = new ServiceResponse();
                List<ToolExecutionResult> results = new ArrayList<>();
                
                for (CompletableFuture<ToolExecutionResult> future : futures) {
                    results.add(future.join());
                }
                
                response.setToolExecutionResults(results);
                response.setSuccess(results.stream().allMatch(ToolExecutionResult::isSuccess));
                response.setTextResponse("Executed " + results.size() + " approved actions");
                
                pendingActions.clear();
                return response;
            });
    }
    
    /**
     * Rejects pending actions
     */
    public void rejectPendingActions() {
        pendingActions.clear();
        logger.info("Rejected pending actions");
    }
    
    /**
     * Gets the current drawing state
     * @return The current drawing state
     */
    public DrawingState getCurrentState() {
        return currentState;
    }
    
    /**
     * Sets the current drawing state
     * @param state The new drawing state
     */
    public void setCurrentState(DrawingState state) {
        this.currentState = state;
    }
    
    /**
     * Clears the conversation history
     */
    public void clearConversation() {
        promptIntegrator.clearConversation();
    }
    
    /**
     * Gets the conversation history as a formatted string
     * @return The conversation history
     */
    public String getConversationHistory() {
        return promptIntegrator.getConversationHistory().export();
    }
    
    /**
     * Validates that a JSON string represents a valid tool call
     * @param jsonString The JSON string to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToolCallJson(String jsonString) {
        try {
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return false;
            }
            
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            
            // Check for tool name with multiple possible field names
            String toolName = null;
            if (json.has("tool")) {
                toolName = json.get("tool").getAsString();
            } else if (json.has("name")) {
                toolName = json.get("name").getAsString();
            } else if (json.has("function")) {
                toolName = json.get("function").getAsString();
            }
            
            if (toolName == null || toolName.trim().isEmpty()) {
                return false;
            }
            
            // Check if tool exists
            if (!toolRegistry.hasTool(toolName)) {
                logger.warning("Tool validation failed: tool '" + toolName + "' not found in registry");
                return false;
            }
            
            // Check for parameters field (with multiple possible field names)
            boolean hasParameters = json.has("parameters") || 
                                   json.has("args") || 
                                   json.has("arguments");
            
            if (!hasParameters) {
                logger.info("Tool call validation: no parameters field found, assuming empty parameters");
            }
            
            // Validate parameters against schema if present
            IAgentTool tool = toolRegistry.getTool(toolName);
            Map<String, Object> schema = tool.schema();
            
            if (schema != null && hasParameters) {
                Map<String, Object> parameters = null;
                if (json.has("parameters")) {
                    parameters = gson.fromJson(json.get("parameters"), Map.class);
                } else if (json.has("args")) {
                    parameters = gson.fromJson(json.get("args"), Map.class);
                } else if (json.has("arguments")) {
                    parameters = gson.fromJson(json.get("arguments"), Map.class);
                }
                
                // Use ToolInvoker's validation
                if (parameters != null && !toolInvoker.canInvokeTool(toolName, parameters)) {
                    logger.warning("Tool validation failed: parameters do not match schema for tool '" + toolName + "'");
                    return false;
                }
            }
            
            logger.info("Tool call JSON validation passed for tool: " + toolName);
            return true;
            
        } catch (JsonSyntaxException e) {
            logger.warning("Tool call JSON validation failed: invalid JSON syntax - " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            logger.warning("Tool call JSON validation failed: illegal JSON state - " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warning("Tool call JSON validation failed: unexpected error - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parses and executes tool calls from raw text content
     * @param rawContent The raw AI response content
     * @param currentState The current drawing state
     * @return CompletableFuture with service response
     */
    public CompletableFuture<ServiceResponse> parseAndExecuteToolCalls(String rawContent, DrawingState currentState) {
        try {
            // Create a mock processed response for the raw content
            AIPromptIntegrator.ProcessedResponse mockResponse = 
                new AIPromptIntegrator.ProcessedResponse();
            mockResponse.setTextResponse(rawContent);
            
            // Use the prompt integrator to extract tool calls
            return promptIntegrator.processMessage("")
                .thenCompose(this::handleAIResponse);
                
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse and execute tool calls from raw content", e);
            return CompletableFuture.completedFuture(
                ServiceResponse.error("Failed to parse tool calls: " + e.getMessage()));
        }
    }
    
    /**
     * Gets detailed information about available tools for debugging
     * @return A formatted string with tool information
     */
    public String getToolRegistryInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Available Tools:\n");
        
        for (String toolName : toolRegistry.getToolNames()) {
            info.append("- ").append(toolName);
            
            try {
                IAgentTool tool = toolRegistry.getTool(toolName);
                Map<String, Object> schema = tool.schema();
                if (schema != null) {
                    info.append(" (").append(schema.size()).append(" schema properties)");
                }
            } catch (Exception e) {
                info.append(" (schema error: ").append(e.getMessage()).append(")");
            }
            
            info.append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Represents a pending action awaiting approval
     */
    private static class PendingAction {
        private final String toolName;
        private final IAgentTool tool;
        private final Map<String, Object> parameters;
        
        public PendingAction(String toolName, IAgentTool tool, Map<String, Object> parameters) {
            this.toolName = toolName;
            this.tool = tool;
            this.parameters = parameters;
        }
        
        public String getToolName() { return toolName; }
        public IAgentTool getTool() { return tool; }
        public Map<String, Object> getParameters() { return parameters; }
    }
}
