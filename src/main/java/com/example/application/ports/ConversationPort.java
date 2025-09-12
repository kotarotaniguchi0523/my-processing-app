package com.example.application.ports;

import com.example.domain.models.DrawingState;
import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Port interface for managing conversations and chat interactions following the Hexagonal Architecture pattern.
 * This interface defines the contract for conversation management required by the application layer,
 * allowing different implementations (UI-based, CLI-based, or test implementations).
 * 
 * <p>Responsibilities:
 * - Define contracts for conversation flow management
 * - Provide abstractions for user input/output handling
 * - Support both synchronous and asynchronous conversation patterns
 * 
 * <p>Design Principles:
 * - Dependency Inversion: Application layer defines interface, adapters implement
 * - Interface Segregation: Focused on conversation-specific operations only
 * - Single Responsibility: Solely responsible for conversation management
 */
public interface ConversationPort {
    
    /**
     * Initiates a new conversation session.
     * 
     * @param sessionId Unique identifier for the conversation session
     * @param initialContext Optional initial context or drawing state
     * @return CompletableFuture that completes when session is ready
     * @throws IllegalArgumentException if sessionId is null or empty
     */
    CompletableFuture<Void> startConversation(String sessionId, DrawingState initialContext);
    
    /**
     * Processes user input and returns the system response.
     * 
     * @param sessionId The conversation session identifier
     * @param userInput The user's input message or command
     * @return CompletableFuture containing the system response
     * @throws IllegalArgumentException if sessionId or userInput is null/empty
     */
    CompletableFuture<ConversationResponse> processUserInput(String sessionId, String userInput);
    
    /**
     * Requests user approval for a proposed action or change.
     * 
     * @param sessionId The conversation session identifier
     * @param proposal The proposal requiring user approval
     * @return CompletableFuture containing the user's decision
     * @throws IllegalArgumentException if sessionId or proposal is null
     */
    CompletableFuture<Boolean> requestApproval(String sessionId, ApprovalRequest proposal);
    
    /**
     * Sends a system message or notification to the user.
     * 
     * @param sessionId The conversation session identifier
     * @param message The message to send to the user
     * @return CompletableFuture that completes when message is delivered
     * @throws IllegalArgumentException if sessionId or message is null/empty
     */
    CompletableFuture<Void> sendSystemMessage(String sessionId, String message);
    
    /**
     * Gets the conversation history for a session.
     * 
     * @param sessionId The conversation session identifier
     * @return List of conversation entries in chronological order
     * @throws IllegalArgumentException if sessionId is null or empty
     */
    List<ConversationEntry> getConversationHistory(String sessionId);
    
    /**
     * Ends the conversation session and performs cleanup.
     * 
     * @param sessionId The conversation session identifier
     * @return CompletableFuture that completes when session is closed
     */
    CompletableFuture<Void> endConversation(String sessionId);
    
    /**
     * Checks if a conversation session is currently active.
     * 
     * @param sessionId The conversation session identifier
     * @return true if session is active, false otherwise
     */
    boolean isConversationActive(String sessionId);
    
    /**
     * Represents a response from the conversation system.
     */
    class ConversationResponse {
        private final String message;
        private final DrawingState updatedState;
        private final boolean requiresApproval;
        private final String error;
        
        public ConversationResponse(String message, DrawingState updatedState, boolean requiresApproval) {
            this.message = message;
            this.updatedState = updatedState;
            this.requiresApproval = requiresApproval;
            this.error = null;
        }
        
        public ConversationResponse(String error) {
            this.message = null;
            this.updatedState = null;
            this.requiresApproval = false;
            this.error = error;
        }
        
        public String getMessage() { return message; }
        public DrawingState getUpdatedState() { return updatedState; }
        public boolean requiresApproval() { return requiresApproval; }
        public boolean hasError() { return error != null; }
        public String getError() { return error; }
    }
    
    /**
     * Represents a request for user approval.
     */
    class ApprovalRequest {
        private final String description;
        private final DrawingState proposedState;
        private final String actionType;
        
        public ApprovalRequest(String description, DrawingState proposedState, String actionType) {
            this.description = description;
            this.proposedState = proposedState;
            this.actionType = actionType;
        }
        
        public String getDescription() { return description; }
        public DrawingState getProposedState() { return proposedState; }
        public String getActionType() { return actionType; }
    }
    
    /**
     * Represents an entry in the conversation history.
     */
    class ConversationEntry {
        private final String speaker;
        private final String message;
        private final long timestamp;
        private final DrawingState associatedState;
        
        public ConversationEntry(String speaker, String message, long timestamp, DrawingState associatedState) {
            this.speaker = speaker;
            this.message = message;
            this.timestamp = timestamp;
            this.associatedState = associatedState;
        }
        
        public String getSpeaker() { return speaker; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public DrawingState getAssociatedState() { return associatedState; }
    }
}