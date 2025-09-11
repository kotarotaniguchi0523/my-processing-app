package com.example.domain.tools;

import com.example.domain.tools.impl.*;

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
        if (initialized) {
            return;
        }
        
        // Register all implemented tools
        registerTool(new CreateCircleTool());
        registerTool(new CreateRectangleTool());
        registerTool(new CreateLineTool());
        registerTool(new DeleteShapeTool());
        registerTool(new ClearCanvasTool());
        registerTool(new MoveShapeTool());
        registerTool(new ResizeShapeTool());
        registerTool(new ChangeColorTool());
        registerTool(new GroupShapesTool());
        registerTool(new AlignShapesTool());
        
        initialized = true;
        LOGGER.info("ToolRegistry initialized with " + tools.size() + " tools");
    }
    
    /**
     * Register a tool in the registry
     */
    public void registerTool(IAgentTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        String name = tool.name();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        
        if (tools.containsKey(name)) {
            LOGGER.warning("Tool with name '" + name + "' already registered, replacing");
        }
        
        tools.put(name, tool);
        LOGGER.fine("Registered tool: " + name);
    }
    
    /**
     * Get a tool by name
     */
    public IAgentTool getTool(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
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
        for (Map.Entry<String, IAgentTool> entry : tools.entrySet()) {
            schemas.put(entry.getKey(), entry.getValue().schema());
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
        List<Map<String, Object>> toolsInfo = new ArrayList<>();
        
        for (IAgentTool tool : tools.values()) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", tool.name());
            info.put("schema", tool.schema());
            
            // Add description from schema if available
            Map<String, Object> schema = tool.schema();
            if (schema != null && schema.containsKey("description")) {
                info.put("description", schema.get("description"));
            }
            
            toolsInfo.add(info);
        }
        
        return toolsInfo;
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