package com.example.infrastructure.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Manages API keys from environment variables and configuration files
 */
public class APIKeyManager {
    
    private static final Logger logger = Logger.getLogger(APIKeyManager.class.getName());
    private static final String ENV_FILE = ".env";
    private static final Map<String, String> API_KEY_CACHE = new HashMap<>();
    
    /**
     * Supported AI providers
     */
    public enum Provider {
        CLAUDE("CLAUDE_API_KEY", "ANTHROPIC_API_KEY"),
        GEMINI("GEMINI_API_KEY", "GOOGLE_API_KEY"),
        OPENAI("OPENAI_API_KEY");
        
        private final String[] possibleKeys;
        
        Provider(String... possibleKeys) {
            this.possibleKeys = possibleKeys;
        }
        
        public String[] getPossibleKeys() {
            return possibleKeys;
        }
    }
    
    /**
     * Gets the API key for the specified provider
     * @param provider The AI provider
     * @return The API key
     * @throws IllegalStateException if no API key is found
     */
    public static String getApiKey(Provider provider) {
        String cachedKey = API_KEY_CACHE.get(provider.name());
        if (cachedKey != null) {
            return cachedKey;
        }
        
        // Try to get from environment variables
        for (String keyName : provider.getPossibleKeys()) {
            String apiKey = System.getenv(keyName);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                logger.info("API key found in environment variable: " + keyName);
                API_KEY_CACHE.put(provider.name(), apiKey);
                return apiKey;
            }
        }
        
        // Try to get from .env file
        Properties envProperties = loadEnvFile();
        for (String keyName : provider.getPossibleKeys()) {
            String apiKey = envProperties.getProperty(keyName);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                logger.info("API key found in .env file: " + keyName);
                API_KEY_CACHE.put(provider.name(), apiKey);
                return apiKey;
            }
        }
        
        // Try system properties
        for (String keyName : provider.getPossibleKeys()) {
            String apiKey = System.getProperty(keyName);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                logger.info("API key found in system properties: " + keyName);
                API_KEY_CACHE.put(provider.name(), apiKey);
                return apiKey;
            }
        }
        
        throw new IllegalStateException(
            String.format("No API key found for %s. Please set one of these environment variables: %s",
                provider.name(), String.join(", ", provider.getPossibleKeys()))
        );
    }
    
    /**
     * Checks if an API key is available for the provider
     * @param provider The AI provider
     * @return true if API key is available
     */
    public static boolean hasApiKey(Provider provider) {
        try {
            getApiKey(provider);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Sets an API key programmatically (useful for testing)
     * @param provider The AI provider
     * @param apiKey The API key
     */
    public static void setApiKey(Provider provider, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        API_KEY_CACHE.put(provider.name(), apiKey);
        logger.info("API key set programmatically for: " + provider.name());
    }
    
    /**
     * Clears cached API keys
     */
    public static void clearCache() {
        API_KEY_CACHE.clear();
    }
    
    /**
     * Loads environment variables from .env file
     * @return Properties containing environment variables
     */
    private static Properties loadEnvFile() {
        Properties props = new Properties();
        
        // Try multiple locations for .env file
        Path[] possiblePaths = {
            Paths.get(ENV_FILE),
            Paths.get(System.getProperty("user.dir"), ENV_FILE),
            Paths.get(System.getProperty("user.home"), ENV_FILE)
        };
        
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                try {
                    Files.lines(path).forEach(line -> {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            int equalsIndex = line.indexOf('=');
                            if (equalsIndex > 0) {
                                String key = line.substring(0, equalsIndex).trim();
                                String value = line.substring(equalsIndex + 1).trim();
                                // Remove quotes if present
                                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                    (value.startsWith("'") && value.endsWith("'"))) {
                                    value = value.substring(1, value.length() - 1);
                                }
                                props.setProperty(key, value);
                            }
                        }
                    });
                    logger.info("Loaded .env file from: " + path.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    logger.warning("Failed to load .env file from: " + path + " - " + e.getMessage());
                }
            }
        }
        
        return props;
    }
    
    /**
     * Creates a sample .env file with placeholder values
     */
    public static void createSampleEnvFile() {
        String sample = """
            # AI API Keys
            # Uncomment and add your API keys below
            
            # Claude (Anthropic)
            #CLAUDE_API_KEY=your_claude_api_key_here
            #ANTHROPIC_API_KEY=your_anthropic_api_key_here
            
            # Google Gemini
            #GEMINI_API_KEY=your_gemini_api_key_here
            #GOOGLE_API_KEY=your_google_api_key_here
            
            # OpenAI
            #OPENAI_API_KEY=your_openai_api_key_here
            """;
        
        Path envPath = Paths.get(ENV_FILE);
        if (!Files.exists(envPath)) {
            try {
                Files.writeString(envPath, sample);
                logger.info("Created sample .env file at: " + envPath.toAbsolutePath());
            } catch (IOException e) {
                logger.warning("Failed to create sample .env file: " + e.getMessage());
            }
        }
    }
}