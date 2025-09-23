package com.example.infrastructure.services;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing drawing state history with undo/redo functionality. Maintains a history
 * stack with configurable size limits.
 */
public class HistoryService {

    public static final int DEFAULT_MAX_HISTORY_SIZE = 50;

    private final IStateManager stateManager;
    private final List<DrawingState> history;
    private final int maxHistorySize;
    private int currentIndex;

    /**
     * Creates a new HistoryService with default maximum history size.
     *
     * @param stateManager the state manager to work with
     */
    public HistoryService(IStateManager stateManager) {
        this(stateManager, DEFAULT_MAX_HISTORY_SIZE);
    }

    /**
     * Creates a new HistoryService with specified maximum history size.
     *
     * @param stateManager the state manager to work with
     * @param maxHistorySize the maximum number of states to keep in history
     */
    public HistoryService(IStateManager stateManager, int maxHistorySize) {
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
    private DrawingState getCurrentHistoryState() {
        if (history.isEmpty() || currentIndex < 0 || currentIndex >= history.size()) {
            return null;
        }
        return history.get(currentIndex);
    }

    /** Clears the entire history and starts fresh with the current state. */
    public void clearHistory() {
        history.clear();
        currentIndex = -1;
        addState(stateManager.getCurrentState());
    }


    /** Notifies the state manager about history changes. */
    private void notifyHistoryChanged() {
        DrawingState currentState = getCurrentHistoryState();
        if (currentState != null) {
            stateManager.notifyHistoryChanged(currentState, canUndo(), canRedo());
        }
    }

    public void saveState(DrawingState state) {
        addState(state);
    }

    public void clear() {
        clearHistory();
    }
}
