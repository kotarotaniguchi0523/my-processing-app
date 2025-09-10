package com.example.infrastructure.agent.tools;

import com.example.domain.agent.AgentContext;
import com.example.domain.agent.tools.Tool;
import com.example.domain.agent.tools.ToolExecutionException;
import com.example.domain.agent.tools.ToolResult;
import com.example.domain.agent.tools.ToolValidationResult;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.serialization.YamlParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge tool: returns current state summary (optionally YAML) without modifying state.
 * name: "get_state"
 */
public class GetStateTool implements Tool {

    @Override
    public String getName() {
        return "get_state";
    }

    @Override
    public String getDescription() {
        return "Return current drawing state summary and optional YAML";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("includeShapes", "boolean: include shape count (default true)");
        params.put("includeSettings", "boolean: include width/height (default true)");
        params.put("summary", "boolean: summary only (default true)");
        params.put("includeYaml", "boolean: include full YAML (default false)");
        return params;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {
        if (context == null || !context.hasCurrentState()) {
            throw ToolExecutionException.stateError(getName(), "No current state available");
        }

        boolean includeShapes = getBoolean(parameters, "includeShapes", true);
        boolean includeSettings = getBoolean(parameters, "includeSettings", true);
        boolean summaryOnly = getBoolean(parameters, "summary", true);
        boolean includeYaml = getBoolean(parameters, "includeYaml", false);

        DrawingState state = context.getCurrentState().get();
        Map<String, Object> data = new HashMap<>();

        if (includeShapes) {
            data.put("shapeCount", state.getShapes().size());
        }
        if (includeSettings) {
            data.put("width", state.getIntSetting("width", 800));
            data.put("height", state.getIntSetting("height", 600));
        }

        if (includeYaml && !summaryOnly) {
            try {
                String yaml = new YamlParser().toYaml(state);
                data.put("stateYaml", yaml);
            } catch (YamlParser.YamlParseException ignored) {
                // YAML生成に失敗してもサマリーは返せる
            }
        }

        String msg = String.format(
                "State: shapes=%d, size=%dx%d",
                state.getShapes().size(),
                state.getIntSetting("width", 800),
                state.getIntSetting("height", 600));

        return ToolResult.success(msg, data).withData("tool", getName());
    }

    @Override
    public ToolValidationResult validate(Map<String, Object> parameters) {
        // 寛容に受け入れる（型/値は execute 内で既定値フォールバック）
        return ToolValidationResult.valid();
    }

    private boolean getBoolean(Map<String, Object> params, String key, boolean def) {
        if (params == null) return def;
        Object v = params.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }
}

