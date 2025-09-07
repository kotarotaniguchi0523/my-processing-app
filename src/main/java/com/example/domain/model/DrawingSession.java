package com.example.domain.model;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a drawing session with a collection of shapes and session metadata. This is the main
 * aggregate root for managing drawing state.
 */
public class DrawingSession {
    private final String sessionId;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private DrawingState currentState;
    private String sessionName;
    private boolean isDirty;

    public DrawingSession() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.currentState = DrawingState.defaultState();
        this.sessionName = "Untitled Session";
        this.isDirty = false;
    }

    public DrawingSession(String sessionName) {
        this();
        this.sessionName = sessionName;
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public DrawingState getCurrentState() {
        return currentState;
    }

    public String getSessionName() {
        return sessionName;
    }

    public boolean isDirty() {
        return isDirty;
    }

    // Setters
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
        markModified();
    }

    // State management
    public void updateState(DrawingState newState) {
        this.currentState = newState;
        markModified();
    }

    public List<Shape> getAllShapes() {
        return currentState.getShapes();
    }

    public void addShape(Shape shape) {
        currentState = currentState.withShape(shape);
        markModified();
    }

    public void removeShape(String shapeId) {
        currentState = currentState.withoutShape(shapeId);
        markModified();
    }

    public void clearAllShapes() {
        currentState = DrawingState.defaultState();
        markModified();
    }

    public Shape findShapeById(String shapeId) {
        return currentState.getShapes().stream()
                .filter(shape -> shape.getId().equals(shapeId))
                .findFirst()
                .orElse(null);
    }

    private void markModified() {
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }

    public void markSaved() {
        this.isDirty = false;
    }

    @Override
    public String toString() {
        return String.format(
                "DrawingSession(id=%s, name=%s, shapes=%d, dirty=%s)",
                sessionId, sessionName, getAllShapes().size(), isDirty);
    }
}
