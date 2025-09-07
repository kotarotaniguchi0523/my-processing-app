package com.example.agent.tools;

import com.example.domain.models.DrawingState;
import com.example.domain.serialization.YamlSerializable;
import com.example.domain.serialization.YamlUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ToolResult represents the result of executing a tool Contains information about success/failure,
 * output data, and any state changes
 */
public final class ToolResult implements YamlSerializable {

  public enum Status {
    SUCCESS, // Tool executed successfully
    ERROR, // Tool execution failed
    PENDING, // Tool execution is pending approval or completion
    CANCELLED // Tool execution was cancelled
  }

  private final Status status;
  private final String message;
  private final Map<String, Object> data;
  private final DrawingState newState;
  private final String errorType;
  private final long executionTime;
  private final String toolName;

  private ToolResult(
      Status status,
      String message,
      Map<String, Object> data,
      DrawingState newState,
      String errorType,
      long executionTime,
      String toolName) {
    this.status = Objects.requireNonNull(status, "Status cannot be null");
    this.message = message != null ? message : "";
    this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    this.newState = newState;
    this.errorType = errorType;
    this.executionTime = executionTime;
    this.toolName = toolName != null ? toolName : "";
  }

  // Builder pattern for creating ToolResult instances
  public static class Builder {
    private Status status = Status.SUCCESS;
    private String message = "";
    private Map<String, Object> data = new HashMap<>();
    private DrawingState newState;
    private String errorType;
    private long executionTime = 0;
    private String toolName = "";

    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder data(Map<String, Object> data) {
      this.data = new HashMap<>(data);
      return this;
    }

    public Builder addData(String key, Object value) {
      this.data.put(key, value);
      return this;
    }

    public Builder newState(DrawingState newState) {
      this.newState = newState;
      return this;
    }

    public Builder errorType(String errorType) {
      this.errorType = errorType;
      return this;
    }

    public Builder executionTime(long executionTime) {
      this.executionTime = executionTime;
      return this;
    }

    public Builder toolName(String toolName) {
      this.toolName = toolName;
      return this;
    }

    public ToolResult build() {
      return new ToolResult(status, message, data, newState, errorType, executionTime, toolName);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  // Static factory methods for common result types
  public static ToolResult success(String message) {
    return builder().status(Status.SUCCESS).message(message).build();
  }

  public static ToolResult success(String message, DrawingState newState) {
    return builder().status(Status.SUCCESS).message(message).newState(newState).build();
  }

  public static ToolResult success(String message, Map<String, Object> data) {
    return builder().status(Status.SUCCESS).message(message).data(data).build();
  }

  public static ToolResult error(String message) {
    return builder().status(Status.ERROR).message(message).build();
  }

  public static ToolResult error(String message, String errorType) {
    return builder().status(Status.ERROR).message(message).errorType(errorType).build();
  }

  public static ToolResult pending(String message) {
    return builder().status(Status.PENDING).message(message).build();
  }

  public static ToolResult cancelled(String message) {
    return builder().status(Status.CANCELLED).message(message).build();
  }

  // Getters
  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, Object> getData() {
    return new HashMap<>(data);
  }

  public Object getData(String key) {
    return data.get(key);
  }

  public String getStringData(String key, String defaultValue) {
    Object value = data.get(key);
    return value instanceof String ? (String) value : defaultValue;
  }

  public DrawingState getNewState() {
    return newState;
  }

  public String getErrorType() {
    return errorType;
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public String getToolName() {
    return toolName;
  }

  // Utility methods
  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }

  public boolean isError() {
    return status == Status.ERROR;
  }

  public boolean isPending() {
    return status == Status.PENDING;
  }

  public boolean isCancelled() {
    return status == Status.CANCELLED;
  }

  public boolean hasNewState() {
    return newState != null;
  }

  public boolean hasData() {
    return !data.isEmpty();
  }

  public boolean hasError() {
    return errorType != null && !errorType.isEmpty();
  }

  // Create a new result with updated status
  public ToolResult withStatus(Status newStatus) {
    return new Builder()
        .status(newStatus)
        .message(message)
        .data(data)
        .newState(newState)
        .errorType(errorType)
        .executionTime(executionTime)
        .toolName(toolName)
        .build();
  }

  // Create a new result with additional data
  public ToolResult withData(String key, Object value) {
    Map<String, Object> newData = new HashMap<>(data);
    newData.put(key, value);
    return new Builder()
        .status(status)
        .message(message)
        .data(newData)
        .newState(newState)
        .errorType(errorType)
        .executionTime(executionTime)
        .toolName(toolName)
        .build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ToolResult)) return false;
    ToolResult result = (ToolResult) obj;
    return status == result.status
        && Objects.equals(message, result.message)
        && Objects.equals(data, result.data)
        && Objects.equals(newState, result.newState)
        && Objects.equals(errorType, result.errorType)
        && executionTime == result.executionTime
        && Objects.equals(toolName, result.toolName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, message, data, newState, errorType, executionTime, toolName);
  }

  @Override
  public String toString() {
    return String.format(
        "ToolResult(status=%s, tool=%s, message='%s', hasNewState=%s)",
        status, toolName, message, hasNewState());
  }

  // YAML Serialization
  @Override
  public Map<String, Object> toYamlMap() {
    Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
    map.put("status", status.name());
    map.put("message", message);
    map.put("data", new HashMap<>(data));
    map.put("toolName", toolName);
    map.put("executionTime", executionTime);

    if (errorType != null) {
      map.put("errorType", errorType);
    }

    if (newState != null) {
      map.put("newState", newState.toYamlMap());
    }

    return map;
  }

  @Override
  public String getYamlType() {
    return "tool_result";
  }

  @SuppressWarnings("unchecked")
  public static ToolResult fromYamlMap(Map<String, Object> yamlMap) {
    YamlUtils.validateYamlType(yamlMap, "tool_result");

    String statusStr = YamlUtils.getString(yamlMap, "status", "SUCCESS");
    Status status = Status.valueOf(statusStr);

    String message = YamlUtils.getString(yamlMap, "message", "");
    String toolName = YamlUtils.getString(yamlMap, "toolName", "");
    String errorType = YamlUtils.getString(yamlMap, "errorType", null);
    long executionTime = YamlUtils.getLong(yamlMap, "executionTime", 0L);

    Map<String, Object> data = YamlUtils.getMap(yamlMap, "data", new HashMap<>());

    DrawingState newState = null;
    Map<String, Object> stateMap = YamlUtils.getMap(yamlMap, "newState", null);
    if (stateMap != null) {
      newState = DrawingState.fromYamlMap(stateMap);
    }

    return new Builder()
        .status(status)
        .message(message)
        .toolName(toolName)
        .errorType(errorType)
        .executionTime(executionTime)
        .data(data)
        .newState(newState)
        .build();
  }
}
