package com.example.application.services;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a drawing component with properties.
 * Used for ADK integration.
 */
public class DrawingComponent {
    private String name;
    private String type;
    private Map<String, Object> properties = new HashMap<>();

    // Constructors
    public DrawingComponent() {}

    public DrawingComponent(String name, String type) {
        this.name = name;
        this.type = type;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    // Helper methods
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String toString() {
        return "DrawingComponent{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                '}';
    }
}