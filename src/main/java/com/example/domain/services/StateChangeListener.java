package com.example.domain.services;

import com.example.domain.models.DrawingState;

/**
 * Listener interface for state changes.
 */
public interface StateChangeListener {
    
    /**
     * Called when the drawing state changes.
     * @param oldState The previous state
     * @param newState The new state
     */
    void onStateChanged(DrawingState oldState, DrawingState newState);
    
    /**
     * Called when the history changes.
     * @param currentState The current state
     * @param canUndo Whether undo is available
     * @param canRedo Whether redo is available
     */
    void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo);
    
    /**
     * Called when state validation fails.
     * @param state The state that failed validation
     * @param error The validation error message
     */
    void onStateValidationError(DrawingState state, String error);
}