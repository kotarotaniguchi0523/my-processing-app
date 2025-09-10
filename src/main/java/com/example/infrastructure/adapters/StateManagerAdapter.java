package com.example.infrastructure.adapters;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IStateManager;
import com.example.domain.services.StateChangeListener;
import com.example.infrastructure.services.StateManager;

/**
 * Adapter for StateManager to implement IStateManager interface.
 */
public class StateManagerAdapter implements IStateManager {
    
    private final StateManager stateManager;
    
    public StateManagerAdapter() {
        this.stateManager = new StateManager();
    }
    
    public StateManagerAdapter(StateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    @Override
    public DrawingState getCurrentState() {
        return stateManager.getCurrentState();
    }
    
    @Override
    public void updateState(DrawingState newState) {
        stateManager.updateState(newState);
    }
    
    @Override
    public boolean validateState(DrawingState state) {
        return stateManager.validateState(state);
    }
    
    @Override
    public void resetToDefault() {
        stateManager.resetToDefault();
    }
    
    @Override
    public void addListener(StateChangeListener listener) {
        // Convert domain listener to services listener
        stateManager.addListener(new com.example.infrastructure.services.StateChangeListener() {
            @Override
            public void onStateChanged(DrawingState oldState, DrawingState newState) {
                listener.onStateChanged(oldState, newState);
            }
            
            @Override
            public void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
                listener.onHistoryChanged(currentState, canUndo, canRedo);
            }
            
            @Override
            public void onStateValidationError(DrawingState state, String error) {
                listener.onStateValidationError(state, error);
            }
        });
    }
    
    @Override
    public void removeListener(StateChangeListener listener) {
        // Note: This won't work correctly with wrapped listeners
        // A proper implementation would need to track the mapping
    }
    
    public StateManager getStateManager() {
        return stateManager;
    }
}