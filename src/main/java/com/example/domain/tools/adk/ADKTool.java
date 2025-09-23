package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;
import java.util.Map;

/**
 * Interface for ADK-compatible tools. Tools implementing this interface can be used directly with
 * ADK's LlmAgent. Note: FunctionDeclaration support removed - ADK 0.3 uses different tool
 * declaration mechanism
 */
public interface ADKTool {

  /**
   * Execute the tool with the given parameters.
   *
   * @param parameters The parameters from the LLM
   * @param state The current drawing state
   * @return The result of executing the tool
   */
  ToolResult execute(Map<String, Object> parameters, DrawingState state);



  /**
   * Result of tool execution
   */
  class ToolResult {
    private final boolean success;
    private final DrawingState newState;
    private final String message;

    public ToolResult(boolean success, DrawingState newState, String message) {
      this.success = success;
      this.newState = newState;
      this.message = message;

    }

    public boolean isSuccess() {
      return success;
    }

    public DrawingState getState() {
      return newState;
    } // Added getState() alias

    public String getMessage() {
      return message;
    }


    public static ToolResult success(DrawingState newState, String message) {
      return new ToolResult(true, newState, message);
    }

    public static ToolResult failure(DrawingState state, String message) {
      return new ToolResult(false, state, message);
    }
  }
}
