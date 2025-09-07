package com.example.agent.prompt;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides template-based prompt generation with variable substitution
 * and formatting capabilities for consistent prompt structure.
 */
public class PromptTemplate {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    
    // Predefined templates
    public static final String TOOL_EXECUTION_TEMPLATE = """
        Task: Execute drawing operation
        Tool: {{tool_name}}
        Parameters: {{parameters}}
        Current State: {{current_state}}
        
        Please execute this tool with the given parameters and return the result.
        """;
    
    public static final String ERROR_RECOVERY_TEMPLATE = """
        An error occurred during tool execution:
        Tool: {{tool_name}}
        Error: {{error_message}}
        Attempted Parameters: {{parameters}}
        
        Please suggest an alternative approach or corrected parameters.
        """;
    
    public static final String USER_INSTRUCTION_TEMPLATE = """
        User Instruction: {{instruction}}
        Available Tools: {{available_tools}}
        Current Canvas State: {{canvas_state}}
        
        Please interpret the user's instruction and execute the appropriate tools.
        """;
    
    public static final String CONFIRMATION_TEMPLATE = """
        Proposed Action: {{action}}
        Tool Chain: {{tool_chain}}
        Expected Outcome: {{expected_outcome}}
        
        This action requires user confirmation. Details:
        {{details}}
        """;
    
    private final String template;
    private final Map<String, Object> variables;
    
    /**
     * Creates a new prompt template
     * @param template The template string with {{variable}} placeholders
     */
    public PromptTemplate(String template) {
        this.template = template;
        this.variables = new HashMap<>();
    }
    
    /**
     * Creates a prompt template from a predefined template
     * @param predefinedTemplate One of the predefined template constants
     * @return A new PromptTemplate instance
     */
    public static PromptTemplate fromPredefined(String predefinedTemplate) {
        return new PromptTemplate(predefinedTemplate);
    }
    
    /**
     * Sets a variable value for substitution
     * @param name The variable name (without brackets)
     * @param value The value to substitute
     * @return This template for chaining
     */
    public PromptTemplate set(String name, Object value) {
        variables.put(name, value);
        return this;
    }
    
    /**
     * Sets multiple variables at once
     * @param vars Map of variable names to values
     * @return This template for chaining
     */
    public PromptTemplate setAll(Map<String, Object> vars) {
        variables.putAll(vars);
        return this;
    }
    
    /**
     * Clears all variables
     * @return This template for chaining
     */
    public PromptTemplate clear() {
        variables.clear();
        return this;
    }
    
    /**
     * Renders the template with current variables
     * @return The rendered prompt string
     */
    public String render() {
        String result = template;
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            
            String replacement;
            if (value == null) {
                replacement = "{{" + variableName + "}}"; // Keep placeholder if not set
            } else {
                replacement = formatValue(value);
            }
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Formats a value for inclusion in the prompt
     * @param value The value to format
     * @return Formatted string representation
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof Map) {
            return formatMap((Map<?, ?>) value);
        }
        
        if (value instanceof Iterable) {
            return formatIterable((Iterable<?>) value);
        }
        
        return value.toString();
    }
    
    /**
     * Formats a map for inclusion in the prompt
     * @param map The map to format
     * @return Formatted string representation
     */
    private String formatMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        map.forEach((key, value) -> {
            sb.append("  ").append(key).append(": ").append(value).append("\n");
        });
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Formats an iterable for inclusion in the prompt
     * @param iterable The iterable to format
     * @return Formatted string representation
     */
    private String formatIterable(Iterable<?> iterable) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (Object item : iterable) {
            sb.append("  - ").append(item).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Validates that all required variables are set
     * @param requiredVars List of required variable names
     * @return true if all required variables are set
     */
    public boolean validateRequired(String... requiredVars) {
        for (String var : requiredVars) {
            if (!variables.containsKey(var)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets a list of all variables in the template
     * @return List of variable names found in the template
     */
    public java.util.List<String> getTemplateVariables() {
        java.util.List<String> vars = new java.util.ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!vars.contains(variableName)) {
                vars.add(variableName);
            }
        }
        
        return vars;
    }
    
    /**
     * Gets a list of unset variables
     * @return List of variable names that haven't been set
     */
    public java.util.List<String> getUnsetVariables() {
        java.util.List<String> unset = new java.util.ArrayList<>();
        for (String var : getTemplateVariables()) {
            if (!variables.containsKey(var)) {
                unset.add(var);
            }
        }
        return unset;
    }
    
    /**
     * Creates a copy of this template with the same variables
     * @return A new PromptTemplate instance
     */
    public PromptTemplate copy() {
        PromptTemplate copy = new PromptTemplate(template);
        copy.variables.putAll(this.variables);
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("PromptTemplate{variables=%d, unset=%d}", 
            variables.size(), getUnsetVariables().size());
    }
}