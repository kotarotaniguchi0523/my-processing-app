package com.example.infrastructure.services;

import com.example.domain.models.DrawingState;
import com.example.application.usecase.dto.UseCaseResult;
import com.example.domain.services.IToolInvoker;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.ToolRegistry;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.values.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of IToolInvoker that manages tool execution
 */
public class ToolInvoker implements IToolInvoker {

  private static final Logger LOGGER = Logger.getLogger(ToolInvoker.class.getName());

  private final ToolRegistry toolRegistry;
  private final ObjectMapper objectMapper;

  public ToolInvoker() {
    this.toolRegistry = ToolRegistry.getInstance();
    this.objectMapper = new ObjectMapper();
  }

  public ToolInvoker(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry != null ? toolRegistry : ToolRegistry.getInstance();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public ToolExecutionResult invokeTool(String toolName, Map<String, Object> parameters,
      DrawingState currentState) {
    if (toolName == null || toolName.isEmpty()) {
      return ToolExecutionResult.error(null, "Tool name cannot be null or empty");
    }

    IAgentTool tool = toolRegistry.getTool(toolName);
    if (tool == null) {
      return ToolExecutionResult.error(toolName, "Tool not found: " + toolName);
    }

    // Validate parameters
    ValidationResult validation = validateToolParameters(toolName, parameters);
    if (!validation.isValid()) {
      return ToolExecutionResult.error(toolName, validation.getErrorMessage());
    }

    try {
      // Execute the tool (now returns UseCaseResult)
      UseCaseResult<?> result =
          tool.execute(parameters != null ? parameters : new HashMap<>(), currentState);

      DrawingState resultState = result.newState();

      // Create metadata enriched with unified result info
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("toolName", toolName);
      metadata.put("parameterCount", parameters != null ? parameters.size() : 0);
      metadata.put("success", result.success());
      if (result.payload() != null)
        metadata.put("payload", result.payload());
      if (result.message() != null)
        metadata.put("message", result.message());

      if (!result.success()) {
        return ToolExecutionResult.error(toolName,
            result.message() != null ? result.message() : "Tool execution failed");
      }
      return ToolExecutionResult.success(toolName, resultState, metadata);

    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.WARNING, "Invalid arguments for tool " + toolName, e);
      return ToolExecutionResult.error(toolName, "Invalid arguments: " + e.getMessage());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error executing tool " + toolName, e);
      return ToolExecutionResult.error(toolName, "Execution failed: " + e.getMessage());
    }
  }

  @Override
  public ToolExecutionResult invokeToolWithJson(String toolName, String jsonParameters,
      DrawingState currentState) {
    if (toolName == null || toolName.isEmpty()) {
      return ToolExecutionResult.error(null, "Tool name cannot be null or empty");
    }

    Map<String, Object> parameters = null;

    if (jsonParameters != null && !jsonParameters.isEmpty()) {
      try {
        parameters =
            objectMapper.readValue(jsonParameters, new TypeReference<Map<String, Object>>() {});
      } catch (JsonProcessingException e) {
        LOGGER.log(Level.WARNING, "Failed to parse JSON parameters", e);
        return ToolExecutionResult.error(toolName, "Invalid JSON parameters: " + e.getMessage());
      }
    }

    return invokeTool(toolName, parameters, currentState);
  }

  @Override
  public boolean canInvokeTool(String toolName, Map<String, Object> parameters) {
    if (toolName == null || !toolRegistry.hasTool(toolName)) {
      return false;
    }

    ValidationResult validation = validateToolParameters(toolName, parameters);
    return validation.isValid();
  }

  @Override
  public ValidationResult validateToolParameters(String toolName, Map<String, Object> parameters) {
    if (toolName == null || toolName.isEmpty()) {
      return ValidationResult.invalid("Tool name cannot be null or empty");
    }

    IAgentTool tool = toolRegistry.getTool(toolName);
    if (tool == null) {
      return ValidationResult.invalid("Tool not found: " + toolName);
    }

    Map<String, Object> schema = tool.schema();
    if (schema == null) {
      // No schema means no validation required
      return ValidationResult.valid();
    }

    // Basic validation - check required fields
    Object requiredObj = schema.get("required");
    if (requiredObj instanceof String[]) {
      String[] required = (String[]) requiredObj;

      if (parameters == null && required.length > 0) {
        return ValidationResult.invalid("Missing required parameters");
      }

      for (String field : required) {
        if (parameters == null || !parameters.containsKey(field)) {
          return ValidationResult.invalid("Missing required field: " + field);
        }
      }
    }

    // Check oneOf constraints if present
    Object oneOfObj = schema.get("oneOf");
    if (oneOfObj instanceof Map[]) {
      Map[] oneOfOptions = (Map[]) oneOfObj;
      boolean anyValid = false;

      for (Map option : oneOfOptions) {
        Object optionRequired = option.get("required");
        if (optionRequired instanceof String[]) {
          String[] fields = (String[]) optionRequired;
          boolean allPresent = true;

          for (String field : fields) {
            if (parameters == null || !parameters.containsKey(field)) {
              allPresent = false;
              break;
            }
          }

          if (allPresent) {
            anyValid = true;
            break;
          }
        }
      }

      if (!anyValid) {
        return ValidationResult
            .invalid("Must satisfy at least one of the required field combinations");
      }
    }

    return ValidationResult.valid();
  }

  @Override
  public Map<String, Object> getToolSchema(String toolName) {
    if (toolName == null || toolName.isEmpty()) {
      return null;
    }
    return toolRegistry.getToolSchema(toolName);
  }
}
