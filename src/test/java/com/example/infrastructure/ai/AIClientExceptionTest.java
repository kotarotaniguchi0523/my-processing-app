package com.example.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIClientException and error type mapping
 */
class AIClientExceptionTest {

    @Test
    void constructor_MessageOnly_ShouldSetCorrectDefaults() {
        // Execute
        AIClientException exception = new AIClientException("Test error message");
        
        // Verify
        assertEquals("Test error message", exception.getMessage());
        assertEquals(AIClientException.ErrorType.UNKNOWN, exception.getErrorType());
        assertNull(exception.getProviderErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_MessageAndCause_ShouldSetCorrectDefaults() {
        // Setup
        RuntimeException cause = new RuntimeException("Root cause");
        
        // Execute
        AIClientException exception = new AIClientException("Test error", cause);
        
        // Verify
        assertEquals("Test error", exception.getMessage());
        assertEquals(AIClientException.ErrorType.UNKNOWN, exception.getErrorType());
        assertNull(exception.getProviderErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructor_MessageAndErrorType_ShouldSetCorrectly() {
        // Execute
        AIClientException exception = new AIClientException(
            "Authentication failed", 
            AIClientException.ErrorType.AUTHENTICATION
        );
        
        // Verify
        assertEquals("Authentication failed", exception.getMessage());
        assertEquals(AIClientException.ErrorType.AUTHENTICATION, exception.getErrorType());
        assertNull(exception.getProviderErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_AllParameters_ShouldSetCorrectly() {
        // Setup
        RuntimeException cause = new RuntimeException("Network timeout");
        
        // Execute
        AIClientException exception = new AIClientException(
            "Request timeout", 
            AIClientException.ErrorType.TIMEOUT,
            "TIMEOUT_001",
            cause
        );
        
        // Verify
        assertEquals("Request timeout", exception.getMessage());
        assertEquals(AIClientException.ErrorType.TIMEOUT, exception.getErrorType());
        assertEquals("TIMEOUT_001", exception.getProviderErrorCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructor_NullValues_ShouldBeHandledCorrectly() {
        // Execute
        AIClientException exception = new AIClientException(
            null, 
            AIClientException.ErrorType.NETWORK,
            null,
            null
        );
        
        // Verify
        assertNull(exception.getMessage());
        assertEquals(AIClientException.ErrorType.NETWORK, exception.getErrorType());
        assertNull(exception.getProviderErrorCode());
        assertNull(exception.getCause());
    }

    @Test
    void errorType_AllValues_ShouldHaveCorrectDescriptions() {
        // Verify all error types have descriptions
        assertEquals("Authentication failed", AIClientException.ErrorType.AUTHENTICATION.getDescription());
        assertEquals("Rate limit exceeded", AIClientException.ErrorType.RATE_LIMIT.getDescription());
        assertEquals("Request timed out", AIClientException.ErrorType.TIMEOUT.getDescription());
        assertEquals("Network error", AIClientException.ErrorType.NETWORK.getDescription());
        assertEquals("Invalid request", AIClientException.ErrorType.INVALID_REQUEST.getDescription());
        assertEquals("Server error", AIClientException.ErrorType.SERVER_ERROR.getDescription());
        assertEquals("Unknown error", AIClientException.ErrorType.UNKNOWN.getDescription());
    }

    @Test
    void errorType_EnumValues_ShouldExistForAllCommonScenarios() {
        // Verify all expected error types exist
        AIClientException.ErrorType[] expectedTypes = {
            AIClientException.ErrorType.AUTHENTICATION,
            AIClientException.ErrorType.RATE_LIMIT,
            AIClientException.ErrorType.TIMEOUT,
            AIClientException.ErrorType.NETWORK,
            AIClientException.ErrorType.INVALID_REQUEST,
            AIClientException.ErrorType.SERVER_ERROR,
            AIClientException.ErrorType.UNKNOWN
        };
        
        AIClientException.ErrorType[] actualTypes = AIClientException.ErrorType.values();
        
        assertEquals(expectedTypes.length, actualTypes.length);
        for (AIClientException.ErrorType expectedType : expectedTypes) {
            boolean found = false;
            for (AIClientException.ErrorType actualType : actualTypes) {
                if (expectedType == actualType) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected error type not found: " + expectedType);
        }
    }

    @Test
    void exception_IsCheckedException_ShouldExtendException() {
        // Verify AIClientException is a checked exception
        assertTrue(Exception.class.isAssignableFrom(AIClientException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(AIClientException.class));
    }

    @Test
    void exception_WithChainedCauses_ShouldPreserveCauseChain() {
        // Setup - create a chain of causes
        IllegalArgumentException rootCause = new IllegalArgumentException("Invalid parameter");
        RuntimeException intermediateCause = new RuntimeException("Processing failed", rootCause);
        
        // Execute
        AIClientException exception = new AIClientException(
            "API request failed",
            AIClientException.ErrorType.INVALID_REQUEST,
            "INVALID_PARAM",
            intermediateCause
        );
        
        // Verify cause chain is preserved
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
        assertEquals("Invalid parameter", exception.getCause().getCause().getMessage());
    }

    @Test
    void exception_ProviderErrorCode_ShouldAcceptVariousFormats() {
        // Test various provider error code formats
        
        // Numeric codes
        AIClientException exception1 = new AIClientException(
            "Error", AIClientException.ErrorType.RATE_LIMIT, "429", null);
        assertEquals("429", exception1.getProviderErrorCode());
        
        // Alphanumeric codes
        AIClientException exception2 = new AIClientException(
            "Error", AIClientException.ErrorType.AUTHENTICATION, "AUTH_001", null);
        assertEquals("AUTH_001", exception2.getProviderErrorCode());
        
        // Complex codes with special characters
        AIClientException exception3 = new AIClientException(
            "Error", AIClientException.ErrorType.SERVER_ERROR, "ERR-500-INTERNAL", null);
        assertEquals("ERR-500-INTERNAL", exception3.getProviderErrorCode());
        
        // Empty string
        AIClientException exception4 = new AIClientException(
            "Error", AIClientException.ErrorType.UNKNOWN, "", null);
        assertEquals("", exception4.getProviderErrorCode());
    }

    @Test
    void exception_MessageFormatting_ShouldWorkCorrectly() {
        // Test message formatting with different scenarios
        
        // Simple message
        AIClientException exception1 = new AIClientException("Simple error");
        assertEquals("Simple error", exception1.getMessage());
        
        // Message with parameters (using String.format style)
        String formattedMessage = String.format("Error %d: %s failed", 500, "API");
        AIClientException exception2 = new AIClientException(formattedMessage);
        assertEquals("Error 500: API failed", exception2.getMessage());
        
        // Empty message
        AIClientException exception3 = new AIClientException("");
        assertEquals("", exception3.getMessage());
    }

    @Test
    void exception_ToString_ShouldIncludeRelevantInformation() {
        // Execute
        AIClientException exception = new AIClientException(
            "Rate limit exceeded",
            AIClientException.ErrorType.RATE_LIMIT,
            "RATE_LIMIT_001",
            new RuntimeException("Too many requests")
        );
        
        // Verify toString includes class name and message
        String toString = exception.toString();
        assertTrue(toString.contains("AIClientException"));
        assertTrue(toString.contains("Rate limit exceeded"));
    }

    @Test
    void exception_SerializationCompatibility_ShouldWorkWithStandardFields() {
        // This test ensures that the exception follows standard Java exception patterns
        // which is important for serialization and logging frameworks
        
        AIClientException exception = new AIClientException(
            "Serialization test",
            AIClientException.ErrorType.SERVER_ERROR,
            "SER_001",
            new RuntimeException("Root cause")
        );
        
        // Verify all standard exception methods work
        assertNotNull(exception.getMessage());
        assertNotNull(exception.getCause());
        assertNotNull(exception.toString());
        assertNotNull(exception.getStackTrace());
        
        // Verify custom fields are accessible
        assertEquals(AIClientException.ErrorType.SERVER_ERROR, exception.getErrorType());
        assertEquals("SER_001", exception.getProviderErrorCode());
    }

    @Test
    void errorType_DescriptionsAreInformative_ShouldProvideUsefulInfo() {
        // Verify that error type descriptions are helpful for debugging
        for (AIClientException.ErrorType errorType : AIClientException.ErrorType.values()) {
            String description = errorType.getDescription();
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
            
            // Description should be a proper sentence (capitalized, reasonable length)
            assertTrue(Character.isUpperCase(description.charAt(0)), 
                "Description should start with capital letter: " + description);
            assertTrue(description.length() > 5, 
                "Description should be informative: " + description);
        }
    }

    @Test
    void exception_StackTrace_ShouldBePreserved() {
        // Execute
        AIClientException exception = new AIClientException("Test exception");
        
        // Verify stack trace is preserved
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
        
        // Verify the exception was created in this test method
        boolean foundTestMethod = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().equals("exception_StackTrace_ShouldBePreserved")) {
                foundTestMethod = true;
                break;
            }
        }
        assertTrue(foundTestMethod, "Stack trace should include the test method");
    }
}