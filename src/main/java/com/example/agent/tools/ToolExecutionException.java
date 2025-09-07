package com.example.agent.tools;

/** Exception thrown when a tool execution fails Provides additional context about the failure */
public class ToolExecutionException extends Exception {
  private static final long serialVersionUID = 1L;

  private final String toolName;
  private final String errorType;
  private final Object context;

  public ToolExecutionException(String message) {
    super(message);
    this.toolName = null;
    this.errorType = null;
    this.context = null;
  }

  public ToolExecutionException(String message, Throwable cause) {
    super(message, cause);
    this.toolName = null;
    this.errorType = null;
    this.context = null;
  }

  public ToolExecutionException(String toolName, String message) {
    super(message);
    this.toolName = toolName;
    this.errorType = null;
    this.context = null;
  }

  public ToolExecutionException(String toolName, String message, Throwable cause) {
    super(message, cause);
    this.toolName = toolName;
    this.errorType = null;
    this.context = null;
  }

  public ToolExecutionException(String toolName, String message, String errorType) {
    super(message);
    this.toolName = toolName;
    this.errorType = errorType;
    this.context = null;
  }

  public ToolExecutionException(String toolName, String message, String errorType,
      Throwable cause) {
    super(message, cause);
    this.toolName = toolName;
    this.errorType = errorType;
    this.context = null;
  }

  public ToolExecutionException(String toolName, String message, String errorType, Object context) {
    super(message);
    this.toolName = toolName;
    this.errorType = errorType;
    this.context = context;
  }

  public ToolExecutionException(String toolName, String message, String errorType, Object context,
      Throwable cause) {
    super(message, cause);
    this.toolName = toolName;
    this.errorType = errorType;
    this.context = context;
  }

  // Getters
  public String getToolName() {
    return toolName;
  }

  public String getErrorType() {
    return errorType;
  }

  public Object getContext() {
    return context;
  }

  // Utility methods
  public boolean hasToolName() {
    return toolName != null && !toolName.isEmpty();
  }

  public boolean hasErrorType() {
    return errorType != null && !errorType.isEmpty();
  }

  public boolean hasContext() {
    return context != null;
  }

  // Create ToolResult from this exception
  public ToolResult toToolResult() {
    ToolResult.Builder builder =
        ToolResult.builder().status(ToolResult.Status.ERROR).message(getMessage());

    if (hasToolName()) {
      builder.toolName(toolName);
    }

    if (hasErrorType()) {
      builder.errorType(errorType);
    }

    if (hasContext()) {
      builder.addData("errorContext", context);
    }

    return builder.build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());

    if (hasToolName()) {
      sb.append('[').append(toolName).append(']');
    }

    if (hasErrorType()) {
      sb.append('(').append(errorType).append(')');
    }

    sb.append(':').append(' ').append(getMessage());

    return sb.toString();
  }

  // Static factory methods for common error types
  public static ToolExecutionException invalidParameters(String toolName, String message) {
    return new ToolExecutionException(toolName, message, "INVALID_PARAMETERS");
  }

  public static ToolExecutionException stateError(String toolName, String message) {
    return new ToolExecutionException(toolName, message, "STATE_ERROR");
  }

  public static ToolExecutionException systemError(String toolName, String message,
      Throwable cause) {
    return new ToolExecutionException(toolName, message, "SYSTEM_ERROR", null, cause);
  }

  public static ToolExecutionException approvalRequired(String toolName, String message) {
    return new ToolExecutionException(toolName, message, "APPROVAL_REQUIRED");
  }

  public static ToolExecutionException timeout(String toolName, String message) {
    return new ToolExecutionException(toolName, message, "TIMEOUT");
  }
}
