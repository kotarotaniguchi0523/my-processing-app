package com.example.domain.services;

import com.example.domain.models.DrawingState;

/**
 * Interface for generating diffs between drawing states
 * This abstracts the diff generation from the application layer
 */
public interface IDiffService {
    
    /**
     * Generate a diff between two drawing states
     * @param oldState The original state
     * @param newState The new state
     * @return The diff result
     */
    DiffResult generateDiff(DrawingState oldState, DrawingState newState);
    
    /**
     * Result of a diff operation
     */
    class DiffResult {
        public final String summary;
        public final String unifiedDiff;
        public final int addedCount;
        public final int removedCount;
        public final int modifiedCount;
        
        public DiffResult(String summary, String unifiedDiff, int addedCount, int removedCount, int modifiedCount) {
            this.summary = summary;
            this.unifiedDiff = unifiedDiff;
            this.addedCount = addedCount;
            this.removedCount = removedCount;
            this.modifiedCount = modifiedCount;
        }
        
        public boolean hasChanges() {
            return addedCount > 0 || removedCount > 0 || modifiedCount > 0;
        }
        
        public String getChangeCountSummary() {
            return String.format("+%d -%d ~%d", addedCount, removedCount, modifiedCount);
        }
    }
}