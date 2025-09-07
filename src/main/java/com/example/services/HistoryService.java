package com.example.services;

import com.example.domain.models.DrawingState;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing drawing state history with undo/redo functionality. Maintains a history
 * stack with configurable size limits.
 */
public class HistoryService {

  public static final int DEFAULT_MAX_HISTORY_SIZE = 50;

  private final StateManager stateManager;
  private final List<DrawingState> history;
  private final int maxHistorySize;
  private int currentIndex;

  /**
   * Creates a new HistoryService with default maximum history size.
   *
   * @param stateManager the state manager to work with
   */
  public HistoryService(StateManager stateManager) {
    this(stateManager, DEFAULT_MAX_HISTORY_SIZE);
  }

  /**
   * Creates a new HistoryService with specified maximum history size.
   *
   * @param stateManager the state manager to work with
   * @param maxHistorySize the maximum number of states to keep in history
   */
  public HistoryService(StateManager stateManager, int maxHistorySize) {
    if (stateManager == null) {
      throw new IllegalArgumentException("StateManager cannot be null");
    }
    if (maxHistorySize <= 0) {
      throw new IllegalArgumentException("Max history size must be positive");
    }

    this.stateManager = stateManager;
    this.maxHistorySize = maxHistorySize;
    this.history = new ArrayList<>();
    this.currentIndex = -1;

    // Add the current state as the initial history entry
    addState(stateManager.getCurrentState());
  }

  /**
   * Adds a new state to the history. This will clear any redo history and add the new state.
   *
   * @param state the state to add
   */
  public void addState(DrawingState state) {
    if (state == null) {
      throw new IllegalArgumentException("State cannot be null");
    }

    // Remove any states after the current index (clear redo history)
    if (currentIndex >= 0 && currentIndex < history.size() - 1) {
      history.subList(currentIndex + 1, history.size()).clear();
    }

    // Add the new state
    history.add(state);
    currentIndex = history.size() - 1;

    // Maintain max history size by removing oldest entries
    while (history.size() > maxHistorySize) {
      history.remove(0);
      currentIndex--;
    }

    // Ensure currentIndex is valid
    if (currentIndex < 0) {
      currentIndex = 0;
    }

    // Notify history change
    notifyHistoryChanged();
  }

  /**
   * Performs an undo operation if possible.
   *
   * @return true if undo was performed, false if no undo available
   */
  public boolean undo() {
    if (!canUndo()) {
      return false;
    }

    currentIndex--;
    DrawingState newCurrentState = history.get(currentIndex);
    stateManager.updateState(newCurrentState);
    notifyHistoryChanged();
    return true;
  }

  /**
   * Performs a redo operation if possible.
   *
   * @return true if redo was performed, false if no redo available
   */
  public boolean redo() {
    if (!canRedo()) {
      return false;
    }

    currentIndex++;
    DrawingState newCurrentState = history.get(currentIndex);
    stateManager.updateState(newCurrentState);
    notifyHistoryChanged();
    return true;
  }

  /**
   * Checks if undo operation is available.
   *
   * @return true if undo is possible, false otherwise
   */
  public boolean canUndo() {
    return currentIndex > 0 && !history.isEmpty();
  }

  /**
   * Checks if redo operation is available.
   *
   * @return true if redo is possible, false otherwise
   */
  public boolean canRedo() {
    return currentIndex >= 0 && currentIndex < history.size() - 1;
  }

  /**
   * Gets the current state from history.
   *
   * @return the current state, or null if history is empty
   */
  public DrawingState getCurrentHistoryState() {
    if (history.isEmpty() || currentIndex < 0 || currentIndex >= history.size()) {
      return null;
    }
    return history.get(currentIndex);
  }

  /**
   * Gets the number of states in the history.
   *
   * @return the history size
   */
  public int getHistorySize() {
    return history.size();
  }

  /**
   * Gets the current history index.
   *
   * @return the current index in history
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * Gets the maximum history size.
   *
   * @return the maximum number of states that can be stored
   */
  public int getMaxHistorySize() {
    return maxHistorySize;
  }

  /** Clears the entire history and starts fresh with the current state. */
  public void clearHistory() {
    history.clear();
    currentIndex = -1;
    addState(stateManager.getCurrentState());
  }

  /**
   * Gets the number of undo steps available.
   *
   * @return the number of states that can be undone
   */
  public int getUndoStepsAvailable() {
    return Math.max(0, currentIndex);
  }

  /**
   * Gets the number of redo steps available.
   *
   * @return the number of states that can be redone
   */
  public int getRedoStepsAvailable() {
    return Math.max(0, history.size() - 1 - currentIndex);
  }

  /**
   * Creates a summary of the current history state.
   *
   * @return a string describing the history state
   */
  public String getHistorySummary() {
    return String.format(
        "History: %d/%d states, index: %d, undo: %d, redo: %d",
        history.size(),
        maxHistorySize,
        currentIndex,
        getUndoStepsAvailable(),
        getRedoStepsAvailable());
  }

  /**
   * Gets a copy of the entire history list.
   *
   * @return a copy of the history list
   */
  public List<DrawingState> getHistoryCopy() {
    return new ArrayList<>(history);
  }

  /**
   * Gets a state at a specific history index.
   *
   * @param index the history index
   * @return the state at the index, or null if index is invalid
   */
  public DrawingState getStateAt(int index) {
    if (index < 0 || index >= history.size()) {
      return null;
    }
    return history.get(index);
  }

  /**
   * Synchronizes the history with the current state manager state. This should be called if the
   * state manager's state was changed outside of the history service.
   */
  public void syncWithCurrentState() {
    DrawingState currentState = stateManager.getCurrentState();
    DrawingState historyState = getCurrentHistoryState();

    // If the current state is different from what we have in history, add it
    if (historyState == null || !historyState.equals(currentState)) {
      addState(currentState);
    }
  }

  /**
   * Optimizes the history by removing similar consecutive states. This helps reduce memory usage
   * for similar states.
   *
   * @return the number of states removed during optimization
   */
  public int optimizeHistory() {
    if (history.size() <= 1) {
      return 0;
    }

    int removedCount = 0;
    List<DrawingState> optimizedHistory = new ArrayList<>();
    DrawingState previousState = null;

    for (DrawingState state : history) {
      // Keep state if it's significantly different from the previous one
      if (previousState == null || !areStatesSimilar(previousState, state)) {
        optimizedHistory.add(state);
        previousState = state;
      } else {
        removedCount++;
      }
    }

    // Update history and current index
    history.clear();
    history.addAll(optimizedHistory);

    // Adjust current index
    currentIndex = Math.min(currentIndex, history.size() - 1);
    if (currentIndex < 0 && !history.isEmpty()) {
      currentIndex = 0;
    }

    notifyHistoryChanged();
    return removedCount;
  }

  /**
   * Checks if two states are similar enough to be considered redundant.
   *
   * @param state1 the first state
   * @param state2 the second state
   * @return true if the states are similar enough to be redundant
   */
  private boolean areStatesSimilar(DrawingState state1, DrawingState state2) {
    if (state1 == null || state2 == null) {
      return false;
    }

    // States are similar if they have the same number of shapes and settings
    return state1.getShapes().size() == state2.getShapes().size()
        && state1.getSettings().equals(state2.getSettings())
        && state1.getArrays().equals(state2.getArrays())
        && state1.getLists().equals(state2.getLists());
  }

  /** Notifies the state manager about history changes. */
  private void notifyHistoryChanged() {
    DrawingState currentState = getCurrentHistoryState();
    if (currentState != null) {
      stateManager.notifyHistoryChanged(currentState, canUndo(), canRedo());
    }
  }
}
