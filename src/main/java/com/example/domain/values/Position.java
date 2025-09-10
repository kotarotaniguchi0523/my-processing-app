package com.example.domain.values;

import com.example.domain.serialization.YamlSerializable;
import com.example.domain.serialization.YamlUtils;

import java.util.Map;

/**
 * Position value object representing x, y coordinates Immutable value object for position
 * information
 */
public final class Position implements YamlSerializable {
    private final float x;
    private final float y;

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Position add(Position other) {
        return new Position(x + other.x, y + other.y);
    }

    public Position subtract(Position other) {
        return new Position(x - other.x, y - other.y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position position = (Position) obj;
        return Float.compare(position.x, x) == 0 && Float.compare(position.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Position(x=%.2f, y=%.2f)", x, y);
    }

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("x", x);
        map.put("y", y);
        return map;
    }

    @Override
    public String getYamlType() {
        return "position";
    }

    public static Position fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "position");
        YamlUtils.validateRequiredFields(yamlMap, "x", "y");

        float x = YamlUtils.getRequiredFloat(yamlMap, "x");
        float y = YamlUtils.getRequiredFloat(yamlMap, "y");

        return new Position(x, y);
    }
    
    /**
     * ファクトリメソッド
     */
    public static Position of(float x, float y) {
        return new Position(x, y);
    }
    
    public static Position of(int x, int y) {
        return new Position((float) x, (float) y);
    }
}
