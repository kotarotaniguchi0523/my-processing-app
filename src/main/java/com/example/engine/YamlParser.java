package com.example.engine;

import com.example.domain.models.DrawingState;
import com.example.domain.serialization.YamlSerializable;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;
import java.util.Objects;

/**
 * YamlParser provides bidirectional conversion between DrawingState objects and YAML strings. This
 * class serves as the main interface for State ⇄ YAML transformations in the drawing engine.
 */
public final class YamlParser {

    private final Yaml yaml;

    /** Creates a new YamlParser with default configuration */
    public YamlParser() {
        this.yaml = new Yaml();
    }

    /**
     * Creates a new YamlParser with custom Yaml instance
     *
     * @param yaml custom Yaml instance
     */
    public YamlParser(Yaml yaml) {
        this.yaml = Objects.requireNonNull(yaml, "Yaml instance cannot be null");
    }

    /**
     * Converts a DrawingState object to YAML string representation
     *
     * @param drawingState the DrawingState to convert
     * @return YAML string representation of the state
     * @throws YamlParseException if serialization fails
     */
    public String toYaml(DrawingState drawingState) throws YamlParseException {
        Objects.requireNonNull(drawingState, "DrawingState cannot be null");

        try {
            Map<String, Object> yamlMap = drawingState.toYamlMap();
            return yaml.dump(yamlMap);
        } catch (YAMLException e) {
            throw new YamlParseException("Failed to serialize DrawingState to YAML", e);
        } catch (Exception e) {
            throw new YamlParseException("Unexpected error during YAML serialization", e);
        }
    }

    /**
     * Converts a YAML string to DrawingState object
     *
     * @param yamlString the YAML string to parse
     * @return DrawingState object reconstructed from YAML
     * @throws YamlParseException if parsing fails or YAML is invalid
     */
    public DrawingState fromYaml(String yamlString) throws YamlParseException {
        Objects.requireNonNull(yamlString, "YAML string cannot be null");

        if (yamlString.isBlank()) {
            throw new YamlParseException("YAML string cannot be empty");
        }

        try {
            Object yamlData = yaml.load(yamlString);

            if (!(yamlData instanceof Map)) {
                throw new YamlParseException("YAML root must be a map/object");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = (Map<String, Object>) yamlData;

            return DrawingState.fromYamlMap(yamlMap);

        } catch (YAMLException e) {
            throw new YamlParseException("Invalid YAML syntax: " + e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new YamlParseException("YAML structure is not compatible with DrawingState", e);
        } catch (Exception e) {
            throw new YamlParseException(
                    "Failed to parse YAML to DrawingState: " + e.getMessage(), e);
        }
    }

    /**
     * Converts any YamlSerializable object to YAML string
     *
     * @param serializable the object to convert
     * @return YAML string representation
     * @throws YamlParseException if serialization fails
     */
    public String serializeToYaml(YamlSerializable serializable) throws YamlParseException {
        Objects.requireNonNull(serializable, "YamlSerializable object cannot be null");

        try {
            Map<String, Object> yamlMap = serializable.toYamlMap();
            return yaml.dump(yamlMap);
        } catch (YAMLException e) {
            throw new YamlParseException("Failed to serialize object to YAML", e);
        } catch (Exception e) {
            throw new YamlParseException("Unexpected error during YAML serialization", e);
        }
    }

    /**
     * Validates YAML string without full parsing
     *
     * @param yamlString the YAML string to validate
     * @return true if YAML is syntactically valid
     */
    public boolean isValidYaml(String yamlString) {
        if (yamlString == null || yamlString.isBlank()) {
            return false;
        }

        try {
            yaml.load(yamlString);
            return true;
        } catch (YAMLException e) {
            return false;
        }
    }

    /**
     * Validates that YAML string can be parsed to DrawingState
     *
     * @param yamlString the YAML string to validate
     * @return true if YAML is valid and can be converted to DrawingState
     */
    public boolean canParseToDrawingState(String yamlString) {
        try {
            fromYaml(yamlString);
            return true;
        } catch (YamlParseException e) {
            return false;
        }
    }

    /**
     * Performs a round-trip test: State → YAML → State
     *
     * @param originalState the original DrawingState
     * @return true if the round-trip preserves the state
     * @throws YamlParseException if conversion fails
     */
    public boolean testRoundTrip(DrawingState originalState) throws YamlParseException {
        Objects.requireNonNull(originalState, "Original DrawingState cannot be null");

        try {
            String yamlString = toYaml(originalState);
            DrawingState reconstructedState = fromYaml(yamlString);
            return originalState.equals(reconstructedState);
        } catch (YamlParseException e) {
            throw e; // Re-throw parsing exceptions
        } catch (Exception e) {
            throw new YamlParseException("Round-trip test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Pretty-prints YAML string with proper formatting
     *
     * @param yamlString the YAML string to format
     * @return formatted YAML string
     * @throws YamlParseException if YAML is invalid
     */
    public String prettyPrint(String yamlString) throws YamlParseException {
        Objects.requireNonNull(yamlString, "YAML string cannot be null");

        try {
            Object yamlData = yaml.load(yamlString);
            return yaml.dump(yamlData);
        } catch (YAMLException e) {
            throw new YamlParseException("Cannot pretty-print invalid YAML: " + e.getMessage(), e);
        }
    }

    /** Custom exception for YAML parsing errors */
    public static class YamlParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public YamlParseException(String message) {
            super(message);
        }

        public YamlParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public YamlParseException(Throwable cause) {
            super(cause);
        }
    }
}
