package com.example.agent.service;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the difference between two drawing states
 */
public class DrawingStateDiff {
    
    private final DrawingState originalState;
    private final DrawingState newState;
    private final List<Change> changes;
    
    public DrawingStateDiff(DrawingState originalState, DrawingState newState) {
        this.originalState = originalState;
        this.newState = newState;
        this.changes = calculateChanges();
    }
    
    /**
     * Calculates the changes between the original and new states
     * @return List of changes
     */
    private List<Change> calculateChanges() {
        List<Change> changeList = new ArrayList<>();
        
        // Check for added shapes
        List<Shape> originalShapes = originalState != null ? originalState.getShapes() : new ArrayList<>();
        List<Shape> newShapes = newState != null ? newState.getShapes() : new ArrayList<>();
        
        // Find added shapes
        for (Shape shape : newShapes) {
            if (!containsShape(originalShapes, shape)) {
                changeList.add(new Change(ChangeType.ADDED, "Shape", shape.toString()));
            }
        }
        
        // Find removed shapes
        for (Shape shape : originalShapes) {
            if (!containsShape(newShapes, shape)) {
                changeList.add(new Change(ChangeType.REMOVED, "Shape", shape.toString()));
            }
        }
        
        // Find modified shapes (simplified - checks by position and type)
        for (Shape newShape : newShapes) {
            for (Shape origShape : originalShapes) {
                if (isSameShapeModified(origShape, newShape)) {
                    changeList.add(new Change(ChangeType.MODIFIED, "Shape", 
                        origShape.toString() + " -> " + newShape.toString()));
                }
            }
        }
        
        // Check background changes
        String origBg = originalState != null ? originalState.getStringSetting("backgroundColor", "white") : "white";
        String newBg = newState != null ? newState.getStringSetting("backgroundColor", "white") : "white";
        if (!Objects.equals(origBg, newBg)) {
            changeList.add(new Change(ChangeType.MODIFIED, "Background", origBg + " -> " + newBg));
        }
        
        return changeList;
    }
    
    /**
     * Checks if a list contains a shape
     * @param shapes List of shapes
     * @param shape Shape to find
     * @return true if the shape is in the list
     */
    private boolean containsShape(List<Shape> shapes, Shape shape) {
        return shapes.stream().anyMatch(s -> shapesEqual(s, shape));
    }
    
    /**
     * Checks if two shapes are equal
     * @param s1 First shape
     * @param s2 Second shape
     * @return true if shapes are equal
     */
    private boolean shapesEqual(Shape s1, Shape s2) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;
        
        return Objects.equals(s1.getType(), s2.getType()) &&
               Objects.equals(s1.getPosition(), s2.getPosition()) &&
               Objects.equals(s1.getBounds(), s2.getBounds()) &&
               Objects.equals(s1.getStyle(), s2.getStyle());
    }
    
    /**
     * Checks if two shapes are the same but modified
     * @param orig Original shape
     * @param newShape New shape
     * @return true if it's the same shape but modified
     */
    private boolean isSameShapeModified(Shape orig, Shape newShape) {
        // Same ID but different properties
        return Objects.equals(orig.getId(), newShape.getId()) &&
               Objects.equals(orig.getType(), newShape.getType()) &&
               (!Objects.equals(orig.getPosition(), newShape.getPosition()) ||
                !Objects.equals(orig.getBounds(), newShape.getBounds()) ||
                !Objects.equals(orig.getStyle(), newShape.getStyle()) ||
                orig.isVisible() != newShape.isVisible() ||
                orig.getLayerOrder() != newShape.getLayerOrder());
    }
    
    /**
     * Gets the list of changes
     * @return List of changes
     */
    public List<Change> getChanges() {
        return new ArrayList<>(changes);
    }
    
    /**
     * Checks if there are any changes
     * @return true if there are changes
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }
    
    /**
     * Gets the number of changes
     * @return Number of changes
     */
    public int getChangeCount() {
        return changes.size();
    }
    
    /**
     * Gets changes of a specific type
     * @param type The type of change
     * @return List of changes of that type
     */
    public List<Change> getChangesByType(ChangeType type) {
        return changes.stream()
            .filter(c -> c.getType() == type)
            .toList();
    }
    
    /**
     * Formats the diff as a string
     * @return Formatted diff string
     */
    public String format() {
        if (changes.isEmpty()) {
            return "No changes";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Changes detected:\n");
        
        for (Change change : changes) {
            switch (change.getType()) {
                case ADDED:
                    sb.append("  + ");
                    break;
                case REMOVED:
                    sb.append("  - ");
                    break;
                case MODIFIED:
                    sb.append("  ~ ");
                    break;
            }
            sb.append(change.getTarget()).append(": ").append(change.getDescription()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Enum for change types
     */
    public enum ChangeType {
        ADDED,
        REMOVED,
        MODIFIED
    }
    
    /**
     * Represents a single change
     */
    public static class Change {
        private final ChangeType type;
        private final String target;
        private final String description;
        
        public Change(ChangeType type, String target, String description) {
            this.type = type;
            this.target = target;
            this.description = description;
        }
        
        public ChangeType getType() { return type; }
        public String getTarget() { return target; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("%s %s: %s", type, target, description);
        }
    }
    
    @Override
    public String toString() {
        return String.format("DrawingStateDiff{changes=%d}", changes.size());
    }
}