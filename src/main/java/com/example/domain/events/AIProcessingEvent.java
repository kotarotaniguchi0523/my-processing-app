package com.example.domain.events;

/**
 * Base class for all AI processing events.
 * Provides common metadata for tracking and correlation.
 */
public abstract class AIProcessingEvent {
    private final EventMeta meta;
    
    protected AIProcessingEvent(EventMeta meta) {
        this.meta = meta;
    }
    
    public EventMeta getMeta() {
        return meta;
    }
}