package com.example.domain.tools.adk;

import com.example.domain.values.Position;
import java.util.Map;
import java.util.UUID;
/**
 * Utility class for common ADK tool operations.
 * Centralizes shared functionality to reduce code duplication.
 */
public class ADKToolUtils {

    /**
     * Generate a unique shape ID with the given prefix.
     * @param prefix The prefix for the ID (e.g., "circle", "rect", "line")
     * @return A unique ID string
     */
    public static String generateShapeId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extract position from parameters supporting both flat and nested formats.
     * Supports both:
     * - Flat format: {x: 100, y: 200, ...}
     * - Nested format: {position: {x: 100, y: 200}, ...}
     *
     * @param parameters The parameters map
     * @return The extracted Position or null if not found
     */
    public static Position extractPosition(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }

        // Try flat format first
        Number x = ParameterExtractor.getNumber(parameters, "x");
        Number y = ParameterExtractor.getNumber(parameters, "y");

        // If flat parameters not found, try nested position object
        if (x == null || y == null) {
            Map<String, Object> position = ParameterExtractor.getMap(parameters, "position");
            if (position != null) {
                x = ParameterExtractor.getNumber(position, "x");
                y = ParameterExtractor.getNumber(position, "y");
            }
        }

        if (x == null || y == null) {
            return null;
        }

        return new Position(x.floatValue(), y.floatValue());
    }

    /**
     * Extract start and end positions for line-like shapes.
     * Supports multiple formats:
     * - Flat: {x1: 100, y1: 200, x2: 300, y2: 400}
     * - Nested: {start: {x: 100, y: 200}, end: {x: 300, y: 400}}
     * - Mixed: {x: 100, y: 200, endX: 300, endY: 400}
     *
     * @param parameters The parameters map
     * @return An array with [startPosition, endPosition] or null if not found
     */
    public static Position[] extractLinePositions(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }

        Position startPos = null;
        Position endPos = null;

        // Try format 1: x1, y1, x2, y2
        Number x1 = ParameterExtractor.getNumber(parameters, "x1");
        Number y1 = ParameterExtractor.getNumber(parameters, "y1");
        Number x2 = ParameterExtractor.getNumber(parameters, "x2");
        Number y2 = ParameterExtractor.getNumber(parameters, "y2");

        if (x1 != null && y1 != null && x2 != null && y2 != null) {
            startPos = new Position(x1.floatValue(), y1.floatValue());
            endPos = new Position(x2.floatValue(), y2.floatValue());
            return new Position[]{startPos, endPos};
        }

        // Try format 2: start and end position objects
        Map<String, Object> start = ParameterExtractor.getMap(parameters, "start");
        Map<String, Object> end = ParameterExtractor.getMap(parameters, "end");

        if (start != null && end != null) {
            Number startX = ParameterExtractor.getNumber(start, "x");
            Number startY = ParameterExtractor.getNumber(start, "y");
            Number endX = ParameterExtractor.getNumber(end, "x");
            Number endY = ParameterExtractor.getNumber(end, "y");

            if (startX != null && startY != null && endX != null && endY != null) {
                startPos = new Position(startX.floatValue(), startY.floatValue());
                endPos = new Position(endX.floatValue(), endY.floatValue());
                return new Position[]{startPos, endPos};
            }
        }

        // Try format 3: x, y, endX, endY
        Number x = ParameterExtractor.getNumber(parameters, "x");
        Number y = ParameterExtractor.getNumber(parameters, "y");
        Number endX = ParameterExtractor.getNumber(parameters, "endX");
        Number endY = ParameterExtractor.getNumber(parameters, "endY");

        if (x != null && y != null && endX != null && endY != null) {
            startPos = new Position(x.floatValue(), y.floatValue());
            endPos = new Position(endX.floatValue(), endY.floatValue());
            return new Position[]{startPos, endPos};
        }

        return null;
    }

    /**
     * Validate that a position is within reasonable bounds.
     * @param position The position to validate
     * @param maxBound The maximum allowed absolute value for coordinates
     * @return true if valid, false otherwise
     */
    public static boolean isPositionValid(Position position, float maxBound) {
        if (position == null) {
            return false;
        }
        return Math.abs(position.getX()) <= maxBound && Math.abs(position.getY()) <= maxBound;
    }

    /**
     * Validate that a dimension value is positive and within bounds.
     * @param value The dimension value (width, height, radius, etc.)
     * @param maxValue The maximum allowed value
     * @return true if valid, false otherwise
     */
    public static boolean isDimensionValid(Number value, float maxValue) {
        if (value == null) {
            return false;
        }
        float floatValue = value.floatValue();
        return floatValue > 0 && floatValue <= maxValue;
    }

    /**
     * Standard validation for positions - checks they're within ±10000.
     */
    public static boolean isPositionInStandardBounds(Position position) {
        return isPositionValid(position, 10000);
    }

    /**
     * Standard validation for dimensions - checks they're positive and <= 5000.
     */
    public static boolean isDimensionInStandardBounds(Number dimension) {
        return isDimensionValid(dimension, 5000);
    }

    /**
     * Format a position validation error message.
     */
    public static String getPositionErrorMessage(String paramName) {
        return String.format("%s coordinates are required and must be within reasonable bounds (±10000)", paramName);
    }

    /**
     * Format a dimension validation error message.
     */
    public static String getDimensionErrorMessage(String paramName, Number value) {
        if (value == null) {
            return String.format("%s is required and must be a valid number", paramName);
        } else if (value.floatValue() <= 0) {
            return String.format("%s must be positive, got %s", paramName, value);
        } else {
            return String.format("%s is too large (max 5000), got %s", paramName, value);
        }
    }
}