package com.example.domain.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SessionSettings contains configuration and preferences for a drawing session Immutable value
 * object for session configuration
 */
public final class SessionSettings {
  private final boolean autoSave;
  private final int autoSaveIntervalMinutes;
  private final int maxUndoHistory;
  private final boolean enableGridSnap;
  private final float gridSize;
  private final boolean showGrid;
  private final String preferredCanvasSize;
  private final Map<String, Object> customSettings;

  public SessionSettings(
      boolean autoSave,
      int autoSaveIntervalMinutes,
      int maxUndoHistory,
      boolean enableGridSnap,
      float gridSize,
      boolean showGrid,
      String preferredCanvasSize,
      Map<String, Object> customSettings) {
    this.autoSave = autoSave;
    this.autoSaveIntervalMinutes = Math.max(1, autoSaveIntervalMinutes);
    this.maxUndoHistory = Math.max(1, maxUndoHistory);
    this.enableGridSnap = enableGridSnap;
    this.gridSize = Math.max(1.0f, gridSize);
    this.showGrid = showGrid;
    this.preferredCanvasSize = preferredCanvasSize != null ? preferredCanvasSize : "800x600";
    this.customSettings = customSettings != null ? new HashMap<>(customSettings) : new HashMap<>();
  }

  // Builder pattern for SessionSettings
  public static class Builder {
    private boolean autoSave = true;
    private int autoSaveIntervalMinutes = 5;
    private int maxUndoHistory = 50;
    private boolean enableGridSnap = false;
    private float gridSize = 10.0f;
    private boolean showGrid = false;
    private String preferredCanvasSize = "800x600";
    private Map<String, Object> customSettings = new HashMap<>();

    public Builder autoSave(boolean autoSave) {
      this.autoSave = autoSave;
      return this;
    }

    public Builder autoSaveIntervalMinutes(int autoSaveIntervalMinutes) {
      this.autoSaveIntervalMinutes = autoSaveIntervalMinutes;
      return this;
    }

    public Builder maxUndoHistory(int maxUndoHistory) {
      this.maxUndoHistory = maxUndoHistory;
      return this;
    }

    public Builder enableGridSnap(boolean enableGridSnap) {
      this.enableGridSnap = enableGridSnap;
      return this;
    }

    public Builder gridSize(float gridSize) {
      this.gridSize = gridSize;
      return this;
    }

    public Builder showGrid(boolean showGrid) {
      this.showGrid = showGrid;
      return this;
    }

    public Builder preferredCanvasSize(String preferredCanvasSize) {
      this.preferredCanvasSize = preferredCanvasSize;
      return this;
    }

    public Builder customSettings(Map<String, Object> customSettings) {
      this.customSettings = new HashMap<>(customSettings);
      return this;
    }

    public Builder addCustomSetting(String key, Object value) {
      this.customSettings.put(key, value);
      return this;
    }

    public SessionSettings build() {
      return new SessionSettings(
          autoSave,
          autoSaveIntervalMinutes,
          maxUndoHistory,
          enableGridSnap,
          gridSize,
          showGrid,
          preferredCanvasSize,
          customSettings);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  // Factory methods for common configurations
  public static SessionSettings defaultSettings() {
    return new Builder().build();
  }

  public static SessionSettings quickSession() {
    return builder().autoSave(false).maxUndoHistory(20).build();
  }

  public static SessionSettings powerUserSettings() {
    return builder()
        .autoSave(true)
        .autoSaveIntervalMinutes(2)
        .maxUndoHistory(100)
        .enableGridSnap(true)
        .showGrid(true)
        .gridSize(5.0f)
        .build();
  }

  // Getters
  public boolean isAutoSave() {
    return autoSave;
  }

  public int getAutoSaveIntervalMinutes() {
    return autoSaveIntervalMinutes;
  }

  public int getMaxUndoHistory() {
    return maxUndoHistory;
  }

  public boolean isEnableGridSnap() {
    return enableGridSnap;
  }

  public float getGridSize() {
    return gridSize;
  }

  public boolean isShowGrid() {
    return showGrid;
  }

  public String getPreferredCanvasSize() {
    return preferredCanvasSize;
  }

  public Map<String, Object> getCustomSettings() {
    return new HashMap<>(customSettings);
  }

  // Helper methods for custom settings
  public Object getCustomSetting(String key) {
    return customSettings.get(key);
  }

  public String getStringCustomSetting(String key, String defaultValue) {
    Object value = customSettings.get(key);
    return value instanceof String ? (String) value : defaultValue;
  }

  public Integer getIntCustomSetting(String key, Integer defaultValue) {
    Object value = customSettings.get(key);
    return value instanceof Number ? ((Number) value).intValue() : defaultValue;
  }

  public Boolean getBooleanCustomSetting(String key, Boolean defaultValue) {
    Object value = customSettings.get(key);
    return value instanceof Boolean ? (Boolean) value : defaultValue;
  }

  // Parse canvas size
  public int getCanvasWidth() {
    String[] parts = preferredCanvasSize.split("x");
    try {
      return Integer.parseInt(parts[0]);
    } catch (Exception e) {
      return 800; // Default width
    }
  }

  public int getCanvasHeight() {
    String[] parts = preferredCanvasSize.split("x");
    try {
      return parts.length > 1 ? Integer.parseInt(parts[1]) : 600;
    } catch (Exception e) {
      return 600; // Default height
    }
  }

  // Immutable update methods
  public SessionSettings withAutoSave(boolean autoSave) {
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        customSettings);
  }

  public SessionSettings withAutoSaveInterval(int autoSaveIntervalMinutes) {
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        customSettings);
  }

  public SessionSettings withMaxUndoHistory(int maxUndoHistory) {
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        customSettings);
  }

  public SessionSettings withGridSettings(boolean enableSnap, boolean showGrid, float gridSize) {
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        customSettings);
  }

  public SessionSettings withCanvasSize(String canvasSize) {
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        canvasSize,
        customSettings);
  }

  public SessionSettings withCanvasSize(int width, int height) {
    return withCanvasSize(width + "x" + height);
  }

  public SessionSettings withCustomSetting(String key, Object value) {
    Map<String, Object> newCustomSettings = new HashMap<>(customSettings);
    newCustomSettings.put(key, value);
    return new SessionSettings(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        newCustomSettings);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SessionSettings)) return false;
    SessionSettings settings = (SessionSettings) obj;
    return autoSave == settings.autoSave
        && autoSaveIntervalMinutes == settings.autoSaveIntervalMinutes
        && maxUndoHistory == settings.maxUndoHistory
        && enableGridSnap == settings.enableGridSnap
        && Float.compare(settings.gridSize, gridSize) == 0
        && showGrid == settings.showGrid
        && Objects.equals(preferredCanvasSize, settings.preferredCanvasSize)
        && Objects.equals(customSettings, settings.customSettings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap,
        gridSize,
        showGrid,
        preferredCanvasSize,
        customSettings);
  }

  @Override
  public String toString() {
    return String.format(
        "SessionSettings(autoSave=%s, interval=%dm, undoHistory=%d, grid=%s, canvas=%s)",
        autoSave,
        autoSaveIntervalMinutes,
        maxUndoHistory,
        enableGridSnap ? "enabled" : "disabled",
        preferredCanvasSize);
  }
}
