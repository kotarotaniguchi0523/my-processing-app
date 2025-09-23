package com.example.domain.tools.adk;

import java.util.Map;

/**
 * Abstract base class for ADK tools providing common functionality.
 * Implements the ADKTool interface with default behavior for common operations.
 */
public abstract class BaseADKTool implements ADKTool {

    /**
     * Get the tool name from the schema.
     * @return The tool name
     */
    public String getName() {
        Map<String, Object> schema = getSchema();
        Object name = schema.get("name");
        return name != null ? name.toString() : getClass().getSimpleName();
    }

    /**
     * Default implementation - subclasses should override if they have a specific schema.
     */
    @Override
    public Map<String, Object> getSchema() {
        // Return a basic schema - subclasses should override this
        Map<String, Object> schema = new java.util.HashMap<>();
        schema.put("name", getName());
        schema.put("description", "Tool: " + getName());
        return schema;
    }
}