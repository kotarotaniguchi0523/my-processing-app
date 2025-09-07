package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.AbstractTool;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import java.util.Map;

/**
 * ClearCanvasTool allows the AI agent to clear the drawing canvas Removes all shapes and optionally
 * resets settings
 */
public class ClearCanvasTool extends AbstractTool {

  public ClearCanvasTool() {
    super(
        "clear_canvas",
        "Clear all shapes from the drawing canvas",
        "drawing",
        true, // requires approval
        true); // state modifying
  }

  @Override
  protected void defineParameters() {
    addParameter(
        "preserveSettings",
        Boolean.class,
        "Keep current canvas settings (width, height, background color)",
        false,
        true);
    addParameter(
        "resetToDefault",
        Boolean.class,
        "Reset canvas to default state instead of just clearing shapes",
        false,
        false);
    addParameter(
        "backgroundColor",
        String.class,
        "Set new background color (hex format, e.g., #FFFFFF)",
        false);
  }

  @Override
  protected ToolResult executeInternal(Map<String, Object> parameters, AgentContext context)
      throws ToolExecutionException {

    DrawingState currentState = requireCurrentState(context);

    // Extract parameters
    boolean preserveSettings =
        getOptionalParameter(parameters, "preserveSettings", Boolean.class, true);
    boolean resetToDefault =
        getOptionalParameter(parameters, "resetToDefault", Boolean.class, false);
    String newBackgroundColor =
        getOptionalParameter(parameters, "backgroundColor", String.class, null);

    // Count current shapes for reporting
    int shapesCleared = currentState.getShapes().size();

    DrawingState newState;
    String actionMessage;

    if (resetToDefault) {
      // Reset to completely default state
      newState = DrawingState.defaultState();
      actionMessage =
          String.format("Reset canvas to default state, cleared %d shapes", shapesCleared);
    } else if (preserveSettings) {
      // Keep settings but clear all shapes
      newState = currentState.withShapes(new java.util.ArrayList<>());
      actionMessage =
          String.format("Cleared %d shapes from canvas, preserved settings", shapesCleared);
    } else {
      // Create empty state
      newState = DrawingState.empty();
      actionMessage =
          String.format(
              "Cleared canvas completely, removed %d shapes and all settings", shapesCleared);
    }

    // Apply new background color if specified
    if (newBackgroundColor != null && !newBackgroundColor.trim().isEmpty()) {
      try {
        // Validate color format
        if (!newBackgroundColor.matches("^#[0-9A-Fa-f]{6}$")) {
          throw new IllegalArgumentException("Invalid hex color format. Expected format: #RRGGBB");
        }
        newState = newState.withSetting("backgroundColor", newBackgroundColor);
        actionMessage += String.format(", set background color to %s", newBackgroundColor);
      } catch (IllegalArgumentException e) {
        throw ToolExecutionException.invalidParameters(
            getName(), "Invalid background color: " + e.getMessage());
      }
    }

    return ToolResult.success(actionMessage, newState)
        .withData("shapesCleared", shapesCleared)
        .withData("preserveSettings", preserveSettings)
        .withData("resetToDefault", resetToDefault)
        .withData("canvasWidth", newState.getIntSetting("width", 800))
        .withData("canvasHeight", newState.getIntSetting("height", 600))
        .withData("backgroundColor", newState.getStringSetting("backgroundColor", "#FFFFFF"));
  }
}
