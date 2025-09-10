package com.example.domain.services;

import com.example.domain.models.DrawingState;

/**
 * Interface for managing drawing state history
 */
public interface IHistoryService {
    
    /**
     * Save the current state to history
     * @param state The state to save
     */
    void saveState(DrawingState state);
    
    /**
     * Add a state to history (used by adapters and session managers)
     * @param state The state to add
     */
    void addState(DrawingState state);
    
    /**
     * Check if undo is available
     * @return true if undo is available
     */
    boolean canUndo();
    
    /**
     * Check if redo is available
     * @return true if redo is available
     */
    boolean canRedo();
    
    /**
     * Undo to the previous state
     * @return true if undo was performed, false if not available
     */
    boolean undo();
    
    /**
     * Redo to the next state
     * @return true if redo was performed, false if not available
     */
    boolean redo();
    
    /**
     * Clear all history
     */
    void clear();
    
    /**
     * Clear all history (alias for clear, used by adapters)
     */
    void clearHistory();
    
    /**
     * Get the current history size
     * @return The number of states in history
     */
    int getHistorySize();
    
    /**
     * Get the current index in history
     * @return The current history index
     */
    int getCurrentIndex();
    
    /**
     * Get a copy of the history list
     * @return A copy of the history states
     */
    java.util.List<DrawingState> getHistoryCopy();
}
