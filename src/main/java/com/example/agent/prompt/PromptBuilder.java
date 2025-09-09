package com.example.agent.prompt;

import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;
import com.example.domain.tools.ToolRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PromptBuilder constructs system prompts with tool schemas and manages conversation context
 * for AI interactions. Supports both Japanese and English instructions.
 */
public class PromptBuilder {
    
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        あなたはProcessingで図形を描画するAIエージェントです。
        ユーザーの指示を理解し、提供されたツールを使用して描画を実行します。
        
        You are an AI agent that draws shapes using Processing.
        You understand user instructions and execute drawings using the provided tools.
        
        使用可能なツール / Available Tools:
        %s
        
        ツール呼び出しのガイドライン / Tool Calling Guidelines:
        1. ユーザーの指示を正確に理解してください / Understand user instructions accurately
        2. 適切なツールとパラメータを選択してください / Select appropriate tools and parameters
        3. 複数のツールを組み合わせて使用できます / You can combine multiple tools
        4. エラーが発生した場合は、ユーザーに説明してください / If an error occurs, explain it to the user
        
        レスポンス形式 / Response Format:
        - ツールを呼び出す場合は、JSON形式で応答してください / When calling tools, respond in JSON format
        - 説明やエラーメッセージは通常のテキストで応答してください / Respond with plain text for explanations or error messages
        """;
    
    private final IToolRegistry toolRegistry;
    private final ConversationHistory conversationHistory;
    private final Gson gson;
    private String customSystemPrompt;
    
    public PromptBuilder() {
        this(ToolRegistry.getInstance());
    }
    
    public PromptBuilder(IToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.conversationHistory = new ConversationHistory();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public IToolRegistry getToolRegistry() {
        return toolRegistry;
    }
    
    /**
     * Builds the system prompt with all registered tool schemas
     * @return The complete system prompt
     */
    public String buildSystemPrompt() {
        if (customSystemPrompt != null) {
            return customSystemPrompt;
        }
        
        List<Map<String, Object>> toolSchemas = toolRegistry.generateToolSchemas();
        String toolDescriptions = formatToolDescriptions(toolSchemas);
        
        return String.format(SYSTEM_PROMPT_TEMPLATE, toolDescriptions);
    }
    
    /**
     * Sets a custom system prompt
     * @param prompt The custom system prompt
     */
    public void setCustomSystemPrompt(String prompt) {
        this.customSystemPrompt = prompt;
    }
    
    /**
     * Formats tool descriptions for inclusion in the system prompt
     * @param toolSchemas List of tool schemas
     * @return Formatted tool descriptions
     */
    private String formatToolDescriptions(List<Map<String, Object>> toolSchemas) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n```json\n");
        sb.append(gson.toJson(toolSchemas));
        sb.append("\n```\n");
        
        // Add human-readable descriptions
        sb.append("\nツール説明 / Tool Descriptions:\n");
        for (Map<String, Object> schema : toolSchemas) {
            Map<String, Object> function = (Map<String, Object>) schema.get("function");
            if (function != null) {
                String name = (String) function.get("name");
                String description = (String) function.get("description");
                sb.append(String.format("- **%s**: %s\n", name, description));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Builds a complete prompt including system prompt and conversation history
     * @return The complete prompt for AI interaction
     */
    public String buildCompletePrompt() {
        StringBuilder prompt = new StringBuilder();
        
        // Add system prompt
        prompt.append("System: ").append(buildSystemPrompt()).append("\n\n");
        
        // Add conversation history
        List<ConversationMessage> messages = conversationHistory.getMessages();
        for (ConversationMessage message : messages) {
            prompt.append(formatMessage(message)).append("\n\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Formats a conversation message for the prompt
     * @param message The message to format
     * @return Formatted message string
     */
    private String formatMessage(ConversationMessage message) {
        switch (message.getRole()) {
            case USER:
                return "User: " + message.getContent();
            case ASSISTANT:
                return "Assistant: " + message.getContent();
            case TOOL_RESULT:
                return "Tool Result: " + message.getContent();
            case SYSTEM:
                return "System: " + message.getContent();
            default:
                return message.getContent();
        }
    }
    
    /**
     * Adds a user message to the conversation history
     * @param message The user's message
     */
    public void addUserMessage(String message) {
        conversationHistory.addMessage(ConversationMessage.user(message));
    }
    
    /**
     * Adds an assistant response to the conversation history
     * @param response The assistant's response
     */
    public void addAssistantResponse(String response) {
        conversationHistory.addMessage(ConversationMessage.assistant(response));
    }
    
    /**
     * Adds a tool execution result to the conversation history
     * @param toolName The name of the executed tool
     * @param result The execution result
     */
    public void addToolResult(String toolName, String result) {
        String formattedResult = String.format("Tool '%s' executed: %s", toolName, result);
        conversationHistory.addMessage(ConversationMessage.toolResult(formattedResult));
    }
    
    /**
     * Adds a system message to the conversation history
     * @param message The system message
     */
    public void addSystemMessage(String message) {
        conversationHistory.addMessage(ConversationMessage.system(message));
    }
    
    /**
     * Gets the conversation history
     * @return The conversation history
     */
    public ConversationHistory getConversationHistory() {
        return conversationHistory;
    }
    
    /**
     * Clears the conversation history
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    /**
     * Gets the current conversation context size
     * @return Number of messages in the conversation
     */
    public int getConversationSize() {
        return conversationHistory.size();
    }
    
    /**
     * Builds a prompt for tool execution confirmation
     * @param toolName The tool to be executed
     * @param parameters The tool parameters
     * @return Confirmation prompt
     */
    public String buildToolConfirmationPrompt(String toolName, Map<String, Object> parameters) {
        return String.format("""
            ツール実行の確認 / Tool Execution Confirmation:
            
            実行するツール / Tool to execute: %s
            パラメータ / Parameters:
            %s
            
            このツールを実行してもよろしいですか？
            Would you like to execute this tool?
            """, toolName, gson.toJson(parameters));
    }
    
    /**
     * Builds an error prompt for tool execution failures
     * @param toolName The tool that failed
     * @param error The error message
     * @return Error prompt
     */
    public String buildToolErrorPrompt(String toolName, String error) {
        return String.format("""
            ツール実行エラー / Tool Execution Error:
            
            ツール / Tool: %s
            エラー / Error: %s
            
            別の方法を試すか、パラメータを修正してください。
            Please try a different approach or correct the parameters.
            """, toolName, error);
    }
    
    /**
     * Validates that all required tools are available
     * @param requiredToolNames List of required tool names
     * @return true if all tools are available
     */
    public boolean validateRequiredTools(List<String> requiredToolNames) {
        for (String toolName : requiredToolNames) {
            if (!toolRegistry.hasTool(toolName)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets a summary of available tools
     * @return Summary string of available tools
     */
    public String getToolsSummary() {
        List<IAgentTool> tools = toolRegistry.getAllTools();
        if (tools.isEmpty()) {
            return "No tools available / 利用可能なツールがありません";
        }
        
        return tools.stream()
            .map(tool -> String.format("- %s", tool.name()))
            .collect(Collectors.joining("\n", 
                "Available tools / 利用可能なツール:\n", ""));
    }
}
