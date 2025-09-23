package com.example.infrastructure.services;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IStateManager;
import com.example.domain.services.StateChangeListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central state management service for the drawing application. Provides thread-safe state
 * management with listener notifications.
 */
public class StateManager implements IStateManager {
  private static final Logger LOGGER = Logger.getLogger(StateManager.class.getName());

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();
  private DrawingState currentState;

  /** Creates a new StateManager with a default initial state. */
  public StateManager() {
    this.currentState = DrawingState.defaultState();
  }


  /**
   * Gets the current drawing state.
   *
   * @return the current state (thread-safe copy)
   */
  public DrawingState getCurrentState() {
    lock.readLock().lock();
    try {
      return currentState;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Updates the current state and notifies all listeners.
   *
   * @param newState the new state
   * @throws IllegalArgumentException if the new state is null
   */
  public void updateState(DrawingState newState) {
    if (newState == null) {
      throw new IllegalArgumentException("New state cannot be null");
    }

    DrawingState oldState;

    lock.writeLock().lock();
    try {
      oldState = this.currentState;
      this.currentState = newState;
    } finally {
      lock.writeLock().unlock();
    }

    // Notify listeners outside of the lock to prevent deadlock
    notifyStateChanged(oldState, newState);
  }



  /**
   * Adds a state change listener.
   *
   * @param listener the listener to add
   */
  public void addListener(StateChangeListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Removes a state change listener.
   *
   * @param listener the listener to remove
   */
  public void removeListener(StateChangeListener listener) {
    listeners.remove(listener);
  }

  /** Resets the state to the default state. */
  public void resetToDefault() {
    updateState(DrawingState.defaultState());
  }

  /**
   * Creates a state difference description between two states.
   *
   * @param oldState the old state
   * @param newState the new state
   * @return a description of the differences
   */
  public String calculateStateDifference(DrawingState oldState, DrawingState newState) {
    if (oldState == null && newState == null) {
      return "No changes";
    }

    if (oldState == null) {
      return "Initial state created with " + newState.getShapes().size() + " shapes";
    }

    if (newState == null) {
      return "State cleared";
    }

    StringBuilder diff = new StringBuilder();

    // Compare shapes
    int oldShapeCount = oldState.getShapes().size();
    int newShapeCount = newState.getShapes().size();

    if (oldShapeCount != newShapeCount) {
      diff.append("Shapes: ").append(oldShapeCount).append(" → ").append(newShapeCount);
    }

    // Compare settings
    if (!oldState.getSettings().equals(newState.getSettings())) {
      if (diff.length() > 0) {
        diff.append(", ");
      }
      diff.append("Settings modified");
    }

    // Compare arrays
    if (!oldState.getArrays().equals(newState.getArrays())) {
      if (diff.length() > 0) {
        diff.append(", ");
      }
      diff.append("Arrays modified");
    }

    // Compare lists
    if (!oldState.getLists().equals(newState.getLists())) {
      if (diff.length() > 0) {
        diff.append(", ");
      }
      diff.append("Lists modified");
    }

    return diff.length() > 0 ? diff.toString() : "No significant changes";
  }

  /**
   * Notifies all listeners of a state change.
   *
   * @param oldState the old state
   * @param newState the new state
   */
  private void notifyStateChanged(DrawingState oldState, DrawingState newState) {
    for (StateChangeListener listener : listeners) {
      try {
        listener.onStateChanged(oldState, newState);
      } catch (Exception e) {
        // Log error but continue notifying other listeners
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.log(Level.WARNING, "Error notifying state change listener: " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Notifies all listeners of a history change.
   *
   * @param currentState the current state
   * @param canUndo whether undo is available
   * @param canRedo whether redo is available
   */
  public void notifyHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
    for (StateChangeListener listener : listeners) {
      try {
        listener.onHistoryChanged(currentState, canUndo, canRedo);
      } catch (Exception e) {
        // Log error but continue notifying other listeners
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.log(Level.WARNING, "Error notifying history change listener: " + e.getMessage(),
              e);
        }
      }
    }
  }

}
