package com.example.domain.events;

/**
 * Listener interface for AI processing events.
 * Implementations can monitor and react to various stages of AI processing.
 */
public interface IAIProcessingListener {
    /**
     * Called when AI processing starts.
     */
    void onProcessingStarted(ProcessingStartedEvent event);
    
    /**
     * Called when AI is analyzing the user prompt.
     */
    void onPromptAnalyzing(PromptAnalyzingEvent event);
    
    /**
     * Called when AI selects a tool for execution.
     */
    void onToolSelecting(ToolSelectingEvent event);
    
    /**
     * Called when a tool is being executed.
     */
    void onToolExecuting(ToolExecutingEvent event);
    
    /**
     * Called when AI processing completes successfully.
     */
    void onProcessingCompleted(ProcessingCompletedEvent event);
    
    /**
     * Called when AI processing fails.
     */
    void onProcessingFailed(ProcessingFailedEvent event);
}