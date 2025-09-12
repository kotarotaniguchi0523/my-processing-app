package com.example.domain.events;

import java.util.UUID;

/**
 * Event fired when AI processing fails.
 */
public class ProcessingFailedEvent extends AIProcessingEvent {
    private final String errorMessage;
    private final FailurePoint failurePoint;
    private final String errorCode;
    private final UUID toolCallId;  // Can be null for non-tool failures
    
    public ProcessingFailedEvent(EventMeta meta, String errorMessage, FailurePoint failurePoint, String errorCode, UUID toolCallId) {
        super(meta);
        this.errorMessage = errorMessage;
        this.failurePoint = failurePoint;
        this.errorCode = errorCode;
        this.toolCallId = toolCallId;
    }
    
    // Simplified constructor for common use
    public ProcessingFailedEvent(EventMeta meta, String errorMessage, FailurePoint failurePoint) {
        this(meta, errorMessage, failurePoint, "GENERAL_ERROR", null);
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public FailurePoint getFailurePoint() {
        return failurePoint;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public UUID getToolCallId() {
        return toolCallId;
    }
}