package com.example.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service for calculating detailed differences between drawing states. Provides comprehensive state
 * comparison and diff reporting for approval workflows.
 */
public class StateDiffService {

  /**
   * Calculates a detailed difference between two drawing states.
   *
   * @param oldState the old state (null for initial state)
   * @param newState the new state
   * @return a detailed difference report
   */
  public StateDiff calculateDifference(DrawingState oldState, DrawingState newState) {
    if (newState == null) {
      throw new IllegalArgumentException("New state cannot be null");
    }

    if (oldState == null) {
      return StateDiff.initialState(newState);
    }

    StateDiff.Builder builder = new StateDiff.Builder(oldState, newState);

    // Compare settings
    compareSettings(oldState, newState, builder);

    // Compare shapes
    compareShapes(oldState, newState, builder);

    // Compare arrays
    compareArrays(oldState, newState, builder);

    // Compare lists
    compareLists(oldState, newState, builder);

    return builder.build();
  }

  /**
   * Creates a human-readable summary of the differences.
   *
   * @param diff the state difference
   * @return a summary string
   */
  public String createSummary(StateDiff diff) {
    if (diff.isInitialState()) {
      return "Initial state created with " + diff.getNewState().getShapes().size() + " shapes";
    }

    if (diff.hasNoChanges()) {
      return "No changes";
    }

    StringBuilder summary = new StringBuilder();

    if (!diff.getShapeChanges().isEmpty()) {
      summary.append("Shapes: ").append(summarizeShapeChanges(diff.getShapeChanges()));
    }

    if (!diff.getSettingChanges().isEmpty()) {
      if (summary.length() > 0) summary.append(", ");
      summary.append("Settings: ").append(diff.getSettingChanges().size()).append(" changed");
    }

    if (!diff.getArrayChanges().isEmpty()) {
      if (summary.length() > 0) summary.append(", ");
      summary.append("Arrays: ").append(diff.getArrayChanges().size()).append(" changed");
    }

    if (!diff.getListChanges().isEmpty()) {
      if (summary.length() > 0) summary.append(", ");
      summary.append("Lists: ").append(diff.getListChanges().size()).append(" changed");
    }

    return summary.toString();
  }

  /**
   * Creates a detailed report of the differences.
   *
   * @param diff the state difference
   * @return a detailed report string
   */
  public String createDetailedReport(StateDiff diff) {
    StringBuilder report = new StringBuilder();

    if (diff.isInitialState()) {
      report.append("=== INITIAL STATE ===\n");
      report
          .append("Created with ")
          .append(diff.getNewState().getShapes().size())
          .append(" shapes\n");
      return report.toString();
    }

    if (diff.hasNoChanges()) {
      report.append("=== NO CHANGES ===\n");
      return report.toString();
    }

    report.append("=== STATE CHANGES ===\n");

    // Shape changes
    if (!diff.getShapeChanges().isEmpty()) {
      report.append("\n--- SHAPE CHANGES ---\n");
      for (ShapeChange change : diff.getShapeChanges()) {
        report.append(formatShapeChange(change)).append("\n");
      }
    }

    // Setting changes
    if (!diff.getSettingChanges().isEmpty()) {
      report.append("\n--- SETTING CHANGES ---\n");
      for (SettingChange change : diff.getSettingChanges()) {
        report.append(formatSettingChange(change)).append("\n");
      }
    }

    // Array changes
    if (!diff.getArrayChanges().isEmpty()) {
      report.append("\n--- ARRAY CHANGES ---\n");
      for (CollectionChange change : diff.getArrayChanges()) {
        report.append(formatCollectionChange("Array", change)).append("\n");
      }
    }

    // List changes
    if (!diff.getListChanges().isEmpty()) {
      report.append("\n--- LIST CHANGES ---\n");
      for (CollectionChange change : diff.getListChanges()) {
        report.append(formatCollectionChange("List", change)).append("\n");
      }
    }

    return report.toString();
  }

  private void compareSettings(
      DrawingState oldState, DrawingState newState, StateDiff.Builder builder) {
    Map<String, Object> oldSettings = oldState.getSettings();
    Map<String, Object> newSettings = newState.getSettings();

    // Find all keys from both maps
    Set<String> allKeys = new HashSet<>(oldSettings.keySet());
    allKeys.addAll(newSettings.keySet());

    for (String key : allKeys) {
      Object oldValue = oldSettings.get(key);
      Object newValue = newSettings.get(key);

      if (!Objects.equals(oldValue, newValue)) {
        ChangeType changeType;
        if (oldValue == null) {
          changeType = ChangeType.ADDED;
        } else if (newValue == null) {
          changeType = ChangeType.REMOVED;
        } else {
          changeType = ChangeType.MODIFIED;
        }

        builder.addSettingChange(new SettingChange(key, oldValue, newValue, changeType));
      }
    }
  }

  private void compareShapes(
      DrawingState oldState, DrawingState newState, StateDiff.Builder builder) {
    List<Shape> oldShapes = oldState.getShapes();
    List<Shape> newShapes = newState.getShapes();

    int oldSize = oldShapes.size();
    int newSize = newShapes.size();
    int minSize = Math.min(oldSize, newSize);

    // Compare existing shapes
    for (int i = 0; i < minSize; i++) {
      Shape oldShape = oldShapes.get(i);
      Shape newShape = newShapes.get(i);

      if (!Objects.equals(oldShape, newShape)) {
        builder.addShapeChange(new ShapeChange(i, oldShape, newShape, ChangeType.MODIFIED));
      }
    }

    // Handle added shapes
    for (int i = minSize; i < newSize; i++) {
      builder.addShapeChange(new ShapeChange(i, null, newShapes.get(i), ChangeType.ADDED));
    }

    // Handle removed shapes
    for (int i = minSize; i < oldSize; i++) {
      builder.addShapeChange(new ShapeChange(i, oldShapes.get(i), null, ChangeType.REMOVED));
    }
  }

  private void compareArrays(
      DrawingState oldState, DrawingState newState, StateDiff.Builder builder) {
    compareCollections(oldState.getArrays(), newState.getArrays(), builder::addArrayChange);
  }

  private void compareLists(
      DrawingState oldState, DrawingState newState, StateDiff.Builder builder) {
    compareCollections(oldState.getLists(), newState.getLists(), builder::addListChange);
  }

  private void compareCollections(
      Map<String, List<Object>> oldCollections,
      Map<String, List<Object>> newCollections,
      java.util.function.Consumer<CollectionChange> changeConsumer) {
    Set<String> allKeys = new HashSet<>(oldCollections.keySet());
    allKeys.addAll(newCollections.keySet());

    for (String key : allKeys) {
      List<Object> oldCollection = oldCollections.get(key);
      List<Object> newCollection = newCollections.get(key);

      if (!Objects.equals(oldCollection, newCollection)) {
        ChangeType changeType;
        if (oldCollection == null) {
          changeType = ChangeType.ADDED;
        } else if (newCollection == null) {
          changeType = ChangeType.REMOVED;
        } else {
          changeType = ChangeType.MODIFIED;
        }

        changeConsumer.accept(new CollectionChange(key, oldCollection, newCollection, changeType));
      }
    }
  }

  private String summarizeShapeChanges(List<ShapeChange> shapeChanges) {
    int added = 0;
    int modified = 0;
    int removed = 0;

    for (ShapeChange change : shapeChanges) {
      switch (change.getChangeType()) {
        case ADDED:
          added++;
          break;
        case MODIFIED:
          modified++;
          break;
        case REMOVED:
          removed++;
          break;
      }
    }

    List<String> parts = new ArrayList<>();
    if (added > 0) parts.add("+" + added);
    if (modified > 0) parts.add("~" + modified);
    if (removed > 0) parts.add("-" + removed);

    return String.join(", ", parts);
  }

  private String formatShapeChange(ShapeChange change) {
    switch (change.getChangeType()) {
      case ADDED:
        return String.format(
            "  [%d] Added: %s", change.getIndex(), formatShapeInfo(change.getNewShape()));
      case REMOVED:
        return String.format(
            "  [%d] Removed: %s", change.getIndex(), formatShapeInfo(change.getOldShape()));
      case MODIFIED:
        return String.format(
            "  [%d] Modified: %s → %s",
            change.getIndex(),
            formatShapeInfo(change.getOldShape()),
            formatShapeInfo(change.getNewShape()));
      default:
        return "  Unknown change";
    }
  }

  private String formatSettingChange(SettingChange change) {
    switch (change.getChangeType()) {
      case ADDED:
        return String.format("  %s: Added = %s", change.getKey(), change.getNewValue());
      case REMOVED:
        return String.format("  %s: Removed (was %s)", change.getKey(), change.getOldValue());
      case MODIFIED:
        return String.format(
            "  %s: %s → %s", change.getKey(), change.getOldValue(), change.getNewValue());
      default:
        return "  Unknown change";
    }
  }

  private String formatCollectionChange(String type, CollectionChange change) {
    switch (change.getChangeType()) {
      case ADDED:
        return String.format(
            "  %s '%s': Added (%d items)",
            type,
            change.getKey(),
            change.getNewCollection() != null ? change.getNewCollection().size() : 0);
      case REMOVED:
        return String.format(
            "  %s '%s': Removed (had %d items)",
            type,
            change.getKey(),
            change.getOldCollection() != null ? change.getOldCollection().size() : 0);
      case MODIFIED:
        int oldSize = change.getOldCollection() != null ? change.getOldCollection().size() : 0;
        int newSize = change.getNewCollection() != null ? change.getNewCollection().size() : 0;
        return String.format(
            "  %s '%s': Modified (%d → %d items)", type, change.getKey(), oldSize, newSize);
      default:
        return "  Unknown change";
    }
  }

  private String formatShapeInfo(Shape shape) {
    if (shape == null) return "null";
    return shape.getClass().getSimpleName();
  }

  // Inner classes for representing changes

  public enum ChangeType {
    ADDED,
    REMOVED,
    MODIFIED
  }

  public static class ShapeChange {
    private final int index;
    private final Shape oldShape;
    private final Shape newShape;
    private final ChangeType changeType;

    public ShapeChange(int index, Shape oldShape, Shape newShape, ChangeType changeType) {
      this.index = index;
      this.oldShape = oldShape;
      this.newShape = newShape;
      this.changeType = changeType;
    }

    public int getIndex() {
      return index;
    }

    public Shape getOldShape() {
      return oldShape;
    }

    public Shape getNewShape() {
      return newShape;
    }

    public ChangeType getChangeType() {
      return changeType;
    }
  }

  public static class SettingChange {
    private final String key;
    private final Object oldValue;
    private final Object newValue;
    private final ChangeType changeType;

    public SettingChange(String key, Object oldValue, Object newValue, ChangeType changeType) {
      this.key = key;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.changeType = changeType;
    }

    public String getKey() {
      return key;
    }

    public Object getOldValue() {
      return oldValue;
    }

    public Object getNewValue() {
      return newValue;
    }

    public ChangeType getChangeType() {
      return changeType;
    }
  }

  public static class CollectionChange {
    private final String key;
    private final List<Object> oldCollection;
    private final List<Object> newCollection;
    private final ChangeType changeType;

    public CollectionChange(
        String key, List<Object> oldCollection, List<Object> newCollection, ChangeType changeType) {
      this.key = key;
      this.oldCollection = oldCollection;
      this.newCollection = newCollection;
      this.changeType = changeType;
    }

    public String getKey() {
      return key;
    }

    public List<Object> getOldCollection() {
      return oldCollection;
    }

    public List<Object> getNewCollection() {
      return newCollection;
    }

    public ChangeType getChangeType() {
      return changeType;
    }
  }

  public static class StateDiff {
    private final DrawingState oldState;
    private final DrawingState newState;
    private final List<ShapeChange> shapeChanges;
    private final List<SettingChange> settingChanges;
    private final List<CollectionChange> arrayChanges;
    private final List<CollectionChange> listChanges;
    private final boolean isInitialState;

    private StateDiff(Builder builder) {
      this.oldState = builder.oldState;
      this.newState = builder.newState;
      this.shapeChanges = Collections.unmodifiableList(builder.shapeChanges);
      this.settingChanges = Collections.unmodifiableList(builder.settingChanges);
      this.arrayChanges = Collections.unmodifiableList(builder.arrayChanges);
      this.listChanges = Collections.unmodifiableList(builder.listChanges);
      this.isInitialState = builder.isInitialState;
    }

    public static StateDiff initialState(DrawingState newState) {
      Builder builder = new Builder(null, newState);
      builder.isInitialState = true;
      return builder.build();
    }

    public DrawingState getOldState() {
      return oldState;
    }

    public DrawingState getNewState() {
      return newState;
    }

    public List<ShapeChange> getShapeChanges() {
      return shapeChanges;
    }

    public List<SettingChange> getSettingChanges() {
      return settingChanges;
    }

    public List<CollectionChange> getArrayChanges() {
      return arrayChanges;
    }

    public List<CollectionChange> getListChanges() {
      return listChanges;
    }

    public boolean isInitialState() {
      return isInitialState;
    }

    public boolean hasNoChanges() {
      return !isInitialState
          && shapeChanges.isEmpty()
          && settingChanges.isEmpty()
          && arrayChanges.isEmpty()
          && listChanges.isEmpty();
    }

    public int getTotalChanges() {
      return shapeChanges.size() + settingChanges.size() + arrayChanges.size() + listChanges.size();
    }

    public static class Builder {
      private final DrawingState oldState;
      private final DrawingState newState;
      private final List<ShapeChange> shapeChanges = new ArrayList<>();
      private final List<SettingChange> settingChanges = new ArrayList<>();
      private final List<CollectionChange> arrayChanges = new ArrayList<>();
      private final List<CollectionChange> listChanges = new ArrayList<>();
      private boolean isInitialState = false;

      public Builder(DrawingState oldState, DrawingState newState) {
        this.oldState = oldState;
        this.newState = newState;
      }

      public Builder addShapeChange(ShapeChange change) {
        shapeChanges.add(change);
        return this;
      }

      public Builder addSettingChange(SettingChange change) {
        settingChanges.add(change);
        return this;
      }

      public Builder addArrayChange(CollectionChange change) {
        arrayChanges.add(change);
        return this;
      }

      public Builder addListChange(CollectionChange change) {
        listChanges.add(change);
        return this;
      }

      public StateDiff build() {
        return new StateDiff(this);
      }
    }
  }
}
