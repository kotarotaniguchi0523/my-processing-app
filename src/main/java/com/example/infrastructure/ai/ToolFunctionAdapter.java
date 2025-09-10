package com.example.infrastructure.ai;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Adapter that converts IAgentTool instances to Java Methods for use with
 * java-genai's automatic function calling feature.
 */
public class ToolFunctionAdapter {
    
    private static final Logger logger = Logger.getLogger(ToolFunctionAdapter.class.getName());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final IToolRegistry toolRegistry;
    private final Map<String, Method> toolMethods;
    private final Map<String, IAgentTool> toolInstances;
    private DrawingState currentState;
    
    public ToolFunctionAdapter(IToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.toolMethods = new HashMap<>();
        this.toolInstances = new HashMap<>();
        this.currentState = DrawingState.empty();
        
        registerAllTools();
    }
    
    /**
     * Register all tools from the registry as callable methods
     */
    private void registerAllTools() {
        for (IAgentTool tool : toolRegistry.getAllTools()) {
            try {
                // Create a wrapper method for each tool
                Method method = createToolMethod(tool);
                if (method != null) {
                    toolMethods.put(tool.name(), method);
                    toolInstances.put(tool.name(), tool);
                    logger.info("Registered tool function: " + tool.name());
                }
            } catch (Exception e) {
                logger.warning("Failed to register tool: " + tool.name() + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Create a Method wrapper for an IAgentTool
     */
    private Method createToolMethod(IAgentTool tool) {
        try {
            // Get the method from this class that will act as a proxy
            Method method = this.getClass().getMethod("executeToolFunction", 
                String.class, Map.class);
            
            // Note: In a real implementation, we would dynamically generate
            // methods with proper parameter names matching the tool schema
            // For now, we use a generic method that accepts tool name and params
            
            return method;
        } catch (NoSuchMethodException e) {
            logger.warning("Could not find tool function method: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generic tool execution method that will be called by function calling
     * This method is public and will be invoked by reflection
     * 
     * @param toolName The name of the tool to execute
     * @param parameters The parameters for the tool
     * @return JSON string of the execution result
     */
    public String executeToolFunction(String toolName, Map<String, Object> parameters) {
        logger.info("Executing tool function: " + toolName + " with params: " + parameters);
        
        IAgentTool tool = toolInstances.get(toolName);
        if (tool == null) {
            return gson.toJson(Map.of(
                "error", "Tool not found: " + toolName
            ));
        }
        
        try {
            // Execute the tool
            DrawingState newState = tool.execute(parameters, currentState);
            
            // Update current state
            this.currentState = newState;
            
            // Return success result
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("tool", toolName);
            result.put("timestamp", newState.getTimestamp());
            result.put("shapeCount", newState.getShapes().size());
            
            // Add tool-specific result data
            if (toolName.equals("CreateShapeTool")) {
                result.put("createdShape", parameters.get("shape"));
            } else if (toolName.equals("ListShapesTool")) {
                result.put("shapes", newState.getShapes());
            }
            
            return gson.toJson(result);
            
        } catch (Exception e) {
            logger.warning("Tool execution failed: " + e.getMessage());
            return gson.toJson(Map.of(
                "error", "Tool execution failed: " + e.getMessage(),
                "tool", toolName
            ));
        }
    }
    
    /**
     * Get all registered tool methods for function calling
     * @return Map of tool names to methods
     */
    public Map<String, Method> getToolMethods() {
        return new HashMap<>(toolMethods);
    }
    
    /**
     * Get current drawing state
     * @return Current drawing state
     */
    public DrawingState getCurrentState() {
        return currentState;
    }
    
    /**
     * Set the drawing state
     * @param state New drawing state
     */
    public void setCurrentState(DrawingState state) {
        this.currentState = state != null ? state : DrawingState.empty();
    }
    
    /**
     * Create function definitions for Gemini based on tool schemas
     * @return List of function definitions
     */
    public List<FunctionDefinition> createFunctionDefinitions() {
        List<FunctionDefinition> definitions = new ArrayList<>();
        
        for (IAgentTool tool : toolRegistry.getAllTools()) {
            FunctionDefinition def = new FunctionDefinition();
            def.name = tool.name();
            def.description = getToolDescription(tool.name());
            def.parameters = tool.schema();
            definitions.add(def);
        }
        
        return definitions;
    }
    
    /**
     * Get description for a tool
     */
    private String getToolDescription(String toolName) {
        switch (toolName) {
            case "CreateShapeTool":
                return "Create a new shape (circle, rectangle, triangle, or line) at specified position";
            case "ModifyShapeTool":
                return "Modify properties of an existing shape by ID";
            case "DeleteShapeTool":
                return "Delete a shape by its ID";
            case "ListShapesTool":
                return "List all shapes in the current drawing";
            case "ClearCanvasTool":
                return "Clear all shapes from the canvas";
            case "UndoTool":
                return "Undo the last action";
            case "RedoTool":
                return "Redo the previously undone action";
            case "SaveStateTool":
                return "Save the current drawing state to a file";
            case "LoadStateTool":
                return "Load a drawing state from a file";
            default:
                return "Tool for drawing operations";
        }
    }
    
    /**
     * Function definition for Gemini
     */
    public static class FunctionDefinition {
        public String name;
        public String description;
        public Map<String, Object> parameters;
        
        @Override
        public String toString() {
            return String.format("Function{name='%s', description='%s'}", name, description);
        }
    }
    
    /**
     * Create a dynamic proxy method for a specific tool
     * This allows for more natural function calling with proper parameter names
     */
    public static class ToolMethodProxy {
        private final IAgentTool tool;
        private final DrawingState state;
        
        public ToolMethodProxy(IAgentTool tool, DrawingState state) {
            this.tool = tool;
            this.state = state;
        }
        
        /**
         * Example method for CreateShapeTool
         * In production, these would be generated dynamically
         */
        public String createShape(String shape, Double x, Double y, 
                                 Double width, Double height, String color) {
            Map<String, Object> params = new HashMap<>();
            params.put("shape", shape);
            params.put("x", x);
            params.put("y", y);
            params.put("width", width);
            params.put("height", height);
            params.put("color", color);
            
            try {
                DrawingState newState = tool.execute(params, state);
                return gson.toJson(Map.of(
                    "success", true,
                    "shapeId", newState.getShapes().get(newState.getShapes().size() - 1).getId()
                ));
            } catch (Exception e) {
                return gson.toJson(Map.of(
                    "error", e.getMessage()
                ));
            }
        }
        
        /**
         * Example method for DeleteShapeTool
         */
        public String deleteShape(String shapeId) {
            Map<String, Object> params = new HashMap<>();
            params.put("shapeId", shapeId);
            
            try {
                tool.execute(params, state);
                return gson.toJson(Map.of(
                    "success", true,
                    "deleted", shapeId
                ));
            } catch (Exception e) {
                return gson.toJson(Map.of(
                    "error", e.getMessage()
                ));
            }
        }
    }
}