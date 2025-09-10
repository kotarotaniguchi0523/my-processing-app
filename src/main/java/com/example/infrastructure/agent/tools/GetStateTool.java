package com.example.infrastructure.agent.tools;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge tool: returns current state summary (optionally YAML) without modifying state.
 * name: "get_state"
 */
public class GetStateTool implements IAgentTool {

    @Override
    public String name() {
        return "get_state";
    }

    @Override
    public Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", "Return current drawing state summary and optional YAML");

        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> includeShapes = new HashMap<>();
        includeShapes.put("type", "boolean");
        includeShapes.put("description", "include shape count (default true)");
        includeShapes.put("default", true);
        properties.put("includeShapes", includeShapes);

        Map<String, Object> includeSettings = new HashMap<>();
        includeSettings.put("type", "boolean");
        includeSettings.put("description", "include width/height (default true)");
        includeSettings.put("default", true);
        properties.put("includeSettings", includeSettings);

        Map<String, Object> summary = new HashMap<>();
        summary.put("type", "boolean");
        summary.put("description", "summary only (default true)");
        summary.put("default", true);
        properties.put("summary", summary);

        Map<String, Object> includeYaml = new HashMap<>();
        includeYaml.put("type", "boolean");
        includeYaml.put("description", "include full YAML (default false)");
        includeYaml.put("default", false);
        properties.put("includeYaml", includeYaml);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
        // このツールは状態を変更せず、情報のみを返すため、
        // 元の状態をそのまま返します。
        // 実際の状態情報の取得は、呼び出し側で処理されることを想定
        return state;
    }

}

