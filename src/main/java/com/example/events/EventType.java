package com.example.events;

/** Enumeration of all event types in the system. Used for event categorization and routing. */
public enum EventType {
    // UI Events
    UI_MOUSE_PRESSED,
    UI_MOUSE_RELEASED,
    UI_MOUSE_DRAGGED,
    UI_KEY_PRESSED,
    UI_CHAT_INPUT_SUBMITTED,
    UI_CHAT_INPUT_FOCUSED,
    UI_CHAT_INPUT_UNFOCUSED,
    UI_SEND_BUTTON_CLICKED,
    UI_CANVAS_CLEARED,

    // Domain Events
    DOMAIN_SHAPE_ADDED,
    DOMAIN_SHAPE_REMOVED,
    DOMAIN_SHAPE_MODIFIED,
    DOMAIN_CANVAS_CLEARED,
    DOMAIN_STATE_CHANGED,
    DOMAIN_COMMAND_EXECUTED,
    DOMAIN_COMMAND_UNDONE,
    DOMAIN_COMMAND_REDONE,

    // System Events
    SYSTEM_SAVE_REQUESTED,
    SYSTEM_LOAD_REQUESTED,
    SYSTEM_EXPORT_REQUESTED,
    SYSTEM_IMPORT_REQUESTED,
    SYSTEM_SESSION_STARTED,
    SYSTEM_SESSION_ENDED,
    SYSTEM_ERROR_OCCURRED,

    // Agent Events
    AGENT_MESSAGE_RECEIVED,
    AGENT_COMMAND_PARSED,
    AGENT_TOOL_INVOKED,
    AGENT_RESPONSE_GENERATED,
    AGENT_STATUS_CHANGED,
    AGENT_ERROR_OCCURRED;

    /** Check if this event type is a UI event */
    public boolean isUIEvent() {
        return this.name().startsWith("UI_");
    }

    /** Check if this event type is a domain event */
    public boolean isDomainEvent() {
        return this.name().startsWith("DOMAIN_");
    }

    /** Check if this event type is a system event */
    public boolean isSystemEvent() {
        return this.name().startsWith("SYSTEM_");
    }

    /** Check if this event type is an agent event */
    public boolean isAgentEvent() {
        return this.name().startsWith("AGENT_");
    }
}
