package com.example.domain.serialization;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
            throw new IllegalArgumentException(
                    "Required int field '" + key + "' is missing or invalid");
        }
        return ((Number) value).intValue();
    }

    public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public static double getRequiredDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Required double field '" + key + "' is missing or invalid");
        }
        return ((Number) value).doubleValue();
    }

    public static long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    public static long getRequiredLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Required long field '" + key + "' is missing or invalid");
        }
        return ((Number) value).longValue();
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

    // Enum utilities
    public static <T extends Enum<T>> T getEnum(Map<String, Object> map, String key, Class<T> enumClass, T defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            try {
                return Enum.valueOf(enumClass, (String) value);
            } catch (IllegalArgumentException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static <T extends Enum<T>> T getRequiredEnum(Map<String, Object> map, String key, Class<T> enumClass) {
        Object value = map.get(key);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Required enum field '" + key + "' is missing or invalid");
        }
        try {
            return Enum.valueOf(enumClass, (String) value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid enum value for field '" + key + "': " + value);
        }
    }

    // Collection utilities
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getRequiredList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(
                    "Required list field '" + key + "' is missing or invalid");
        }
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key, Map<String, Object> defaultValue) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getRequiredMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(
                    "Required map field '" + key + "' is missing or invalid");
        }
        return (Map<String, Object>) value;
    }

    // Date/Time utilities
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
            throw new IllegalArgumentException("Invalid date/time format: " + dateTimeString, e);
        }
    }

    // Validation utilities
    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' cannot be null");
        }
    }

    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' cannot be null or empty");
        }
    }

    public static void validatePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' must be positive, got: " + value);
        }
    }

    public static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' must be non-negative, got: " + value);
        }
    }

    public static void validateRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.US,
                            "Field '%s' must be between %.2f and %.2f, got: %.2f",
                            fieldName,
                            min,
                            max,
                            value));
        }
    }

    // Builder utilities
    public static Map<String, Object> createMap(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even number of arguments");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public static Map<String, Object> deepCopy(Map<String, Object> original) {
        if (original == null) {
            return null;
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                copy.put(entry.getKey(), deepCopy(mapValue));
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> listValue = (List<Object>) value;
                copy.put(entry.getKey(), deepCopyList(listValue));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    private static List<Object> deepCopyList(List<Object> original) {
        List<Object> copy = new ArrayList<>();
        for (Object item : original) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                copy.add(deepCopy(mapItem));
            } else if (item instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> listItem = (List<Object>) item;
                copy.add(deepCopyList(listItem));
            } else {
                copy.add(item);
            }
        }
        return copy;
    }

    // Type checking utilities
    public static boolean isValidYamlValue(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof List
                || value instanceof Map;
    }

    public static void validateYamlCompatible(Object value, String fieldName) {
        if (!isValidYamlValue(value)) {
            throw new IllegalArgumentException(
                    "Field '"
                            + fieldName
                            + "' contains non-YAML-compatible type: "
                            + (value != null ? value.getClass().getSimpleName() : "null"));
        }
    }

    // Comparison utilities
    public static boolean mapsEqual(Map<String, Object> map1, Map<String, Object> map2) {
        return Objects.equals(map1, map2);
    }

    // Helper for creating type-safe YAML maps
    public static Map<String, Object> yamlMap() {
        return new LinkedHashMap<>();
    }

    public static Map<String, Object> yamlMap(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    // Map serialization utilities
    public static Map<String, List<Object>> serializeMapOfLists(
            Map<String, List<Object>> original) {
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

    // Float utilities
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

    // YAML validation utilities
    public static void validateYamlType(Map<String, Object> yamlMap, String expectedType) {
        String actualType = getString(yamlMap, "type", null);
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException(
                    "Invalid YAML type. Expected '" + expectedType + "' but got '" + actualType + "'");
        }
    }

    public static void validateRequiredFields(Map<String, Object> yamlMap, String... requiredFields) {
        for (String field : requiredFields) {
            if (!yamlMap.containsKey(field)) {
                throw new IllegalArgumentException("Required field '" + field + "' is missing");
            }
        }
    }

    // List of Maps utilities
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getMapList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                } else {
                    throw new IllegalArgumentException(
                            "List field '" + key + "' contains non-map elements");
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    // Null-safe operations
    public static Object putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            return map.put(key, value);
        }
        return null;
    }
}