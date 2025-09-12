package com.example.application.usecase.dto;

import com.example.domain.models.DrawingState;

/**
 * Unified result wrapper for all use cases.
 * success: operation success flag
 * newState: non-null immutable DrawingState after the operation (on failure = previous state)
 * payload: domain specific small payload (e.g. shapeId, counts, etc.)
 * message: human/debug message
 */
public record UseCaseResult<T>(boolean success, DrawingState newState, T payload, String message) {
    public static <T> UseCaseResult<T> ok(DrawingState state, T payload, String message) {
        return new UseCaseResult<>(true, state, payload, message);
    }
    public static <T> UseCaseResult<T> fail(DrawingState state, String message) {
        return new UseCaseResult<>(false, state, null, message);
    }
}
