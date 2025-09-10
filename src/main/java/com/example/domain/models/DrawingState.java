package com.example.domain.models;

import com.example.infrastructure.serialization.YamlSerializable;
import com.example.infrastructure.serialization.YamlUtils;
import com.example.domain.values.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DrawingState represents the complete state of the drawing canvas This is an immutable object that
 * captures all drawing elements and settings
 */
public final class DrawingState implements YamlSerializable {
    private final Map<String, Object> settings;
    private final List<Shape> shapes;
    private final Map<String, List<Object>> arrays;
    private final Map<String, List<Object>> lists;
    private final long timestamp;

    public DrawingState(
            Map<String, Object> settings,
            List<Shape> shapes,
            Map<String, List<Object>> arrays,
            Map<String, List<Object>> lists) {
        this.settings = settings != null ? new HashMap<>(settings) : new HashMap<>();
        this.shapes = shapes != null ? new ArrayList<>(shapes) : new ArrayList<>();
        this.arrays = arrays != null ? new HashMap<>(arrays) : new HashMap<>();
        this.lists = lists != null ? new HashMap<>(lists) : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Builder pattern for creating DrawingState
    public static class Builder {
        private Map<String, Object> settings = new HashMap<>();
        private List<Shape> shapes = new ArrayList<>();
        private Map<String, List<Object>> arrays = new HashMap<>();
        private Map<String, List<Object>> lists = new HashMap<>();

        public Builder settings(Map<String, Object> settings) {
            this.settings = new HashMap<>(settings);
            return this;
        }

        public Builder addSetting(String key, Object value) {
            this.settings.put(key, value);
            return this;
        }

        public Builder shapes(List<Shape> shapes) {
            this.shapes = new ArrayList<>(shapes);
            return this;
        }

        public Builder addShape(Shape shape) {
            this.shapes.add(shape);
            return this;
        }
        
        public Builder shape(Shape shape) {
            return addShape(shape);
        }

        public Builder arrays(Map<String, List<Object>> arrays) {
            this.arrays = new HashMap<>(arrays);
            return this;
        }

        public Builder addArray(String name, List<Object> array) {
            this.arrays.put(name, new ArrayList<>(array));
            return this;
        }

        public Builder lists(Map<String, List<Object>> lists) {
            this.lists = new HashMap<>(lists);
            return this;
        }

        public Builder addList(String name, List<Object> list) {
            this.lists.put(name, new ArrayList<>(list));
            return this;
        }

        public DrawingState build() {
            return new DrawingState(settings, shapes, arrays, lists);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters that return defensive copies to maintain immutability
    public Map<String, Object> getSettings() {
        return new HashMap<>(settings);
    }

    public List<Shape> getShapes() {
        return new ArrayList<>(shapes);
    }

    public Map<String, List<Object>> getArrays() {
        Map<String, List<Object>> copy = new HashMap<>();
        for (Map.Entry<String, List<Object>> entry : arrays.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public Map<String, List<Object>> getLists() {
        Map<String, List<Object>> copy = new HashMap<>();
        for (Map.Entry<String, List<Object>> entry : lists.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Helper methods for accessing specific settings
    public Object getSetting(String key) {
        return settings.get(key);
    }

    public String getStringSetting(String key, String defaultValue) {
        Object value = settings.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public int getIntSetting(String key, int defaultValue) {
        Object value = settings.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public float getFloatSetting(String key, float defaultValue) {
        Object value = settings.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        Object value = settings.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    // Methods to create new state with modifications (immutable updates)
    public DrawingState withSettings(Map<String, Object> newSettings) {
        return new DrawingState(newSettings, shapes, arrays, lists);
    }

    public DrawingState withSetting(String key, Object value) {
        Map<String, Object> newSettings = new HashMap<>(settings);
        newSettings.put(key, value);
        return new DrawingState(newSettings, shapes, arrays, lists);
    }

    public DrawingState withShapes(List<Shape> newShapes) {
        return new DrawingState(settings, newShapes, arrays, lists);
    }

    public DrawingState withAddedShape(Shape shape) {
        List<Shape> newShapes = new ArrayList<>(shapes);
        newShapes.add(shape);
        return new DrawingState(settings, newShapes, arrays, lists);
    }

    public DrawingState withShape(Shape shape) {
        return withAddedShape(shape);
    }

    public DrawingState withoutShape(String shapeId) {
        List<Shape> newShapes = new ArrayList<>();
        for (Shape shape : shapes) {
            if (!shape.getId().equals(shapeId)) {
                newShapes.add(shape);
            }
        }
        return new DrawingState(settings, newShapes, arrays, lists);
    }

    public DrawingState withRemovedShape(int index) {
        if (index < 0 || index >= shapes.size()) {
            return this; // Return unchanged if index is invalid
        }
        List<Shape> newShapes = new ArrayList<>(shapes);
        newShapes.remove(index);
        return new DrawingState(settings, newShapes, arrays, lists);
    }

    public DrawingState withReplacedShape(int index, Shape shape) {
        if (index < 0 || index >= shapes.size()) {
            return this; // Return unchanged if index is invalid
        }
        List<Shape> newShapes = new ArrayList<>(shapes);
        newShapes.set(index, shape);
        return new DrawingState(settings, newShapes, arrays, lists);
    }

    // Create empty state
    public static DrawingState empty() {
        return new DrawingState(
                new HashMap<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    // Create default state with common settings
    public static DrawingState defaultState() {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("width", 800);
        defaultSettings.put("height", 600);
        defaultSettings.put("backgroundColor", Color.WHITE.toHex());
        defaultSettings.put("title", "Drawing Canvas");

        return new DrawingState(
                defaultSettings, new ArrayList<>(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DrawingState)) {
            return false;
        }
        DrawingState state = (DrawingState) obj;
        return Objects.equals(settings, state.settings)
                && Objects.equals(shapes, state.shapes)
                && Objects.equals(arrays, state.arrays)
                && Objects.equals(lists, state.lists);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settings, shapes, arrays, lists);
    }

    @Override
    public String toString() {
        return String.format(
                "DrawingState(settings=%d, shapes=%d, arrays=%d, lists=%d, timestamp=%d)",
                settings.size(), shapes.size(), arrays.size(), lists.size(), timestamp);
    }

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("settings", new HashMap<>(settings));
        // Convert shapes to YamlSerializable list
        List<Map<String, Object>> shapeMapList = new ArrayList<>();
        for (Shape shape : shapes) {
            shapeMapList.add(shape.toYamlMap());
        }
        map.put("shapes", shapeMapList);
        map.put("arrays", YamlUtils.serializeMapOfLists(arrays));
        map.put("lists", YamlUtils.serializeMapOfLists(lists));
        map.put("stateTimestamp", timestamp);
        return map;
    }

    @Override
    public String getYamlType() {
        return "drawing_state";
    }

    @SuppressWarnings("unchecked")
    public static DrawingState fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "drawing_state");

        Map<String, Object> settings = YamlUtils.getMap(yamlMap, "settings", new HashMap<>());

        // Safely deserialize arrays and lists
        Map<String, List<Object>> arrays = new HashMap<>();
        Map<String, Object> arraysMap = YamlUtils.getMap(yamlMap, "arrays", new HashMap<>());
        for (Map.Entry<String, Object> entry : arraysMap.entrySet()) {
            if (entry.getValue() instanceof List) {
                arrays.put(entry.getKey(), (List<Object>) entry.getValue());
            }
        }

        Map<String, List<Object>> lists = new HashMap<>();
        Map<String, Object> listsMap = YamlUtils.getMap(yamlMap, "lists", new HashMap<>());
        for (Map.Entry<String, Object> entry : listsMap.entrySet()) {
            if (entry.getValue() instanceof List) {
                lists.put(entry.getKey(), (List<Object>) entry.getValue());
            }
        }

        // Deserialize shapes
        List<Shape> shapes = new ArrayList<>();
        List<Map<String, Object>> shapeMapList = YamlUtils.getMapList(yamlMap, "shapes");
        for (Map<String, Object> shapeMap : shapeMapList) {
            Shape shape = deserializeShape(shapeMap);
            if (shape != null) {
                shapes.add(shape);
            }
        }

        return new DrawingState(settings, shapes, arrays, lists);
    }

    private static Shape deserializeShape(Map<String, Object> shapeMap) {
        String shapeType = YamlUtils.getString(shapeMap, "type", null);
        if (shapeType == null) {
            return null;
        }

        switch (shapeType) {
            case "circle":
                return Circle.fromYamlMap(shapeMap);
            case "rectangle":
                return Rectangle.fromYamlMap(shapeMap);
            case "line":
                return Line.fromYamlMap(shapeMap);
            case "text":
                return Text.fromYamlMap(shapeMap);
            default:
                return null; // Unknown shape type
        }
    }
}
