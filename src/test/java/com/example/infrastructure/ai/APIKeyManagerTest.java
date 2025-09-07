package com.example.infrastructure.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for APIKeyManager
 */
class APIKeyManagerTest {

    @TempDir
    Path tempDir;

    private final Map<String, String> originalSystemProperties = new HashMap<>();
    private final Map<String, String> originalEnvironmentVariables = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        APIKeyManager.clearCache();
        
        // Save original system properties for providers
        saveOriginalProperty("CLAUDE_API_KEY");
        saveOriginalProperty("ANTHROPIC_API_KEY");
        saveOriginalProperty("GEMINI_API_KEY");
        saveOriginalProperty("GOOGLE_API_KEY");
        saveOriginalProperty("OPENAI_API_KEY");
        
        // Clear system properties
        clearSystemProperty("CLAUDE_API_KEY");
        clearSystemProperty("ANTHROPIC_API_KEY");
        clearSystemProperty("GEMINI_API_KEY");
        clearSystemProperty("GOOGLE_API_KEY");
        clearSystemProperty("OPENAI_API_KEY");
    }

    @AfterEach
    void tearDown() {
        // Restore original system properties
        restoreOriginalProperties();
        
        // Clear cache after each test
        APIKeyManager.clearCache();
    }

    @Test
    void getApiKey_FromSystemProperty_ShouldReturnCorrectKey() {
        // Setup
        System.setProperty("CLAUDE_API_KEY", "test-claude-key");
        
        // Execute
        String apiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
        
        // Verify
        assertEquals("test-claude-key", apiKey);
    }

    @Test
    void getApiKey_FromAlternativeSystemProperty_ShouldReturnCorrectKey() {
        // Setup - use alternative key name
        System.setProperty("ANTHROPIC_API_KEY", "test-anthropic-key");
        
        // Execute
        String apiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
        
        // Verify
        assertEquals("test-anthropic-key", apiKey);
    }

    @Test
    void getApiKey_FromEnvFile_ShouldReturnCorrectKey() throws IOException {
        // Setup - create .env file in temp directory
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "CLAUDE_API_KEY=env-file-claude-key\n");
        
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Execute
            String apiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
            
            // Verify
            assertEquals("env-file-claude-key", apiKey);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void getApiKey_EnvFileWithComments_ShouldIgnoreComments() throws IOException {
        // Setup - create .env file with comments and empty lines
        Path envFile = tempDir.resolve(".env");
        String envContent = """
            # This is a comment
            
            CLAUDE_API_KEY=test-key-with-comments
            # Another comment
            GEMINI_API_KEY=gemini-key
            """;
        Files.writeString(envFile, envContent);
        
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Execute
            String claudeKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
            String geminiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.GEMINI);
            
            // Verify
            assertEquals("test-key-with-comments", claudeKey);
            assertEquals("gemini-key", geminiKey);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void getApiKey_EnvFileWithQuotes_ShouldRemoveQuotes() throws IOException {
        // Setup - create .env file with quoted values
        Path envFile = tempDir.resolve(".env");
        String envContent = """
            CLAUDE_API_KEY="quoted-claude-key"
            GEMINI_API_KEY='single-quoted-gemini-key'
            """;
        Files.writeString(envFile, envContent);
        
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Execute
            String claudeKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
            String geminiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.GEMINI);
            
            // Verify
            assertEquals("quoted-claude-key", claudeKey);
            assertEquals("single-quoted-gemini-key", geminiKey);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void getApiKey_NoApiKeyFound_ShouldThrowException() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE));
        
        assertTrue(exception.getMessage().contains("No API key found for CLAUDE"));
        assertTrue(exception.getMessage().contains("CLAUDE_API_KEY, ANTHROPIC_API_KEY"));
    }

    @Test
    void getApiKey_CachesBetweenCalls_ShouldReturnSameValue() {
        // Setup
        System.setProperty("CLAUDE_API_KEY", "cached-key");
        
        // Execute - first call
        String firstCall = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
        
        // Remove system property to test caching
        System.clearProperty("CLAUDE_API_KEY");
        
        // Execute - second call should use cache
        String secondCall = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
        
        // Verify
        assertEquals("cached-key", firstCall);
        assertEquals("cached-key", secondCall);
    }

    @Test
    void hasApiKey_KeyExists_ShouldReturnTrue() {
        // Setup
        System.setProperty("GEMINI_API_KEY", "test-gemini-key");
        
        // Execute & Verify
        assertTrue(APIKeyManager.hasApiKey(APIKeyManager.Provider.GEMINI));
    }

    @Test
    void hasApiKey_KeyDoesNotExist_ShouldReturnFalse() {
        // Make sure there's really no key by clearing environment variable
        String originalOpenAIKey = System.getenv("OPENAI_API_KEY");
        // Note: We can't clear environment variables in tests, but we can clear system properties
        
        // Execute & Verify - use a provider that's less likely to have keys set
        // In this test environment, check specifically for no cached keys
        APIKeyManager.clearCache();
        assertFalse(APIKeyManager.hasApiKey(APIKeyManager.Provider.OPENAI));
    }

    @Test
    void setApiKey_ValidKey_ShouldSetAndCache() {
        // Execute
        APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, "programmatic-key");
        
        // Verify
        String retrievedKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
        assertEquals("programmatic-key", retrievedKey);
    }

    @Test
    void setApiKey_NullKey_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, null));
        
        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    @Test
    void setApiKey_EmptyKey_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, ""));
        
        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    @Test
    void setApiKey_WhitespaceKey_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, "   "));
        
        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    @Test
    void clearCache_ShouldRemoveAllCachedKeys() {
        // Setup
        APIKeyManager.setApiKey(APIKeyManager.Provider.CLAUDE, "cached-claude-key");
        APIKeyManager.setApiKey(APIKeyManager.Provider.GEMINI, "cached-gemini-key");
        
        // Verify keys are cached
        assertEquals("cached-claude-key", APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE));
        assertEquals("cached-gemini-key", APIKeyManager.getApiKey(APIKeyManager.Provider.GEMINI));
        
        // Execute
        APIKeyManager.clearCache();
        
        // Verify keys are no longer cached (should throw exceptions)
        assertThrows(IllegalStateException.class, () -> 
            APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE));
        assertThrows(IllegalStateException.class, () -> 
            APIKeyManager.getApiKey(APIKeyManager.Provider.GEMINI));
    }

    @Test
    void getApiKey_SystemPropertyTakesPrecedenceOverEnvFile() throws IOException {
        // Setup - create .env file
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "CLAUDE_API_KEY=env-file-key\n");
        
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Setup - also set system property
            System.setProperty("CLAUDE_API_KEY", "system-property-key");
            
            // Execute
            String apiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
            
            // Verify - system property should take precedence
            assertEquals("system-property-key", apiKey);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void createSampleEnvFile_ShouldCreateFileWithCorrectContent() throws IOException {
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Execute
            APIKeyManager.createSampleEnvFile();
            
            // Verify
            Path envFile = tempDir.resolve(".env");
            assertTrue(Files.exists(envFile));
            
            String content = Files.readString(envFile);
            assertTrue(content.contains("CLAUDE_API_KEY"));
            assertTrue(content.contains("ANTHROPIC_API_KEY"));
            assertTrue(content.contains("GEMINI_API_KEY"));
            assertTrue(content.contains("GOOGLE_API_KEY"));
            assertTrue(content.contains("OPENAI_API_KEY"));
            assertTrue(content.contains("#"));  // Should contain comments
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void createSampleEnvFile_FileAlreadyExists_ShouldNotOverwrite() throws IOException {
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            Path envFile = tempDir.resolve(".env");
            Files.writeString(envFile, "existing content");
            
            // Execute
            APIKeyManager.createSampleEnvFile();
            
            // Verify - should not overwrite
            String content = Files.readString(envFile);
            assertEquals("existing content", content);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void providerEnums_ShouldHaveCorrectPossibleKeys() {
        // Verify CLAUDE provider
        String[] claudeKeys = APIKeyManager.Provider.CLAUDE.getPossibleKeys();
        assertEquals(2, claudeKeys.length);
        assertEquals("CLAUDE_API_KEY", claudeKeys[0]);
        assertEquals("ANTHROPIC_API_KEY", claudeKeys[1]);
        
        // Verify GEMINI provider
        String[] geminiKeys = APIKeyManager.Provider.GEMINI.getPossibleKeys();
        assertEquals(2, geminiKeys.length);
        assertEquals("GEMINI_API_KEY", geminiKeys[0]);
        assertEquals("GOOGLE_API_KEY", geminiKeys[1]);
        
        // Verify OPENAI provider
        String[] openaiKeys = APIKeyManager.Provider.OPENAI.getPossibleKeys();
        assertEquals(1, openaiKeys.length);
        assertEquals("OPENAI_API_KEY", openaiKeys[0]);
    }

    @Test
    void getApiKey_TrimsWhitespaceFromValues() throws IOException {
        // Setup - create .env file with whitespace
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "CLAUDE_API_KEY=  key-with-whitespace  \n");
        
        // Save original user.dir and change to temp directory
        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            
            // Execute
            String apiKey = APIKeyManager.getApiKey(APIKeyManager.Provider.CLAUDE);
            
            // Verify
            assertEquals("key-with-whitespace", apiKey);
        } finally {
            // Restore original user.dir
            System.setProperty("user.dir", originalUserDir);
        }
    }

    // Helper methods
    private void saveOriginalProperty(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            originalSystemProperties.put(key, value);
        }
    }

    private void clearSystemProperty(String key) {
        System.clearProperty(key);
    }

    private void restoreOriginalProperties() {
        originalSystemProperties.forEach(System::setProperty);
    }
}