package com.example.domain.services;

import com.example.domain.models.DrawingState;
import java.util.List;

/**
 * Interface for history management service.
 * Provides undo/redo functionality for drawing states.
 */
public interface IHistoryService {
    
    /**
     * Add a new state to the history.
     * @param state The drawing state to add
     */
    void addState(DrawingState state);
    
    /**
     * Undo to the previous state.
     * @return true if undo was successful, false otherwise
     */
    boolean undo();
    
    /**
     * Redo to the next state.
     * @return true if redo was successful, false otherwise
     */
    boolean redo();
    
    /**
     * Check if undo is possible.
     * @return true if can undo, false otherwise
     */
    boolean canUndo();
    
    /**
     * Check if redo is possible.
     * @return true if can redo, false otherwise
     */
    boolean canRedo();
    
    /**
     * Clear all history.
     */
    void clearHistory();
    
    /**
     * Get the current index in history.
     * @return The current index
     */
    int getCurrentIndex();
    
    /**
     * Get a copy of the history.
     * @return List of drawing states in history
     */
    List<DrawingState> getHistoryCopy();
}