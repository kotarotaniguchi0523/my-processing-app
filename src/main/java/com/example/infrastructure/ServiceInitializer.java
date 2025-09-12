package com.example.infrastructure;

import com.example.application.services.StateService;
import com.example.application.services.AIAgentService;
import com.example.application.usecase.UseCaseFacade;
import com.example.domain.services.ShapeOps;
import com.example.infrastructure.services.InMemoryShapeOps;
import com.example.infrastructure.adapters.StateManagerAdapter;
import com.example.infrastructure.adapters.HistoryServiceAdapter;
import com.example.infrastructure.services.StateManager;

/**
 * Initializes domain services with infrastructure implementations.
 */
public class ServiceInitializer {

  private static boolean initialized = false;
  private static ShapeOps shapeOps;
  private static UseCaseFacade facade;
  private static AIAgentService aiAgentService;

  /**
   * Initialize all domain services with infrastructure implementations.
   */
  public static synchronized void initialize() {
    if (!initialized) {
      // Create infrastructure implementations
      StateManager stateManager = new StateManager();
      StateManagerAdapter stateManagerAdapter = new StateManagerAdapter(stateManager);
      HistoryServiceAdapter historyServiceAdapter = new HistoryServiceAdapter(stateManager);

      // Initialize domain service with adapters
      StateService.initialize(stateManagerAdapter, historyServiceAdapter);

      // Build ShapeOps + Facade (A3)
      shapeOps = new InMemoryShapeOps(StateService.getInstance());
      facade = new UseCaseFacade(StateService.getInstance(), shapeOps);

      initialized = true;
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

  public static ShapeOps getShapeOps() {
    return shapeOps;
  }

  public static UseCaseFacade getUseCaseFacade() {
    return facade;
  }
  
  public static AIAgentService getAIAgentService() {
    return aiAgentService;
  }
  
  public static void setAIAgentService(AIAgentService service) {
    aiAgentService = service;
  }
}
