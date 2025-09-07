package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.IAgentTool;
import java.util.*;

/**
 * DeleteShapeTool enables removal of shapes from the drawing state.
 * Can delete shapes by ID, index, or multiple shapes at once.
 */
public class DeleteShapeTool implements IAgentTool {
    
    @Override
    public String name() {
        return "deleteShape";
    }
    
    @Override
    public Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", "Delete one or more shapes from the drawing canvas");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Single shape deletion by ID
        Map<String, Object> idProperty = new HashMap<>();
        idProperty.put("type", "string");
        idProperty.put("description", "The ID of the shape to delete");
        properties.put("id", idProperty);
        
        // Single shape deletion by index
        Map<String, Object> indexProperty = new HashMap<>();
        indexProperty.put("type", "integer");
        indexProperty.put("description", "The index of the shape to delete (alternative to ID)");
        indexProperty.put("minimum", 0);
        properties.put("index", indexProperty);
        
        // Multiple shape deletion by IDs
        Map<String, Object> idsProperty = new HashMap<>();
        idsProperty.put("type", "array");
        idsProperty.put("description", "Array of shape IDs to delete");
        Map<String, Object> idsItems = new HashMap<>();
        idsItems.put("type", "string");
        idsProperty.put("items", idsItems);
        properties.put("ids", idsProperty);
        
        // Multiple shape deletion by indices
        Map<String, Object> indicesProperty = new HashMap<>();
        indicesProperty.put("type", "array");
        indicesProperty.put("description", "Array of shape indices to delete");
        Map<String, Object> indicesItems = new HashMap<>();
        indicesItems.put("type", "integer");
        indicesItems.put("minimum", 0);
        indicesProperty.put("items", indicesItems);
        properties.put("indices", indicesProperty);
        
        // Delete by criteria
        Map<String, Object> criteriaProperty = new HashMap<>();
        criteriaProperty.put("type", "object");
        criteriaProperty.put("description", "Criteria for selecting shapes to delete");
        Map<String, Object> criteriaProps = new HashMap<>();
        
        // Filter by shape type
        Map<String, Object> typeProperty = new HashMap<>();
        typeProperty.put("type", "string");
        typeProperty.put("description", "Delete all shapes of this type");
        typeProperty.put("enum", Arrays.asList("circle", "rectangle", "line", "text"));
        criteriaProps.put("type", typeProperty);
        
        // Filter by visibility
        criteriaProps.put("visible", createBooleanProperty("Delete only visible or invisible shapes"));
        
        // Filter by layer order range
        Map<String, Object> layerRangeProperty = new HashMap<>();
        layerRangeProperty.put("type", "object");
        layerRangeProperty.put("description", "Delete shapes within layer order range");
        Map<String, Object> layerProps = new HashMap<>();
        layerProps.put("min", createNumberProperty("Minimum layer order"));
        layerProps.put("max", createNumberProperty("Maximum layer order"));
        layerRangeProperty.put("properties", layerProps);
        criteriaProps.put("layerRange", layerRangeProperty);
        
        criteriaProperty.put("properties", criteriaProps);
        properties.put("criteria", criteriaProperty);
        
        // Delete all shapes
        properties.put("deleteAll", createBooleanProperty("Delete all shapes on the canvas"));
        
        schema.put("properties", properties);
        
        // At least one deletion method must be specified
        Map<String, Object> anyOf = new HashMap<>();
        List<Map<String, Object>> anyOfConditions = Arrays.asList(
            Map.of("required", Arrays.asList("id")),
            Map.of("required", Arrays.asList("index")),
            Map.of("required", Arrays.asList("ids")),
            Map.of("required", Arrays.asList("indices")),
            Map.of("required", Arrays.asList("criteria")),
            Map.of("required", Arrays.asList("deleteAll"))
        );
        anyOf.put("anyOf", anyOfConditions);
        schema.putAll(anyOf);
        
        return schema;
    }
    
    @Override
    public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
        validateParameters(parameters);
        
        List<Shape> currentShapes = new ArrayList<>(state.getShapes());
        Set<Integer> indicesToDelete = new HashSet<>();
        
        // Collect indices of shapes to delete based on different criteria
        if (parameters.containsKey("deleteAll") && getBoolean(parameters, "deleteAll", false)) {
            // Delete all shapes
            for (int i = 0; i < currentShapes.size(); i++) {
                indicesToDelete.add(i);
            }
        } else {
            // Delete by single ID
            if (parameters.containsKey("id")) {
                String targetId = (String) parameters.get("id");
                int index = findShapeIndexById(currentShapes, targetId);
                if (index >= 0) {
                    indicesToDelete.add(index);
                }
            }
            
            // Delete by single index
            if (parameters.containsKey("index")) {
                int index = getInt(parameters, "index");
                if (index >= 0 && index < currentShapes.size()) {
                    indicesToDelete.add(index);
                }
            }
            
            // Delete by multiple IDs
            if (parameters.containsKey("ids")) {
                List<String> ids = getStringList(parameters, "ids");
                for (String id : ids) {
                    int index = findShapeIndexById(currentShapes, id);
                    if (index >= 0) {
                        indicesToDelete.add(index);
                    }
                }
            }
            
            // Delete by multiple indices
            if (parameters.containsKey("indices")) {
                List<Integer> indices = getIntegerList(parameters, "indices");
                for (Integer index : indices) {
                    if (index >= 0 && index < currentShapes.size()) {
                        indicesToDelete.add(index);
                    }
                }
            }
            
            // Delete by criteria
            if (parameters.containsKey("criteria")) {
                Map<String, Object> criteria = getMap(parameters, "criteria");
                Set<Integer> criteriaMatches = findShapesByCriteria(currentShapes, criteria);
                indicesToDelete.addAll(criteriaMatches);
            }
        }
        
        // If no shapes found to delete, return original state
        if (indicesToDelete.isEmpty()) {
            return state;
        }
        
        // Create new list without deleted shapes
        // Sort indices in descending order to avoid index shifting issues
        List<Integer> sortedIndices = new ArrayList<>(indicesToDelete);
        sortedIndices.sort(Collections.reverseOrder());
        
        List<Shape> newShapes = new ArrayList<>(currentShapes);
        for (int index : sortedIndices) {
            if (index < newShapes.size()) {
                newShapes.remove(index);
            }
        }
        
        return state.withShapes(newShapes);
    }
    
    private int findShapeIndexById(List<Shape> shapes, String id) {
        for (int i = 0; i < shapes.size(); i++) {
            if (id.equals(shapes.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
    
    private Set<Integer> findShapesByCriteria(List<Shape> shapes, Map<String, Object> criteria) {
        Set<Integer> matchingIndices = new HashSet<>();
        
        for (int i = 0; i < shapes.size(); i++) {
            Shape shape = shapes.get(i);
            boolean matches = true;
            
            // Check type criteria
            if (criteria.containsKey("type")) {
                String targetType = (String) criteria.get("type");
                String shapeType = shape.getType().getTypeName();
                if (!targetType.equalsIgnoreCase(shapeType)) {
                    matches = false;
                }
            }
            
            // Check visibility criteria
            if (criteria.containsKey("visible")) {
                boolean targetVisible = getBoolean(criteria, "visible", true);
                if (shape.isVisible() != targetVisible) {
                    matches = false;
                }
            }
            
            // Check layer range criteria
            if (criteria.containsKey("layerRange")) {
                Map<String, Object> layerRange = getMap(criteria, "layerRange");
                int shapeLayer = shape.getLayerOrder();
                
                if (layerRange.containsKey("min")) {
                    int minLayer = getInt(layerRange, "min");
                    if (shapeLayer < minLayer) {
                        matches = false;
                    }
                }
                
                if (layerRange.containsKey("max")) {
                    int maxLayer = getInt(layerRange, "max");
                    if (shapeLayer > maxLayer) {
                        matches = false;
                    }
                }
            }
            
            if (matches) {
                matchingIndices.add(i);
            }
        }
        
        return matchingIndices;
    }
    
    private void validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        
        // Check that at least one deletion method is specified
        Set<String> deletionMethods = Set.of("id", "index", "ids", "indices", "criteria", "deleteAll");
        boolean hasValidMethod = parameters.keySet().stream()
            .anyMatch(deletionMethods::contains);
        
        if (!hasValidMethod) {
            throw new IllegalArgumentException(
                "At least one deletion method must be specified: " + deletionMethods);
        }
        
        // Validate that deleteAll is not combined with other methods
        if (parameters.containsKey("deleteAll") && getBoolean(parameters, "deleteAll", false)) {
            Set<String> otherMethods = Set.of("id", "index", "ids", "indices", "criteria");
            boolean hasOtherMethod = parameters.keySet().stream()
                .anyMatch(otherMethods::contains);
            
            if (hasOtherMethod) {
                throw new IllegalArgumentException(
                    "deleteAll cannot be combined with other deletion methods");
            }
        }
        
        // Validate array parameters are not empty
        if (parameters.containsKey("ids")) {
            List<String> ids = getStringList(parameters, "ids");
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("ids array cannot be empty");
            }
        }
        
        if (parameters.containsKey("indices")) {
            List<Integer> indices = getIntegerList(parameters, "indices");
            if (indices.isEmpty()) {
                throw new IllegalArgumentException("indices array cannot be empty");
            }
        }
    }
    
    // Utility methods for safe parameter extraction
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return new ArrayList<>();
    }
    
    @SuppressWarnings("unchecked")
    private List<Integer> getIntegerList(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                .filter(item -> item instanceof Number)
                .map(item -> ((Number) item).intValue())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return new ArrayList<>();
    }
    
    private int getInt(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter missing: " + key);
        }
        return ((Number) value).intValue();
    }
    
    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }
    
    private boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    // Schema helper methods
    private Map<String, Object> createNumberProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "number");
        prop.put("description", description);
        return prop;
    }
    
    private Map<String, Object> createBooleanProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "boolean");
        prop.put("description", description);
        return prop;
    }
}