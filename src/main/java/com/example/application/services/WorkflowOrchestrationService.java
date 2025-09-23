package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.agents.adk.ADKLlmWorkflowOrchestrator;
import com.example.domain.events.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service responsible for orchestrating drawing workflows. Extracted from AIAgentService to follow
 * Single Responsibility Principle.
 *
 * Responsibilities: - Coordinate workflow execution with ADK LLM - Manage workflow state
 * transitions - Handle workflow events and notifications - Provide workflow metrics and monitoring
 */
public class WorkflowOrchestrationService {

  private static final Logger LOGGER =
      Logger.getLogger(WorkflowOrchestrationService.class.getName());

  private final ADKLlmWorkflowOrchestrator llmWorkflowOrchestrator;
  private final List<IAIProcessingListener> listeners;
  private final AtomicLong sequenceCounter;
  private final String sessionId;
  private DrawingState currentState;

  public WorkflowOrchestrationService(String sessionId) {
    this.sessionId = sessionId;
    this.llmWorkflowOrchestrator = new ADKLlmWorkflowOrchestrator();
    this.listeners = new CopyOnWriteArrayList<>();
    this.sequenceCounter = new AtomicLong(0);
    this.currentState = DrawingState.defaultState();

    LOGGER.info("WorkflowOrchestrationService initialized with ADK LLM Workflow");
  }


  /**
   * Updates the current drawing state.
   */
  public void setCurrentState(DrawingState state) {
    this.currentState = state != null ? state : DrawingState.defaultState();
  }

  /**
   * Gets the current drawing state.
   */
  public DrawingState getCurrentState() {
    return currentState;
  }

  /**
   * Executes a workflow based on user prompt.
   *
   * @param userPrompt The user's natural language request
   * @return WorkflowResult containing the execution outcome
   */
  public CompletableFuture<WorkflowResult> executeWorkflow(String userPrompt) {
    String requestId = UUID.randomUUID().toString();
    fireProcessingStarted(userPrompt, requestId);

    return CompletableFuture.supplyAsync(() -> {
      try {
        LOGGER.info("Starting workflow execution for request: " + requestId);

        // Execute through ADK LLM workflow
        ADKLlmWorkflowOrchestrator.WorkflowResult adkResult =
            llmWorkflowOrchestrator.execute(userPrompt, currentState);

        // Convert ADK result to our domain result
        WorkflowResult result = convertResult(adkResult, requestId);

        // Update state if successful
        if (result.isSuccess()) {
          currentState = result.getFinalState();
          fireProcessingCompleted(result.getMessage(), currentState, requestId);
        } else {
          fireProcessingFailed(result.getErrorMessage(), FailurePoint.PROCESSING, requestId);
        }

        return result;

      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Workflow execution failed", e);
        String errorMessage = "Workflow execution error: " + e.getMessage();
        fireProcessingFailed(errorMessage, FailurePoint.PROCESSING, requestId);

        return WorkflowResult.failure(errorMessage, currentState);
      }
    });
  }

  /**
   * Validates a workflow without executing it.
   *
   * @param userPrompt The user's natural language request
   * @return ValidationResult indicating if the workflow is valid
   */
  public ValidationResult validateWorkflow(String userPrompt) {
    try {
      // Basic validation logic
      if (userPrompt == null || userPrompt.trim().isEmpty()) {
        return new ValidationResult(false, "Empty prompt is not valid");
      }

      // Could add more sophisticated validation here
      // For example, checking if the prompt contains valid tool references

      return new ValidationResult(true, "Workflow is valid");

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Workflow validation failed", e);
      return new ValidationResult(false, "Validation error: " + e.getMessage());
    }
  }

  /**
   * Gets workflow metrics for monitoring.
   */
  public WorkflowMetrics getMetrics() {
    // In a real implementation, these would be tracked over time
    return new WorkflowMetrics(sequenceCounter.get(), // Total workflows executed
        0, // Success count (would be tracked)
        0, // Failure count (would be tracked)
        0.0 // Average execution time (would be calculated)
    );
  }

  // Event listener management

  public void addProcessingListener(IAIProcessingListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  public void removeProcessingListener(IAIProcessingListener listener) {
    listeners.remove(listener);
  }

  // Event firing methods

  private void fireProcessingStarted(String userPrompt, String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ProcessingStartedEvent event = new ProcessingStartedEvent(meta, userPrompt);
    listeners.forEach(l -> {
      try {
        l.onProcessingStarted(event);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Listener exception in onProcessingStarted", e);
      }
    });
  }

  private void fireProcessingCompleted(String result, DrawingState newState, String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    String stateVersion = "v-" + System.currentTimeMillis();
    ProcessingCompletedEvent event = new ProcessingCompletedEvent(meta, result, stateVersion, null);
    listeners.forEach(l -> {
      try {
        l.onProcessingCompleted(event);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Listener exception in onProcessingCompleted", e);
      }
    });
  }

  private void fireProcessingFailed(String errorMessage, FailurePoint failurePoint,
      String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ProcessingFailedEvent event = new ProcessingFailedEvent(meta, errorMessage, failurePoint);
    listeners.forEach(l -> {
      try {
        l.onProcessingFailed(event);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Listener exception in onProcessingFailed", e);
      }
    });
  }

  // Helper methods

  private WorkflowResult convertResult(ADKLlmWorkflowOrchestrator.WorkflowResult adkResult,
      String requestId) {
    if (adkResult.isSuccess()) {
      return new WorkflowResult(true, adkResult.getMessage(), null, adkResult.getFinalState(),
          adkResult.getProgressLog(), adkResult.getScore(), requestId);
    } else {
      return new WorkflowResult(false, null, adkResult.getMessage(), adkResult.getFinalState(),
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

  /**
   * Represents validation result for a workflow.
   */
  public static class ValidationResult {
    private final boolean valid;
    private final String message;

    public ValidationResult(boolean valid, String message) {
      this.valid = valid;
      this.message = message;
    }

    public boolean isValid() {
      return valid;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Workflow execution metrics.
   */
  public static class WorkflowMetrics {
    private final long totalExecutions;
    private final long successCount;
    private final long failureCount;
    private final double averageExecutionTime;

    public WorkflowMetrics(long totalExecutions, long successCount, long failureCount,
        double averageExecutionTime) {
      this.totalExecutions = totalExecutions;
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.averageExecutionTime = averageExecutionTime;
    }

    public long getTotalExecutions() {
      return totalExecutions;
    }

    public long getSuccessCount() {
      return successCount;
    }

    public long getFailureCount() {
      return failureCount;
    }

    public double getAverageExecutionTime() {
      return averageExecutionTime;
    }

    public double getSuccessRate() {
      return totalExecutions > 0 ? (double) successCount / totalExecutions : 0.0;
    }
  }
}
