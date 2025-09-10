package com.example.domain.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents domain-related events such as drawing changes, state modifications, and command
 * executions.
 */
public class DomainEvent extends Event {
    private final String entityId;
    private final String commandType;
    private final Map<String, Object> data;
    private final Object previousState;
    private final Object newState;

    // Constructor for command-related events
    public DomainEvent(EventType type, String commandType, String entityId) {
        super(type);
        this.entityId = entityId;
        this.commandType = commandType;
        this.data = new HashMap<>();
        this.previousState = null;
        this.newState = null;
    }

    // Constructor for state change events
    public DomainEvent(EventType type, String entityId, Object previousState, Object newState) {
        super(type);
        this.entityId = entityId;
        this.commandType = null;
        this.data = new HashMap<>();
        this.previousState = previousState;
        this.newState = newState;
    }

    // Constructor for events with custom data
    public DomainEvent(EventType type, String entityId, Map<String, Object> data) {
        super(type);
        this.entityId = entityId;
        this.commandType = null;
        this.data = new HashMap<>(data);
        this.previousState = null;
        this.newState = null;
    }

    // Constructor for full control
    public DomainEvent(
            EventType type,
            String entityId,
            String commandType,
            Map<String, Object> data,
            Object previousState,
            Object newState) {
        super(type);
        this.entityId = entityId;
        this.commandType = commandType;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.previousState = previousState;
        this.newState = newState;
    }

    // Getters
    public String getEntityId() {
        return entityId;
    }

    public String getCommandType() {
        return commandType;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public Object getPreviousState() {
        return previousState;
    }

    public Object getNewState() {
        return newState;
    }

    // Data manipulation methods
    public void addData(String key, Object value) {
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    // Helper methods
    public boolean isStateChangeEvent() {
        return previousState != null || newState != null;
    }

    public boolean isCommandEvent() {
        return commandType != null && !commandType.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("DomainEvent[type=%s, entityId=%s", getType(), entityId));

        if (commandType != null) {
            sb.append(String.format(", command=%s", commandType));
        }

        if (!data.isEmpty()) {
            sb.append(String.format(", data=%s", data));
        }

        if (isStateChangeEvent()) {
            sb.append(", stateChange=true");
        }

        sb.append("]");
        return sb.toString();
    }
}
