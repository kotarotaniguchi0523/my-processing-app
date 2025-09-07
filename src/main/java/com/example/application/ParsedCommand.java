package com.example.application;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a parsed command from user input. Contains command type and parameters extracted from
 * natural language input.
 */
public class ParsedCommand {
    private final String type;
    private final Map<String, String> parameters;
    private final boolean valid;
    private final String originalInput;
    private final String errorMessage;

    /** Create a valid parsed command. */
    public ParsedCommand(String type, Map<String, String> parameters, String originalInput) {
        this.type = type;
        this.parameters = new HashMap<>(parameters);
        this.valid = true;
        this.originalInput = originalInput;
        this.errorMessage = null;
    }

    /** Create an invalid parsed command with error. */
    public ParsedCommand(String originalInput, String errorMessage) {
        this.type = null;
        this.parameters = new HashMap<>();
        this.valid = false;
        this.originalInput = originalInput;
        this.errorMessage = errorMessage;
    }

    // Getters
    public String getType() {
        return type;
    }

    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }

    public boolean isValid() {
        return valid;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Parameter access methods
    public String getStringParam(String key) {
        return parameters.get(key);
    }

    public String getStringParam(String key, String defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public int getIntParam(String key, int defaultValue) {
        try {
            String value = parameters.get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public float getFloatParam(String key, float defaultValue) {
        try {
            String value = parameters.get(key);
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanParam(String key, boolean defaultValue) {
        String value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value)
                || "yes".equalsIgnoreCase(value)
                || "true".equalsIgnoreCase(value)
                || "1".equals(value);
    }

    public boolean hasParam(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public String toString() {
        if (!valid) {
            return String.format(
                    "ParsedCommand[invalid, input='%s', error='%s']", originalInput, errorMessage);
        }
        return String.format(
                "ParsedCommand[type='%s', params=%s, input='%s']", type, parameters, originalInput);
    }
}
