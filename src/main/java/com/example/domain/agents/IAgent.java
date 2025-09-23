package com.example.domain.agents;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all workflow agents.
 * Each agent represents a specialized step in the drawing generation pipeline.
 */
public interface IAgent<TInput, TOutput> {

    /**
     * Get the name of this agent for logging and identification.
     */
    String getName();

    /**
     * Process the input and produce an output.
     * This is the main processing method for each agent.
     *
     * @param input The input data for this agent
     * @param progressCallback Callback for reporting progress
     * @return The processed output
     */
    TOutput process(TInput input, ProgressCallback progressCallback) throws AgentException;

    /**
     * Asynchronously process the input.
     * Default implementation wraps the synchronous process method.
     *
     * @param input The input data for this agent
     * @param progressCallback Callback for reporting progress
     * @return A future containing the processed output
     */
    default CompletableFuture<TOutput> processAsync(TInput input, ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return process(input, progressCallback);
            } catch (AgentException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Validate the input before processing.
     *
     * @param input The input to validate
     * @return true if the input is valid, false otherwise
     */
    boolean validateInput(TInput input);

    /**
     * Get a description of what this agent does.
     */
    String getDescription();

    /**
     * Callback interface for reporting progress.
     */
    @FunctionalInterface
    interface ProgressCallback {
        /**
         * Report progress with a message and optional AI response.
         *
         * @param stage Current processing stage
         * @param message Progress message
         * @param aiResponse Optional AI response text
         */
        void onProgress(String stage, String message, String aiResponse);
    }

    /**
     * Exception thrown when agent processing fails.
     */
    class AgentException extends Exception {
        private final String agentName;

        public AgentException(String agentName, String message) {
            super(message);
            this.agentName = agentName;
        }

        public AgentException(String agentName, String message, Throwable cause) {
            super(message, cause);
            this.agentName = agentName;
        }

        public String getAgentName() {
            return agentName;
        }
    }
}