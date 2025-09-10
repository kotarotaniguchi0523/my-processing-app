package com.example.domain.services;

import com.example.domain.models.DrawingState;
import java.util.concurrent.CompletableFuture;

/**
 * Domain interface for agent service that processes user input and generates drawing commands.
 * Implementations should be in the application layer.
 */
public interface IAgentService {
    
    /**
     * Process user instruction asynchronously
     * @param instruction The user's instruction
     * @return CompletableFuture with the service response
     */
    CompletableFuture<Object> processInstruction(String instruction);
    
    /**
     * Process user instruction synchronously
     * @param instruction The user's instruction
     * @return The service response
     */
    Object processInstructionSync(String instruction);
    
    /**
     * Gets the current drawing state
     * @return The current drawing state
     */
    DrawingState getCurrentState();
    
    /**
     * Sets the current drawing state
     * @param state The new drawing state
     */
    void setCurrentState(DrawingState state);
    
    /**
     * Waits for user approval
     * @return CompletableFuture with approval result
     */
    CompletableFuture<Boolean> awaitApproval();
    
    /**
     * Completes the approval process
     * @param approved Whether the change was approved
     */
    void completeApproval(boolean approved);
    
    /**
     * Checks if there is a pending approval
     * @return true if approval is pending
     */
    boolean hasPendingApproval();
    
    /**
     * Progress listener for streaming user-visible logs
     */
    interface ProgressListener {
        void onProgress(String message);
    }
    
    /**
     * Sets the progress listener
     * @param listener The progress listener
     */
    void setProgressListener(ProgressListener listener);
    
    /**
     * Clears the conversation history
     */
    void clearConversation();
    
    /**
     * Gets the conversation history as a formatted string
     * @return The conversation history
     */
    String getConversationHistory();
    
    /**
     * Gets detailed information about available tools for debugging
     * @return A formatted string with tool information
     */
    String getToolRegistryInfo();
}