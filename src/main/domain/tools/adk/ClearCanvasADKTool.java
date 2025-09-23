package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;

import java.util.Map;
import java.util.logging.Logger;

/**
 * ADK-compatible tool for clearing the canvas. Directly manipulates DrawingState without UseCase
 * layer.
 */
public class ClearCanvasADKTool implements ADKTool {

  private static final Logger LOGGER = Logger.getLogger(ClearCanvasADKTool.class.getName());

  @Override
  public ToolResult execute(Map<String, Object> parameters, DrawingState state) {
    try {
      LOGGER.info("Executing clear_canvas");

      // Create a new empty state
      DrawingState newState = DrawingState.defaultState();

      String message = "Canvas cleared successfully";
      return ToolResult.success(newState, message);

    } catch (Exception e) {
      LOGGER.severe("Error clearing canvas: " + e.getMessage());
      return ToolResult.failure(state, "Failed to clear canvas: " + e.getMessage());
    }
  }

}
