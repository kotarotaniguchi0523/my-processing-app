package com.example.domain.agents.adk;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for agent output and session state. Single responsibility: Parse and normalize agent
 * outputs.
 */
public class AgentOutputParser {

  private static final Logger LOGGER = Logger.getLogger(AgentOutputParser.class.getName());
  private static final Gson gson = new Gson();

  /**
   * Parse agent output and extract key-value pairs into state map
   */
  public static void parseAgentOutput(String output, Map<String, Object> state) {
    if (output == null || output.trim().isEmpty()) {
      return;
    }

    // Pattern to match key = value or key: value
    Pattern pattern = Pattern.compile("(\\w+(?::\\w+)?)\\s*[=:]\\s*(.+)");

    for (String line : output.split("\n")) {
      Matcher matcher = pattern.matcher(line.trim());
      if (matcher.find()) {
        String key = matcher.group(1).trim();
        String rawValue = matcher.group(2).trim();

        // Skip empty values
        if (rawValue.isEmpty()) {
          continue;
        }

        try {
          Object value = parseValue(rawValue);
          state.put(key, value);
          LOGGER.fine("Parsed state: " + key + " = " + value);
        } catch (Exception e) {
          LOGGER.warning("Failed to parse value for key " + key + ": " + rawValue);
          // Store as string if parsing fails
          state.put(key, rawValue);
        }
      }
    }
  }

  /**
   * Parse a value string into appropriate type
   */
  private static Object parseValue(String rawValue) {
    // Remove surrounding quotes if present
    if ((rawValue.startsWith("\"") && rawValue.endsWith("\""))
        || (rawValue.startsWith("'") && rawValue.endsWith("'"))) {
      rawValue = rawValue.substring(1, rawValue.length() - 1);
    }

    // Try to parse as JSON first
    if (rawValue.startsWith("[") || rawValue.startsWith("{")) {
      try {
        JsonElement element = JsonParser.parseString(rawValue);

        if (element.isJsonArray()) {
          // Parse as list
          return gson.fromJson(element, new TypeToken<List<Object>>() {}.getType());
        } else if (element.isJsonObject()) {
          // Parse as map
          return gson.fromJson(element, new TypeToken<Map<String, Object>>() {}.getType());
        }
      } catch (JsonSyntaxException e) {
        LOGGER.fine("Value is not valid JSON, parsing as string: " + rawValue);
      }
    }

    // Try to parse as boolean
    if ("true".equalsIgnoreCase(rawValue)) {
      return true;
    }
    if ("false".equalsIgnoreCase(rawValue)) {
      return false;
    }

    // Try to parse as number
    try {
      if (rawValue.contains(".")) {
        return Double.parseDouble(rawValue);
      } else {
        return Integer.parseInt(rawValue);
      }
    } catch (NumberFormatException e) {
      // Not a number, continue
    }

    // Default to string
    return rawValue;
  }

  /**
   * Normalize a tool list from various formats
   */
  public static List<String> normalizeToolList(Object rawTools) {
    if (rawTools == null) {
      return new ArrayList<>();
    }

    if (rawTools instanceof List) {
      List<String> tools = new ArrayList<>();
      for (Object item : (List<?>) rawTools) {
        if (item != null) {
          tools.add(item.toString());
        }
      }
      return tools;
    }

    if (rawTools instanceof String) {
      String toolString = (String) rawTools;
      // Try parsing as JSON array
      if (toolString.startsWith("[")) {
        try {
          return gson.fromJson(toolString, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
          LOGGER.warning("Failed to parse tools as JSON: " + toolString);
        }
      }
      // Single tool
      return List.of(toolString);
    }

    LOGGER.warning("Unexpected tools format: " + rawTools.getClass());
    return new ArrayList<>();
  }

  /**
   * Normalize a parameter list from various formats
   */
  public static List<Map<String, Object>> normalizeParameterList(Object rawParams) {
    if (rawParams == null) {
      return new ArrayList<>();
    }

    if (rawParams instanceof List) {
      List<Map<String, Object>> params = new ArrayList<>();
      for (Object item : (List<?>) rawParams) {
        Map<String, Object> paramMap = convertParamEntry(item);
        if (paramMap != null) {
          params.add(paramMap);
        }
      }
      return params;
    }

    if (rawParams instanceof String) {
      String paramString = (String) rawParams;
      // Try parsing as JSON array
      if (paramString.startsWith("[")) {
        try {
          return gson.fromJson(paramString,
              new TypeToken<List<Map<String, Object>>>() {}.getType());
        } catch (Exception e) {
          LOGGER.warning("Failed to parse parameters as JSON: " + paramString);
        }
      }
    }

    LOGGER.warning("Unexpected parameters format: " + rawParams.getClass());
    return new ArrayList<>();
  }

  /**
   * Convert a parameter entry to a Map
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> convertParamEntry(Object item) {
    if (item instanceof Map) {
      return (Map<String, Object>) item;
    }

    if (item instanceof String) {
      String paramString = (String) item;
      if (paramString.startsWith("{")) {
        try {
          return gson.fromJson(paramString, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
          LOGGER.warning("Failed to parse parameter as JSON: " + paramString);
        }
      }
    }

    LOGGER.warning("Cannot convert parameter entry: " + item);
    return null;
  }
}
