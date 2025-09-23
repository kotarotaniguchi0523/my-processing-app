package com.example.domain.agents;

/**
 * Domain layer interface for LLM services.
 * This interface abstracts the LLM provider to maintain clean architecture.
 */
public interface ILLMService {

    /**
     * Generate content based on a prompt.
     *
     * @param prompt The prompt to send to the LLM
     * @return The generated response text
     * @throws Exception if generation fails
     */
    String generateContent(String prompt) throws Exception;

    /**
     * Generate content with a specific model configuration.
     *
     * @param prompt The prompt to send to the LLM
     * @param temperature Temperature setting for generation (0.0 to 1.0)
     * @return The generated response text
     * @throws Exception if generation fails
     */
    String generateContent(String prompt, double temperature) throws Exception;
}