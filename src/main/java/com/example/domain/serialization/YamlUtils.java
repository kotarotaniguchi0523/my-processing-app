package com.example.domain.serialization;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility class for YAML serialization operations Provides common serialization helpers and
 * validation
 */
public final class YamlUtils {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private YamlUtils() {
    // Utility class - prevent instantiation
  }

  // String utilities
  public static String getString(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value instanceof String ? (String) value : defaultValue;
  }

  public static String getRequiredString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof String)) {
      throw new IllegalArgumentException(
          "Required string field '" + key + "' is missing or invalid");
    }
    return (String) value;
  }

  // Number utilities
  public static int getInt(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return defaultValue;
  }

  public static int getRequiredInt(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("Required int field '" + key + "' is missing or invalid");
    }
    return ((Number) value).intValue();
  }

  public static float getFloat(Map<String, Object> map, String key, float defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).floatValue();
    }
    return defaultValue;
  }

  public static float getRequiredFloat(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException(
          "Required float field '" + key + "' is missing or invalid");
    }
    return ((Number) value).floatValue();
  }

  public static long getLong(Map<String, Object> map, String key, long defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return defaultValue;
  }

  // Boolean utilities
  public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
    Object value = map.get(key);
    return value instanceof Boolean ? (Boolean) value : defaultValue;
  }

  public static boolean getRequiredBoolean(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof Boolean)) {
      throw new IllegalArgumentException(
          "Required boolean field '" + key + "' is missing or invalid");
    }
    return (Boolean) value;
  }

  // List utilities
  @SuppressWarnings("unchecked")
  public static List<Object> getList(
      Map<String, Object> map, String key, List<Object> defaultValue) {
    Object value = map.get(key);
    return value instanceof List ? (List<Object>) value : defaultValue;
  }

  @SuppressWarnings("unchecked")
  public static List<Object> getRequiredList(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof List)) {
      throw new IllegalArgumentException("Required list field '" + key + "' is missing or invalid");
    }
    return (List<Object>) value;
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> getMapList(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof List)) {
      return new ArrayList<>();
    }

    List<Map<String, Object>> result = new ArrayList<>();
    List<?> list = (List<?>) value;
    for (Object item : list) {
      if (item instanceof Map) {
        result.add((Map<String, Object>) item);
      }
    }
    return result;
  }

  // Map utilities
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getMap(
      Map<String, Object> map, String key, Map<String, Object> defaultValue) {
    Object value = map.get(key);
    return value instanceof Map ? (Map<String, Object>) value : defaultValue;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getRequiredMap(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("Required map field '" + key + "' is missing or invalid");
    }
    return (Map<String, Object>) value;
  }

  // DateTime utilities
  public static String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
  }

  public static LocalDateTime parseDateTime(String dateTimeString) {
    if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid date-time format: " + dateTimeString, e);
    }
  }

  public static LocalDateTime getDateTime(
      Map<String, Object> map, String key, LocalDateTime defaultValue) {
    String dateTimeString = getString(map, key, null);
    return dateTimeString != null ? parseDateTime(dateTimeString) : defaultValue;
  }

  // Validation utilities
  public static void validateRequiredFields(Map<String, Object> map, String... requiredFields) {
    List<String> missingFields = new ArrayList<>();
    for (String field : requiredFields) {
      if (!map.containsKey(field) || map.get(field) == null) {
        missingFields.add(field);
      }
    }
    if (!missingFields.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing required fields: " + String.join(", ", missingFields));
    }
  }

  public static void validateYamlType(Map<String, Object> map, String expectedType) {
    String actualType = getString(map, "type", null);
    if (!Objects.equals(expectedType, actualType)) {
      throw new IllegalArgumentException(
          "Expected YAML type '" + expectedType + "' but got '" + actualType + "'");
    }
  }

  // Enum utilities
  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> E getEnum(
      Map<String, Object> map, String key, Class<E> enumClass, E defaultValue) {
    String enumString = getString(map, key, null);
    if (enumString == null) {
      return defaultValue;
    }
    try {
      return Enum.valueOf(enumClass, enumString.toUpperCase());
    } catch (IllegalArgumentException e) {
      return defaultValue;
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> E getRequiredEnum(
      Map<String, Object> map, String key, Class<E> enumClass) {
    String enumString = getRequiredString(map, key);
    try {
      return Enum.valueOf(enumClass, enumString.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid enum value '" + enumString + "' for " + enumClass.getSimpleName());
    }
  }

  // Collection conversion utilities
  public static List<Map<String, Object>> serializeList(List<? extends YamlSerializable> items) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (YamlSerializable item : items) {
      result.add(item.toYamlMap());
    }
    return result;
  }

  public static Map<String, List<Object>> serializeMapOfLists(Map<String, List<Object>> original) {
    Map<String, List<Object>> result = new HashMap<>();
    for (Map.Entry<String, List<Object>> entry : original.entrySet()) {
      result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return result;
  }

  // Create a basic YAML map with type and common fields
  public static Map<String, Object> createYamlMap(String type) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", type);
    map.put("timestamp", formatDateTime(LocalDateTime.now()));
    return map;
  }

  // Null-safe operations
  public static Object putIfNotNull(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      return map.put(key, value);
    }
    return null;
  }

  // Deep copy a map for safe modification
  @SuppressWarnings("unchecked")
  public static Map<String, Object> deepCopy(Map<String, Object> original) {
    Map<String, Object> copy = new HashMap<>();
    for (Map.Entry<String, Object> entry : original.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        copy.put(entry.getKey(), deepCopy((Map<String, Object>) value));
      } else if (value instanceof List) {
        copy.put(entry.getKey(), new ArrayList<>((List<?>) value));
      } else {
        copy.put(entry.getKey(), value);
      }
    }
    return copy;
  }
}
