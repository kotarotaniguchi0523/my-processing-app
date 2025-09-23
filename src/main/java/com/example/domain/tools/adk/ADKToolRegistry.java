package com.example.domain.tools.adk;

import com.example.domain.models.DrawingState;

import java.util.*;
import java.util.logging.Logger;

/**
 * Registry for ADK-compatible tools.
 * Manages all tools that can be used with ADK's LlmAgent.
 * Refactored to work without UseCase layer - tools directly manipulate DrawingState.
 */
public class ADKToolRegistry {

    private static final Logger LOGGER = Logger.getLogger(ADKToolRegistry.class.getName());
    private static ADKToolRegistry instance;

    private final Map<String, ADKTool> tools = new HashMap<>();

    private ADKToolRegistry() {
        // Initialize tools in constructor
        initialize();
    }

    public static synchronized ADKToolRegistry getInstance() {
        if (instance == null) {
            instance = new ADKToolRegistry();
        }
        return instance;
    }

    /**
     * Initialize the registry with all ADK tools.
     * Tools now directly manipulate DrawingState without UseCase layer.
     */
    private void initialize() {
        LOGGER.info("Initializing ADK Tool Registry");

        // Clear existing tools
        tools.clear();

        // Register all ADK tools with underscore names to match orchestrator expectations
        registerTool("create_circle", new CreateCircleADKTool());
        registerTool("create_rectangle", new CreateRectangleADKTool());
        registerTool("create_line", new CreateLineADKTool());
        registerTool("create_triangle", new CreateTriangleADKTool());
        registerTool("create_ellipse", new CreateEllipseADKTool());
        registerTool("clear_canvas", new ClearCanvasADKTool());
        registerTool("change_color", new ChangeColorADKTool());

        LOGGER.info("ADK Tool Registry initialized with " + tools.size() + " tools");
    }

    /**
     * Register a tool with the registry.
     */
    private void registerTool(String name, ADKTool tool) {
        tools.put(name, tool);
        LOGGER.fine("Registered ADK tool: " + name);
    }

    /**
     * Execute a tool by name with the given parameters.
     */
    public ADKTool.ToolResult executeTool(String toolName, Map<String, Object> parameters, DrawingState state) {
        ADKTool tool = tools.get(toolName);
        if (tool == null) {
            LOGGER.warning("Tool not found: " + toolName);
            return ADKTool.ToolResult.failure(state, "Tool not found: " + toolName);
        }

        try {
            LOGGER.info("Executing ADK tool: " + toolName);
            return tool.execute(parameters, state);
        } catch (Exception e) {
            LOGGER.severe("Error executing tool " + toolName + ": " + e.getMessage());
            return ADKTool.ToolResult.failure(state, "Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Get a specific tool by name.
     */
    public ADKTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get all registered tools.
     */
    public Collection<ADKTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Check if a tool exists.
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * Get all tool names.
     */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * Get tool schemas for ADK agent configuration.
     */
    public List<Map<String, Object>> getToolSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (ADKTool tool : tools.values()) {
            schemas.add(tool.getSchema());
        }
        return schemas;
    }
}