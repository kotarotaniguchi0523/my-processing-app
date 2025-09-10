package com.example.application.agent;

import com.example.infrastructure.agent.adapter.IAgentToolAdapter;
import com.example.domain.agent.AgentContext;
import com.example.domain.agent.tools.Tool;
import com.example.domain.agent.tools.ToolExecutionException;
import com.example.domain.agent.tools.ToolResult;
import com.example.domain.agent.tools.ToolValidationResult;
import com.example.domain.services.IToolManager;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * ToolManager handles registration, validation, and execution of agent tools Maintains tool
 * registry and execution history
 */
public final class ToolManager implements IToolManager {

  private final Map<String, Tool> tools = new ConcurrentHashMap<>();
  private final Map<String, ToolCategory> categories = new ConcurrentHashMap<>();
  private final List<ToolExecutionRecord> executionHistory =
      Collections.synchronizedList(new ArrayList<>());
  private final AtomicLong executionCounter = new AtomicLong(0);
  private final int maxHistorySize;

  public ToolManager(int maxHistorySize) {
    this.maxHistorySize = Math.max(1, maxHistorySize);
    initializeDefaultCategories();
  }

  public ToolManager() {
    this(1000); // Default max history size
  }

  private void initializeDefaultCategories() {
    categories.put("general", new ToolCategory("general", "General purpose tools"));
    categories.put("drawing",
        new ToolCategory("drawing", "Tools for creating and modifying shapes"));
    categories.put("state", new ToolCategory("state", "Tools for managing drawing state"));
    categories.put("navigation",
        new ToolCategory("navigation", "Tools for navigating through states"));
  }

  // Tool registration
  @Override
  public IToolManager registerTool(Tool tool) {
    Objects.requireNonNull(tool, "Tool cannot be null");

    String name = tool.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tool name cannot be null or empty");
    }

    tools.put(name, tool);

    // Ensure category exists
    String category = tool.getCategory();
    if (!categories.containsKey(category)) {
      categories.put(category, new ToolCategory(category, "Custom category"));
    }

    return this;
  }

  @Override
  public IToolManager registerTools(Tool... tools) {
    for (Tool tool : tools) {
      registerTool(tool);
    }
    return this;
  }

  @Override
  public IToolManager registerTools(Collection<Tool> tools) {
    for (Tool tool : tools) {
      registerTool(tool);
    }
    return this;
  }

  // Tool removal
  @Override
  public boolean unregisterTool(String toolName) {
    return tools.remove(toolName) != null;
  }

  @Override
  public void clearTools() {
    tools.clear();
  }

  // Tool retrieval
  @Override
  public Optional<Tool> getTool(String name) {
    return Optional.ofNullable(tools.get(name));
  }

  @Override
  public List<Tool> getAllTools() {
    return new ArrayList<>(tools.values());
  }

  @Override
  public List<Tool> getToolsByCategory(String category) {
    return tools.values().stream().filter(tool -> Objects.equals(tool.getCategory(), category))
        .collect(Collectors.toList());
  }

  @Override
  public Set<String> getRegisteredToolNames() {
    return new HashSet<>(tools.keySet());
  }

  @Override
  public Set<String> getCategories() {
    return new HashSet<>(categories.keySet());
  }

  @Override
  public boolean isToolRegistered(String toolName) {
    return tools.containsKey(toolName);
  }

  // M1/M2統一: domain側のレジストリから一括登録（アダプタ経由）
  @Override
  public int registerFromRegistry(IToolRegistry registry) {
    return registerFromRegistry(registry, false);
  }

  @Override
  public int registerFromRegistry(IToolRegistry registry, boolean overwrite) {
    Objects.requireNonNull(registry, "registry cannot be null");
    int added = 0;
    for (IAgentTool domainTool : registry.getAllTools()) {
      Tool adapter = new IAgentToolAdapter(domainTool);
      String name = adapter.getName();
      if (overwrite || !isToolRegistered(name)) {
        registerTool(adapter);
        added++;
      }
    }
    return added;
  }

  // Tool execution
  @Override
  public ToolResult executeTool(String toolName, Map<String, Object> parameters,
      AgentContext context) throws ToolExecutionException {

    Tool tool = tools.get(toolName);
    if (tool == null) {
      throw ToolExecutionException.invalidParameters("ToolManager",
          "Tool '" + toolName + "' is not registered");
    }

    long executionId = executionCounter.incrementAndGet();
    long startTime = System.currentTimeMillis();

    try {
      // Execute the tool
      ToolResult result = tool.execute(parameters, context);

      // Record execution
      long endTime = System.currentTimeMillis();
      recordExecution(executionId, toolName, parameters, result, endTime - startTime, null);

      return result;

    } catch (ToolExecutionException e) {
      // Record failed execution
      long endTime = System.currentTimeMillis();
      recordExecution(executionId, toolName, parameters, e.toToolResult(), endTime - startTime, e);
      throw e;
    } catch (Exception e) {
      // Record unexpected error
      long endTime = System.currentTimeMillis();
      ToolResult errorResult =
          ToolResult.error("Unexpected error: " + e.getMessage(), "SYSTEM_ERROR");
      recordExecution(executionId, toolName, parameters, errorResult, endTime - startTime, e);
      throw new ToolExecutionException(toolName, "Unexpected error during tool execution",
          "SYSTEM_ERROR", e);
    }
  }

  // Tool validation
  @Override
  public ToolValidationResult validateTool(String toolName, Map<String, Object> parameters) {
    Tool tool = tools.get(toolName);
    if (tool == null) {
      return ToolValidationResult.invalid("Tool '" + toolName + "' is not registered");
    }

    return tool.validate(parameters);
  }

  @Override
  public Map<String, ToolValidationResult> validateAllTools(
      Map<String, Map<String, Object>> toolParameters) {
    Map<String, ToolValidationResult> results = new HashMap<>();

    for (Map.Entry<String, Map<String, Object>> entry : toolParameters.entrySet()) {
      String toolName = entry.getKey();
      Map<String, Object> parameters = entry.getValue();
      results.put(toolName, validateTool(toolName, parameters));
    }

    return results;
  }

  // Tool information
  @Override
  public Map<String, Object> getToolInfo(String toolName) {
    Tool tool = tools.get(toolName);
    if (tool == null) {
      return java.util.Collections.emptyMap();
    }

    Map<String, Object> info = new HashMap<>();
    info.put("name", tool.getName());
    info.put("description", tool.getDescription());
    info.put("category", tool.getCategory());
    info.put("requiresApproval", tool.requiresApproval());
    info.put("isStateModifying", tool.isStateModifying());
    info.put("parameters", tool.getParameters());

    return info;
  }

  @Override
  public Map<String, Map<String, Object>> getAllToolsInfo() {
    Map<String, Map<String, Object>> allInfo = new HashMap<>();

    for (String toolName : tools.keySet()) {
      allInfo.put(toolName, getToolInfo(toolName));
    }

    return allInfo;
  }

  // Execution history
  private void recordExecution(long executionId, String toolName, Map<String, Object> parameters,
      ToolResult result, long duration, Exception exception) {
    ToolExecutionRecord record = new ToolExecutionRecord(executionId, toolName,
        new HashMap<>(parameters), result, System.currentTimeMillis(), duration, exception);

    synchronized (executionHistory) {
      executionHistory.add(record);

      // Trim history if it exceeds max size
      while (executionHistory.size() > maxHistorySize) {
        executionHistory.remove(0);
      }
    }
  }

  @Override
  public List<Object> getExecutionHistory() {
    synchronized (executionHistory) {
      return new ArrayList<Object>(executionHistory);
    }
  }

  @Override
  public List<Object> getExecutionHistory(String toolName) {
    synchronized (executionHistory) {
      return executionHistory.stream()
          .filter(record -> Objects.equals(record.getToolName(), toolName))
          .collect(Collectors.toList())
          .stream()
          .map(Object.class::cast)
          .collect(Collectors.toList());
    }
  }

  @Override
  public Optional<Object> getLastExecution() {
    synchronized (executionHistory) {
      return executionHistory.isEmpty() ? Optional.empty()
          : Optional.of((Object) executionHistory.get(executionHistory.size() - 1));
    }
  }

  @Override
  public Optional<Object> getLastExecution(String toolName) {
    synchronized (executionHistory) {
      return executionHistory.stream()
          .filter(record -> Objects.equals(record.getToolName(), toolName))
          .reduce((first, second) -> second)
          .map(Object.class::cast);
    }
  }

  @Override
  public void clearExecutionHistory() {
    synchronized (executionHistory) {
      executionHistory.clear();
    }
  }

  // Statistics
  @Override
  public Object getStats() {
    synchronized (executionHistory) {
      int totalExecutions = executionHistory.size();
      int successfulExecutions = (int) executionHistory.stream()
          .mapToLong(record -> record.getResult().isSuccess() ? 1 : 0).sum();

      Map<String, Long> toolUsageCounts = executionHistory.stream()
          .collect(Collectors.groupingBy(ToolExecutionRecord::getToolName, Collectors.counting()));

      double averageExecutionTime = executionHistory.stream()
          .mapToLong(ToolExecutionRecord::getDuration).average().orElse(0.0);

      return new ToolManagerStats(tools.size(), categories.size(), totalExecutions,
          successfulExecutions, toolUsageCounts, averageExecutionTime);
    }
  }

  @Override
  public String toString() {
    return String.format("ToolManager(tools=%d, categories=%d, executions=%d)", tools.size(),
        categories.size(), executionHistory.size());
  }

  // Inner classes
  public static class ToolCategory {
    private final String name;
    private final String description;

    public ToolCategory(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return String.format("ToolCategory(%s: %s)", name, description);
    }
  }

  public static class ToolExecutionRecord {
    private final long executionId;
    private final String toolName;
    private final Map<String, Object> parameters;
    private final ToolResult result;
    private final long timestamp;
    private final long duration;
    private final Exception exception;

    public ToolExecutionRecord(long executionId, String toolName, Map<String, Object> parameters,
        ToolResult result, long timestamp, long duration, Exception exception) {
      this.executionId = executionId;
      this.toolName = toolName;
      this.parameters = parameters;
      this.result = result;
      this.timestamp = timestamp;
      this.duration = duration;
      this.exception = exception;
    }

    public long getExecutionId() {
      return executionId;
    }

    public String getToolName() {
      return toolName;
    }

    public Map<String, Object> getParameters() {
      return new HashMap<>(parameters);
    }

    public ToolResult getResult() {
      return result;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public long getDuration() {
      return duration;
    }

    public Optional<Exception> getException() {
      return Optional.ofNullable(exception);
    }

    public boolean wasSuccessful() {
      return result.isSuccess();
    }

    @Override
    public String toString() {
      return String.format("ToolExecutionRecord(id=%d, tool=%s, success=%s, duration=%dms)",
          executionId, toolName, wasSuccessful(), duration);
    }
  }

  public static class ToolManagerStats {
    private final int registeredTools;
    private final int categories;
    private final int totalExecutions;
    private final int successfulExecutions;
    private final Map<String, Long> toolUsageCounts;
    private final double averageExecutionTime;

    public ToolManagerStats(int registeredTools, int categories, int totalExecutions,
        int successfulExecutions, Map<String, Long> toolUsageCounts, double averageExecutionTime) {
      this.registeredTools = registeredTools;
      this.categories = categories;
      this.totalExecutions = totalExecutions;
      this.successfulExecutions = successfulExecutions;
      this.toolUsageCounts = new HashMap<>(toolUsageCounts);
      this.averageExecutionTime = averageExecutionTime;
    }

    public int getRegisteredTools() {
      return registeredTools;
    }

    public int getCategories() {
      return categories;
    }

    public int getTotalExecutions() {
      return totalExecutions;
    }

    public int getSuccessfulExecutions() {
      return successfulExecutions;
    }

    public int getFailedExecutions() {
      return totalExecutions - successfulExecutions;
    }

    public double getSuccessRate() {
      return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions : 0.0;
    }

    public Map<String, Long> getToolUsageCounts() {
      return new HashMap<>(toolUsageCounts);
    }

    public double getAverageExecutionTime() {
      return averageExecutionTime;
    }

    @Override
    public String toString() {
      return String.format(
          "ToolManagerStats(tools=%d, executions=%d, success=%.1f%%, avgTime=%.1fms)",
          registeredTools, totalExecutions, getSuccessRate() * 100, averageExecutionTime);
    }
  }
}
