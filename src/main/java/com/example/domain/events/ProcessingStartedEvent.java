package com.example.domain.events;

/**
 * Event fired when AI processing starts for a user prompt.
 */
public class ProcessingStartedEvent extends AIProcessingEvent {
    private final String userPrompt;
    
    public ProcessingStartedEvent(EventMeta meta, String userPrompt) {
        super(meta);
        this.userPrompt = userPrompt;
    }
    
    public String getUserPrompt() {
        return userPrompt;
    }
}