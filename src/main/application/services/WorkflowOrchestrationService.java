package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.agents.adk.ADKLlmWorkflowOrchestrator;
import com.example.infrastructure.services.StateManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service responsible for orchestrating drawing workflows. Extracted from AIAgentService to follow
 * Single Responsibility Principle.
 *
 * Responsibilities: - Coordinate workflow execution with ADK LLM - Manage workflow state
 * transitions - Provide workflow metrics and monitoring
 */
public class WorkflowOrchestrationService {

  private static final Logger LOGGER =
      Logger.getLogger(WorkflowOrchestrationService.class.getName());

  private final ADKLlmWorkflowOrchestrator llmWorkflowOrchestrator;
  private StateManager stateManager;

  public WorkflowOrchestrationService(String sessionId) {
    this.llmWorkflowOrchestrator = new ADKLlmWorkflowOrchestrator();
    // StateManager will be set by AIAgentService

    LOGGER.info("WorkflowOrchestrationService initialized with ADK LLM Workflow");
  }


  /**
   * Sets the StateManager reference for accessing current state.
   */
  public void setStateManager(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  /**
   * Gets the current drawing state from StateManager.
   */
  public DrawingState getCurrentState() {
    if (stateManager == null) {
      throw new IllegalStateException("StateManager not set. Call setStateManager() first.");
    }
    return stateManager.getCurrentState();
  }

  /**
   * Executes a workflow based on user prompt.
   *
   * @param userPrompt The user's natural language request
   * @return WorkflowResult containing the execution outcome
   */
  public CompletableFuture<WorkflowResult> executeWorkflow(String userPrompt) {
    String requestId = UUID.randomUUID().toString();

    return CompletableFuture.supplyAsync(() -> {
      try {
        LOGGER.info("Starting workflow execution for request: " + requestId);

        // Get current state from StateManager
        DrawingState currentState = getCurrentState();

        // Execute through ADK LLM workflow
        com.example.domain.agents.adk.WorkflowResult adkResult =
            llmWorkflowOrchestrator.execute(userPrompt, currentState);

        // Convert ADK result to our domain result
        WorkflowResult result = convertResult(adkResult, requestId);

        // Note: State updates will be handled by AIAgentService to maintain single source of truth

        return result;

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Workflow execution failed", e);
        String errorMessage = "Workflow execution error: " + e.getMessage();

        return WorkflowResult.failure(errorMessage, getCurrentState());
      }
    });
  }



  // Helper methods

  private WorkflowResult convertResult(com.example.domain.agents.adk.WorkflowResult adkResult,
      String requestId) {
    if (adkResult.isSuccess()) {
      return new WorkflowResult(true, adkResult.getMessage(), null, adkResult.getState(),
          adkResult.getProgressLog(), 90.0, requestId);
    } else {
      return new WorkflowResult(false, null, adkResult.getMessage(), adkResult.getState(),
          adkResult.getProgressLog(), 0.0, requestId);
    }
  }

  // Result classes

  /**
   * Represents the result of a workflow execution.
   */
  public static class WorkflowResult {
    private final boolean success;
    private final String message;
    private final String errorMessage;
    private final DrawingState finalState;
    private final List<String> progressLog;
    private final double score;
    private final String requestId;

    public WorkflowResult(boolean success, String message, String errorMessage,
        DrawingState finalState, List<String> progressLog, double score, String requestId) {
      this.success = success;
      this.message = message;
      this.errorMessage = errorMessage;
      this.finalState = finalState;
      this.progressLog = progressLog;
      this.score = score;
      this.requestId = requestId;
    }

    public static WorkflowResult failure(String errorMessage, DrawingState currentState) {
      return new WorkflowResult(false, null, errorMessage, currentState, null, 0.0, null);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public DrawingState getFinalState() {
      return finalState;
    }

    public List<String> getProgressLog() {
      return progressLog;
    }

    public double getScore() {
      return score;
    }

    public String getRequestId() {
      return requestId;
    }
  }


}
