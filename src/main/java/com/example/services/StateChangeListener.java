package com.example.services;

import com.example.domain.models.DrawingState;

/**
 * Interface for objects that want to be notified when the drawing state changes. Implements the
 * Observer pattern for state management.
 */
public interface StateChangeListener {

  /**
   * Called when the drawing state changes.
   *
   * @param oldState the previous state (null if this is the first state)
   * @param newState the new current state
   */
  void onStateChanged(DrawingState oldState, DrawingState newState);

  /**
   * Called when the state change history is modified (undo/redo operation).
   *
   * @param currentState the current state after the history operation
   * @param canUndo whether undo operation is available
   * @param canRedo whether redo operation is available
   */
  void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo);

  /**
   * Called when a state validation error occurs.
   *
   * @param state the invalid state
   * @param error the validation error message
   */
  void onStateValidationError(DrawingState state, String error);
}
