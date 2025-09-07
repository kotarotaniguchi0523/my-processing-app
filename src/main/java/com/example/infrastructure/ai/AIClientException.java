package com.example.infrastructure.ai;

/**
 * Exception thrown by AI client operations
 */
public class AIClientException extends Exception {
    
    private final ErrorType errorType;
    private final String providerErrorCode;
    
    public AIClientException(String message) {
        this(message, ErrorType.UNKNOWN, null, null);
    }
    
    public AIClientException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN, null, cause);
    }
    
    public AIClientException(String message, ErrorType errorType) {
        this(message, errorType, null, null);
    }
    
    public AIClientException(String message, ErrorType errorType, String providerErrorCode, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.providerErrorCode = providerErrorCode;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public String getProviderErrorCode() {
        return providerErrorCode;
    }
    
    /**
     * Types of errors that can occur
     */
    public enum ErrorType {
        AUTHENTICATION("Authentication failed"),
        RATE_LIMIT("Rate limit exceeded"),
        TIMEOUT("Request timed out"),
        NETWORK("Network error"),
        INVALID_REQUEST("Invalid request"),
        SERVER_ERROR("Server error"),
        UNKNOWN("Unknown error");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}