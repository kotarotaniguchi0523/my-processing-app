package com.example.engine;

import com.example.domain.models.DrawingState;
import processing.core.PGraphics;

/**
 * YamlRenderer provides a unified interface for processing YAML-based drawing instructions and
 * rendering them using the drawing engine.
 */
public interface YamlRenderer {

  /**
   * Renders YAML drawing instructions directly to the canvas
   *
   * @param yamlString the YAML string containing drawing state
   * @throws YamlRenderException if parsing or rendering fails
   */
  void render(String yamlString) throws YamlRenderException;

  /**
   * Renders YAML drawing instructions to the specified graphics context
   *
   * @param yamlString the YAML string containing drawing state
   * @param canvas the PGraphics context to render to
   * @throws YamlRenderException if parsing or rendering fails
   */
  void render(String yamlString, PGraphics canvas) throws YamlRenderException;

  /**
   * Parses YAML to DrawingState without rendering
   *
   * @param yamlString the YAML string to parse
   * @return the parsed DrawingState
   * @throws YamlRenderException if parsing fails
   */
  DrawingState parseToState(String yamlString) throws YamlRenderException;

  /**
   * Converts DrawingState to YAML string
   *
   * @param drawingState the state to convert
   * @return YAML string representation
   * @throws YamlRenderException if serialization fails
   */
  String stateToYaml(DrawingState drawingState) throws YamlRenderException;

  /**
   * Validates YAML drawing instructions
   *
   * @param yamlString the YAML string to validate
   * @return validation result with details
   */
  ValidationResult validateYaml(String yamlString);

  /**
   * Gets the current drawing engine
   *
   * @return the DrawingEngine instance
   */
  DrawingEngine getDrawingEngine();

  /**
   * Gets the current YAML parser
   *
   * @return the YamlParser instance
   */
  YamlParser getYamlParser();

  /** Custom exception for YAML rendering errors */
  class YamlRenderException extends Exception {

    public YamlRenderException(String message) {
      super(message);
    }

    public YamlRenderException(String message, Throwable cause) {
      super(message, cause);
    }

    public YamlRenderException(Throwable cause) {
      super(cause);
    }
  }

  /** Validation result for YAML content */
  class ValidationResult {
    private final boolean valid;
    private final String message;
    private final Exception cause;

    public ValidationResult(boolean valid, String message) {
      this(valid, message, null);
    }

    public ValidationResult(boolean valid, String message, Exception cause) {
      this.valid = valid;
      this.message = message;
      this.cause = cause;
    }

    public boolean isValid() {
      return valid;
    }

    public String getMessage() {
      return message;
    }

    public Exception getCause() {
      return cause;
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, "YAML is valid and can be rendered");
    }

    public static ValidationResult valid(String message) {
      return new ValidationResult(true, message);
    }

    public static ValidationResult invalid(String message) {
      return new ValidationResult(false, message);
    }

    public static ValidationResult invalid(String message, Exception cause) {
      return new ValidationResult(false, message, cause);
    }

    @Override
    public String toString() {
      return String.format("ValidationResult(valid=%s, message='%s')", valid, message);
    }
  }
}
