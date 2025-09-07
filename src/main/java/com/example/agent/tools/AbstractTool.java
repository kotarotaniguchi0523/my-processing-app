package com.example.agent.tools;

import com.example.agent.core.AgentContext;
import com.example.domain.models.DrawingState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AbstractTool provides base implementation for all tools Handles common functionality like
 * parameter validation and error handling
 */
public abstract class AbstractTool implements Tool {

  private final String name;
  private final String description;
  private final String category;
  // Rename to avoid matching method names (PMD: AvoidFieldNameMatchingMethodName)
  private final boolean requiresApprovalFlag;
  private final boolean stateModifyingFlag;
  private final Map<String, ParameterDefinition> parameterDefinitions;

  protected AbstractTool(String name, String description, String category, boolean requiresApproval,
      boolean isStateModifying) {
    this.name = Objects.requireNonNull(name, "Name cannot be null");
    this.description = Objects.requireNonNull(description, "Description cannot be null");
    this.category = category != null ? category : "general";
    this.requiresApprovalFlag = requiresApproval;
    this.stateModifyingFlag = isStateModifying;
    this.parameterDefinitions = new HashMap<>();
    defineParameters();
  }

  protected AbstractTool(String name, String description) {
    this(name, description, "general", false, false);
  }

  /** Subclasses should override this method to define their parameters */
  protected void defineParameters() {
    // Default implementation does nothing
    // Subclasses should call addParameter() to define parameters
  }

  /** Add a parameter definition for this tool */
  protected void addParameter(String name, Class<?> type, String description, boolean required) {
    parameterDefinitions.put(name,
        new ParameterDefinition(name, type, description, required, null));
  }

  /** Add a parameter definition with default value */
  protected void addParameter(String name, Class<?> type, String description, boolean required,
      Object defaultValue) {
    parameterDefinitions.put(name,
        new ParameterDefinition(name, type, description, required, defaultValue));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getCategory() {
    return category;
  }

  @Override
  public boolean requiresApproval() {
    return requiresApprovalFlag;
  }

  @Override
  public boolean isStateModifying() {
    return stateModifyingFlag;
  }

  @Override
  public Map<String, String> getParameters() {
    Map<String, String> params = new HashMap<>();
    for (ParameterDefinition def : parameterDefinitions.values()) {
      StringBuilder sb = new StringBuilder(def.description);
      if (def.required) {
        sb.append(" (required)");
      }
      if (def.defaultValue != null) {
        sb.append(" (default: ").append(def.defaultValue).append(')');
      }
      params.put(def.name, sb.toString());
    }
    return params;
  }

  @Override
  public final ToolResult execute(Map<String, Object> parameters, AgentContext context)
      throws ToolExecutionException {

    long startTime = System.currentTimeMillis();

    try {
      // Validate parameters first
      ToolValidationResult validation = validate(parameters);
      if (!validation.isValid()) {
        return ToolResult.error("Parameter validation failed: " + validation.getErrorSummary(),
            "INVALID_PARAMETERS");
      }

      // Add default values for missing parameters
      Map<String, Object> normalizedParams = normalizeParameters(parameters);

      // Perform the actual execution
      ToolResult result = executeInternal(normalizedParams, context);

      // Add execution time
      long executionTime = System.currentTimeMillis() - startTime;
      return result.withData("executionTimeMs", executionTime);

    } catch (ToolExecutionException e) {
      long executionTime = System.currentTimeMillis() - startTime;
      return e.toToolResult().withData("executionTimeMs", executionTime);
    } catch (Exception e) {
      long executionTime = System.currentTimeMillis() - startTime;
      return ToolResult.error("Unexpected error: " + e.getMessage(), "SYSTEM_ERROR")
          .withData("executionTimeMs", executionTime)
          .withData("exceptionType", e.getClass().getSimpleName());
    }
  }

  /** Subclasses must implement this method to perform the actual tool execution */
  protected abstract ToolResult executeInternal(Map<String, Object> parameters,
      AgentContext context) throws ToolExecutionException;

  @Override
  public ToolValidationResult validate(Map<String, Object> parameters) {
    ToolValidationResult.Builder builder = ToolValidationResult.builder();

    // Check for required parameters
    for (ParameterDefinition def : parameterDefinitions.values()) {
      if (def.required && !parameters.containsKey(def.name)) {
        builder.addError("Required parameter '" + def.name + "' is missing");
      }
    }

    // Check parameter types
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String paramName = entry.getKey();
      Object value = entry.getValue();

      ParameterDefinition def = parameterDefinitions.get(paramName);
      if (def == null) {
        builder.addWarning("Unknown parameter '" + paramName + "'");
        continue;
      }

      if (value != null && !def.type.isAssignableFrom(value.getClass())) {
        // Try some basic type coercion
        if (!canCoerceType(value, def.type)) {
          builder.addError("Parameter '" + paramName + "' expected " + def.type.getSimpleName()
              + " but got " + value.getClass().getSimpleName());
        }
      }
    }

    return builder.build();
  }

  /** Check if a value can be coerced to the target type */
  private boolean canCoerceType(Object value, Class<?> targetType) {
    if (targetType == String.class) {
      return true; // Everything can be converted to string
    }

    if (value instanceof Number) {
      return Number.class.isAssignableFrom(targetType) || targetType == int.class
          || targetType == Integer.class || targetType == float.class || targetType == Float.class
          || targetType == double.class || targetType == Double.class || targetType == long.class
          || targetType == Long.class;
    }

    if (value instanceof String && targetType == boolean.class) {
      String str = (String) value;
      return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }

    return false;
  }

  /** Normalize parameters by adding default values and performing type coercion */
  private Map<String, Object> normalizeParameters(Map<String, Object> parameters) {
    Map<String, Object> normalized = new HashMap<>(parameters);

    for (ParameterDefinition def : parameterDefinitions.values()) {
      if (!normalized.containsKey(def.name) && def.defaultValue != null) {
        normalized.put(def.name, def.defaultValue);
      }

      // Perform type coercion if needed
      Object value = normalized.get(def.name);
      if (value != null && !def.type.isAssignableFrom(value.getClass())) {
        Object coercedValue = coerceValue(value, def.type);
        if (coercedValue != null) {
          normalized.put(def.name, coercedValue);
        }
      }
    }

    return normalized;
  }

  /** Coerce a value to the target type */
  private Object coerceValue(Object value, Class<?> targetType) {
    if (targetType == String.class) {
      return value.toString();
    }

    if (value instanceof Number) {
      Number num = (Number) value;
      if (targetType == int.class || targetType == Integer.class) {
        return num.intValue();
      }
      if (targetType == float.class || targetType == Float.class) {
        return num.floatValue();
      }
      if (targetType == double.class || targetType == Double.class) {
        return num.doubleValue();
      }
      if (targetType == long.class || targetType == Long.class) {
        return num.longValue();
      }
    }

    if (value instanceof String && targetType == boolean.class) {
      return Boolean.parseBoolean((String) value);
    }

    return null; // Unable to coerce
  }

  /** Helper method to get a required parameter value */
  protected <T> T getRequiredParameter(Map<String, Object> parameters, String name, Class<T> type)
      throws ToolExecutionException {
    Object value = parameters.get(name);
    if (value == null) {
      throw ToolExecutionException.invalidParameters(getName(),
          "Required parameter '" + name + "' is missing");
    }

    if (!type.isAssignableFrom(value.getClass())) {
      throw ToolExecutionException.invalidParameters(getName(),
          "Parameter '" + name + "' must be of type " + type.getSimpleName());
    }

    return type.cast(value);
  }

  /** Helper method to get an optional parameter value */
  protected <T> T getOptionalParameter(Map<String, Object> parameters, String name, Class<T> type,
      T defaultValue) {
    Object value = parameters.get(name);
    if (value == null) {
      return defaultValue;
    }

    if (type.isAssignableFrom(value.getClass())) {
      return type.cast(value);
    }

    return defaultValue;
  }

  /** Helper method to require current state in context */
  protected DrawingState requireCurrentState(AgentContext context) throws ToolExecutionException {
    if (!context.hasCurrentState()) {
      throw ToolExecutionException.stateError(getName(),
          "Current drawing state is required but not available");
    }
    return context.getCurrentState().get();
  }

  @Override
  public String toString() {
    return String.format("%s('%s', category='%s', stateModifying=%s)", getClass().getSimpleName(),
        name, category, stateModifyingFlag);
  }

  // Inner class for parameter definition
  protected static class ParameterDefinition {
    final String name;
    final Class<?> type;
    final String description;
    final boolean required;
    final Object defaultValue;

    ParameterDefinition(String name, Class<?> type, String description, boolean required,
        Object defaultValue) {
      this.name = name;
      this.type = type;
      this.description = description;
      this.required = required;
      this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
      return String.format("Parameter(name='%s', type=%s, required=%s)", name, type.getSimpleName(),
          required);
    }
  }
}
