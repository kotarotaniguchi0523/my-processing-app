package com.example.domain.services;

import com.example.domain.models.DrawingState;

/**
 * Interface for state management. Manages the current drawing state and provides validation.
 */
public interface IStateManager {

  /**
   * Get the current drawing state.
   *
   * @return The current drawing state
   */
  DrawingState getCurrentState();

  /**
   * Update the current state.
   *
   * @param newState The new drawing state
   */
  void updateState(DrawingState newState);


  /**
   * Add a state change listener.
   *
   * @param listener The listener to add
   */
  void addListener(StateChangeListener listener);

  /**
   * Notify listeners about history changes.
   *
   * @param currentState The current state
   * @param canUndo Whether undo is available
   * @param canRedo Whether redo is available
   */
  void notifyHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo);
}
