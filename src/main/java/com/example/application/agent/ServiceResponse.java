package com.example.application.agent;

import com.example.domain.agent.DrawingStateDiff;
import com.example.domain.values.ToolExecutionResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the response from the AgentService after processing an instruction
 */
public class ServiceResponse {
    
    private boolean success;
    private String textResponse;
    private String errorMessage;
    private List<ToolExecutionResult> toolExecutionResults;
    private boolean requiresApproval;
    private DrawingStateDiff stateDiff;
    
    public ServiceResponse() {
        this.toolExecutionResults = new ArrayList<>();
        this.success = false;
        this.requiresApproval = false;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getTextResponse() {
        return textResponse;
    }
    
    public void setTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<ToolExecutionResult> getToolExecutionResults() {
        return toolExecutionResults;
    }
    
    public void setToolExecutionResults(List<ToolExecutionResult> toolExecutionResults) {
        this.toolExecutionResults = toolExecutionResults != null ? 
            toolExecutionResults : new ArrayList<>();
    }
    
    public boolean requiresApproval() {
        return requiresApproval;
    }
    
    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }
    
    public DrawingStateDiff getStateDiff() {
        return stateDiff;
    }
    
    public void setStateDiff(DrawingStateDiff stateDiff) {
        this.stateDiff = stateDiff;
    }
    
    /**
     * Checks if there are tool execution results
     * @return true if there are tool execution results
     */
    public boolean hasToolExecutionResults() {
        return toolExecutionResults != null && !toolExecutionResults.isEmpty();
    }
    
    /**
     * Checks if there is a text response
     * @return true if there is a text response
     */
    public boolean hasTextResponse() {
        return textResponse != null && !textResponse.trim().isEmpty();
    }
    
    /**
     * Checks if there is an error message
     * @return true if there is an error message
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    /**
     * Checks if there is a result state (state diff)
     * @return true if there is a result state
     */
    public boolean hasResultState() {
        return stateDiff != null;
    }
    
    /**
     * Gets the error message (alias for getErrorMessage for compatibility)
     * @return the error message
     */
    public String getError() {
        return errorMessage;
    }
    
    /**
     * Gets a summary of the response
     * @return A summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (success) {
            sb.append("Success");
            if (hasToolExecutionResults()) {
                sb.append(" - Executed ").append(toolExecutionResults.size()).append(" tool(s)");
            }
            if (requiresApproval) {
                sb.append(" (Requires Approval)");
            }
        } else {
            sb.append("Failed");
            if (hasErrorMessage()) {
                sb.append(" - ").append(errorMessage);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a success response with text
     * @param text The response text
     * @return A new success response
     */
    public static ServiceResponse success(String text) {
        ServiceResponse response = new ServiceResponse();
        response.setSuccess(true);
        response.setTextResponse(text);
        return response;
    }
    
    /**
     * Creates an error response
     * @param error The error message
     * @return A new error response
     */
    public static ServiceResponse error(String error) {
        ServiceResponse response = new ServiceResponse();
        response.setSuccess(false);
        response.setErrorMessage(error);
        return response;
    }
    
    @Override
    public String toString() {
        return String.format("ServiceResponse{success=%s, requiresApproval=%s, tools=%d, text='%s'}",
            success, requiresApproval, 
            toolExecutionResults != null ? toolExecutionResults.size() : 0,
            textResponse != null ? textResponse.substring(0, Math.min(50, textResponse.length())) : "");
    }
}