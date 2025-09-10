package com.example.infrastructure.agent.core;

import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import com.example.domain.session.Session;
import com.example.domain.session.SessionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * AgentContext provides context information for tool execution Contains the current drawing state,
 * session information, and execution environment
 */
public final class AgentContext {

    private final DrawingState currentState;
    private final StateHistory stateHistory;
    private final Session session;
    private final SessionContext sessionContext;
    private final Map<String, Object> variables;
    private final Map<String, Object> metadata;
    private final long timestamp;

    private AgentContext(
            DrawingState currentState,
            StateHistory stateHistory,
            Session session,
            SessionContext sessionContext,
            Map<String, Object> variables,
            Map<String, Object> metadata) {
        this.currentState = currentState;
        this.stateHistory = stateHistory;
        this.session = session;
        this.sessionContext = sessionContext;
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public static class Builder {
        private DrawingState currentState;
        private StateHistory stateHistory;
        private Session session;
        private SessionContext sessionContext;
        private Map<String, Object> variables = new HashMap<>();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder currentState(DrawingState currentState) {
            this.currentState = currentState;
            return this;
        }

        public Builder stateHistory(StateHistory stateHistory) {
            this.stateHistory = stateHistory;
            return this;
        }

        public Builder session(Session session) {
            this.session = session;
            return this;
        }

        public Builder sessionContext(SessionContext sessionContext) {
            this.sessionContext = sessionContext;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = new HashMap<>(variables);
            return this;
        }

        public Builder addVariable(String key, Object value) {
            this.variables.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public AgentContext build() {
            return new AgentContext(
                    currentState, stateHistory, session, sessionContext, variables, metadata);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Static factory methods
    public static AgentContext withCurrentState(DrawingState currentState) {
        return builder().currentState(currentState).build();
    }

    public static AgentContext withStateAndHistory(
            DrawingState currentState, StateHistory stateHistory) {
        return builder().currentState(currentState).stateHistory(stateHistory).build();
    }

    public static AgentContext fromSession(Session session) {
        return builder().session(session).build();
    }

    // Getters
    public Optional<DrawingState> getCurrentState() {
        return Optional.ofNullable(currentState);
    }

    public Optional<StateHistory> getStateHistory() {
        return Optional.ofNullable(stateHistory);
    }

    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }

    public Optional<SessionContext> getSessionContext() {
        return Optional.ofNullable(sessionContext);
    }

    public Map<String, Object> getVariables() {
        return new HashMap<>(variables);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public String getStringVariable(String key, String defaultValue) {
        Object value = variables.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public int getIntVariable(String key, int defaultValue) {
        Object value = variables.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public boolean getBooleanVariable(String key, boolean defaultValue) {
        Object value = variables.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Utility methods for checking context availability
    public boolean hasCurrentState() {
        return currentState != null;
    }

    public boolean hasStateHistory() {
        return stateHistory != null;
    }

    public boolean hasSession() {
        return session != null;
    }

    public boolean hasSessionContext() {
        return sessionContext != null;
    }

    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    // Methods to create new context with modifications
    public AgentContext withNewCurrentState(DrawingState newCurrentState) {
        return new Builder()
                .currentState(newCurrentState)
                .stateHistory(stateHistory)
                .session(session)
                .sessionContext(sessionContext)
                .variables(variables)
                .metadata(metadata)
                .build();
    }

    public AgentContext withStateHistory(StateHistory newStateHistory) {
        return new Builder()
                .currentState(currentState)
                .stateHistory(newStateHistory)
                .session(session)
                .sessionContext(sessionContext)
                .variables(variables)
                .metadata(metadata)
                .build();
    }

    public AgentContext withVariable(String key, Object value) {
        Map<String, Object> newVariables = new HashMap<>(variables);
        newVariables.put(key, value);
        return new Builder()
                .currentState(currentState)
                .stateHistory(stateHistory)
                .session(session)
                .sessionContext(sessionContext)
                .variables(newVariables)
                .metadata(metadata)
                .build();
    }

    public AgentContext withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new Builder()
                .currentState(currentState)
                .stateHistory(stateHistory)
                .session(session)
                .sessionContext(sessionContext)
                .variables(variables)
                .metadata(newMetadata)
                .build();
    }

    // Helper methods for common operations
    public boolean canUndo() {
        return hasStateHistory() && stateHistory.canUndo();
    }

    public boolean canRedo() {
        return hasStateHistory() && stateHistory.canRedo();
    }

    public int getShapeCount() {
        return hasCurrentState() ? currentState.getShapes().size() : 0;
    }

    public boolean isEmpty() {
        return !hasCurrentState() || currentState.getShapes().isEmpty();
    }

    // Get drawing dimensions from current state or defaults
    public int getCanvasWidth() {
        if (hasCurrentState()) {
            return currentState.getIntSetting("width", 800);
        }
        return 800;
    }

    public int getCanvasHeight() {
        if (hasCurrentState()) {
            return currentState.getIntSetting("height", 600);
        }
        return 600;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AgentContext)) {
            return false;
        }
        AgentContext context = (AgentContext) obj;
        return Objects.equals(currentState, context.currentState)
                && Objects.equals(stateHistory, context.stateHistory)
                && Objects.equals(session, context.session)
                && Objects.equals(sessionContext, context.sessionContext)
                && Objects.equals(variables, context.variables)
                && Objects.equals(metadata, context.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                currentState, stateHistory, session, sessionContext, variables, metadata);
    }

    @Override
    public String toString() {
        return String.format(
                "AgentContext(hasState=%s, hasHistory=%s, hasSession=%s, vars=%d, meta=%d)",
                hasCurrentState(),
                hasStateHistory(),
                hasSession(),
                variables.size(),
                metadata.size());
    }
}
