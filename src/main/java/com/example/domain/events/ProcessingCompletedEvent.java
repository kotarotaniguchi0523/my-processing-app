package com.example.domain.events;

import java.util.UUID;

/**
 * Event fired when AI processing completes successfully.
 */
public class ProcessingCompletedEvent extends AIProcessingEvent {
    private final String result;
    private final String stateVersion;  // Instead of full DrawingState
    private final UUID toolCallId;  // Can be null for non-tool responses
    
    public ProcessingCompletedEvent(EventMeta meta, String result, String stateVersion, UUID toolCallId) {
        super(meta);
        this.result = result;
        this.stateVersion = stateVersion;
        this.toolCallId = toolCallId;
    }
    
    public String getResult() {
        return result;
    }
    
    public String getStateVersion() {
        return stateVersion;
    }
    
    public UUID getToolCallId() {
        return toolCallId;
    }
}