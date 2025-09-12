package com.example.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Metadata for all AI processing events.
 * Provides correlation IDs and timing information for tracking.
 */
public class EventMeta {
    private final String sessionId;
    private final String requestId;
    private final long sequence;
    private final Instant occurredAt;
    private final Map<String, String> metadata;
    
    public EventMeta(String sessionId, String requestId, long sequence) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.sequence = sequence;
        this.occurredAt = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    public EventMeta(String sessionId, String requestId, long sequence, Map<String, String> metadata) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.sequence = sequence;
        this.occurredAt = Instant.now();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public long getSequence() {
        return sequence;
    }
    
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }
}