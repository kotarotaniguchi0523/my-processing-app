package com.example.infrastructure.adapters;

import com.example.application.agent.AgentService;
import com.example.application.agent.ServiceResponse;
import com.example.domain.models.DrawingState;
import com.example.domain.services.IAgentService;
import com.example.infrastructure.ai.AIClient;
import com.example.infrastructure.ai.AIClientFactory;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Adapter that implements the domain IAgentService interface using the agent layer This provides
 * dependency inversion, allowing the domain to define the interface while the infrastructure
 * provides the implementation
 */
public class AgentServiceAdapter implements IAgentService {

  private static final Logger logger = Logger.getLogger(AgentServiceAdapter.class.getName());

  private final AgentService agentService;

  /**
   * Create an adapter with default AI client
   */
  public AgentServiceAdapter() {
    this(createDefaultAgentService());
  }

  /**
   * Create an adapter with a specific AI client
   * 
   * @param aiClient The AI client to use
   */
  public AgentServiceAdapter(AIClient aiClient) {
    this(new AgentService(aiClient));
  }

  /**
   * Create an adapter with a specific agent service
   * 
   * @param agentService The agent service to wrap
   */
  public AgentServiceAdapter(AgentService agentService) {
    if (agentService == null) {
      throw new IllegalArgumentException("AgentService cannot be null");
    }
    this.agentService = agentService;
  }

  public CompletableFuture<ServiceResponse> processUserInput(String input,
      DrawingState currentState) {
    // Set the current state before processing
    agentService.setCurrentState(currentState);
    return agentService.processInstruction(input).thenApply(this::convertToServiceResponse)
        .exceptionally(throwable -> {
          logger.warning("Agent processing failed: " + throwable.getMessage());
          return ServiceResponse.error(throwable.getMessage());
        });
  }

  @Override
  public CompletableFuture<Object> processInstruction(String instruction) {
    return agentService.processInstruction(instruction).thenApply(response -> (Object) response)
        .exceptionally(throwable -> {
          logger.warning("Agent instruction processing failed: " + throwable.getMessage());
          return ServiceResponse.error(throwable.getMessage());
        });
  }

  @Override
  public DrawingState getCurrentState() {
    return agentService.getCurrentState();
  }

  @Override
  public void setCurrentState(DrawingState state) {
    agentService.setCurrentState(state);
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
  public boolean hasPendingApproval() {
    return agentService.hasPendingApproval();
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
   * Convert agent service response to domain service response
   */
  private ServiceResponse convertToServiceResponse(Object responseObj) {
    if (responseObj == null) {
      return ServiceResponse.error("Empty response from agent");
    }

    // Type check and cast to the expected ServiceResponse type
    if (!(responseObj instanceof ServiceResponse)) {
      logger.warning("Unexpected response type: " + responseObj.getClass().getName());
      return ServiceResponse
          .error("Invalid response type from agent: " + responseObj.getClass().getName());
    }

    ServiceResponse agentResponse = (ServiceResponse) responseObj;

    // Since we're using the same ServiceResponse class, we can return it directly
    return agentResponse;
  }

  /**
   * Create a default agent service
   */
  private static AgentService createDefaultAgentService() {
    // Initialize tools before creating AgentService
    com.example.application.tools.ToolInitializer.initialize();
    
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
   * Check if the agent service is available
   * 
   * @return true if agent service is available
   */
  public boolean isAvailable() {
    return agentService != null;
  }

  @Override
  public Object processInstructionSync(String instruction) {
    try {
      return processInstruction(instruction).get();
    } catch (Exception e) {
      logger.warning("Synchronous processing failed: " + e.getMessage());
      return ServiceResponse.error(e.getMessage());
    }
  }

  @Override
  public void clearConversation() {
    agentService.clearConversation();
  }

  @Override
  public String getConversationHistory() {
    return agentService.getConversationHistory();
  }

  @Override
  public String getToolRegistryInfo() {
    return agentService.getToolRegistryInfo();
  }

}
