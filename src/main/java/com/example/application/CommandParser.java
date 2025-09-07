package com.example.application;

import java.util.HashMap;
import java.util.Locale;
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
      Pattern.compile("(?i)(?:draw\\s+)?(?:a\\s+)?" + "(?:(\\w+)\\s+)?circle\\s+" // Color prefix
      // (e.g., "red
      // circle")
          + "(?:at\\s+)?(?:position\\s+)?(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?"
          + "(?:with\\s+)?(?:diameter\\s+|size\\s+|radius\\s+)?(\\d+)"
          + "(?:\\s+(?:in\\s+|color\\s+)(\\w+))?"); // Color suffix (e.g., "circle
  // in red")

  // Japanese circle pattern - matches "赤い円 100,200 直径60"
  private static final Pattern JAPANESE_CIRCLE_PATTERN = Pattern
      .compile("(赤い|青い|緑の|黄色い|黒い|白い)?円\\s+" + "(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?" + "(?:直径|半径)(\\d+)");

  private static final Pattern RECTANGLE_PATTERN =
      Pattern.compile("(?i)(?:draw\\s+)?(?:a\\s+)?(?:rectangle|rect|square)\\s+"
          + "(?:at\\s+)?(?:position\\s+)?(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?"
          + "(?:with\\s+)?(?:size\\s+|width\\s+)(\\d+)(?:\\s*[x,]\\s*(\\d+))?"
          + "(?:\\s+(?:in\\s+|color\\s+)(\\w+))?");

  // Japanese rectangle pattern - matches "赤い四角 100,200 幅60 高さ40"
  private static final Pattern JAPANESE_RECTANGLE_PATTERN =
      Pattern.compile("(赤い|青い|緑の|黄色い|黒い|白い)?(?:四角|長方形)\\s+" + "(?:(\\d+)\\s*,\\s*(\\d+)\\s+)?"
          + "(?:幅(\\d+))(?:\\s+高さ(\\d+))?");

  private static final Pattern CLEAR_PATTERN =
      Pattern.compile("(?i)(?:clear|clean)(?:\\s+(?:canvas|screen|everything))?");

  // File operation patterns
  private static final Pattern SAVE_PATTERN = Pattern.compile("(?i)save(?:\\s+([\\w\\-_.]+))?");

  private static final Pattern LOAD_PATTERN = Pattern.compile("(?i)load(?:\\s+([\\w\\-_.]+))");

  private static final Pattern LIST_PATTERN =
      Pattern.compile("(?i)(?:list|ls)(?:\\s+(?:saved?|files?))?");

  private static final Pattern MODIFY_PATTERN =
      Pattern.compile("(?i)modify\\s+(\\w+)\\s+(\\w+)\\s+(.+)");

  // Color name mappings - English and Japanese
  private static final Map<String, String> COLOR_NAMES = new HashMap<>();
  private static final Map<String, String> JAPANESE_COLOR_NAMES = new HashMap<>();

  static {
    // English color names
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

    // Japanese color names
    JAPANESE_COLOR_NAMES.put("赤い", "#FF0000");
    JAPANESE_COLOR_NAMES.put("青い", "#0000FF");
    JAPANESE_COLOR_NAMES.put("緑の", "#00FF00");
    JAPANESE_COLOR_NAMES.put("黄色い", "#FFFF00");
    JAPANESE_COLOR_NAMES.put("黒い", "#000000");
    JAPANESE_COLOR_NAMES.put("白い", "#FFFFFF");
  }

  /**
   * Parse a command string into a ParsedCommand object.
   *
   * @param input The user input to parse
   * @return ParsedCommand object with parsed data or error information
   */
  public ParsedCommand parse(String input) {
    if (input == null || input.isBlank()) {
      return new ParsedCommand(input, "Empty command");
    }

    String trimmedInput = input.trim();
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Parsing command: " + trimmedInput);
    }

    // Try to parse as circle command (English)
    ParsedCommand circleResult = parseCircle(trimmedInput);
    if (circleResult.isValid()) {
      return circleResult;
    }

    // Try to parse as Japanese circle command
    ParsedCommand japaneseCircleResult = parseJapaneseCircle(trimmedInput);
    if (japaneseCircleResult.isValid()) {
      return japaneseCircleResult;
    }

    // Try to parse as rectangle command (English)
    ParsedCommand rectangleResult = parseRectangle(trimmedInput);
    if (rectangleResult.isValid()) {
      return rectangleResult;
    }

    // Try to parse as Japanese rectangle command
    ParsedCommand japaneseRectangleResult = parseJapaneseRectangle(trimmedInput);
    if (japaneseRectangleResult.isValid()) {
      return japaneseRectangleResult;
    }

    // Try to parse as clear command
    ParsedCommand clearResult = parseClear(trimmedInput);
    if (clearResult.isValid()) {
      return clearResult;
    }

    // Try to parse as modify command
    ParsedCommand modifyResult = parseModify(trimmedInput);
    if (modifyResult.isValid()) {
      return modifyResult;
    }

    // Try to parse as save command
    ParsedCommand saveResult = parseSave(trimmedInput);
    if (saveResult.isValid()) {
      return saveResult;
    }

    // Try to parse as load command
    ParsedCommand loadResult = parseLoad(trimmedInput);
    if (loadResult.isValid()) {
      return loadResult;
    }

    // Try to parse as list command
    ParsedCommand listResult = parseList(trimmedInput);
    if (listResult.isValid()) {
      return listResult;
    }

    // Could not parse as a simple command
    return new ParsedCommand(input, "Could not parse as a simple drawing command");
  }

  /**
   * Parse circle command. Examples: "circle 100,200 diameter 60", "red circle at 50,50 size 40"
   */
  private ParsedCommand parseCircle(String input) {
    Matcher matcher = CIRCLE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract color prefix (optional) - group 1
      String colorPrefixStr = matcher.group(1);

      // Extract coordinates (optional) - groups 2 and 3
      String xStr = matcher.group(2);
      String yStr = matcher.group(3);
      if (xStr != null && yStr != null) {
        params.put("x", xStr);
        params.put("y", yStr);
      }

      // Extract diameter/size - group 4
      String sizeStr = matcher.group(4);
      if (sizeStr != null) {
        params.put("diameter", sizeStr);
      }

      // Extract color suffix (optional) - group 5
      String colorSuffixStr = matcher.group(5);

      // Determine final color (prefix takes priority)
      String colorStr = colorPrefixStr != null ? colorPrefixStr : colorSuffixStr;
      if (colorStr != null) {
        String colorValue = COLOR_NAMES.getOrDefault(colorStr.toLowerCase(Locale.ROOT), colorStr);
        params.put("color", colorValue);
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed circle command: " + params);
      }
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
        String colorValue = COLOR_NAMES.getOrDefault(colorStr.toLowerCase(Locale.ROOT), colorStr);
        params.put("color", colorValue);
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed rectangle command: " + params);
      }
      return new ParsedCommand("rectangle", params, input);
    }

    return new ParsedCommand(input, "Not a rectangle command");
  }

  /** Parse clear command. Examples: "clear", "clear canvas", "clean screen" */
  private ParsedCommand parseClear(String input) {
    Matcher matcher = CLEAR_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed clear command");
      }
      return new ParsedCommand("clear", params, input);
    }

    return new ParsedCommand(input, "Not a clear command");
  }

  /**
   * Parse modify command. Examples: "modify 1 color blue", "modify shape2 position 200,300",
   * "modify 3 size 100"
   */
  private ParsedCommand parseModify(String input) {
    Matcher matcher = MODIFY_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract shape ID
      String shapeId = matcher.group(1);
      if (shapeId != null) {
        params.put("id", shapeId);
      }

      // Extract property type
      String property = matcher.group(2);
      if (property != null) {
        params.put("property", property.toLowerCase(Locale.ROOT));
      }

      // Extract value and parse based on property type
      String value = matcher.group(3);
      if (value != null && property != null) {
        parsePropertyValue(params, property.toLowerCase(Locale.ROOT), value.trim());
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed modify command: " + params);
      }
      return new ParsedCommand("modify", params, input);
    }

    return new ParsedCommand(input, "Not a modify command");
  }

  /** Parse property-specific values for modify commands. */
  private void parsePropertyValue(Map<String, String> params, String property, String value) {
    switch (property) {
      case "color":
        // Parse color value - can be color name or hex
        String colorValue = COLOR_NAMES.getOrDefault(value.toLowerCase(Locale.ROOT), value);
        params.put("color", colorValue);
        break;

      case "position":
        // Parse position as "x,y"
        String[] positionParts = value.split(",");
        if (positionParts.length == 2) {
          try {
            String x = positionParts[0].trim();
            String y = positionParts[1].trim();
            params.put("x", x);
            params.put("y", y);
          } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
              LOGGER.warning("Invalid position format: " + value);
            }
          }
        } else {
          if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
            LOGGER.warning("Invalid position format, expected 'x,y': " + value);
          }
        }
        break;

      case "size":
        // Parse size - can be single value or "width,height"
        String[] sizeParts = value.split("[,x]");
        if (sizeParts.length == 1) {
          // Single value - applies to both width and height (for square/circle radius)
          params.put("size", sizeParts[0].trim());
        } else if (sizeParts.length == 2) {
          // Width and height
          params.put("width", sizeParts[0].trim());
          params.put("height", sizeParts[1].trim());
        } else {
          if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
            LOGGER.warning("Invalid size format: " + value);
          }
        }
        break;

      default:
        if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
          LOGGER.warning("Unknown property type: " + property);
        }
        // Still store the raw value for unknown properties
        params.put("value", value);
    }
  }

  /** Parse Japanese circle command. Examples: "赤い円 100,200 直径60", "円 50,50 半径30" */
  private ParsedCommand parseJapaneseCircle(String input) {
    Matcher matcher = JAPANESE_CIRCLE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract color (optional)
      String colorStr = matcher.group(1);
      if (colorStr != null) {
        String colorValue = JAPANESE_COLOR_NAMES.get(colorStr);
        if (colorValue != null) {
          params.put("color", colorValue);
        }
      }

      // Extract coordinates (optional)
      String xStr = matcher.group(2);
      String yStr = matcher.group(3);
      if (xStr != null && yStr != null) {
        params.put("x", xStr);
        params.put("y", yStr);
      }

      // Extract diameter/radius
      String sizeStr = matcher.group(4);
      if (sizeStr != null) {
        // Check if it was 半径 (radius) or 直径 (diameter)
        if (input.contains("半径")) {
          // Convert radius to diameter
          int radius = Integer.parseInt(sizeStr);
          params.put("diameter", String.valueOf(radius * 2));
        } else {
          params.put("diameter", sizeStr);
        }
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed Japanese circle command: " + params);
      }
      return new ParsedCommand("circle", params, input);
    }

    return new ParsedCommand(input, "Not a Japanese circle command");
  }

  /** Parse Japanese rectangle command. Examples: "赤い四角 100,200 幅60 高さ40", "長方形 50,50 幅80" */
  private ParsedCommand parseJapaneseRectangle(String input) {
    Matcher matcher = JAPANESE_RECTANGLE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract color (optional)
      String colorStr = matcher.group(1);
      if (colorStr != null) {
        String colorValue = JAPANESE_COLOR_NAMES.get(colorStr);
        if (colorValue != null) {
          params.put("color", colorValue);
        }
      }

      // Extract coordinates (optional)
      String xStr = matcher.group(2);
      String yStr = matcher.group(3);
      if (xStr != null && yStr != null) {
        params.put("x", xStr);
        params.put("y", yStr);
      }

      // Extract width
      String widthStr = matcher.group(4);
      if (widthStr != null) {
        params.put("width", widthStr);
      }

      // Extract height (optional, defaults to width for square)
      String heightStr = matcher.group(5);
      if (heightStr != null) {
        params.put("height", heightStr);
      } else if (widthStr != null) {
        // Square - height equals width
        params.put("height", widthStr);
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed Japanese rectangle command: " + params);
      }
      return new ParsedCommand("rectangle", params, input);
    }

    return new ParsedCommand(input, "Not a Japanese rectangle command");
  }

  /** Parse save command. Examples: "save", "save myfile" */
  private ParsedCommand parseSave(String input) {
    Matcher matcher = SAVE_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract filename (optional)
      String filename = matcher.group(1);
      if (filename != null) {
        params.put("filename", filename);
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed save command: " + params);
      }
      return new ParsedCommand("save", params, input);
    }

    return new ParsedCommand(input, "Not a save command");
  }

  /** Parse load command. Examples: "load myfile" */
  private ParsedCommand parseLoad(String input) {
    Matcher matcher = LOAD_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();

      // Extract filename (required)
      String filename = matcher.group(1);
      if (filename != null) {
        params.put("filename", filename);
      }

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed load command: " + params);
      }
      return new ParsedCommand("load", params, input);
    }

    return new ParsedCommand(input, "Not a load command");
  }

  /** Parse list command. Examples: "list", "ls", "list files" */
  private ParsedCommand parseList(String input) {
    Matcher matcher = LIST_PATTERN.matcher(input);

    if (matcher.find()) {
      Map<String, String> params = new HashMap<>();
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Parsed list command");
      }
      return new ParsedCommand("list", params, input);
    }

    return new ParsedCommand(input, "Not a list command");
  }

  /**
   * Check if input looks like a drawing command. Useful for determining whether to send to AI agent
   * or try parsing first.
   */
  public boolean looksLikeDrawingCommand(String input) {
    if (input == null || input.isBlank()) {
      return false;
    }

    String lower = input.toLowerCase(Locale.ROOT).trim();

    // Check for English drawing-related keywords
    boolean hasEnglishKeywords = lower.contains("circle") || lower.contains("rectangle")
        || lower.contains("rect") || lower.contains("square") || lower.contains("clear")
        || lower.contains("draw") || lower.contains("modify");

    // Check for Japanese drawing-related keywords
    boolean hasJapaneseKeywords =
        input.contains("円") || input.contains("四角") || input.contains("長方形") || input.contains("直径")
            || input.contains("半径") || input.contains("幅") || input.contains("高さ")
            || input.contains("赤い") || input.contains("青い") || input.contains("緑の")
            || input.contains("黄色い") || input.contains("黒い") || input.contains("白い");

    // Check for coordinates pattern
    boolean hasCoordinates = lower.matches(".*\\d+\\s*,\\s*\\d+.*");

    return hasEnglishKeywords || hasJapaneseKeywords || hasCoordinates;
  }
}
