package com.example.domain.tools;

import com.example.domain.tools.impl.*;
import com.example.application.usecase.api.ToolUseCasePort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for managing and accessing available tools
 */
public class ToolRegistry {
  private static final Logger LOGGER = Logger.getLogger(ToolRegistry.class.getName());
  private static ToolRegistry instance;
  private final Map<String, IAgentTool> tools;
  private boolean initialized = false;
  private ToolUseCasePort port;

  private ToolRegistry() {
    this.tools = new ConcurrentHashMap<>();
  }

  /**
   * Get the singleton instance of the ToolRegistry
   */
  public static synchronized ToolRegistry getInstance() {
    if (instance == null) {
      instance = new ToolRegistry();
      // Defer initialization until an explicit initializeWithPort is called
    }
    return instance;
  }

  /**
   * Initialize the registry with all available tools
   */
  private void initializeInternal(ToolUseCasePort port) {
    if (initialized)
      return;
    this.port = port;
    CreateCircleTool circleTool = new CreateCircleTool();
    CreateEllipseTool ellipseTool = new CreateEllipseTool();
    CreateLineTool lineTool = new CreateLineTool();
    CreateRectangleTool rectTool = new CreateRectangleTool();
    CreateTriangleTool triangleTool = new CreateTriangleTool();
    MoveShapeTool moveTool = new MoveShapeTool();
    ChangeColorTool colorTool = new ChangeColorTool();
    DeleteShapeTool deleteTool = new DeleteShapeTool();
    ClearCanvasTool clearTool = new ClearCanvasTool();
    ResizeShapeTool resizeTool = new ResizeShapeTool();
    AlignShapesTool alignTool = new AlignShapesTool();
    GroupShapesTool groupTool = new GroupShapesTool();
    if (port != null) {
      circleTool.withPort(port);
      ellipseTool.withPort(port);
      lineTool.withPort(port);
      rectTool.withPort(port);
      triangleTool.withPort(port);
      moveTool.withPort(port);
      colorTool.withPort(port);
      deleteTool.withPort(port);
      clearTool.withPort(port);
      resizeTool.withPort(port);
      alignTool.withPort(port);
      groupTool.withPort(port);
    }
    registerTool(circleTool);
    registerTool(ellipseTool);
    registerTool(lineTool);
    registerTool(rectTool);
    registerTool(triangleTool);
    registerTool(moveTool);
    registerTool(colorTool);
    registerTool(deleteTool);
    registerTool(clearTool);
    registerTool(resizeTool);
    registerTool(groupTool);
    registerTool(alignTool);
    initialized = true;
    LOGGER.info("ToolRegistry initialized with " + tools.size() + " tools");
  }

  /**
   * Explicitly initialize registry with a provided ToolUseCasePort.
   */
  public synchronized void initializeWithPort(ToolUseCasePort port) {
    if (initialized && this.port != null)
      return;
    initializeInternal(port);
  }

  /**
   * Register a tool in the registry
   */
  public void registerTool(IAgentTool tool) {
    if (tool == null)
      throw new IllegalArgumentException("Tool cannot be null");
    String name = tool.name();
    if (name == null || name.isEmpty())
      throw new IllegalArgumentException("Tool name cannot be null or empty");
    if (tools.containsKey(name))
      LOGGER.warning("Tool with name '" + name + "' already registered, replacing");
    tools.put(name, tool);
    LOGGER.fine("Registered tool: " + name);
  }

  /**
   * Get a tool by name
   */
  public IAgentTool getTool(String name) {
    if (name == null || name.isEmpty())
      return null;
    return tools.get(name);
  }

  /**
   * Check if a tool exists
   */
  public boolean hasTool(String name) {
    return name != null && tools.containsKey(name);
  }

  /**
   * Get all available tool names
   */
  public Set<String> getToolNames() {
    return new HashSet<>(tools.keySet());
  }

  /**
   * Get all available tools
   */
  public Collection<IAgentTool> getAllTools() {
    return new ArrayList<>(tools.values());
  }

  /**
   * Get tool schema by name
   */
  public Map<String, Object> getToolSchema(String name) {
    IAgentTool tool = getTool(name);
    return tool != null ? tool.schema() : null;
  }

  /**
   * Get all tool schemas
   */
  public Map<String, Map<String, Object>> getAllSchemas() {
    Map<String, Map<String, Object>> schemas = new HashMap<>();
    for (Map.Entry<String, IAgentTool> e : tools.entrySet()) {
      schemas.put(e.getKey(), e.getValue().schema());
    }
    return schemas;
  }

  /**
   * Clear all registered tools
   */
  public void clear() {
    tools.clear();
    initialized = false;
    port = null;
    LOGGER.info("ToolRegistry cleared");
  }

  /**
   * Get tool information for AI consumption
   */
  public List<Map<String, Object>> getToolsInfo() {
    List<Map<String, Object>> list = new ArrayList<>();
    for (IAgentTool tool : tools.values()) {
      Map<String, Object> info = new HashMap<>();
      info.put("name", tool.name());
      info.put("schema", tool.schema());
      Map<String, Object> schema = tool.schema();
      if (schema != null && schema.containsKey("description"))
        info.put("description", schema.get("description"));
      list.add(info);
    }
    return list;
  }

  /**
   * Reset the singleton instance (mainly for testing)
   */
  public static synchronized void reset() {
    if (instance != null) {
      instance.clear();
      instance = null;
    }
  }
}
