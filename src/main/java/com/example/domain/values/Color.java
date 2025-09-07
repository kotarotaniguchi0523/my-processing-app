package com.example.domain.values;

import com.example.domain.serialization.YamlSerializable;
import com.example.domain.serialization.YamlUtils;

import java.util.Map;

/**
 * Color value object representing RGBA color values Immutable value object for color information
 */
public final class Color implements YamlSerializable {
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public Color(int red, int green, int blue, int alpha) {
        this.red = clamp(red, 0, 255);
        this.green = clamp(green, 0, 255);
        this.blue = clamp(blue, 0, 255);
        this.alpha = clamp(alpha, 0, 255);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public int toProcessingColor() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static Color fromProcessingColor(int processingColor) {
        int alpha = (processingColor >> 24) & 0xFF;
        int red = (processingColor >> 16) & 0xFF;
        int green = (processingColor >> 8) & 0xFF;
        int blue = processingColor & 0xFF;
        return new Color(red, green, blue, alpha);
    }

    public static Color fromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.length() == 6) {
            int rgb = Integer.parseInt(hex, 16);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } else if (hex.length() == 8) {
            long rgba = Long.parseLong(hex, 16);
            return new Color(
                    (int) ((rgba >> 24) & 0xFF),
                    (int) ((rgba >> 16) & 0xFF),
                    (int) ((rgba >> 8) & 0xFF),
                    (int) (rgba & 0xFF));
        }

        throw new IllegalArgumentException("Invalid hex color format: " + hex);
    }

    public String toHex() {
        if (alpha == 255) {
            return String.format("#%02x%02x%02x", red, green, blue);
        } else {
            return String.format("#%02x%02x%02x%02x", red, green, blue, alpha);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Color)) {
            return false;
        }
        Color color = (Color) obj;
        return red == color.red
                && green == color.green
                && blue == color.blue
                && alpha == color.alpha;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        result = 31 * result + alpha;
        return result;
    }

    @Override
    public String toString() {
        return String.format("Color(r=%d, g=%d, b=%d, a=%d)", red, green, blue, alpha);
    }

    // Common colors
    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("red", red);
        map.put("green", green);
        map.put("blue", blue);
        map.put("alpha", alpha);
        map.put("hex", toHex());
        return map;
    }

    @Override
    public String getYamlType() {
        return "color";
    }

    public static Color fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "color");

        // Try to parse from hex first (more compact)
        String hex = YamlUtils.getString(yamlMap, "hex", null);
        if (hex != null) {
            try {
                return fromHex(hex);
            } catch (Exception e) {
                // Fall back to RGBA values
            }
        }

        // Parse from RGBA values
        YamlUtils.validateRequiredFields(yamlMap, "red", "green", "blue");
        int red = YamlUtils.getRequiredInt(yamlMap, "red");
        int green = YamlUtils.getRequiredInt(yamlMap, "green");
        int blue = YamlUtils.getRequiredInt(yamlMap, "blue");
        int alpha = YamlUtils.getInt(yamlMap, "alpha", 255);

        return new Color(red, green, blue, alpha);
    }
}
