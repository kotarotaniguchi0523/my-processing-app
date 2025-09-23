package com.example.infrastructure.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages API key configuration for ADK.
 * Single responsibility: Handle API key setup and validation.
 */
public class ApiKeyManager {

    private static final Logger LOGGER = Logger.getLogger(ApiKeyManager.class.getName());

    /**
     * Set up API key for ADK from .env file.
     * ADK requires the API key to be in GOOGLE_API_KEY environment variable.
     * This project uses GEMINI_API_KEY in .env, which is mapped to GOOGLE_API_KEY by PowerShell script.
     */
    public static void setupApiKeyForADK() {
        // Check if GOOGLE_API_KEY environment variable is already set (by PowerShell script)
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey != null && !googleApiKey.isEmpty()) {
            LOGGER.info("GOOGLE_API_KEY environment variable is set (from GEMINI_API_KEY via PowerShell)");
            return;
        }

        // Check if GEMINI_API_KEY is directly available
        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            LOGGER.info("GEMINI_API_KEY environment variable found");
            // Note: ADK requires GOOGLE_API_KEY, which should be set by run-adk.ps1
            LOGGER.warning("ADK requires GOOGLE_API_KEY. Please use run-adk.ps1 to execute.");
        }

        // Try to load from .env file for information
        try {
            Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

            String apiKey = dotenv.get("GEMINI_API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                LOGGER.info("GEMINI_API_KEY found in .env file");
                if (googleApiKey == null) {
                    LOGGER.warning("Please run with PowerShell script: powershell -ExecutionPolicy Bypass -File run-adk.ps1");
                    LOGGER.warning("This will automatically map GEMINI_API_KEY to GOOGLE_API_KEY for ADK");
                }
            } else {
                LOGGER.severe("No GEMINI_API_KEY found in .env file. Please add GEMINI_API_KEY to .env file");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load API key from .env file", e);
        }
    }

}