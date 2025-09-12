package com.example.application.repositories;

import com.example.domain.models.DrawingState;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing drawing state history following the Dependency Inversion Principle.
 * This interface defines the contract for persistence operations required by the application layer,
 * while the implementation details are handled by the infrastructure layer.
 * 
 * <p>Responsibilities:
 * - Define contracts for state persistence and retrieval
 * - Provide undo/redo capability abstractions
 * - Ensure clean separation between application logic and persistence details
 * 
 * <p>Design Principles:
 * - Dependency Inversion: Application layer defines interface, infrastructure implements
 * - Interface Segregation: Focused on history-specific operations only
 * - Single Responsibility: Solely responsible for history data access
 */
public interface HistoryRepository {
    
    /**
     * Saves a drawing state to the history repository.
     * 
     * @param state The drawing state to save. Must not be null.
     * @throws IllegalArgumentException if state is null
     * @return A unique identifier for the saved state
     */
    String saveState(DrawingState state);
    
    /**
     * Retrieves a specific drawing state by its identifier.
     * 
     * @param stateId The unique identifier of the state to retrieve
     * @return Optional containing the state if found, empty otherwise
     * @throws IllegalArgumentException if stateId is null or empty
     */
    Optional<DrawingState> getStateById(String stateId);
    
    /**
     * Retrieves all drawing states in chronological order.
     * 
     * @return List of all drawing states, empty list if no states exist
     */
    List<DrawingState> getAllStates();
    
    /**
     * Gets the previous state relative to the current position in history.
     * 
     * @return Optional containing the previous state if available, empty otherwise
     */
    Optional<DrawingState> getPreviousState();
    
    /**
     * Gets the next state relative to the current position in history.
     * 
     * @return Optional containing the next state if available, empty otherwise
     */
    Optional<DrawingState> getNextState();
    
    /**
     * Checks if there is a previous state available for undo operation.
     * 
     * @return true if undo is possible, false otherwise
     */
    boolean canUndo();
    
    /**
     * Checks if there is a next state available for redo operation.
     * 
     * @return true if redo is possible, false otherwise
     */
    boolean canRedo();
    
    /**
     * Removes all states from the history repository.
     * This operation is irreversible.
     */
    void clearHistory();
    
    /**
     * Gets the total number of states stored in the repository.
     * 
     * @return The count of stored states, 0 if empty
     */
    int getStateCount();
    
    /**
     * Gets the current position index in the history.
     * 
     * @return The current index position, -1 if no states exist
     */
    int getCurrentPosition();
}