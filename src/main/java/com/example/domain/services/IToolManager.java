package com.example.domain.services;

import com.example.domain.agent.AgentContext;
import com.example.domain.agent.tools.Tool;
import com.example.domain.agent.tools.ToolExecutionException;
import com.example.domain.agent.tools.ToolResult;
import com.example.domain.agent.tools.ToolValidationResult;
import com.example.domain.tools.IToolRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Domain interface for tool manager that handles registration, validation, and execution of agent tools.
 * Maintains tool registry and execution history.
 */
public interface IToolManager {
    
    // Tool registration
    IToolManager registerTool(Tool tool);
    IToolManager registerTools(Tool... tools);
    IToolManager registerTools(Collection<Tool> tools);
    
    // Tool removal
    boolean unregisterTool(String toolName);
    void clearTools();
    
    // Tool retrieval
    Optional<Tool> getTool(String name);
    List<Tool> getAllTools();
    List<Tool> getToolsByCategory(String category);
    Set<String> getRegisteredToolNames();
    Set<String> getCategories();
    boolean isToolRegistered(String toolName);
    
    // Domain registry integration
    int registerFromRegistry(IToolRegistry registry);
    int registerFromRegistry(IToolRegistry registry, boolean overwrite);
    
    // Tool execution
    ToolResult executeTool(String toolName, Map<String, Object> parameters, AgentContext context) 
        throws ToolExecutionException;
    
    // Tool validation
    ToolValidationResult validateTool(String toolName, Map<String, Object> parameters);
    Map<String, ToolValidationResult> validateAllTools(Map<String, Map<String, Object>> toolParameters);
    
    // Tool information
    Map<String, Object> getToolInfo(String toolName);
    Map<String, Map<String, Object>> getAllToolsInfo();
    
    // Execution history
    List<Object> getExecutionHistory();
    List<Object> getExecutionHistory(String toolName);
    Optional<Object> getLastExecution();
    Optional<Object> getLastExecution(String toolName);
    void clearExecutionHistory();
    
    // Statistics
    Object getStats();
}