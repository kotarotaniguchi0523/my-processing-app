package com.example.application.tools;

import com.example.domain.tools.IAgentTool;
import com.example.infrastructure.agent.tools.CreateShapeTool;
import com.example.infrastructure.agent.tools.ClearCanvasTool;
import com.example.infrastructure.agent.tools.DeleteShapeTool;
import com.example.infrastructure.agent.tools.ModifyShapeTool;
import com.example.infrastructure.agent.tools.GetStateTool;
import com.example.infrastructure.agent.tools.UndoRedoTool;

import java.util.logging.Logger;

/**
 * ToolInitializer handles the registration of all domain tools in the ToolRegistry. Provides a
 * centralized way to initialize the tool system at application startup.
 */
public class ToolInitializer {
  private static final Logger LOGGER = Logger.getLogger(ToolInitializer.class.getName());

  private static boolean initialized = false;

  /**
   * Initialize and register all domain tools in the ToolRegistry. This method is idempotent -
   * calling it multiple times has no effect.
   */
  public static void initialize() {
    if (initialized) {
      if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
        LOGGER.info("ToolInitializer already initialized, skipping");
      }
      return;
    }

    ToolRegistry registry = ToolRegistry.getInstance();

    try {
      // Register all domain tools
      registry.registerTool(new CreateShapeTool());
      registry.registerTool(new ModifyShapeTool());
      registry.registerTool(new DeleteShapeTool());
      registry.registerTool(new ClearCanvasTool());
      registry.registerTool(new GetStateTool());
      registry.registerTool(new UndoRedoTool());

      if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
        LOGGER.info("Successfully registered " + registry.getToolCount() + " domain tools");
      }

      // Log registered tools for debugging
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        for (String toolName : registry.getToolNames()) {
          LOGGER.fine("Registered tool: " + toolName);
        }
      }

      initialized = true;

    } catch (Exception e) {
      if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
        LOGGER.severe("Failed to initialize domain tools: " + e.getMessage());
      }
      throw new RuntimeException("Failed to initialize domain tools", e);
    }
  }

  /**
   * Check if the tool system has been initialized.
   *
   * @return true if tools have been registered, false otherwise
   */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * Get the number of registered tools.
   *
   * @return the number of registered tools, or 0 if not initialized
   */
  public static int getRegisteredToolCount() {
    if (!initialized) {
      return 0;
    }
    return ToolRegistry.getInstance().getToolCount();
  }

  /**
   * Reset the initialization state (primarily for testing). This clears the registry and resets the
   * initialized flag.
   */
  public static void reset() {
    if (initialized) {
      ToolRegistry.getInstance().clearAllTools();
      initialized = false;
      if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
        LOGGER.info("ToolInitializer reset - all tools cleared");
      }
    }
  }

  /**
   * Validate that all expected tools are registered and functional.
   *
   * @throws IllegalStateException if validation fails
   */
  public static void validate() {
    if (!initialized) {
      throw new IllegalStateException("ToolInitializer has not been initialized");
    }

    ToolRegistry registry = ToolRegistry.getInstance();

    // Check that expected tools are registered
    String[] expectedTools = {"createShape", "modifyShape", "deleteShape", "clearCanvas", "get_state", "undo_redo"};

    for (String expectedTool : expectedTools) {
      if (!registry.hasTool(expectedTool)) {
        throw new IllegalStateException("Expected tool not registered: " + expectedTool);
      }
    }

    // Validate tool schemas
    for (IAgentTool tool : registry.getAllTools()) {
      if (!registry.validateToolSchema(tool)) {
        throw new IllegalStateException("Invalid schema for tool: " + tool.name());
      }
    }

    if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
      LOGGER.info("Tool validation completed successfully");
    }
  }
}
