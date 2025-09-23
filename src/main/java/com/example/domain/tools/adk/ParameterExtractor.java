package com.example.domain.tools.adk;

import com.example.domain.values.Color;
import com.example.domain.values.Style;

import java.util.Map;

/**
 * Utility class for extracting and validating parameters from ADK tool inputs.
 * Centralizes common parameter extraction logic to avoid code duplication.
 */
public class ParameterExtractor {

    /**
     * Extract a Number value from parameters.
     */
    public static Number getNumber(Map<String, Object> params, String key) {
        if (params == null || key == null) {
            return null;
        }

        Object value = params.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extract a String value from parameters.
     */
    public static String getString(Map<String, Object> params, String key) {
        if (params == null || key == null) {
            return null;
        }

        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extract a Boolean value from parameters.
     */
    public static Boolean getBoolean(Map<String, Object> params, String key) {
        if (params == null || key == null) {
            return null;
        }

        Object value = params.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * Extract a Map value from parameters.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> params, String key) {
        if (params == null || key == null) {
            return null;
        }

        Object value = params.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * Parse a color from a hex string or return null if invalid.
     */
    public static Color parseColor(String colorStr) {
        if (colorStr == null || !colorStr.startsWith("#")) {
            return null;
        }

        try {
            return Color.fromHex(colorStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a color by name from predefined color constants.
     */
    public static Color getColorByName(String name) {
        if (name == null) return null;

        switch (name.toUpperCase()) {
            case "BLACK": return Color.BLACK;
            case "WHITE": return Color.WHITE;
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            case "YELLOW": return new Color(255, 255, 0);
            case "CYAN": return new Color(0, 255, 255);
            case "MAGENTA": return new Color(255, 0, 255);
            case "ORANGE": return new Color(255, 165, 0);
            case "PURPLE": return new Color(128, 0, 128);
            case "PINK": return new Color(255, 192, 203);
            case "GRAY":
            case "GREY": return new Color(128, 128, 128);
            case "LIGHT_GRAY":
            case "LIGHT_GREY": return new Color(192, 192, 192);
            case "DARK_GRAY":
            case "DARK_GREY": return new Color(64, 64, 64);
            case "BROWN": return new Color(165, 42, 42);
            default: return null;
        }
    }

    /**
     * Build a Style object from parameters with common defaults.
     */
    public static Style.Builder buildStyleFromParameters(Map<String, Object> parameters,
                                                         Color defaultFillColor,
                                                         Color defaultStrokeColor,
                                                         float defaultStrokeWeight,
                                                         boolean defaultFilled) {
        Style.Builder styleBuilder = Style.builder();

        // Handle fill color
        String fillColorStr = getString(parameters, "fillColor");
        if (fillColorStr != null) {
            Color color = fillColorStr.startsWith("#") ? parseColor(fillColorStr) : getColorByName(fillColorStr);
            if (color != null) {
                styleBuilder.fillColor(color);
            } else {
                styleBuilder.fillColor(defaultFillColor);
            }
        } else {
            styleBuilder.fillColor(defaultFillColor);
        }

        // Handle stroke color
        String strokeColorStr = getString(parameters, "strokeColor");
        if (strokeColorStr != null) {
            Color color = strokeColorStr.startsWith("#") ? parseColor(strokeColorStr) : getColorByName(strokeColorStr);
            if (color != null) {
                styleBuilder.strokeColor(color);
            } else {
                styleBuilder.strokeColor(defaultStrokeColor);
            }
        } else {
            styleBuilder.strokeColor(defaultStrokeColor);
        }

        // Handle stroke weight
        Number strokeWeight = getNumber(parameters, "strokeWeight");
        if (strokeWeight != null) {
            styleBuilder.strokeWeight(strokeWeight.floatValue());
        } else {
            styleBuilder.strokeWeight(defaultStrokeWeight);
        }

        // Handle filled flag
        Boolean filled = getBoolean(parameters, "filled");
        if (filled != null && !filled) {
            styleBuilder.noFill();
        } else {
            styleBuilder.hasFill(defaultFilled);
        }

        return styleBuilder;
    }

    /**
     * Extract an integer value from parameters with a default value.
     */
    public static int getInt(Map<String, Object> params, String key, int defaultValue) {
        Number num = getNumber(params, key);
        return num != null ? num.intValue() : defaultValue;
    }

    /**
     * Extract a float value from parameters with a default value.
     */
    public static float getFloat(Map<String, Object> params, String key, float defaultValue) {
        Number num = getNumber(params, key);
        return num != null ? num.floatValue() : defaultValue;
    }

    /**
     * Extract a double value from parameters with a default value.
     */
    public static double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Number num = getNumber(params, key);
        return num != null ? num.doubleValue() : defaultValue;
    }

    /**
     * Validate that required parameters are present.
     */
    public static boolean hasRequiredParameters(Map<String, Object> params, String... requiredKeys) {
        if (params == null || requiredKeys == null) {
            return false;
        }

        for (String key : requiredKeys) {
            if (!params.containsKey(key) || params.get(key) == null) {
                return false;
            }
        }
        return true;
    }
}