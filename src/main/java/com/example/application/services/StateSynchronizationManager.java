package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.services.StateChangeListener;
import com.example.infrastructure.services.StateManager;
import com.example.infrastructure.services.HistoryService;
import com.example.domain.models.AgentHistory;

import java.util.logging.Logger;

/**
 * Manages state synchronization between AIAgentService components.
 * Extracted from AIAgentService to follow Single Responsibility Principle.
 * Handles:
 * - State change listening and notification
 * - Synchronization between StateManager and HistoryService
 * - Bidirectional state updates
 */
public class StateSynchronizationManager {

    private static final Logger LOGGER = Logger.getLogger(StateSynchronizationManager.class.getName());

    private final StateManager stateManager;
    private final HistoryService historyService;
    private final StateChangeNotifier stateChangeNotifier;
    private AgentHistory.Session currentSession;

    public StateSynchronizationManager(StateManager stateManager,
                                        HistoryService historyService,
                                        StateChangeNotifier stateChangeNotifier) {
        this.stateManager = stateManager;
        this.historyService = historyService;
        this.stateChangeNotifier = stateChangeNotifier;

        setupStateChangeListener();
        setupStateManagerListener();
    }

    /**
     * Set the current session for state updates
     */
    public void setCurrentSession(AgentHistory.Session session) {
        this.currentSession = session;
    }

    /**
     * Set up the state change listener to connect StateChangeNotifier with StateManager.
     */
    private void setupStateChangeListener() {
        stateChangeNotifier.addListener(new StateChangeNotifier.StateChangeListener() {
            @Override
            public void onStateChanged(DrawingState newState, String changeDescription) {
                LOGGER.info("State change processed: " + changeDescription);
            }
        });
    }

    /**
     * Set up the StateManager listener to sync state changes back to AIAgentService.
     * This ensures undo/redo operations update the AI agent's current state.
     */
    private void setupStateManagerListener() {
        stateManager.addListener(new StateChangeListener() {
            @Override
            public void onStateChanged(DrawingState oldState, DrawingState newState) {
                // Update session state if available
                if (currentSession != null) {
                    currentSession.setFinalState(newState);
                }

                LOGGER.info("State synchronized: " +
                    stateManager.calculateStateDifference(oldState, newState));
            }

            @Override
            public void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
                LOGGER.fine("History status: canUndo=" + canUndo + ", canRedo=" + canRedo);
            }

            @Override
            public void onStateValidationError(DrawingState state, String error) {
                LOGGER.warning("State validation error: " + error);
            }
        });
    }

    /**
     * Apply an approved state change with proper synchronization
     */
    public void applyStateChange(DrawingState previousState, DrawingState newState, String message) {
        if (!newState.equals(previousState)) {
            stateManager.updateState(newState);
            historyService.addState(newState);
            stateChangeNotifier.notifyStateChanged(newState, message);
        }
    }

    /**
     * Get the current state from StateManager
     */
    public DrawingState getCurrentState() {
        return stateManager.getCurrentState();
    }

    /**
     * Update the current state
     */
    public void updateState(DrawingState state) {
        stateManager.updateState(state);
    }
}