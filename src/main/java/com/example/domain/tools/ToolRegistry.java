package com.example.domain.tools;

import com.example.domain.tools.impl.*;
import com.example.application.usecase.UseCaseFacade;
import com.example.infrastructure.ServiceInitializer;

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

  private ToolRegistry() {
    this.tools = new ConcurrentHashMap<>();
  }

  /**
   * Get the singleton instance of the ToolRegistry
   */
  public static synchronized ToolRegistry getInstance() {
    if (instance == null) {
      instance = new ToolRegistry();
      instance.initialize();
    }
    return instance;
  }

  /**
   * Initialize the registry with all available tools
   */
  private void initialize() {
    if (initialized)
      return;
    // Ensure full service graph initialized (StateService, ShapeOps, Facade, etc.)
    ServiceInitializer.initialize();
    UseCaseFacade facade = ServiceInitializer.getUseCaseFacade();
    CreateCircleTool circleTool = new CreateCircleTool();
    CreateLineTool lineTool = new CreateLineTool();
    CreateRectangleTool rectTool = new CreateRectangleTool();
    MoveShapeTool moveTool = new MoveShapeTool();
    ChangeColorTool colorTool = new ChangeColorTool();
    DeleteShapeTool deleteTool = new DeleteShapeTool();
    ClearCanvasTool clearTool = new ClearCanvasTool();
    ResizeShapeTool resizeTool = new ResizeShapeTool();
    AlignShapesTool alignTool = new AlignShapesTool();
    GroupShapesTool groupTool = new GroupShapesTool();
    if (facade != null) {
      circleTool.withFacade(facade);
      lineTool.withFacade(facade);
      rectTool.withFacade(facade);
      moveTool.withFacade(facade);
      colorTool.withFacade(facade);
      deleteTool.withFacade(facade);
      clearTool.withFacade(facade);
      resizeTool.withFacade(facade);
      alignTool.withFacade(facade);
      groupTool.withFacade(facade);
    }
    registerTool(circleTool);
    registerTool(lineTool);
    registerTool(rectTool);
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
