package com.example.infrastructure.validation;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.infrastructure.serialization.YamlParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ValidationService provides comprehensive validation for YAML drawing instructions and
 * DrawingState objects to ensure safe and accurate rendering.
 */
public final class ValidationService {

  private static final Logger LOGGER = Logger.getLogger(ValidationService.class.getName());

  private final YamlParser yamlParser;

  // Validation constraints
  private static final float MAX_COORDINATE = 10000.0f;
  private static final float MIN_COORDINATE = -10000.0f;
  private static final float MAX_SIZE = 5000.0f;
  private static final float MIN_SIZE = 0.0f;
  private static final int MAX_SHAPES = 10000;
  private static final int MAX_ID_LENGTH = 200;

  /** Creates a new ValidationService with default YamlParser */
  public ValidationService() {
    this.yamlParser = new YamlParser();
  }

  /**
   * Creates a new ValidationService with custom YamlParser
   *
   * @param yamlParser custom YAML parser
   */
  public ValidationService(YamlParser yamlParser) {
    this.yamlParser = Objects.requireNonNull(yamlParser, "YamlParser cannot be null");
  }

  /**
   * Validates YAML string for syntax and drawing compatibility
   *
   * @param yamlString the YAML string to validate
   * @return detailed validation result
   */
  public ValidationResult validateYaml(String yamlString) {
    if (yamlString == null) {
      return ValidationResult.error("YAML string cannot be null");
    }

    if (yamlString.isBlank()) {
      return ValidationResult.error("YAML string cannot be empty");
    }

    try {
      // Step 1: Basic YAML syntax validation
      if (!yamlParser.isValidYaml(yamlString)) {
        LOGGER.log(Level.WARNING, "Invalid YAML syntax: {0}",
            yamlString.substring(0, Math.min(100, yamlString.length())));
        return ValidationResult.error("YAML syntax is invalid");
      }

      // Step 2: Parse to DrawingState
      DrawingState drawingState;
      try {
        drawingState = yamlParser.fromYaml(yamlString);
      } catch (YamlParser.YamlParseException e) {
        LOGGER.log(Level.WARNING, "Failed to parse YAML to DrawingState", e);
        return ValidationResult.error("Cannot parse YAML to DrawingState: " + e.getMessage());
      }

      // Step 3: Validate DrawingState structure
      ValidationResult stateResult = validateDrawingState(drawingState);
      if (!stateResult.isValid()) {
        return stateResult;
      }

      // Step 4: Test round-trip conversion
      try {
        boolean roundTrip = yamlParser.testRoundTrip(drawingState);
        if (!roundTrip) {
          return ValidationResult.warning("Round-trip conversion may lose data");
        }
      } catch (YamlParser.YamlParseException e) {
        LOGGER.log(Level.WARNING, "Round-trip test failed", e);
        return ValidationResult.warning("Round-trip validation failed: " + e.getMessage());
      }

      return ValidationResult.success("YAML is valid and ready for rendering");

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error during YAML validation", e);
      return ValidationResult.error("Unexpected validation error: " + e.getMessage());
    }
  }

  /**
   * Validates a DrawingState object for rendering compatibility
   *
   * @param drawingState the DrawingState to validate
   * @return validation result with details
   */
  public ValidationResult validateDrawingState(DrawingState drawingState) {
    if (drawingState == null) {
      return ValidationResult.error("DrawingState cannot be null");
    }

    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    try {
      // Validate settings
      validateSettings(drawingState, warnings, errors);

      // Validate shapes
      validateShapes(drawingState.getShapes(), warnings, errors);

      // Validate arrays and lists
      validateCollections(drawingState, warnings, errors);

      // Determine result based on errors and warnings
      if (!errors.isEmpty()) {
        return ValidationResult.error("Validation failed: " + String.join("; ", errors));
      } else if (!warnings.isEmpty()) {
        return ValidationResult
            .warning("Validation passed with warnings: " + String.join("; ", warnings));
      } else {
        return ValidationResult.success("DrawingState is valid and ready for rendering");
      }

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error during DrawingState validation", e);
      return ValidationResult.error("Unexpected validation error: " + e.getMessage());
    }
  }

  /** Validates DrawingState settings */
  private void validateSettings(DrawingState drawingState, List<String> warnings,
      List<String> errors) {
    Map<String, Object> settings = drawingState.getSettings();

    // Validate dimensions
    Object widthObj = settings.get("width");
    Object heightObj = settings.get("height");

    if (widthObj != null) {
      try {
        int width = ((Number) widthObj).intValue();
        if (width <= 0 || width > MAX_SIZE) {
          errors.add("Invalid width: " + width + " (must be 1-" + (int) MAX_SIZE + ")");
        }
      } catch (ClassCastException e) {
        errors.add("Width must be a number");
      }
    }

    if (heightObj != null) {
      try {
        int height = ((Number) heightObj).intValue();
        if (height <= 0 || height > MAX_SIZE) {
          errors.add("Invalid height: " + height + " (must be 1-" + (int) MAX_SIZE + ")");
        }
      } catch (ClassCastException e) {
        errors.add("Height must be a number");
      }
    }

    // Validate background color
    String backgroundColor = drawingState.getStringSetting("backgroundColor", null);
    if (backgroundColor != null && !isValidColorString(backgroundColor)) {
      errors.add("Invalid background color format: " + backgroundColor);
    }
  }

  /** Validates all shapes in the drawing state */
  private void validateShapes(List<Shape> shapes, List<String> warnings, List<String> errors) {
    if (shapes == null) {
      errors.add("Shapes list cannot be null");
      return;
    }

    if (shapes.size() > MAX_SHAPES) {
      errors.add("Too many shapes: " + shapes.size() + " (max: " + MAX_SHAPES + ")");
    }

    Set<String> usedIds = new HashSet<>();

    for (int i = 0; i < shapes.size(); i++) {
      Shape shape = shapes.get(i);
      ValidationResult shapeResult = validateShape(shape, i, usedIds);

      if (shapeResult.getLevel() == ValidationLevel.ERROR) {
        errors.add(shapeResult.getMessage());
      } else if (shapeResult.getLevel() == ValidationLevel.WARNING) {
        warnings.add(shapeResult.getMessage());
      }
    }
  }

  /** Validates a single shape */
  private ValidationResult validateShape(Shape shape, int index, Set<String> usedIds) {
    if (shape == null) {
      return ValidationResult.error("Shape at index " + index + " is null");
    }

    // Validate ID
    String id = shape.getId();
    if (id == null || id.isBlank()) {
      return ValidationResult.error("Shape at index " + index + " has null or empty ID");
    }

    if (id.length() > MAX_ID_LENGTH) {
      return ValidationResult
          .error("Shape ID too long: " + id.length() + " characters (max: " + MAX_ID_LENGTH + ")");
    }

    if (usedIds.contains(id)) {
      return ValidationResult.error("Duplicate shape ID: " + id);
    }
    usedIds.add(id);

    // Validate position
    Position position = shape.getPosition();
    if (position == null) {
      return ValidationResult.error("Shape " + id + " has null position");
    }

    if (!isValidCoordinate(position.getX()) || !isValidCoordinate(position.getY())) {
      return ValidationResult.error("Shape " + id + " has invalid coordinates: " + position);
    }

    // Validate style
    Style style = shape.getStyle();
    if (style == null) {
      return ValidationResult.error("Shape " + id + " has null style");
    }

    ValidationResult styleResult = validateStyle(style, id);
    if (styleResult.getLevel() == ValidationLevel.ERROR) {
      return styleResult;
    }

    // Validate shape-specific properties
    return validateShapeSpecific(shape);
  }

  /** Validates shape-specific properties */
  private ValidationResult validateShapeSpecific(Shape shape) {
    String id = shape.getId();

    switch (shape.getType()) {
      case CIRCLE:
        if (shape instanceof Circle) {
          Circle circle = (Circle) shape;
          if (circle.getRadius() < MIN_SIZE || circle.getRadius() > MAX_SIZE) {
            return ValidationResult
                .error("Circle " + id + " has invalid radius: " + circle.getRadius());
          }
        } else {
          return ValidationResult.error("Shape type mismatch for " + id + ": expected Circle");
        }
        break;

      case RECTANGLE:
        if (shape instanceof Rectangle) {
          Rectangle rect = (Rectangle) shape;
          if (rect.getWidth() < MIN_SIZE || rect.getWidth() > MAX_SIZE
              || rect.getHeight() < MIN_SIZE || rect.getHeight() > MAX_SIZE) {
            return ValidationResult.error("Rectangle " + id + " has invalid dimensions: "
                + rect.getWidth() + "x" + rect.getHeight());
          }
        } else {
          return ValidationResult.error("Shape type mismatch for " + id + ": expected Rectangle");
        }
        break;

      case LINE:
        if (shape instanceof Line) {
          Line line = (Line) shape;
          Position start = line.getStartPosition();
          Position end = line.getEndPosition();
          if (start == null || end == null) {
            return ValidationResult.error("Line " + id + " has null start or end position");
          }
          if (!isValidCoordinate(start.getX()) || !isValidCoordinate(start.getY())
              || !isValidCoordinate(end.getX()) || !isValidCoordinate(end.getY())) {
            return ValidationResult.error("Line " + id + " has invalid coordinates");
          }
        } else {
          return ValidationResult.error("Shape type mismatch for " + id + ": expected Line");
        }
        break;

      case TEXT:
        if (shape instanceof Text) {
          Text text = (Text) shape;
          if (text.getContent() == null) {
            return ValidationResult.error("Text " + id + " has null content");
          }
          if (text.getContent().length() > 1000) {
            return ValidationResult.warning("Text " + id + " has very long content ("
                + text.getContent().length() + " characters)");
          }
        } else {
          return ValidationResult.error("Shape type mismatch for " + id + ": expected Text");
        }
        break;

      default:
        return ValidationResult.warning("Unknown shape type for " + id + ": " + shape.getType());
    }

    return ValidationResult.success("Shape " + id + " is valid");
  }

  /** Validates a style object */
  private ValidationResult validateStyle(Style style, String shapeId) {
    if (style.hasFill() && style.getFillColor() == null) {
      return ValidationResult.error("Shape " + shapeId + " has fill enabled but null fill color");
    }

    if (style.hasStroke() && style.getStrokeColor() == null) {
      return ValidationResult
          .error("Shape " + shapeId + " has stroke enabled but null stroke color");
    }

    if (style.hasStroke() && (style.getStrokeWeight() < 0 || style.getStrokeWeight() > 100)) {
      return ValidationResult
          .warning("Shape " + shapeId + " has unusual stroke weight: " + style.getStrokeWeight());
    }

    return ValidationResult.success("Style is valid");
  }

  /** Validates collections (arrays and lists) in drawing state */
  private void validateCollections(DrawingState drawingState, List<String> warnings,
      List<String> errors) {
    // Currently arrays and lists are generic, so minimal validation
    Map<String, List<Object>> arrays = drawingState.getArrays();
    Map<String, List<Object>> lists = drawingState.getLists();

    if (arrays.size() > 100) {
      warnings.add("Large number of arrays: " + arrays.size());
    }

    if (lists.size() > 100) {
      warnings.add("Large number of lists: " + lists.size());
    }
  }

  /** Validates coordinate values */
  private boolean isValidCoordinate(float coordinate) {
    return !Float.isNaN(coordinate) && !Float.isInfinite(coordinate) && coordinate >= MIN_COORDINATE
        && coordinate <= MAX_COORDINATE;
  }

  /** Validates color string format */
  private boolean isValidColorString(String colorString) {
    try {
      Color.fromHex(colorString);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Validation result with different severity levels */
  public static class ValidationResult {
    private final ValidationLevel level;
    private final String message;
    private final Exception cause;

    private ValidationResult(ValidationLevel level, String message, Exception cause) {
      this.level = level;
      this.message = message;
      this.cause = cause;
    }

    public ValidationLevel getLevel() {
      return level;
    }

    public String getMessage() {
      return message;
    }

    public Exception getCause() {
      return cause;
    }

    public boolean isValid() {
      return level != ValidationLevel.ERROR;
    }

    public static ValidationResult success(String message) {
      return new ValidationResult(ValidationLevel.SUCCESS, message, null);
    }

    public static ValidationResult warning(String message) {
      return new ValidationResult(ValidationLevel.WARNING, message, null);
    }

    public static ValidationResult error(String message) {
      return new ValidationResult(ValidationLevel.ERROR, message, null);
    }

    public static ValidationResult error(String message, Exception cause) {
      return new ValidationResult(ValidationLevel.ERROR, message, cause);
    }

    @Override
    public String toString() {
      return String.format("ValidationResult(%s: %s)", level, message);
    }
  }

  /** Validation severity levels */
  public enum ValidationLevel {
    SUCCESS, WARNING, ERROR
  }
}
