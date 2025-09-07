package com.example.engine;

import com.example.domain.models.DrawingState;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.Objects;

/**
 * Default implementation of YamlRenderer Integrates YamlParser and DrawingEngine to provide a
 * complete YAML rendering solution
 */
public final class YamlRendererImpl implements YamlRenderer {

  private final YamlParser yamlParser;
  private final DrawingEngine drawingEngine;
  private final PApplet sketch;

  /**
   * Creates a new YamlRendererImpl with the specified Processing sketch
   *
   * @param sketch the PApplet instance to render to
   */
  public YamlRendererImpl(PApplet sketch) {
    this.sketch = Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.yamlParser = new YamlParser();
    this.drawingEngine = new DrawingEngine(sketch);
  }

  /**
   * Creates a new YamlRendererImpl with custom parser and engine
   *
   * @param sketch the PApplet instance
   * @param yamlParser custom YAML parser
   * @param drawingEngine custom drawing engine
   */
  public YamlRendererImpl(PApplet sketch, YamlParser yamlParser, DrawingEngine drawingEngine) {
    this.sketch = Objects.requireNonNull(sketch, "PApplet sketch cannot be null");
    this.yamlParser = Objects.requireNonNull(yamlParser, "YamlParser cannot be null");
    this.drawingEngine = Objects.requireNonNull(drawingEngine, "DrawingEngine cannot be null");
  }

  @Override
  public void render(String yamlString) throws YamlRenderException {
    render(yamlString, sketch.g);
  }

  @Override
  public void render(String yamlString, PGraphics canvas) throws YamlRenderException {
    Objects.requireNonNull(yamlString, "YAML string cannot be null");
    Objects.requireNonNull(canvas, "Canvas cannot be null");

    try {
      // Parse YAML to DrawingState
      DrawingState drawingState = parseToState(yamlString);

      // Set the canvas and render
      PGraphics originalCanvas = drawingEngine.getCanvas();
      try {
        drawingEngine.setCanvas(canvas);
        drawingEngine.renderState(drawingState);
      } finally {
        // Restore original canvas
        drawingEngine.setCanvas(originalCanvas);
      }

    } catch (DrawingEngine.DrawingException e) {
      throw new YamlRenderException("Failed to render parsed drawing state", e);
    } catch (Exception e) {
      throw new YamlRenderException("Unexpected error during rendering", e);
    }
  }

  @Override
  public DrawingState parseToState(String yamlString) throws YamlRenderException {
    Objects.requireNonNull(yamlString, "YAML string cannot be null");

    try {
      return yamlParser.fromYaml(yamlString);
    } catch (YamlParser.YamlParseException e) {
      throw new YamlRenderException("Failed to parse YAML to DrawingState", e);
    }
  }

  @Override
  public String stateToYaml(DrawingState drawingState) throws YamlRenderException {
    Objects.requireNonNull(drawingState, "DrawingState cannot be null");

    try {
      return yamlParser.toYaml(drawingState);
    } catch (YamlParser.YamlParseException e) {
      throw new YamlRenderException("Failed to serialize DrawingState to YAML", e);
    }
  }

  @Override
  public ValidationResult validateYaml(String yamlString) {
    if (yamlString == null) {
      return ValidationResult.invalid("YAML string cannot be null");
    }

    if (yamlString.isBlank()) {
      return ValidationResult.invalid("YAML string cannot be empty");
    }

    // First check basic YAML syntax
    if (!yamlParser.isValidYaml(yamlString)) {
      return ValidationResult.invalid("YAML syntax is invalid");
    }

    // Check if it can be parsed to DrawingState
    try {
      DrawingState state = parseToState(yamlString);

      // Additional validation checks
      if (state == null) {
        return ValidationResult.invalid("Parsed DrawingState is null");
      }

      // Validate that the state has reasonable structure
      if (state.getShapes() == null) {
        return ValidationResult.invalid("DrawingState has null shapes list");
      }

      // Check for any obvious issues with shapes
      for (var shape : state.getShapes()) {
        if (shape == null) {
          return ValidationResult.invalid("DrawingState contains null shape");
        }
        if (shape.getId() == null || shape.getId().isBlank()) {
          return ValidationResult.invalid("Shape has null or empty ID");
        }
        if (shape.getPosition() == null) {
          return ValidationResult.invalid("Shape has null position");
        }
        if (shape.getStyle() == null) {
          return ValidationResult.invalid("Shape has null style");
        }
      }

      // Test round-trip conversion to ensure consistency
      try {
        boolean roundTripSuccess = yamlParser.testRoundTrip(state);
        if (!roundTripSuccess) {
          return ValidationResult.invalid("Round-trip conversion failed - data may be lost");
        }
      } catch (YamlParser.YamlParseException e) {
        return ValidationResult.invalid("Round-trip test failed", e);
      }

      return ValidationResult.valid("YAML is valid and ready for rendering");

    } catch (YamlRenderException e) {
      return ValidationResult.invalid("Cannot parse YAML to valid DrawingState: " + e.getMessage(),
          e);
    } catch (Exception e) {
      return ValidationResult.invalid("Unexpected validation error: " + e.getMessage(), e);
    }
  }

  @Override
  public DrawingEngine getDrawingEngine() {
    return drawingEngine;
  }

  @Override
  public YamlParser getYamlParser() {
    return yamlParser;
  }

  /**
   * Convenience method to create and render a simple test drawing
   *
   * @param canvas the canvas to render to
   * @throws YamlRenderException if rendering fails
   */
  public void renderTestDrawing(PGraphics canvas) throws YamlRenderException {
    String testYaml = generateTestYaml();
    render(testYaml, canvas);
  }

  /**
   * Generates a simple test YAML for validation
   *
   * @return YAML string with basic shapes
   */
  public String generateTestYaml() {
    try {
      // Create a simple test state with basic shapes
      DrawingState testState =
          DrawingState.builder().addSetting("width", 800).addSetting("height", 600)
              .addSetting("backgroundColor", "#FFFFFF").addSetting("title", "Test Drawing").build();

      return stateToYaml(testState);
    } catch (YamlRenderException e) {
      // Fallback to hardcoded YAML if state generation fails
      return """
          type: drawing_state
          settings:
            width: 800
            height: 600
            backgroundColor: "#FFFFFF"
            title: "Test Drawing"
          shapes: []
          arrays: {}
          lists: {}
          stateTimestamp: %d
          """.formatted(System.currentTimeMillis());
    }
  }

  /**
   * Gets diagnostic information about the renderer
   *
   * @return diagnostic information string
   */
  public String getDiagnosticInfo() {
    return String.format("""
        YamlRendererImpl Diagnostic Info:
        - Parser class: %s
        - Engine class: %s
        - Sketch class: %s
        - Current canvas: %dx%d
        """, yamlParser.getClass().getSimpleName(), drawingEngine.getClass().getSimpleName(),
        sketch.getClass().getSimpleName(), drawingEngine.getCanvas().width,
        drawingEngine.getCanvas().height);
  }
}
