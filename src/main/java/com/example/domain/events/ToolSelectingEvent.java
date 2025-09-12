package com.example.domain.events;

import java.util.Map;
import java.util.UUID;

/**
 * Event fired when AI selects a tool for execution.
 */
public class ToolSelectingEvent extends AIProcessingEvent {
    private final String toolName;
    private final String reason;
    private final Map<String, Object> parameters;
    private final UUID toolCallId;
    
    public ToolSelectingEvent(EventMeta meta, String toolName, String reason, Map<String, Object> parameters, UUID toolCallId) {
        super(meta);
        this.toolName = toolName;
        this.reason = reason;
        this.parameters = parameters;
        this.toolCallId = toolCallId;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public UUID getToolCallId() {
        return toolCallId;
    }
}