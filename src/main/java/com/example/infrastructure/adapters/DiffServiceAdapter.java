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
            // Use the existing ProcessingCodeDiffService which already provides complete DiffResult
            return codeDiffService.generateDiff(oldState, newState);
            
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