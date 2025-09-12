package com.example.domain.events;

/**
 * Event fired when AI is analyzing the user prompt.
 */
public class PromptAnalyzingEvent extends AIProcessingEvent {
    private final String analysis;
    
    public PromptAnalyzingEvent(EventMeta meta, String analysis) {
        super(meta);
        this.analysis = analysis;
    }
    
    public String getAnalysis() {
        return analysis;
    }
}