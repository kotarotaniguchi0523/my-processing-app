package com.example.agent.service;

import com.example.agent.prompt.AIPromptIntegrator;
import com.example.agent.prompt.PromptBuilder;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
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
    private final ToolRegistry toolRegistry;
    private final Gson gson;
    private DrawingState currentState;
    private final List<PendingAction> pendingActions;
    
    /**
     * Creates a new AgentService with the specified AI client
     * @param aiClient The AI client to use for communication
     */
    public AgentService(AIClient aiClient) {
        this.aiClient = aiClient;
        this.promptIntegrator = new AIPromptIntegrator(aiClient);
        this.toolInvoker = new ToolInvoker();
        this.toolRegistry = ToolRegistry.getInstance();
        this.gson = new Gson();
        this.currentState = new DrawingState();
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
        
        if (response.hasToolCalls()) {
            // Process tool calls
            List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
            
            for (AIResponse.ToolCall toolCall : response.getToolCalls()) {
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
                            results.add(ToolExecutionResult.error(e.getMessage()));
                        }
                    }
                    
                    serviceResponse.setToolExecutionResults(results);
                    serviceResponse.setRequiresApproval(checkIfApprovalRequired(results));
                    serviceResponse.setSuccess(results.stream().anyMatch(ToolExecutionResult::isSuccess));
                    
                    // Add tool results to conversation
                    for (ToolExecutionResult result : results) {
                        promptIntegrator.addToolResult(result.getToolName(), 
                            result.isSuccess() ? "Success" : "Failed: " + result.getErrorMessage());
                    }
                    
                    return serviceResponse;
                });
        } else if (response.hasTextResponse()) {
            // Handle text-only response
            serviceResponse.setTextResponse(response.getTextResponse());
            serviceResponse.setSuccess(true);
            return CompletableFuture.completedFuture(serviceResponse);
        } else {
            // No valid response
            serviceResponse.setErrorMessage("No valid response from AI");
            serviceResponse.setSuccess(false);
            return CompletableFuture.completedFuture(serviceResponse);
        }
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
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            
            // Check required fields
            if (!json.has("tool") || !json.has("parameters")) {
                return false;
            }
            
            String toolName = json.get("tool").getAsString();
            
            // Check if tool exists
            if (!toolRegistry.hasTool(toolName)) {
                return false;
            }
            
            // Validate parameters against schema
            IAgentTool tool = toolRegistry.getTool(toolName);
            Map<String, Object> schema = tool.schema();
            
            // Basic validation - can be enhanced
            return schema != null && schema.containsKey("properties");
            
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
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