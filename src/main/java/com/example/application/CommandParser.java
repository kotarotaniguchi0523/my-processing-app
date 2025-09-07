package com.example.application;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses simple drawing commands from natural language input. Supports basic shapes like circles
 * and rectangles with position and size parameters.
 */
public class CommandParser {
  private static final Logger LOGGER = Logger.getLogger(CommandParser.class.getName());

  // Regular expressions for command parsing
  private static final Pattern CIRCLE_PATTERN =
      Pattern.compile(
          "(?i)(?:draw\\s+)?(?:a\\s+)?circle\\s+"
              + "(?:at\\s+)?(?:position\\s+)?(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?"
              + "(?:with\\s+)?(?:diameter\\s+|size\\s+|radius\\s+)?(\\d+)"
              + "(?:\\s+(?:in\\s+|color\\s+)(\\w+))?");

  private static final Pattern RECTANGLE_PATTERN =
      Pattern.compile(
          "(?i)(?:draw\\s+)?(?:a\\s+)?(?:rectangle|rect|square)\\s+"
              + "(?:at\\s+)?(?:position\\s+)?(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?"
              + "(?:with\\s+)?(?:size\\s+|width\\s+)(\\d+)(?:\\s*[x,]\\s*(\\d+))?"
              + "(?:\\s+(?:in\\s+|color\\s+)(\\w+))?");

  private static final Pattern CLEAR_PATTERN =
      Pattern.compile("(?i)(?:clear|clean)(?:\\s+(?:canvas|screen|everything))?");

  // Color name mappings
  private static final Map<String, String> COLOR_NAMES = new HashMap<>();

  static {
    COLOR_NAMES.put("red", "#FF0000");
    COLOR_NAMES.put("green", "#00FF00");
    COLOR_NAMES.put("blue", "#0000FF");
    COLOR_NAMES.put("yellow", "#FFFF00");
    COLOR_NAMES.put("purple", "#800080");
    COLOR_NAMES.put("orange", "#FFA500");
    COLOR_NAMES.put("pink", "#FFC0CB");
    COLOR_NAMES.put("brown", "#A52A2A");
    COLOR_NAMES.put("gray", "#808080");
    COLOR_NAMES.put("grey", "#808080");
    COLOR_NAMES.put("black", "#000000");
    COLOR_NAMES.put("white", "#FFFFFF");
  }

  /**
   * Parse a command string into a ParsedCommand object.
   *
   * @param input The user input to parse
   * @return ParsedCommand object with parsed data or error information
   */
  public ParsedCommand parse(String input) {
    if (input == null || input.trim().isEmpty()) {
      return new ParsedCommand(input, "Empty command");
    }

    String trimmedInput = input.trim();
    LOGGER.fine("Parsing command: " + trimmedInput);

    // Try to parse as circle command
    ParsedCommand circleResult = parseCircle(trimmedInput);
    if (circleResult.isValid()) {
      return circleResult;
    }

    // Try to parse as rectangle command
    ParsedCommand rectangleResult = parseRectangle(trimmedInput);
    if (rectangleResult.isValid()) {
      return rectangleResult;
    }

    // Try to parse as clear command
    ParsedCommand clearResult = parseClear(trimmedInput);
    if (clearResult.isValid()) {
      return clearResult;
    }

    // Could not parse as a simple command
    return new ParsedCommand(input, "Could not parse as a simple drawing command");
  }

  /** Parse circle command. Examples: "circle 100,200 diameter 60", "red circle at 50,50 size 40" */
  private ParsedCommand parseCircle(String input) {
    Matcher matcher = CIRCLE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract coordinates (optional)
      String xStr = matcher.group(1);
      String yStr = matcher.group(2);
      if (xStr != null && yStr != null) {
        params.put("x", xStr);
        params.put("y", yStr);
      }

      // Extract diameter/size
      String sizeStr = matcher.group(3);
      if (sizeStr != null) {
        params.put("diameter", sizeStr);
      }

      // Extract color (optional)
      String colorStr = matcher.group(4);
      if (colorStr != null) {
        String colorValue = COLOR_NAMES.getOrDefault(colorStr.toLowerCase(), colorStr);
        params.put("color", colorValue);
      }

      LOGGER.fine("Parsed circle command: " + params);
      return new ParsedCommand("circle", params, input);
    }

    return new ParsedCommand(input, "Not a circle command");
  }

  /**
   * Parse rectangle command. Examples: "rectangle 100,100 size 80x60", "blue rect at 200,150 width
   * 100"
   */
  private ParsedCommand parseRectangle(String input) {
    Matcher matcher = RECTANGLE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract coordinates (optional)
      String xStr = matcher.group(1);
      String yStr = matcher.group(2);
      if (xStr != null && yStr != null) {
        params.put("x", xStr);
        params.put("y", yStr);
      }

      // Extract width
      String widthStr = matcher.group(3);
      if (widthStr != null) {
        params.put("width", widthStr);
      }

      // Extract height (optional, defaults to width for square)
      String heightStr = matcher.group(4);
      if (heightStr != null) {
        params.put("height", heightStr);
      } else if (widthStr != null) {
        // Square - height equals width
        params.put("height", widthStr);
      }

      // Extract color (optional)
      String colorStr = matcher.group(5);
      if (colorStr != null) {
        String colorValue = COLOR_NAMES.getOrDefault(colorStr.toLowerCase(), colorStr);
        params.put("color", colorValue);
      }

      LOGGER.fine("Parsed rectangle command: " + params);
      return new ParsedCommand("rectangle", params, input);
    }

    return new ParsedCommand(input, "Not a rectangle command");
  }

  /** Parse clear command. Examples: "clear", "clear canvas", "clean screen" */
  private ParsedCommand parseClear(String input) {
    Matcher matcher = CLEAR_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();
      LOGGER.fine("Parsed clear command");
      return new ParsedCommand("clear", params, input);
    }

    return new ParsedCommand(input, "Not a clear command");
  }

  /**
   * Check if input looks like a drawing command. Useful for determining whether to send to AI agent
   * or try parsing first.
   */
  public boolean looksLikeDrawingCommand(String input) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }

    String lower = input.toLowerCase().trim();

    // Check for drawing-related keywords
    return lower.contains("circle")
        || lower.contains("rectangle")
        || lower.contains("rect")
        || lower.contains("square")
        || lower.contains("clear")
        || lower.contains("draw")
        || lower.matches(".*\\d+\\s*,\\s*\\d+.*"); // Contains coordinates
  }
}
