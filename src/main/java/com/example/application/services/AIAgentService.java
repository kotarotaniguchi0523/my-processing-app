package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.AgentHistory;
import com.example.infrastructure.ServiceInitializer;
import com.example.infrastructure.services.StateManager;
import com.example.infrastructure.services.HistoryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Refactored AIAgentService for managing AI agent interactions. Delegates workflow orchestration to
 * WorkflowOrchestrationService for better separation of concerns. Follows Single Responsibility
 * Principle by focusing on: - Managing conversation history - Coordinating between services -
 * Handling approval workflows
 */
public class AIAgentService {

  // --- Separated Service Components ---
  private final ConversationService conversationService;
  private final JsonHistoryRepository historyRepository;
  private final WorkflowOrchestrationService workflowService;
  private final StateChangeNotifier stateChangeNotifier;
  private StateManager stateManager;
  private HistoryService historyService;

  // --- Extracted Components ---
  private final StateSynchronizationManager stateSyncManager;
  private final HistoryManager historyManager;

  // --- Session Management ---
  private AgentHistory.Session currentSession;

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
    // Initialize the StateManager with the initial state
    if (initialState == null) {
      initialState = DrawingState.defaultState();
    }

    // Initialize separated service components
    this.conversationService = new ConversationService();
    this.historyRepository = new JsonHistoryRepository(persistHistory);
    this.workflowService = new WorkflowOrchestrationService(conversationService.getSessionId());
    this.stateChangeNotifier = new StateChangeNotifier();

    // Get StateManager and HistoryService from ServiceInitializer
    // ServiceInitializer should already be initialized by SimpleSketch
    this.stateManager = ServiceInitializer.getStateManager();
    this.historyService = ServiceInitializer.getHistoryService();

    // Initialize extracted components
    this.stateSyncManager = new StateSynchronizationManager(stateManager, historyService, stateChangeNotifier);
    this.historyManager = new HistoryManager(conversationService, historyRepository, loadHistoryOnStartup, persistHistory);

    // Set initial state for state manager first, then workflow service
    this.stateManager.updateState(initialState);
    this.workflowService.setStateManager(this.stateManager);


    Logger.getLogger(getClass().getName())
        .info("AIAgentService initialized with WorkflowOrchestrationService");


    // Get session from history manager
    this.currentSession = historyManager.getCurrentSession();
    stateSyncManager.setCurrentSession(currentSession);

    // Load final state if available
    DrawingState sessionState = historyManager.getSessionFinalState();
    if (sessionState != null) {
      stateManager.updateState(sessionState);
    }
  }

  public DrawingState getCurrentState() {
    return stateManager.getCurrentState();
  }

  /**
   * Get the state change notifier for registering listeners.
   *
   * @return the state change notifier
   */
  public StateChangeNotifier getStateChangeNotifier() {
    return stateChangeNotifier;
  }

  /**
   * Get the StateManager instance for direct access to state operations.
   *
   * @return the state manager
   */
  public StateManager getStateManager() {
    return stateManager;
  }

  /**
   * Get the HistoryService instance for direct access to undo/redo operations.
   *
   * @return the history service
   */
  public HistoryService getHistoryService() {
    return historyService;
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
    DrawingState previousState = stateSyncManager.getCurrentState();

    // Record the approved change in conversation history
    conversationService.addAssistantMessage(message);
    historyManager.addMessageToSession("assistant", message);
    historyManager.updateSessionState(newState);

    // Persist history
    historyManager.persistSession(newState);

    // Apply state change through sync manager
    stateSyncManager.applyStateChange(previousState, newState, "Approved drawing change: " + message);
  }




  /**
   * Handle user prompt using WorkflowOrchestrationService.
   */
  public AgentReply handleUserPrompt(String userPrompt) {

    try {
      // Add user message to conversation history
      conversationService.addUserMessage(userPrompt);
      historyManager.addMessageToSession("user", userPrompt);

      // Execute workflow through orchestration service
      WorkflowOrchestrationService.WorkflowResult result =
          workflowService.executeWorkflow(userPrompt).get();

      if (result.isSuccess()) {
        DrawingState newState = result.getFinalState();
        String message = result.getMessage();

        // Get current state from StateManager for comparison
        DrawingState currentState = stateManager.getCurrentState();

        // Request approval if callback is set
        if (approvalCallback != null && !newState.equals(currentState)) {
          approvalCallback.onApprove(newState, message);
        }

        // Update state through StateManager (single source of truth)
        DrawingState previousState = currentState;

        // Record in conversation history
        conversationService.addAssistantMessage(message);
        historyManager.addMessageToSession("assistant", message);
        historyManager.updateSessionState(newState);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", true);
        metadata.put("progressLog", result.getProgressLog());
        metadata.put("score", result.getScore());

        // Persist history
        historyManager.persistSession(newState);

        // Apply state change through sync manager
        stateSyncManager.applyStateChange(previousState, newState,
            "Workflow execution completed: " + message);

        return new AgentReply(message, newState, metadata);
      } else {
        String errorMessage = result.getErrorMessage();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", false);
        metadata.put("progressLog", result.getProgressLog());

        return new AgentReply(errorMessage, stateManager.getCurrentState(), metadata);
      }
    } catch (Exception e) {
      String errorMessage = "Workflow execution error: " + e.getMessage();
      Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Workflow execution failed", e);

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("success", false);
      metadata.put("error", e.getMessage());

      return new AgentReply(errorMessage, stateManager.getCurrentState(), metadata);
    }
  }





  /**
   * Clear persisted history file (if exists) and reset in-memory history to seeded system prompt.
   * Best-effort: exceptions are swallowed to avoid affecting agent runtime.
   */
  public void clearHistoryFile() {
    historyManager.clearHistory();
    this.currentSession = historyManager.getCurrentSession();
    stateSyncManager.setCurrentSession(currentSession);
  }

  /** Public wrapper to allow UI to trigger history loading on demand. */
  public boolean loadHistoryOnDemand() {
    boolean loaded = historyManager.loadHistoryOnDemand();
    if (loaded) {
      this.currentSession = historyManager.getCurrentSession();
      stateSyncManager.setCurrentSession(currentSession);
      DrawingState sessionState = historyManager.getSessionFinalState();
      if (sessionState != null) {
        stateSyncManager.updateState(sessionState);
      }
    }
    return loaded;
  }


  public List<MessageConverter.ChatMessage> getHistory() {
    return conversationService.getHistory();
  }

  public void resetHistory() {
    conversationService.resetHistory();
  }




  /** Simple immutable reply DTO */

  public static class AgentReply {
    public final boolean success;
    public final String message;
    public final DrawingState state;
    public final Map<String, Object> metadata;
    public final List<String> progressLog;

    // Main constructor with all fields
    public AgentReply(boolean success, String message, DrawingState state,
        List<String> progressLog) {
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
