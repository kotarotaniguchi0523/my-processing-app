package com.example.domain.repositories;

import com.example.domain.entities.DrawingSessionEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DrawingSessionEntity
 * Defines the contract for persistence operations on drawing sessions
 */
public interface IDrawingSessionRepository {
    
    /**
     * Save a drawing session
     * @param session The session to save
     * @return The saved session
     */
    DrawingSessionEntity save(DrawingSessionEntity session);
    
    /**
     * Find a session by its ID
     * @param id The session ID
     * @return Optional containing the session if found, empty otherwise
     */
    Optional<DrawingSessionEntity> findById(String id);
    
    /**
     * Find all sessions
     * @return List of all sessions
     */
    List<DrawingSessionEntity> findAll();
    
    /**
     * Delete a session by its ID
     * @param id The session ID to delete
     * @return true if the session was deleted, false if it didn't exist
     */
    boolean deleteById(String id);
    
    /**
     * Delete a session entity
     * @param session The session to delete
     * @return true if the session was deleted, false if it didn't exist
     */
    boolean delete(DrawingSessionEntity session);
    
    /**
     * Check if a session exists by ID
     * @param id The session ID
     * @return true if the session exists, false otherwise
     */
    boolean existsById(String id);
    
    /**
     * Count the total number of sessions
     * @return The count of sessions
     */
    long count();
    
    /**
     * Find sessions by name (partial match)
     * @param name The name to search for
     * @return List of sessions matching the name
     */
    List<DrawingSessionEntity> findByNameContaining(String name);
}