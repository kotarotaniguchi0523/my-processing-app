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
        // StateManager already uses domain StateChangeListener interface
        stateManager.addListener(listener);
    }
    
    @Override
    public void removeListener(StateChangeListener listener) {
        // StateManager already uses domain StateChangeListener interface
        stateManager.removeListener(listener);
    }
    
    @Override
    public void notifyHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
        // Delegate to StateManager's notifyHistoryChanged method
        stateManager.notifyHistoryChanged(currentState, canUndo, canRedo);
    }
    
    public StateManager getStateManager() {
        return stateManager;
    }
}