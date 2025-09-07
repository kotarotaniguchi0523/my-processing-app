package com.example.domain.session;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Session represents a user's drawing session Contains session metadata, settings, and state
 * tracking
 */
public final class Session {
    private final String sessionId;
    private final String userId;
    private final String sessionName;
    private final LocalDateTime startTime;
    private final LocalDateTime lastActiveTime;
    private final SessionStatus status;
    private final Map<String, Object> metadata;
    private final SessionSettings settings;

    public Session(
            String sessionId,
            String userId,
            String sessionName,
            LocalDateTime startTime,
            LocalDateTime lastActiveTime,
            SessionStatus status,
            Map<String, Object> metadata,
            SessionSettings settings) {
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
        this.userId = userId != null ? userId : "anonymous";
        this.sessionName = sessionName != null ? sessionName : "Untitled Session";
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.lastActiveTime =
                Objects.requireNonNull(lastActiveTime, "Last active time cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.settings = Objects.requireNonNull(settings, "Settings cannot be null");
    }

    // Builder pattern for Session creation
    public static class Builder {
        private String sessionId = UUID.randomUUID().toString();
        private String userId = "anonymous";
        private String sessionName = "Untitled Session";
        private LocalDateTime startTime = LocalDateTime.now();
        private LocalDateTime lastActiveTime = LocalDateTime.now();
        private SessionStatus status = SessionStatus.ACTIVE;
        private Map<String, Object> metadata = new HashMap<>();
        private SessionSettings settings = SessionSettings.defaultSettings();

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder sessionName(String sessionName) {
            this.sessionName = sessionName;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder lastActiveTime(LocalDateTime lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
            return this;
        }

        public Builder status(SessionStatus status) {
            this.status = status;
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

        public Builder settings(SessionSettings settings) {
            this.settings = settings;
            return this;
        }

        public Session build() {
            return new Session(
                    sessionId,
                    userId,
                    sessionName,
                    startTime,
                    lastActiveTime,
                    status,
                    metadata,
                    settings);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Factory method for new session
    public static Session newSession(String userId, String sessionName) {
        return builder().userId(userId).sessionName(sessionName).build();
    }

    // Factory method for anonymous session
    public static Session newAnonymousSession() {
        return builder().build();
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public SessionSettings getSettings() {
        return settings;
    }

    // Helper methods for metadata
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public String getStringMetadata(String key, String defaultValue) {
        Object value = metadata.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public Integer getIntMetadata(String key, Integer defaultValue) {
        Object value = metadata.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    // Immutable update methods
    public Session withSessionName(String newSessionName) {
        return new Session(
                sessionId,
                userId,
                newSessionName,
                startTime,
                lastActiveTime,
                status,
                metadata,
                settings);
    }

    public Session withStatus(SessionStatus newStatus) {
        return new Session(
                sessionId,
                userId,
                sessionName,
                startTime,
                lastActiveTime,
                newStatus,
                metadata,
                settings);
    }

    public Session withLastActiveTime(LocalDateTime newLastActiveTime) {
        return new Session(
                sessionId,
                userId,
                sessionName,
                startTime,
                newLastActiveTime,
                status,
                metadata,
                settings);
    }

    public Session updateActivity() {
        return withLastActiveTime(LocalDateTime.now());
    }

    public Session withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new Session(
                sessionId,
                userId,
                sessionName,
                startTime,
                lastActiveTime,
                status,
                newMetadata,
                settings);
    }

    public Session withSettings(SessionSettings newSettings) {
        return new Session(
                sessionId,
                userId,
                sessionName,
                startTime,
                lastActiveTime,
                status,
                metadata,
                newSettings);
    }

    // Utility methods
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean isPaused() {
        return status == SessionStatus.PAUSED;
    }

    public boolean isEnded() {
        return status == SessionStatus.ENDED;
    }

    public boolean isAnonymous() {
        return "anonymous".equals(userId);
    }

    // Calculate session duration
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, lastActiveTime).toMinutes();
    }

    public long getDurationSeconds() {
        return java.time.Duration.between(startTime, lastActiveTime).getSeconds();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Session)) {
            return false;
        }
        Session session = (Session) obj;
        return Objects.equals(sessionId, session.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return String.format(
                "Session(id=%s, name='%s', user=%s, status=%s, duration=%dm)",
                sessionId, sessionName, userId, status, getDurationMinutes());
    }

    // Enum for session status
    public enum SessionStatus {
        ACTIVE("active"),
        PAUSED("paused"),
        ENDED("ended"),
        ARCHIVED("archived");

        private final String statusName;

        SessionStatus(String statusName) {
            this.statusName = statusName;
        }

        public String getStatusName() {
            return statusName;
        }

        @Override
        public String toString() {
            return statusName;
        }
    }
}
