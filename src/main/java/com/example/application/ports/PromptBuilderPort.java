package com.example.application.ports;

import com.example.domain.models.DrawingState;
import java.util.Map;
import java.util.List;

/**
 * Port interface for building AI prompts following the Hexagonal Architecture pattern.
 * This interface defines the contract for prompt construction required by the application layer,
 * allowing different implementations for various AI models or prompt strategies.
 * 
 * <p>Responsibilities:
 * - Define contracts for structured prompt building
 * - Provide abstractions for context-aware prompt generation
 * - Support different prompt formats and AI model requirements
 * 
 * <p>Design Principles:
 * - Dependency Inversion: Application layer defines interface, adapters implement
 * - Interface Segregation: Focused on prompt building operations only
 * - Single Responsibility: Solely responsible for prompt construction
 */
public interface PromptBuilderPort {
    
    /**
     * Builds a complete AI prompt from user input and current context.
     * 
     * @param userInput The user's natural language instruction
     * @param currentState The current drawing state for context
     * @return A structured prompt ready for AI processing
     * @throws IllegalArgumentException if userInput is null or empty
     */
    StructuredPrompt buildPrompt(String userInput, DrawingState currentState);
    
    /**
     * Builds a system prompt with tool capabilities and constraints.
     * 
     * @param availableTools List of available tool names and descriptions
     * @param constraints Optional constraints or safety rules
     * @return A system prompt defining the AI's capabilities and behavior
     * @throws IllegalArgumentException if availableTools is null
     */
    StructuredPrompt buildSystemPrompt(List<ToolCapability> availableTools, List<String> constraints);
    
    /**
     * Builds a context prompt from the current drawing state.
     * 
     * @param state The current drawing state to describe
     * @return A prompt describing the current visual context
     */
    StructuredPrompt buildContextPrompt(DrawingState state);
    
    /**
     * Builds a follow-up prompt for clarification or additional information.
     * 
     * @param previousResponse The AI's previous response
     * @param clarificationNeeded The specific clarification required
     * @return A prompt requesting additional information
     * @throws IllegalArgumentException if parameters are null
     */
    StructuredPrompt buildFollowUpPrompt(String previousResponse, String clarificationNeeded);
    
    /**
     * Combines multiple prompts into a single structured prompt.
     * 
     * @param prompts List of prompts to combine
     * @return A combined structured prompt
     * @throws IllegalArgumentException if prompts is null or empty
     */
    StructuredPrompt combinePrompts(List<StructuredPrompt> prompts);
    
    /**
     * Validates a prompt for completeness and safety.
     * 
     * @param prompt The prompt to validate
     * @return Validation result with any issues found
     * @throws IllegalArgumentException if prompt is null
     */
    PromptValidationResult validatePrompt(StructuredPrompt prompt);
    
    /**
     * Gets the maximum token limit for prompts in the current configuration.
     * 
     * @return The maximum number of tokens allowed in a prompt
     */
    int getMaxTokenLimit();
    
    /**
     * Estimates the token count for a given prompt.
     * 
     * @param prompt The prompt to analyze
     * @return Estimated token count
     * @throws IllegalArgumentException if prompt is null
     */
    int estimateTokenCount(StructuredPrompt prompt);
    
    /**
     * Represents a structured AI prompt with metadata.
     */
    class StructuredPrompt {
        private final String content;
        private final PromptType type;
        private final Map<String, Object> metadata;
        private final int estimatedTokens;
        
        public StructuredPrompt(String content, PromptType type, Map<String, Object> metadata, int estimatedTokens) {
            this.content = content;
            this.type = type;
            this.metadata = metadata;
            this.estimatedTokens = estimatedTokens;
        }
        
        public String getContent() { return content; }
        public PromptType getType() { return type; }
        public Map<String, Object> getMetadata() { return metadata; }
        public int getEstimatedTokens() { return estimatedTokens; }
    }
    
    /**
     * Enumeration of prompt types.
     */
    enum PromptType {
        USER_INSTRUCTION,
        SYSTEM_PROMPT,
        CONTEXT_DESCRIPTION,
        FOLLOW_UP_CLARIFICATION,
        COMBINED_PROMPT
    }
    
    /**
     * Represents a tool capability for system prompt building.
     */
    class ToolCapability {
        private final String name;
        private final String description;
        private final Map<String, Object> parameters;
        private final List<String> examples;
        
        public ToolCapability(String name, String description, Map<String, Object> parameters, List<String> examples) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.examples = examples;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getParameters() { return parameters; }
        public List<String> getExamples() { return examples; }
    }
    
    /**
     * Result of prompt validation.
     */
    class PromptValidationResult {
        private final boolean isValid;
        private final List<String> warnings;
        private final List<String> errors;
        
        public PromptValidationResult(boolean isValid, List<String> warnings, List<String> errors) {
            this.isValid = isValid;
            this.warnings = warnings;
            this.errors = errors;
        }
        
        public boolean isValid() { return isValid; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
    }
}