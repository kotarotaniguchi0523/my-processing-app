package com.example.application.services;

import com.example.domain.models.AgentHistory;
import com.example.domain.models.DrawingState;

import java.util.logging.Logger;

/**
 * Manages conversation history and session persistence. Extracted from AIAgentService to follow
 * Single Responsibility Principle. Handles: - Session creation and management - History loading and
 * persistence coordination - History file operations
 */
public class HistoryManager {

  private static final Logger LOGGER = Logger.getLogger(HistoryManager.class.getName());

  private final ConversationService conversationService;
  private final JsonHistoryRepository historyRepository;
  private final boolean loadHistoryOnStartup;
  private AgentHistory.Session currentSession;

  public HistoryManager(ConversationService conversationService,
      JsonHistoryRepository historyRepository, boolean loadHistoryOnStartup,
      boolean persistHistory) {
    this.conversationService = conversationService;
    this.historyRepository = historyRepository;
    this.loadHistoryOnStartup = loadHistoryOnStartup;

    initializeSession(persistHistory);
  }

  /**
   * Initialize the session, either by loading existing history or creating new
   */
  private void initializeSession(boolean persistHistory) {
    if (loadHistoryOnStartup) {
      loadHistoryIfExists();
    } else {
      // Create new session if not loading from file
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession
          .addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }

    // Create initial history file if persistence is enabled and file doesn't exist
    if (persistHistory && !historyRepository.historyFileExists()) {
      DrawingState initialState = DrawingState.defaultState();
      historyRepository.persistHistoryImmediately(currentSession, initialState);
    }
  }

  /**
   * Load history from file if it exists
   */
  private void loadHistoryIfExists() {
    AgentHistory.Session loadedSession = historyRepository.loadHistoryFromFile();

    if (loadedSession != null) {
      // Load the session and update conversation service
      this.currentSession = loadedSession;
      conversationService.loadFromAgentHistory(loadedSession);

      LOGGER.info("History loaded from file successfully");
    } else {
      // Create new session if no history loaded
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession
          .addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));

      LOGGER.info("Created new session - no history file found");
    }
  }

  /**
   * Public wrapper to allow UI to trigger history loading on demand.
   */
  public boolean loadHistoryOnDemand() {
    int before = conversationService.getHistory().size();
    loadHistoryIfExists();
    return conversationService.getHistory().size() > before;
  }


  /**
   * Add a message to the current session
   */
  public void addMessageToSession(String role, String content) {
    if (currentSession != null) {
      currentSession.addMessage(new AgentHistory.Message(role, content));
    }
  }

  /**
   * Update the final state of the current session
   */
  public void updateSessionState(DrawingState state) {
    if (currentSession != null) {
      currentSession.setFinalState(state);
    }
  }

  /**
   * Persist the current session
   */
  public void persistSession(DrawingState currentState) {
    historyRepository.persistHistoryMaybe(currentSession, currentState);
  }

  /**
   * Get the current session
   */
  public AgentHistory.Session getCurrentSession() {
    return currentSession;
  }

  /**
   * Get the final state from the current session if available
   */
  public DrawingState getSessionFinalState() {
    return currentSession != null ? currentSession.getFinalState() : null;
  }
}
