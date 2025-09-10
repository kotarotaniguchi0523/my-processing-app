package com.example.infrastructure.adapters;

import com.example.application.agent.AgentService;
import com.example.application.agent.ServiceResponse;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.models.DrawingState;
import com.example.domain.services.IAIService;
import com.example.infrastructure.ai.AIClient;
import com.example.infrastructure.ai.AIClientFactory;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Adapter that implements the domain IAIService interface using the agent layer
 * This provides dependency inversion, allowing the domain to define the interface
 * while the infrastructure provides the implementation
 */
public class AIServiceAdapter implements IAIService {
    
    private static final Logger logger = Logger.getLogger(AIServiceAdapter.class.getName());
    
    private final AgentService agentService;
    
    /**
     * Create an adapter with default AI client
     */
    public AIServiceAdapter() {
        this(createDefaultAgentService());
    }
    
    /**
     * Create an adapter with a specific AI client
     * @param aiClient The AI client to use
     */
    public AIServiceAdapter(AIClient aiClient) {
        this(new AgentService(aiClient));
    }
    
    /**
     * Create an adapter with a specific agent service
     * @param agentService The agent service to wrap
     */
    public AIServiceAdapter(AgentService agentService) {
        if (agentService == null) {
            throw new IllegalArgumentException("AgentService cannot be null");
        }
        this.agentService = agentService;
    }
    
    @Override
    public CompletableFuture<IAIService.AIServiceResponse> processInstruction(String instruction) {
        return agentService.processInstruction(instruction)
            .thenApply(obj -> convertResponse(obj))
            .exceptionally(throwable -> {
                logger.warning("AI processing failed: " + throwable.getMessage());
                return IAIService.AIServiceResponse.error(throwable.getMessage());
            });
    }
    
    @Override
    public void setCurrentState(DrawingState state) {
        agentService.setCurrentState(state);
    }
    
    @Override
    public DrawingState getCurrentState() {
        return agentService.getCurrentState();
    }
    
    @Override
    public CompletableFuture<Boolean> awaitApproval() {
        return agentService.awaitApproval();
    }
    
    @Override
    public void completeApproval(boolean approved) {
        agentService.completeApproval(approved);
    }
    
    @Override
    public void setProgressListener(ProgressListener listener) {
        if (listener != null) {
            // Adapt the domain listener to the agent listener
            agentService.setProgressListener(message -> {
                if (message != null && !message.isEmpty()) {
                    listener.onProgress(message);
                }
            });
        } else {
            agentService.setProgressListener(null);
        }
    }
    
    /**
     * Convert agent service response to domain response
     */
    private IAIService.AIServiceResponse convertResponse(Object obj) {
        if (obj == null) {
            return IAIService.AIServiceResponse.error("Empty response from AI");
        }
        
        // Cast the Object to ServiceResponse
        if (!(obj instanceof ServiceResponse)) {
            logger.warning("Unexpected response type: " + obj.getClass().getName());
            return IAIService.AIServiceResponse.error("Invalid response type from AI service");
        }
        
        ServiceResponse response = (ServiceResponse) obj;
        
        if (response.hasErrorMessage()) {
            return IAIService.AIServiceResponse.error(response.getErrorMessage());
        }
        
        String textResponse = response.hasTextResponse() ? response.getTextResponse() : null;
        DrawingState resultState = null;
        
        // Extract the last successful state from tool execution results
        if (response.hasToolExecutionResults()) {
            for (ToolExecutionResult result : response.getToolExecutionResults()) {
                if (result.isSuccess() && result.hasResultState()) {
                    resultState = result.getResultState();
                }
            }
        }
        
        if (resultState != null) {
            return IAIService.AIServiceResponse.withState(textResponse, resultState, response.requiresApproval());
        } else if (textResponse != null) {
            return IAIService.AIServiceResponse.text(textResponse);
        } else {
            return IAIService.AIServiceResponse.error("No valid response from AI");
        }
    }
    
    /**
     * Create a default agent service
     */
    private static AgentService createDefaultAgentService() {
        try {
            AIClient aiClient = AIClientFactory.createApplicationClient();
            return new AgentService(aiClient);
        } catch (Exception e) {
            logger.warning("Failed to create default AI client: " + e.getMessage());
            // Return null to allow offline operation
            return null;
        }
    }
    
    /**
     * Check if the AI service is available
     * @return true if AI service is available
     */
    public boolean isAvailable() {
        return agentService != null;
    }
}