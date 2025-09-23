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

    // Set initial state for state manager first, then workflow service
    this.stateManager.updateState(initialState);
    this.workflowService.setStateManager(this.stateManager);

    // Register as a state change listener
    setupStateChangeListener();

    // Register with StateManager to receive state updates (for undo/redo)
    setupStateManagerListener();


    Logger.getLogger(getClass().getName())
        .info("AIAgentService initialized with WorkflowOrchestrationService");

    this.loadHistoryOnStartup = loadHistoryOnStartup;

    if (this.loadHistoryOnStartup) {
      loadHistoryIfExists();
    } else {
      // Create new session if not loading from file
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession
          .addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }

    // Create initial history file if persistence is enabled and file doesn't exist
    if (persistHistory && !historyRepository.historyFileExists()) {
      historyRepository.persistHistoryImmediately(currentSession, stateManager.getCurrentState());
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
   * Set up the state change listener to connect StateChangeNotifier with StateManager.
   */
  private void setupStateChangeListener() {
    stateChangeNotifier.addListener(new StateChangeNotifier.StateChangeListener() {
      @Override
      public void onStateChanged(DrawingState newState, String changeDescription) {
        // This is already handled in the calling methods, but we can log it
        Logger.getLogger(getClass().getName()).info("State change processed: " + changeDescription);
      }
    });
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
   * Set up the StateManager listener to sync state changes back to AIAgentService. This ensures
   * undo/redo operations update the AI agent's current state.
   */
  private void setupStateManagerListener() {
    stateManager.addListener(new com.example.domain.services.StateChangeListener() {
      @Override
      public void onStateChanged(DrawingState oldState, DrawingState newState) {
        // Update session state if available
        if (currentSession != null) {
          currentSession.setFinalState(newState);
        }

        Logger.getLogger(getClass().getName())
            .info("AIAgentService notified of state change: "
                + stateManager.calculateStateDifference(oldState, newState));
      }

      @Override
      public void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
        // Log history status changes
        Logger.getLogger(getClass().getName())
            .fine("History status: canUndo=" + canUndo + ", canRedo=" + canRedo);
      }

      @Override
      public void onStateValidationError(DrawingState state, String error) {
        Logger.getLogger(getClass().getName()).warning("State validation error: " + error);
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
    DrawingState previousState = stateManager.getCurrentState();

    // Record the approved change in conversation history
    conversationService.addAssistantMessage(message);
    if (currentSession != null) {
      currentSession.addMessage(new AgentHistory.Message("assistant", message));
      currentSession.setFinalState(newState);
    }

    // Persist history
    historyRepository.persistHistoryMaybe(currentSession, newState);

    // Update StateManager and HistoryService (StateManager is single source of truth)
    if (!newState.equals(previousState)) {
      stateManager.updateState(newState);
      historyService.addState(newState);
      stateChangeNotifier.notifyStateChanged(newState, "Approved drawing change: " + message);
    }
  }




  /**
   * Handle user prompt using WorkflowOrchestrationService.
   */
  public AgentReply handleUserPrompt(String userPrompt) {

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
        if (currentSession != null) {
          currentSession.addMessage(new AgentHistory.Message("assistant", message));
          currentSession.setFinalState(newState);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("success", true);
        metadata.put("progressLog", result.getProgressLog());
        metadata.put("score", result.getScore());

        // Persist history
        historyRepository.persistHistoryMaybe(currentSession, newState);

        // Update StateManager and HistoryService, then notify listeners
        if (!newState.equals(previousState)) {
          stateManager.updateState(newState);
          historyService.addState(newState);
          stateChangeNotifier.notifyStateChanged(newState,
              "Workflow execution completed: " + message);
        }

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
    historyRepository.clearHistoryFile();
    conversationService.resetHistory();
  }

  private void loadHistoryIfExists() {
    AgentHistory.Session loadedSession = historyRepository.loadHistoryFromFile();

    if (loadedSession != null) {
      // Load the session and update conversation service
      this.currentSession = loadedSession;
      conversationService.loadFromAgentHistory(loadedSession);

      // Load final state if available and update StateManager
      if (currentSession.getFinalState() != null) {
        stateManager.updateState(currentSession.getFinalState());
      }
    } else {
      // Create new session if no history loaded
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession
          .addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
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
