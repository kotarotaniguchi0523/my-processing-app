package com.example.domain.entities;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DrawingSession Entity
 * Represents a drawing session with identity and lifecycle.
 * This is an entity in DDD terms - it has identity and mutable state.
 */
public class DrawingSessionEntity {
    
    // Identity
    private final String id;
    private final LocalDateTime createdAt;
    
    // Mutable state
    private String name;
    private DrawingState currentState;
    private LocalDateTime lastModified;
    private boolean isDirty;
    
    /**
     * Create a new session with auto-generated ID
     */
    public DrawingSessionEntity(String name) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.name = name;
        this.currentState = DrawingState.empty();
        this.lastModified = LocalDateTime.now();
        this.isDirty = false;
    }
    
    /**
     * Reconstitute a session from persistence
     */
    public DrawingSessionEntity(String id, LocalDateTime createdAt, String name, 
                               DrawingState currentState, LocalDateTime lastModified) {
        this.id = id;
        this.createdAt = createdAt;
        this.name = name;
        this.currentState = currentState;
        this.lastModified = lastModified;
        this.isDirty = false;
    }
    
    // Business methods
    
    /**
     * Update the session state
     */
    public void updateState(DrawingState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.currentState = newState;
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }
    
    /**
     * Add a shape to the session
     */
    public void addShape(Shape shape) {
        if (shape == null) {
            throw new IllegalArgumentException("Shape cannot be null");
        }
        this.currentState = currentState.withAddedShape(shape);
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }
    
    /**
     * Remove a shape from the session
     */
    public void removeShape(String shapeId) {
        if (shapeId == null || shapeId.isBlank()) {
            throw new IllegalArgumentException("Shape ID cannot be null or empty");
        }
        this.currentState = currentState.withoutShape(shapeId);
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }
    
    /**
     * Clear all shapes from the session
     */
    public void clearAllShapes() {
        this.currentState = DrawingState.empty();
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }
    
    /**
     * Mark the session as saved
     */
    public void markAsSaved() {
        this.isDirty = false;
    }
    
    /**
     * Rename the session
     */
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = newName;
        this.lastModified = LocalDateTime.now();
        this.isDirty = true;
    }
    
    // Getters (no setters for encapsulation)
    
    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getName() { return name; }
    public DrawingState getCurrentState() { return currentState; }
    public LocalDateTime getLastModified() { return lastModified; }
    public boolean isDirty() { return isDirty; }
    
    /**
     * Check if two sessions have the same identity
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DrawingSessionEntity)) return false;
        DrawingSessionEntity other = (DrawingSessionEntity) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}