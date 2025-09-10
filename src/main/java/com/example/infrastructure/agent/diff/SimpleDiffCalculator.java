package com.example.infrastructure.agent.diff;

import com.example.domain.models.DrawingState;
import com.example.infrastructure.services.DiffCalculator;
import com.example.domain.agent.DrawingStateDiff;

/**
 * Simple diff calculator for agents that returns a lightweight DrawingStateDiff
 * result. Useful for quick summaries in agent workflows.
 */
public class SimpleDiffCalculator implements DiffCalculator<DrawingStateDiff> {

    @Override
    public DrawingStateDiff calculateDifference(DrawingState oldState, DrawingState newState) {
        return new DrawingStateDiff(oldState, newState);
    }
}

