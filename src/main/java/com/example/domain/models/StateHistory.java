package com.example.domain.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * StateHistory manages the history of drawing state changes Provides undo/redo functionality and
 * state navigation
 */
public final class StateHistory {
    private final List<StateSnapshot> history;
    private final int currentIndex;
    private final int maxHistorySize;

    public StateHistory(List<StateSnapshot> history, int currentIndex, int maxHistorySize) {
        this.history = new ArrayList<>(Objects.requireNonNull(history, "History cannot be null"));
        this.currentIndex = Math.max(-1, Math.min(currentIndex, history.size() - 1));
        this.maxHistorySize = Math.max(1, maxHistorySize);
    }

    public StateHistory(int maxHistorySize) {
        this(new ArrayList<>(), -1, maxHistorySize);
    }

    public StateHistory() {
        this(100); // Default max size
    }

    // Add a new snapshot to the history
    public StateHistory addSnapshot(StateSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");

        List<StateSnapshot> newHistory = new ArrayList<>();

        // If we're not at the end of history, truncate everything after current position
        if (currentIndex >= 0) {
            newHistory.addAll(history.subList(0, currentIndex + 1));
        }

        // Add the new snapshot
        newHistory.add(snapshot);

        // Trim history if it exceeds max size
        while (newHistory.size() > maxHistorySize) {
            newHistory.remove(0);
        }

        return new StateHistory(newHistory, newHistory.size() - 1, maxHistorySize);
    }

    // Navigate to previous state (undo)
    public StateHistory undo() {
        if (!canUndo()) {
            return this;
        }
        return new StateHistory(history, currentIndex - 1, maxHistorySize);
    }

    // Navigate to next state (redo)
    public StateHistory redo() {
        if (!canRedo()) {
            return this;
        }
        return new StateHistory(history, currentIndex + 1, maxHistorySize);
    }

    // Navigate to a specific snapshot by index
    public StateHistory goTo(int index) {
        if (index < 0 || index >= history.size()) {
            return this;
        }
        return new StateHistory(history, index, maxHistorySize);
    }

    // Navigate to a specific snapshot by ID
    public StateHistory goTo(String snapshotId) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getId().equals(snapshotId)) {
                return new StateHistory(history, i, maxHistorySize);
            }
        }
        return this; // ID not found, return unchanged
    }

    // Check if undo is possible
    public boolean canUndo() {
        return currentIndex > 0;
    }

    // Check if redo is possible
    public boolean canRedo() {
        return currentIndex >= 0 && currentIndex < history.size() - 1;
    }

    // Get current snapshot
    public StateSnapshot getCurrentSnapshot() {
        if (currentIndex >= 0 && currentIndex < history.size()) {
            return history.get(currentIndex);
        }
        return null;
    }

    // Get current state
    public DrawingState getCurrentState() {
        StateSnapshot current = getCurrentSnapshot();
        return current != null ? current.getState() : null;
    }

    // Get all snapshots in history order
    public List<StateSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    // Get snapshots after current position (for redo preview)
    public List<StateSnapshot> getFutureSnapshots() {
        if (currentIndex >= 0 && currentIndex < history.size() - 1) {
            return history.subList(currentIndex + 1, history.size());
        }
        return new ArrayList<>();
    }

    // Get snapshots before current position (for undo preview)
    public List<StateSnapshot> getPastSnapshots() {
        if (currentIndex > 0) {
            return history.subList(0, currentIndex);
        }
        return new ArrayList<>();
    }

    // Get snapshot at specific index
    public StateSnapshot getSnapshot(int index) {
        if (index >= 0 && index < history.size()) {
            return history.get(index);
        }
        return null;
    }

    // Find snapshots by operation type
    public List<StateSnapshot> findByOperation(String operation) {
        return history.stream()
                .filter(snapshot -> Objects.equals(snapshot.getOperation(), operation))
                .collect(Collectors.toList());
    }

    // Find snapshots within time range
    public List<StateSnapshot> findByTimeRange(long startTime, long endTime) {
        return history.stream()
                .filter(
                        snapshot ->
                                snapshot.getTimestamp() >= startTime
                                        && snapshot.getTimestamp() <= endTime)
                .collect(Collectors.toList());
    }

    // Clear all history
    public StateHistory clear() {
        return new StateHistory(maxHistorySize);
    }

    // Trim history to specified size
    public StateHistory trimTo(int newMaxSize) {
        if (newMaxSize <= 0) {
            return clear();
        }

        List<StateSnapshot> newHistory = new ArrayList<>(history);
        while (newHistory.size() > newMaxSize) {
            newHistory.remove(0);
        }

        int newCurrentIndex = currentIndex;
        if (newCurrentIndex >= newHistory.size()) {
            newCurrentIndex = newHistory.size() - 1;
        }

        return new StateHistory(newHistory, newCurrentIndex, newMaxSize);
    }

    // Get statistics about the history
    public HistoryStats getStats() {
        return new HistoryStats(
                history.size(),
                currentIndex + 1,
                maxHistorySize,
                canUndo() ? currentIndex : 0,
                canRedo() ? history.size() - currentIndex - 1 : 0);
    }

    public int size() {
        return history.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public boolean isEmpty() {
        return history.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StateHistory)) {
            return false;
        }
        StateHistory that = (StateHistory) obj;
        return currentIndex == that.currentIndex
                && maxHistorySize == that.maxHistorySize
                && Objects.equals(history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(history, currentIndex, maxHistorySize);
    }

    @Override
    public String toString() {
        return String.format(
                "StateHistory(size=%d, current=%d, max=%d)",
                history.size(), currentIndex + 1, maxHistorySize);
    }

    // Inner class for history statistics
    public static class HistoryStats {
        private final int totalSnapshots;
        private final int currentPosition;
        private final int maxCapacity;
        private final int undoSteps;
        private final int redoSteps;

        public HistoryStats(
                int totalSnapshots,
                int currentPosition,
                int maxCapacity,
                int undoSteps,
                int redoSteps) {
            this.totalSnapshots = totalSnapshots;
            this.currentPosition = currentPosition;
            this.maxCapacity = maxCapacity;
            this.undoSteps = undoSteps;
            this.redoSteps = redoSteps;
        }

        public int getTotalSnapshots() {
            return totalSnapshots;
        }

        public int getCurrentPosition() {
            return currentPosition;
        }

        public int getMaxCapacity() {
            return maxCapacity;
        }

        public int getUndoSteps() {
            return undoSteps;
        }

        public int getRedoSteps() {
            return redoSteps;
        }

        @Override
        public String toString() {
            return String.format(
                    "HistoryStats(total=%d, position=%d/%d, undo=%d, redo=%d)",
                    totalSnapshots, currentPosition, maxCapacity, undoSteps, redoSteps);
        }
    }
}
