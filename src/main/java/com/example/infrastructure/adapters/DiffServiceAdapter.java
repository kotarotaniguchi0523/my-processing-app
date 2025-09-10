package com.example.infrastructure.adapters;

import com.example.domain.agent.services.ProcessingCodeDiffService;
import com.example.domain.models.DrawingState;
import com.example.domain.services.IDiffService;

import java.util.logging.Logger;

/**
 * Adapter that implements the domain IDiffService interface using the agent layer
 * This provides dependency inversion for diff generation
 */
public class DiffServiceAdapter implements IDiffService {
    
    private static final Logger logger = Logger.getLogger(DiffServiceAdapter.class.getName());
    
    private final ProcessingCodeDiffService codeDiffService;
    
    /**
     * Create a diff service adapter
     */
    public DiffServiceAdapter() {
        this.codeDiffService = new ProcessingCodeDiffService();
    }
    
    @Override
    public DiffResult generateDiff(DrawingState oldState, DrawingState newState) {
        try {
            // Use the existing ProcessingCodeDiffService
            var diff = codeDiffService.generateCodeDiff(oldState, newState);
            
            // Count changes
            int addedCount = 0;
            int removedCount = 0;
            int modifiedCount = 0;
            
            // Parse the unified diff to count changes
            if (diff.unifiedDiff != null) {
                String[] lines = diff.unifiedDiff.split("\n");
                for (String line : lines) {
                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        addedCount++;
                    } else if (line.startsWith("-") && !line.startsWith("---")) {
                        removedCount++;
                    }
                }
            }
            
            // Check for modified shapes
            if (oldState != null && newState != null) {
                var oldShapes = oldState.getShapes();
                var newShapes = newState.getShapes();
                
                for (var oldShape : oldShapes) {
                    for (var newShape : newShapes) {
                        if (oldShape.getId().equals(newShape.getId()) && 
                            !oldShape.equals(newShape)) {
                            modifiedCount++;
                        }
                    }
                }
            }
            
            return new DiffResult(
                diff.summary,
                diff.unifiedDiff,
                addedCount,
                removedCount,
                modifiedCount
            );
            
        } catch (Exception e) {
            logger.warning("Failed to generate diff: " + e.getMessage());
            return new DiffResult(
                "Failed to generate diff",
                e.getMessage(),
                0, 0, 0
            );
        }
    }
}