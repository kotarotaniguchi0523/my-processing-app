package com.example.events;

import java.util.HashMap;
import java.util.Map;

/** Represents system-level events such as save/load operations, session management, and errors. */
public class SystemEvent extends Event {
  private final String operation;
  private final String filePath;
  private final Map<String, Object> metadata;
  private final Exception error;
  private final boolean success;

  // Constructor for file operations
  public SystemEvent(EventType type, String operation, String filePath) {
    super(type);
    this.operation = operation;
    this.filePath = filePath;
    this.metadata = new HashMap<>();
    this.error = null;
    this.success = true;
  }

  // Constructor for error events
  public SystemEvent(EventType type, String operation, Exception error) {
    super(type);
    this.operation = operation;
    this.filePath = null;
    this.metadata = new HashMap<>();
    this.error = error;
    this.success = false;
  }

  // Constructor for general system events
  public SystemEvent(EventType type, String operation, boolean success) {
    super(type);
    this.operation = operation;
    this.filePath = null;
    this.metadata = new HashMap<>();
    this.error = null;
    this.success = success;
  }

  // Constructor with metadata
  public SystemEvent(EventType type, String operation, Map<String, Object> metadata) {
    super(type);
    this.operation = operation;
    this.filePath = null;
    this.metadata = new HashMap<>(metadata);
    this.error = null;
    this.success = true;
  }

  // Constructor for complete control
  public SystemEvent(
      EventType type,
      String operation,
      String filePath,
      Map<String, Object> metadata,
      Exception error,
      boolean success) {
    super(type);
    this.operation = operation;
    this.filePath = filePath;
    this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    this.error = error;
    this.success = success;
  }

  // Getters
  public String getOperation() {
    return operation;
  }

  public String getFilePath() {
    return filePath;
  }

  public Map<String, Object> getMetadata() {
    return new HashMap<>(metadata);
  }

  public Exception getError() {
    return error;
  }

  public boolean isSuccess() {
    return success;
  }

  // Metadata manipulation
  public void addMetadata(String key, Object value) {
    metadata.put(key, value);
  }

  public Object getMetadata(String key) {
    return metadata.get(key);
  }

  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  // Helper methods
  public boolean hasError() {
    return error != null;
  }

  public boolean isFileOperation() {
    return filePath != null && !filePath.isEmpty();
  }

  public String getErrorMessage() {
    return hasError() ? error.getMessage() : null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            "SystemEvent[type=%s, operation=%s, success=%s", getType(), operation, success));

    if (filePath != null) {
      sb.append(String.format(", filePath=%s", filePath));
    }

    if (hasError()) {
      sb.append(String.format(", error=%s", error.getMessage()));
    }

    if (!metadata.isEmpty()) {
      sb.append(String.format(", metadata=%s", metadata));
    }

    sb.append("]");
    return sb.toString();
  }
}
