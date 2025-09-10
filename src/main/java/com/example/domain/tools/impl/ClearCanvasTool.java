package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.HashMap;
import java.util.Map;

/**
 * ClearCanvasTool removes all shapes from the current drawing while preserving settings/arrays/lists.
 */
public class ClearCanvasTool implements IAgentTool {

    @Override
    public String name() {
        return "clearCanvas";
    }

    @Override
    public Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", "Clear all shapes from the canvas while keeping other settings");
        schema.put("properties", new HashMap<>()); // no params
        schema.put("required", new java.util.ArrayList<>());
        return schema;
    }

    @Override
    public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
        // Create a new state with the same settings/arrays/lists but no shapes
        return DrawingState.builder()
                .settings(state.getSettings())
                .arrays(state.getArrays())
                .lists(state.getLists())
                .build();
    }
}

