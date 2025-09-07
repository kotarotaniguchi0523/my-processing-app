package com.example.agent.service;

import com.example.domain.models.DrawingState;
import com.example.services.DiffCalculator;

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

