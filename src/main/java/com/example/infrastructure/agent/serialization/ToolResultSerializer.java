package com.example.infrastructure.agent.serialization;

import com.example.domain.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.serialization.YamlUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializer for ToolResult objects to/from YAML format.
 * This class handles all serialization concerns for ToolResult,
 * keeping the domain object free of infrastructure dependencies.
 */
public final class ToolResultSerializer {

    private static final String YAML_TYPE = "tool_result";

    private ToolResultSerializer() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert ToolResult to YAML Map representation
     *
     * @param toolResult The ToolResult to serialize
     * @return Map suitable for YAML serialization
     */
    public static Map<String, Object> toYamlMap(ToolResult toolResult) {
        Objects.requireNonNull(toolResult, "ToolResult cannot be null");

        Map<String, Object> map = YamlUtils.createYamlMap(YAML_TYPE);
        map.put("status", toolResult.getStatus().name());
        map.put("message", toolResult.getMessage());
        map.put("data", new HashMap<>(toolResult.getData()));
        map.put("toolName", toolResult.getToolName());
        map.put("executionTime", toolResult.getExecutionTime());

        if (toolResult.getErrorType() != null) {
            map.put("errorType", toolResult.getErrorType());
        }

        if (toolResult.getNewState() != null) {
            map.put("newState", toolResult.getNewState().toYamlMap());
        }

        return map;
    }

    /**
     * Create ToolResult from YAML Map representation
     *
     * @param yamlMap The Map containing serialized data
     * @return ToolResult instance reconstructed from the map
     */
    @SuppressWarnings("unchecked")
    public static ToolResult fromYamlMap(Map<String, Object> yamlMap) {
        Objects.requireNonNull(yamlMap, "YamlMap cannot be null");
        YamlUtils.validateYamlType(yamlMap, YAML_TYPE);

        String statusStr = YamlUtils.getString(yamlMap, "status", "SUCCESS");
        ToolResult.Status status = ToolResult.Status.valueOf(statusStr);

        String message = YamlUtils.getString(yamlMap, "message", "");
        String toolName = YamlUtils.getString(yamlMap, "toolName", "");
        String errorType = YamlUtils.getString(yamlMap, "errorType", null);
        long executionTime = YamlUtils.getLong(yamlMap, "executionTime", 0L);

        Map<String, Object> data = YamlUtils.getMap(yamlMap, "data", new HashMap<>());

        DrawingState newState = null;
        Map<String, Object> stateMap = YamlUtils.getMap(yamlMap, "newState", null);
        if (stateMap != null) {
            newState = DrawingState.fromYamlMap(stateMap);
        }

        return ToolResult.builder()
                .status(status)
                .message(message)
                .toolName(toolName)
                .errorType(errorType)
                .executionTime(executionTime)
                .data(data)
                .newState(newState)
                .build();
    }

    /**
     * Get the YAML type identifier for ToolResult
     *
     * @return The YAML type string
     */
    public static String getYamlType() {
        return YAML_TYPE;
    }
}