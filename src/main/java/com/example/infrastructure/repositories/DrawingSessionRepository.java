package com.example.infrastructure.repositories;

import com.example.domain.entities.DrawingSessionEntity;
import com.example.domain.repositories.IDrawingSessionRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of IDrawingSessionRepository
 * Uses a ConcurrentHashMap for thread-safe operations
 */
public class DrawingSessionRepository implements IDrawingSessionRepository {
    
    private final Map<String, DrawingSessionEntity> sessions;
    
    /**
     * Constructor
     */
    public DrawingSessionRepository() {
        this.sessions = new ConcurrentHashMap<>();
    }
    
    @Override
    public DrawingSessionEntity save(DrawingSessionEntity session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        
        // Clone the session to avoid external modification
        DrawingSessionEntity savedSession = new DrawingSessionEntity(
            session.getId(),
            session.getCreatedAt(),
            session.getName(),
            session.getCurrentState(),
            session.getLastModified()
        );
        
        // Mark as saved
        savedSession.markAsSaved();
        
        sessions.put(session.getId(), savedSession);
        return savedSession;
    }
    
    @Override
    public Optional<DrawingSessionEntity> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        
        DrawingSessionEntity session = sessions.get(id);
        if (session == null) {
            return Optional.empty();
        }
        
        // Return a copy to prevent external modification
        return Optional.of(new DrawingSessionEntity(
            session.getId(),
            session.getCreatedAt(),
            session.getName(),
            session.getCurrentState(),
            session.getLastModified()
        ));
    }
    
    @Override
    public List<DrawingSessionEntity> findAll() {
        return sessions.values().stream()
            .map(session -> new DrawingSessionEntity(
                session.getId(),
                session.getCreatedAt(),
                session.getName(),
                session.getCurrentState(),
                session.getLastModified()
            ))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        
        return sessions.remove(id) != null;
    }
    
    @Override
    public boolean delete(DrawingSessionEntity session) {
        if (session == null) {
            return false;
        }
        
        return deleteById(session.getId());
    }
    
    @Override
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        
        return sessions.containsKey(id);
    }
    
    @Override
    public long count() {
        return sessions.size();
    }
    
    @Override
    public List<DrawingSessionEntity> findByNameContaining(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        
        String searchName = name.toLowerCase();
        
        return sessions.values().stream()
            .filter(session -> session.getName().toLowerCase().contains(searchName))
            .map(session -> new DrawingSessionEntity(
                session.getId(),
                session.getCreatedAt(),
                session.getName(),
                session.getCurrentState(),
                session.getLastModified()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Clear all sessions (useful for testing)
     */
    public void clear() {
        sessions.clear();
    }
    
    /**
     * Get the raw session count (for testing purposes)
     */
    public int size() {
        return sessions.size();
    }
}