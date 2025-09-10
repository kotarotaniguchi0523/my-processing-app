package com.example.infrastructure;

import com.example.application.services.StateService;
import com.example.infrastructure.adapters.StateManagerAdapter;
import com.example.infrastructure.adapters.HistoryServiceAdapter;
import com.example.infrastructure.services.StateManager;

/**
 * Initializes domain services with infrastructure implementations.
 */
public class ServiceInitializer {
    
    private static boolean initialized = false;
    
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
            
            initialized = true;
        }
    }
    
    /**
     * Check if services are initialized.
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}