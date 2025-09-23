package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.events.*;
import com.example.domain.models.AgentHistory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Refactored AIAgentService for managing AI agent interactions.
 * Delegates workflow orchestration to WorkflowOrchestrationService for better separation of concerns.
 * Follows Single Responsibility Principle by focusing on:
 * - Managing conversation history
 * - Coordinating between services
 * - Handling approval workflows
 */
public class AIAgentService {
  private DrawingState currentState;

  // --- Separated Service Components ---
  private final ConversationService conversationService;
  private final JsonHistoryRepository historyRepository;
  private final WorkflowOrchestrationService workflowService;

  // --- Event System ---
  private final List<IAIProcessingListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicLong sequenceCounter = new AtomicLong(0);

  // --- Session Management ---
  private AgentHistory.Session currentSession;
  private final boolean loadHistoryOnStartup;

  // --- Diff Approval ---
  private ApprovalCallback approvalCallback;

  /**
   * Default constructor keeps previous behavior: load history on startup and enable persistence.
   */
  public AIAgentService(DrawingState initialState) {
    this(initialState, true, true);
  }

  /**
   * New constructor: control whether to load history on startup and whether to persist history.
   *
   * @param initialState initial drawing state (nullable)
   * @param loadHistoryOnStartup if true, attempt to load `.agent_history.json` on construction
   * @param persistHistory if true, persist history to `.agent_history.json` at intervals
   */
  public AIAgentService(DrawingState initialState, boolean loadHistoryOnStartup,
      boolean persistHistory) {
    this.currentState = initialState != null ? initialState : DrawingState.defaultState();

    // Initialize separated service components
    this.conversationService = new ConversationService();
    this.historyRepository = new JsonHistoryRepository(persistHistory);
    this.workflowService = new WorkflowOrchestrationService(conversationService.getSessionId());

    // Set initial state for workflow service
    this.workflowService.setCurrentState(this.currentState);

    // Forward event listeners to workflow service
    this.workflowService.addProcessingListener(new ForwardingListener());

    Logger.getLogger(getClass().getName())
        .info("AIAgentService initialized with WorkflowOrchestrationService");
    
    this.loadHistoryOnStartup = loadHistoryOnStartup;
    
    if (this.loadHistoryOnStartup) {
      loadHistoryIfExists();
    } else {
      // Create new session if not loading from file
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession.addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }
    
    // Create initial history file if persistence is enabled and file doesn't exist
    if (persistHistory && !historyRepository.historyFileExists()) {
      historyRepository.persistHistoryImmediately(currentSession, currentState);
    }
  }

  public DrawingState getCurrentState() {
    return currentState;
  }

  // --- Event Listener Management ---

  /**
   * Add a listener for AI processing events.
   */
  public void addProcessingListener(IAIProcessingListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener for AI processing events.
   */
  public void removeProcessingListener(IAIProcessingListener listener) {
    listeners.remove(listener);
  }

  // --- Event Firing Methods ---

  private void fireProcessingStarted(String userPrompt, String requestId) {
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
    ProcessingStartedEvent event = new ProcessingStartedEvent(meta, userPrompt);
    listeners.forEach(l -> {
      try {
        l.onProcessingStarted(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  // Removed unused event firing methods - these are now handled by LLM workflow

  private void fireProcessingCompleted(String result, DrawingState newState, String requestId) {
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
    String stateVersion = "v-" + System.currentTimeMillis();
    UUID toolCallId = null; // Non-tool response
    ProcessingCompletedEvent event =
        new ProcessingCompletedEvent(meta, result, stateVersion, toolCallId);
    listeners.forEach(l -> {
      try {
        l.onProcessingCompleted(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void fireProcessingFailed(String errorMessage, FailurePoint failurePoint,
      String requestId) {
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
    ProcessingFailedEvent event = new ProcessingFailedEvent(meta, errorMessage, failurePoint);
    listeners.forEach(l -> {
      try {
        l.onProcessingFailed(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  /**
   * Interface for approval/rejection callbacks
   */
  public interface ApprovalCallback {
    void onApprove(DrawingState newState, String message);
    void onReject();
  }
  
  /**
   * Set the approval callback for state changes
   */
  public void setApprovalCallback(ApprovalCallback callback) {
    this.approvalCallback = callback;
  }
  
  /**
   * Apply approved state changes
   */
  public void applyApprovedState(DrawingState newState, String message) {
    this.currentState = newState;
    
    // Record the approved change in conversation history
    conversationService.addAssistantMessage(message);
    if (currentSession != null) {
      currentSession.addMessage(new AgentHistory.Message("assistant", message));
      currentSession.setFinalState(currentState);
    }
    
    // Persist history
    historyRepository.persistHistoryMaybe(currentSession, currentState);
    
    // State has been applied and approved
  }
  

  /**
   * @deprecated LLM workflow is now always enabled through WorkflowOrchestrationService
   */
  @Deprecated
  public void setLlmWorkflowMode(boolean enabled) {
    // No-op: LLM workflow is always enabled now
    if (!enabled) {
      Logger.getLogger(getClass().getName())
          .warning("AFC mode has been removed. ADK LLM Workflow is always enabled through WorkflowOrchestrationService.");
    }
  }


  /**
   * Handle user prompt using WorkflowOrchestrationService.
   */
  public AgentReply handleUserPrompt(String userPrompt) {
    String requestId = UUID.randomUUID().toString();
    fireProcessingStarted(userPrompt, requestId);

    try {
      // Add user message to conversation history
      conversationService.addUserMessage(userPrompt);
      if (currentSession != null) {
        currentSession.addMessage(new AgentHistory.Message("user", userPrompt));
      }

      // Execute workflow through orchestration service
      WorkflowOrchestrationService.WorkflowResult result =
          workflowService.executeWorkflow(userPrompt).get();

      if (result.isSuccess()) {
        DrawingState newState = result.getFinalState();
        String message = result.getMessage();

        // Request approval if callback is set
        if (approvalCallback != null && !newState.equals(currentState)) {
          approvalCallback.onApprove(newState, message);
        }

        // Update current state
        this.currentState = newState;
        workflowService.setCurrentState(newState);

        // Record in conversation history
        conversationService.addAssistantMessage(message);
        if (currentSession != null) {
          currentSession.addMessage(new AgentHistory.Message("assistant", message));
          currentSession.setFinalState(newState);
        }

        fireProcessingCompleted(message, newState, requestId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", true);
        metadata.put("progressLog", result.getProgressLog());
        metadata.put("score", result.getScore());

        // Persist history
        historyRepository.persistHistoryMaybe(currentSession, currentState);

        return new AgentReply(message, newState, metadata);
      } else {
        String errorMessage = result.getErrorMessage();
        fireProcessingFailed(errorMessage, FailurePoint.PROCESSING, requestId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", false);
        metadata.put("progressLog", result.getProgressLog());

        return new AgentReply(errorMessage, currentState, metadata);
      }
    } catch (Exception e) {
      String errorMessage = "Workflow execution error: " + e.getMessage();
      Logger.getLogger(getClass().getName())
          .log(Level.SEVERE, "Workflow execution failed", e);

      fireProcessingFailed(errorMessage, FailurePoint.PROCESSING, requestId);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("success", false);
      metadata.put("error", e.getMessage());

      return new AgentReply(errorMessage, currentState, metadata);
    }
  }






  /**
   * @deprecated Tool execution is now handled through WorkflowOrchestrationService
   */
  @Deprecated
  public AgentReply executeTool(String toolName, Map<String, Object> params) {
    // This method is no longer used - tools are executed through ADK workflow
    return new AgentReply("Tool execution moved to WorkflowOrchestrationService", currentState, new HashMap<>());
  }


  /**
   * Clear persisted history file (if exists) and reset in-memory history to seeded system prompt.
   * Best-effort: exceptions are swallowed to avoid affecting agent runtime.
   */
  public void clearHistoryFile() {
    historyRepository.clearHistoryFile();
    conversationService.resetHistory();
  }

  private void loadHistoryIfExists() {
    AgentHistory.Session loadedSession = historyRepository.loadHistoryFromFile();
    
    if (loadedSession != null) {
      // Load the session and update conversation service
      this.currentSession = loadedSession;
      conversationService.loadFromAgentHistory(loadedSession);
      
      // Load final state if available
      if (currentSession.getFinalState() != null) {
        currentState = currentSession.getFinalState();
      }
    } else {
      // Create new session if no history loaded
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession.addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }
  }

  /** Public wrapper to allow UI to trigger history loading on demand. */
  public boolean loadHistoryOnDemand() {
    int before = conversationService.getHistory().size();
    loadHistoryIfExists();
    return conversationService.getHistory().size() > before;
  }


  public List<ChatMessage> getHistory() {
    return conversationService.getHistory();
  }

  public void resetHistory() {
    conversationService.resetHistory();
  }

  // --- ChatMessage DTO ---
  public static class ChatMessage {
    public final Role role;
    public final String content;
    public final List<ToolCall> toolCalls;

    private ChatMessage(Role role, String content) {
      this(role, content, null);
    }

    private ChatMessage(Role role, String content, List<ToolCall> toolCalls) {
      this.role = role;
      this.content = content;
      this.toolCalls = toolCalls;
    }

    public static ChatMessage system(String c) {
      return new ChatMessage(Role.SYSTEM, c);
    }

    public static ChatMessage user(String c) {
      return new ChatMessage(Role.USER, c);
    }

    public static ChatMessage assistant(String c) {
      return new ChatMessage(Role.ASSISTANT, c);
    }

    public static ChatMessage assistantWithTools(String c, List<ToolCall> tools) {
      return new ChatMessage(Role.ASSISTANT, c, tools);
    }
  }

  // --- ToolCall DTO ---
  public static class ToolCall {
    public final String name;
    public final Map<String, Object> arguments;
    public final String result;

    public ToolCall(String name, Map<String, Object> arguments, String result) {
      this.name = name;
      this.arguments = arguments;
      this.result = result;
    }
  }

  public enum Role {
    SYSTEM("System"), USER("User"), ASSISTANT("Assistant");

    public final String display;

    Role(String d) {
      this.display = d;
    }
  }



  /** Simple immutable reply DTO */
  /**
   * Inner class to forward events from workflow service to local listeners.
   */
  private class ForwardingListener implements IAIProcessingListener {
    @Override
    public void onProcessingStarted(ProcessingStartedEvent event) {
      listeners.forEach(l -> {
        try {
          l.onProcessingStarted(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }

    @Override
    public void onPromptAnalyzing(PromptAnalyzingEvent event) {
      listeners.forEach(l -> {
        try {
          l.onPromptAnalyzing(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }

    @Override
    public void onToolSelecting(ToolSelectingEvent event) {
      listeners.forEach(l -> {
        try {
          l.onToolSelecting(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }

    @Override
    public void onToolExecuting(ToolExecutingEvent event) {
      listeners.forEach(l -> {
        try {
          l.onToolExecuting(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }

    @Override
    public void onProcessingCompleted(ProcessingCompletedEvent event) {
      listeners.forEach(l -> {
        try {
          l.onProcessingCompleted(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }

    @Override
    public void onProcessingFailed(ProcessingFailedEvent event) {
      listeners.forEach(l -> {
        try {
          l.onProcessingFailed(event);
        } catch (Exception e) {
          // Prevent listener exceptions from affecting processing
        }
      });
    }
  }

  public static class AgentReply {
    public final boolean success;
    public final String message;
    public final DrawingState state;
    public final Map<String, Object> metadata;
    public final List<String> progressLog;

    // Main constructor with all fields
    public AgentReply(boolean success, String message, DrawingState state, List<String> progressLog) {
      this.success = success;
      this.message = message;
      this.state = state;
      this.metadata = null;
      this.progressLog = progressLog != null ? progressLog : new ArrayList<>();
    }

    // Backward compatibility constructor
    public AgentReply(String message, DrawingState state, Map<String, Object> metadata) {
      this.success = true;
      this.message = message;
      this.state = state;
      this.metadata = metadata;
      this.progressLog = new ArrayList<>();
    }
  }

}
