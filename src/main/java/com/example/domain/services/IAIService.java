package com.example.domain.services;

import com.example.domain.models.DrawingState;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI services in the domain layer
 * This abstracts the AI interaction from the application layer
 */
public interface IAIService {
    
    /**
     * Process a user instruction using AI
     * @param instruction The user's natural language instruction
     * @return A future containing the AI response
     */
    CompletableFuture<AIServiceResponse> processInstruction(String instruction);
    
    /**
     * Set the current drawing state
     * @param state The current drawing state
     */
    void setCurrentState(DrawingState state);
    
    /**
     * Get the current drawing state
     * @return The current drawing state
     */
    DrawingState getCurrentState();
    
    /**
     * Wait for user approval
     * @return A future that completes with true if approved, false if rejected
     */
    CompletableFuture<Boolean> awaitApproval();
    
    /**
     * Complete the approval process
     * @param approved true if approved, false if rejected
     */
    void completeApproval(boolean approved);
    
    /**
     * Set a progress listener for AI processing updates
     * @param listener The progress listener
     */
    void setProgressListener(ProgressListener listener);
    
    /**
     * Interface for progress updates
     */
    interface ProgressListener {
        void onProgress(String message);
    }
    
    /**
     * Response from the AI service
     */
    class AIServiceResponse {
        private final String textResponse;
        private final DrawingState resultState;
        private final boolean requiresApproval;
        private final String error;
        
        public AIServiceResponse(String textResponse, DrawingState resultState, boolean requiresApproval) {
            this.textResponse = textResponse;
            this.resultState = resultState;
            this.requiresApproval = requiresApproval;
            this.error = null;
        }
        
        public AIServiceResponse(String error) {
            this.textResponse = null;
            this.resultState = null;
            this.requiresApproval = false;
            this.error = error;
        }
        
        public boolean hasTextResponse() {
            return textResponse != null && !textResponse.isEmpty();
        }
        
        public String getTextResponse() {
            return textResponse;
        }
        
        public boolean hasResultState() {
            return resultState != null;
        }
        
        public DrawingState getResultState() {
            return resultState;
        }
        
        public boolean requiresApproval() {
            return requiresApproval;
        }
        
        public boolean hasError() {
            return error != null;
        }
        
        public String getError() {
            return error;
        }
        
        public static AIServiceResponse text(String text) {
            return new AIServiceResponse(text, null, false);
        }
        
        public static AIServiceResponse withState(String text, DrawingState state, boolean requiresApproval) {
            return new AIServiceResponse(text, state, requiresApproval);
        }
        
        public static AIServiceResponse error(String error) {
            return new AIServiceResponse(error);
        }
    }
}