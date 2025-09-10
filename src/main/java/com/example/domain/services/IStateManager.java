package com.example.domain.services;

import com.example.domain.models.DrawingState;

/**
 * Interface for state management.
 * Manages the current drawing state and provides validation.
 */
public interface IStateManager {
    
    /**
     * Get the current drawing state.
     * @return The current drawing state
     */
    DrawingState getCurrentState();
    
    /**
     * Update the current state.
     * @param newState The new drawing state
     */
    void updateState(DrawingState newState);
    
    /**
     * Validate a drawing state.
     * @param state The state to validate
     * @return true if valid, false otherwise
     */
    boolean validateState(DrawingState state);
    
    /**
     * Reset to default state.
     */
    void resetToDefault();
    
    /**
     * Add a state change listener.
     * @param listener The listener to add
     */
    void addListener(StateChangeListener listener);
    
    /**
     * Remove a state change listener.
     * @param listener The listener to remove
     */
    void removeListener(StateChangeListener listener);
}