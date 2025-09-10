package com.example.infrastructure.agent.tools;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridge tool to perform undo/redo using AgentContext's StateHistory.
 * name: "undo_redo"
 * params: { operation: "undo" | "redo" }
 */
public class UndoRedoTool implements IAgentTool {

    @Override
    public String name() {
        return "undo_redo";
    }

    @Override
    public Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", "Perform undo or redo and return the new drawing state");

        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> operation = new HashMap<>();
        operation.put("type", "string");
        operation.put("description", "'undo' or 'redo'");
        operation.put("enum", Arrays.asList("undo", "redo"));
        properties.put("operation", operation);

        schema.put("properties", properties);
        schema.put("required", Arrays.asList("operation"));
        return schema;
    }

    @Override
    public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
        // このツールは履歴管理に依存するため、
        // 実際の undo/redo 処理は、呼び出し側で履歴管理と連携して処理されることを想定
        // ここでは元の状態をそのまま返す
        return state;
    }

}
