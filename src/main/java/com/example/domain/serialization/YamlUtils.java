package com.example.domain.serialization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for YAML serialization and deserialization of domain objects.
 */
public class YamlUtils {

    /**
     * Creates a new YAML map with a specified type.
     */
    public static Map<String, Object> createYamlMap(String type) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        return map;
    }

    /**
     * Validates that a YAML map has the expected type.
     */
    public static void validateYamlType(Map<String, Object> yamlMap, String expectedType) {
        String type = (String) yamlMap.get("type");
        if (!expectedType.equals(type)) {
            throw new IllegalArgumentException(
                String.format("Expected YAML type '%s' but got '%s'", expectedType, type));
        }
    }

    /**
     * Validates that required fields are present in the YAML map.
     */
    public static void validateRequiredFields(Map<String, Object> yamlMap, String... fields) {
        for (String field : fields) {
            if (!yamlMap.containsKey(field)) {
                throw new IllegalArgumentException(
                    String.format("Required field '%s' is missing", field));
            }
        }
    }

    /**
     * Gets a required string value from the YAML map.
     */
    public static String getRequiredString(Map<String, Object> yamlMap, String key) {
        Object value = yamlMap.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Required field '%s' is missing", key));
        }
        return value.toString();
    }

    /**
     * Gets an optional string value from the YAML map.
     */
    public static String getString(Map<String, Object> yamlMap, String key, String defaultValue) {
        Object value = yamlMap.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Gets a required float value from the YAML map.
     */
    public static float getRequiredFloat(Map<String, Object> yamlMap, String key) {
        Object value = yamlMap.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Required field '%s' is missing", key));
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    /**
     * Gets an optional float value from the YAML map.
     */
    public static float getFloat(Map<String, Object> yamlMap, String key, float defaultValue) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    /**
     * Gets a required int value from the YAML map.
     */
    public static int getRequiredInt(Map<String, Object> yamlMap, String key) {
        Object value = yamlMap.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Required field '%s' is missing", key));
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * Gets an optional int value from the YAML map.
     */
    public static int getInt(Map<String, Object> yamlMap, String key, int defaultValue) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * Gets an optional boolean value from the YAML map.
     */
    public static boolean getBoolean(Map<String, Object> yamlMap, String key, boolean defaultValue) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Gets a required map value from the YAML map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getRequiredMap(Map<String, Object> yamlMap, String key) {
        Object value = yamlMap.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                String.format("Required field '%s' is missing", key));
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(
                String.format("Field '%s' must be a map", key));
        }
        return (Map<String, Object>) value;
    }

    /**
     * Gets an optional map value from the YAML map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> yamlMap, String key, Map<String, Object> defaultValue) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Map)) {
            return defaultValue;
        }
        return (Map<String, Object>) value;
    }

    /**
     * Gets a list value from the YAML map.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getList(Map<String, Object> yamlMap, String key) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(
                String.format("Field '%s' must be a list", key));
        }
        return (List<Map<String, Object>>) value;
    }

    /**
     * Formats a float value for YAML output.
     */
    public static String formatFloat(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    /**
     * Formats a double value for YAML output.
     */
    public static String formatDouble(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    /**
     * Gets an enum value from the YAML map.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(Map<String, Object> yamlMap, String key, Class<T> enumClass, T defaultValue) {
        Object value = yamlMap.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Enum) {
            return (T) value;
        }
        try {
            return Enum.valueOf(enumClass, value.toString());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Puts a value in the map if it's not null.
     */
    public static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}