package com.example.presentation.diff;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.*;

/**
 * Calculates differences between drawing states.
 * Single responsibility: Compute diff between two states.
 */
public class DiffCalculator {

    /**
     * Compute diff summary between two drawing states
     */
    public static DiffSummary computeDiff(DrawingState previous, DrawingState current) {
        if (previous == null || current == null) {
            return new DiffSummary(new ArrayList<>(), 0, 0, 0);
        }

        List<DiffItem> items = new ArrayList<>();

        // Track shape IDs for comparison
        Set<String> previousIds = new HashSet<>();
        Set<String> currentIds = new HashSet<>();

        Map<String, Shape> previousShapes = new HashMap<>();
        Map<String, Shape> currentShapes = new HashMap<>();

        // Collect previous shapes
        for (Shape shape : previous.getShapes()) {
            previousIds.add(shape.getId());
            previousShapes.put(shape.getId(), shape);
        }

        // Collect current shapes
        for (Shape shape : current.getShapes()) {
            currentIds.add(shape.getId());
            currentShapes.put(shape.getId(), shape);
        }

        // Find added shapes
        int added = 0;
        for (String id : currentIds) {
            if (!previousIds.contains(id)) {
                Shape shape = currentShapes.get(id);
                items.add(new DiffItem(shape, "ADD"));
                added++;
            }
        }

        // Find removed shapes
        int removed = 0;
        for (String id : previousIds) {
            if (!currentIds.contains(id)) {
                Shape shape = previousShapes.get(id);
                items.add(new DiffItem(shape, "REMOVE"));
                removed++;
            }
        }

        // Find modified shapes
        int modified = 0;
        for (String id : currentIds) {
            if (previousIds.contains(id)) {
                Shape prev = previousShapes.get(id);
                Shape curr = currentShapes.get(id);
                if (!prev.equals(curr)) {
                    items.add(new DiffItem(curr, "MODIFY"));
                    modified++;
                }
            }
        }

        return new DiffSummary(items, added, removed, modified);
    }

    /**
     * Result of diff calculation
     */
    public static class DiffSummary {
        private final List<DiffItem> items;
        private final int added;
        private final int removed;
        private final int modified;

        public DiffSummary(List<DiffItem> items, int added, int removed, int modified) {
            this.items = items;
            this.added = added;
            this.removed = removed;
            this.modified = modified;
        }

        public List<DiffItem> getItems() {
            return items;
        }

        public int getAdded() {
            return added;
        }

        public int getRemoved() {
            return removed;
        }

        public int getModified() {
            return modified;
        }

        public boolean hasChanges() {
            return added > 0 || removed > 0 || modified > 0;
        }

    }

    /**
     * Single diff item
     */
    public static class DiffItem {
        private final Shape shape;
        private final String action;
        private final String description;

        public DiffItem(Shape shape, String action) {
            this.shape = shape;
            this.action = action;
            this.description = formatDescription(shape, action);
        }

        private static String formatDescription(Shape shape, String action) {
            String shapeType = shape.getType().getTypeName();
            return String.format("%s %s at (%.0f, %.0f)",
                action, shapeType,
                shape.getPosition().getX(),
                shape.getPosition().getY());
        }

        public Shape getShape() {
            return shape;
        }

        public String getAction() {
            return action;
        }

        public String getDescription() {
            return description;
        }
    }
}