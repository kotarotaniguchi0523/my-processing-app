package com.example.domain.tools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ToolRegistry manages the registration and retrieval of agent tools.
 * Uses singleton pattern to ensure consistent tool registration across the application.
 * Thread-safe implementation supports concurrent access.
 */
public final class ToolRegistry {
    
    private static volatile ToolRegistry instance;
    private final Map<String, IAgentTool> tools = new ConcurrentHashMap<>();
    
    // Private constructor for singleton pattern
    private ToolRegistry() {}
    
    /**
     * Get the singleton instance of ToolRegistry
     * 
     * @return the ToolRegistry instance
     */
    public static ToolRegistry getInstance() {
        if (instance == null) {
            synchronized (ToolRegistry.class) {
                if (instance == null) {
                    instance = new ToolRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a tool in the registry
     * 
     * @param tool the tool to register
     * @throws IllegalArgumentException if tool is null or name is invalid
     * @throws IllegalStateException if a tool with the same name is already registered
     */
    public void registerTool(IAgentTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }
        
        String toolName = tool.name();
        if (toolName == null || toolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }
        
        if (tools.containsKey(toolName)) {
            throw new IllegalStateException("Tool with name '" + toolName + "' is already registered");
        }
        
        tools.put(toolName, tool);
    }
    
    /**
     * Register multiple tools at once
     * 
     * @param tools the tools to register
     */
    public void registerTools(IAgentTool... tools) {
        for (IAgentTool tool : tools) {
            registerTool(tool);
        }
    }
    
    /**
     * Register multiple tools from a collection
     * 
     * @param tools the tools to register
     */
    public void registerTools(Collection<IAgentTool> tools) {
        for (IAgentTool tool : tools) {
            registerTool(tool);
        }
    }
    
    /**
     * Get a tool by name
     * 
     * @param name the tool name
     * @return the tool if found, null otherwise
     */
    public IAgentTool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * Check if a tool is registered
     * 
     * @param name the tool name
     * @return true if the tool is registered, false otherwise
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * Get all registered tool names
     * 
     * @return a set of all registered tool names
     */
    public Set<String> getToolNames() {
        return new HashSet<>(tools.keySet());
    }
    
    /**
     * Get all registered tools
     * 
     * @return a list of all registered tools
     */
    public List<IAgentTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Remove a tool from the registry
     * 
     * @param name the name of the tool to remove
     * @return true if the tool was removed, false if it wasn't registered
     */
    public boolean unregisterTool(String name) {
        return tools.remove(name) != null;
    }
    
    /**
     * Clear all registered tools
     */
    public void clearAllTools() {
        tools.clear();
    }
    
    /**
     * Get the number of registered tools
     * 
     * @return the number of registered tools
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * Generate a combined JSON schema for all registered tools
     * This creates an OpenAI functions schema that can be used by AI models
     * 
     * @return a list of tool schemas in OpenAI functions format
     */
    public List<Map<String, Object>> generateToolSchemas() {
        return tools.values().stream()
                .map(this::convertToOpenAISchema)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert an IAgentTool to OpenAI function schema format
     * 
     * @param tool the tool to convert
     * @return the tool schema in OpenAI functions format
     */
    private Map<String, Object> convertToOpenAISchema(IAgentTool tool) {
        Map<String, Object> functionSchema = new HashMap<>();
        functionSchema.put("type", "function");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", tool.name());
        function.put("description", getToolDescription(tool));
        function.put("parameters", tool.schema());
        
        functionSchema.put("function", function);
        return functionSchema;
    }
    
    /**
     * Get description for a tool from its schema or generate a default one
     * 
     * @param tool the tool
     * @return the tool description
     */
    private String getToolDescription(IAgentTool tool) {
        Map<String, Object> schema = tool.schema();
        Object description = schema.get("description");
        if (description instanceof String) {
            return (String) description;
        }
        
        // Generate default description based on tool name
        String name = tool.name();
        return "Execute the " + name + " tool with the provided parameters";
    }
    
    /**
     * Validate that a tool's schema is well-formed
     * 
     * @param tool the tool to validate
     * @return true if the schema is valid, false otherwise
     */
    public boolean validateToolSchema(IAgentTool tool) {
        if (tool == null) {
            return false;
        }
        
        try {
            Map<String, Object> schema = tool.schema();
            if (schema == null) {
                return false;
            }
            
            // Basic validation - schema should have type and properties
            Object type = schema.get("type");
            Object properties = schema.get("properties");
            
            return "object".equals(type) && properties instanceof Map;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get tools that match a specific pattern or category
     * 
     * @param pattern the pattern to match against tool names
     * @return a list of matching tools
     */
    public List<IAgentTool> findTools(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return getAllTools();
        }
        
        String lowerPattern = pattern.toLowerCase();
        return tools.values().stream()
                .filter(tool -> tool.name().toLowerCase().contains(lowerPattern))
                .collect(Collectors.toList());
    }
    
    @Override
    public String toString() {
        return String.format("ToolRegistry(tools=%d)", tools.size());
    }
}