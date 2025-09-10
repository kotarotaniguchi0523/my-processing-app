package com.example.infrastructure.agent.adapter;

import com.example.domain.agent.AgentContext;
import com.example.application.agent.DefaultParameterValidator;
import com.example.application.agent.ParameterValidator;
import com.example.domain.values.ValidationResult;
import com.example.domain.agent.tools.Tool;
import com.example.domain.agent.tools.ToolExecutionException;
import com.example.domain.agent.tools.ToolResult;
import com.example.domain.agent.tools.ToolValidationResult;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * IAgentTool を com.example.agent.tools.Tool として見せるアダプタ。
 * これにより ToolManager 系からも domain 側の唯一のツール抽象を利用可能にする。
 */
public class IAgentToolAdapter implements Tool {

    private final IAgentTool delegate;
    private final ParameterValidator validator;
    private final String category;

    public IAgentToolAdapter(IAgentTool delegate) {
        this(delegate, new DefaultParameterValidator(), "general");
    }

    public IAgentToolAdapter(IAgentTool delegate, ParameterValidator validator, String category) {
        this.delegate = delegate;
        this.validator = validator != null ? validator : new DefaultParameterValidator();
        this.category = category != null ? category : "general";
    }

    @Override
    public String getName() {
        return delegate.name();
    }

    @Override
    public String getDescription() {
        Object desc = delegate.schema() != null ? delegate.schema().get("description") : null;
        return desc instanceof String ? (String) desc : ("Execute tool '" + delegate.name() + "'");
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, Object> schema = delegate.schema();
        if (schema == null) {
            return Collections.emptyMap();
        }
        Object propsObj = schema.get("properties");
        if (!(propsObj instanceof Map)) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) propsObj;
        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            String key = e.getKey();
            Object prop = e.getValue();
            String type = (prop instanceof Map && ((Map<?, ?>) prop).get("type") != null)
                    ? String.valueOf(((Map<?, ?>) prop).get("type"))
                    : "any";
            String description = (prop instanceof Map && ((Map<?, ?>) prop).get("description") instanceof String)
                    ? (String) ((Map<?, ?>) prop).get("description")
                    : "type=" + type;
            parameters.put(key, description);
        }
        return parameters;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        if (context == null || !context.hasCurrentState()) {
            throw ToolExecutionException.stateError(getName(), "Current drawing state is required");
        }
        DrawingState current = context.getCurrentState().get();
        try {
            DrawingState newState = delegate.execute(parameters, current);
            return ToolResult.success("Executed " + getName(), newState).withData("adapter", true);
        } catch (IllegalArgumentException ex) {
            throw ToolExecutionException.invalidParameters(getName(), ex.getMessage());
        } catch (RuntimeException ex) {
            throw ToolExecutionException.systemError(getName(), ex.getMessage(), ex);
        }
    }

    @Override
    public ToolValidationResult validate(Map<String, Object> parameters) {
        ValidationResult vr = validator.validateParameters(delegate, parameters != null ? parameters : Collections.emptyMap());
        if (vr.isValid()) {
            return ToolValidationResult.valid();
        }
        return ToolValidationResult.invalid(vr.getErrorMessage());
    }

    @Override
    public boolean requiresApproval() {
        return false;
    }

    @Override
    public boolean isStateModifying() {
        // domainツールは多くが状態変更だが、静的に判断できないためデフォルトfalse
        return false;
    }

    @Override
    public String getCategory() {
        return category;
    }
}

