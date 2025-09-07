package com.example.domain.serialization;

import java.util.Map;

/**
 * Interface for objects that can be serialized to and from YAML format Provides a contract for
 * roundtrip consistency in YAML serialization
 */
public interface YamlSerializable {

    /**
     * Convert this object to a Map representation suitable for YAML serialization
     *
     * @return Map containing all necessary data to reconstruct this object
     */
    Map<String, Object> toYamlMap();

    /**
     * Get the YAML type identifier for this object Used for polymorphic deserialization
     *
     * @return String identifier for this object type
     */
    String getYamlType();

    /**
     * Create an instance of this object from a YAML Map This is typically implemented as a static
     * method in concrete classes
     *
     * @param yamlMap Map containing serialized object data
     * @return New instance reconstructed from the map
     */
    // Note: This would be implemented as static methods in concrete classes:
    // public static ConcreteClass fromYamlMap(Map<String, Object> yamlMap)
}
