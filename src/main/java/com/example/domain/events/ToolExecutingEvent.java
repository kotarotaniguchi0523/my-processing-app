package com.example.domain.events;

import java.util.UUID;

/**
 * Event fired when a tool is being executed.
 */
public class ToolExecutingEvent extends AIProcessingEvent {
    private final String toolName;
    private final ToolStatus status;
    private final UUID toolCallId;
    
    public ToolExecutingEvent(EventMeta meta, String toolName, ToolStatus status, UUID toolCallId) {
        super(meta);
        this.toolName = toolName;
        this.status = status;
        this.toolCallId = toolCallId;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public ToolStatus getStatus() {
        return status;
    }
    
    public UUID getToolCallId() {
        return toolCallId;
    }
}