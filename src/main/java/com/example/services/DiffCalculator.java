package com.example.services;

import com.example.domain.models.DrawingState;

/**
 * Generic diff calculator abstraction. High-level code should depend on this
 * interface rather than concrete diff implementations.
 *
 * @param <R> result type of the diff
 */
public interface DiffCalculator<R> {

    /**
     * Calculate difference between two drawing states.
     *
     * @param oldState the previous state (nullable)
     * @param newState the next state (non-null)
     * @return diff result
     */
    R calculateDifference(com.example.domain.models.DrawingState oldState,
                          com.example.domain.models.DrawingState newState);
}

