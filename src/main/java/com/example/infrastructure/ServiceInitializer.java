package com.example.infrastructure;

import com.example.application.services.AIAgentService;
import com.example.infrastructure.services.StateManager;
import com.example.infrastructure.services.HistoryService;

/**
 * Initializes domain services with infrastructure implementations. Simplified after removing
 * UseCase layer and StateService.
 */
public class ServiceInitializer {

  private static boolean initialized = false;
  private static AIAgentService aiAgentService;
  private static StateManager stateManager;
  private static HistoryService historyService;

  /**
   * Initialize all domain services with infrastructure implementations.
   *
   * @throws IllegalStateException if initialization fails
   */
  public static synchronized void initialize() throws IllegalStateException {
    if (!initialized) {
      try {
        // Create infrastructure implementations with null checks
        stateManager = new StateManager();

        historyService = new HistoryService(stateManager);

        initialized = true;

      } catch (Exception e) {
        // Reset state on failure
        stateManager = null;
        historyService = null;
        initialized = false;

        String message = "Failed to initialize services: " + e.getMessage();
        throw new IllegalStateException(message, e);
      }
    }
  }

  /**
   * Check if services are initialized.
   *
   * @return true if initialized, false otherwise
   */
  public static boolean isInitialized() {
    return initialized;
  }

  public static AIAgentService getAIAgentService() {
    if (!initialized) {
      throw new IllegalStateException(
          "ServiceInitializer must be initialized first. Call initialize() before accessing services.");
    }
    return aiAgentService;
  }

  public static void setAIAgentService(AIAgentService service) {
    if (service == null) {
      throw new IllegalArgumentException("AIAgentService cannot be null");
    }
    aiAgentService = service;
  }

  public static StateManager getStateManager() {
    if (!initialized) {
      // Auto-initialize if not done yet for backward compatibility
      initialize();
    }
    if (stateManager == null) {
      throw new IllegalStateException(
          "StateManager is not available. Initialization may have failed.");
    }
    return stateManager;
  }

  public static HistoryService getHistoryService() {
    if (!initialized) {
      // Auto-initialize if not done yet for backward compatibility
      initialize();
    }
    if (historyService == null) {
      throw new IllegalStateException(
          "HistoryService is not available. Initialization may have failed.");
    }
    return historyService;
  }
}
