package com.example.domain.models;

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
public final class DrawingState {
  private final Map<String, Object> settings;
  private final List<Shape> shapes;
  private final Map<String, List<Object>> arrays;
  private final Map<String, List<Object>> lists;
  private final long timestamp;

  public DrawingState(Map<String, Object> settings, List<Shape> shapes,
      Map<String, List<Object>> arrays, Map<String, List<Object>> lists) {
    // Defensive copy with null safety
    this.settings = settings != null ? new HashMap<>(settings) : new HashMap<>();

    // Validate shapes list and ensure no null shapes
    if (shapes != null) {
      this.shapes = new ArrayList<>();
      for (Shape shape : shapes) {
        if (shape == null) {
          throw new IllegalArgumentException("Shapes list cannot contain null elements");
        }
        this.shapes.add(shape);
      }
    } else {
      this.shapes = new ArrayList<>();
    }

    // Validate arrays map
    if (arrays != null) {
      this.arrays = new HashMap<>();
      for (Map.Entry<String, List<Object>> entry : arrays.entrySet()) {
        if (entry.getKey() == null) {
          throw new IllegalArgumentException("Arrays map cannot contain null keys");
        }
        this.arrays.put(entry.getKey(),
            entry.getValue() != null ? new ArrayList<>(entry.getValue()) : new ArrayList<>());
      }
    } else {
      this.arrays = new HashMap<>();
    }

    // Validate lists map
    if (lists != null) {
      this.lists = new HashMap<>();
      for (Map.Entry<String, List<Object>> entry : lists.entrySet()) {
        if (entry.getKey() == null) {
          throw new IllegalArgumentException("Lists map cannot contain null keys");
        }
        this.lists.put(entry.getKey(),
            entry.getValue() != null ? new ArrayList<>(entry.getValue()) : new ArrayList<>());
      }
    } else {
      this.lists = new HashMap<>();
    }

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


  public DrawingState withShapes(List<Shape> newShapes) {
    return new DrawingState(settings, newShapes, arrays, lists);
  }

  public DrawingState withAddedShape(Shape shape) {
    if (shape == null) {
      throw new IllegalArgumentException("Shape cannot be null");
    }
    List<Shape> newShapes = new ArrayList<>(shapes);
    newShapes.add(shape);
    return new DrawingState(settings, newShapes, arrays, lists);
  }


  // Create default state with common settings
  public static DrawingState defaultState() {
    Map<String, Object> defaultSettings = new HashMap<>();
    defaultSettings.put("width", 800);
    defaultSettings.put("height", 600);
    defaultSettings.put("backgroundColor", Color.WHITE.toHex());
    defaultSettings.put("title", "Drawing Canvas");

    return new DrawingState(defaultSettings, new ArrayList<>(), new HashMap<>(), new HashMap<>());
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
    return Objects.equals(settings, state.settings) && Objects.equals(shapes, state.shapes)
        && Objects.equals(arrays, state.arrays) && Objects.equals(lists, state.lists);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settings, shapes, arrays, lists);
  }

  @Override
  public String toString() {
    return String.format("DrawingState(settings=%d, shapes=%d, arrays=%d, lists=%d, timestamp=%d)",
        settings.size(), shapes.size(), arrays.size(), lists.size(), timestamp);
  }

}
