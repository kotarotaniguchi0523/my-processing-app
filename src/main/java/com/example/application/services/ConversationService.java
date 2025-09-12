package com.example.application.services;

import com.example.domain.models.AgentHistory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles conversation history management for AI agent interactions.
 * Responsible for maintaining chat messages, trimming history, and session management.
 */
public class ConversationService {
    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are an AI drawing assistant.",
            "User interacts with a vector drawing canvas (shapes: circle, rectangle, line, text).",
            "Keep answers concise. If a drawing action is requested, you may later emit a tool call (not yet implemented).",
            "Ask clarifying questions when parameters are ambiguous."
    );

    private static final int MAX_HISTORY = 20;
    private final List<AIAgentService.ChatMessage> history = new ArrayList<>();
    private final String sessionId = UUID.randomUUID().toString();
    
    public ConversationService() {
        // Seed system message
        history.add(AIAgentService.ChatMessage.system(SYSTEM_PROMPT));
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
    
    public void addUserMessage(String content) {
        history.add(AIAgentService.ChatMessage.user(content));
        trimHistory();
    }
    
    public void addAssistantMessage(String content) {
        history.add(AIAgentService.ChatMessage.assistant(content));
        trimHistory();
    }
    
    public void addAssistantMessageWithTools(String content, List<AIAgentService.ToolCall> toolCalls) {
        history.add(AIAgentService.ChatMessage.assistantWithTools(content, toolCalls));
        trimHistory();
    }
    
    public List<AIAgentService.ChatMessage> getHistory() {
        return Collections.unmodifiableList(history);
    }
    
    public void resetHistory() {
        history.clear();
        history.add(AIAgentService.ChatMessage.system(SYSTEM_PROMPT));
    }
    
    public void loadFromAgentHistory(AgentHistory.Session session) {
        if (session == null) return;
        
        history.clear();
        boolean hasSystemMessage = false;
        
        for (AgentHistory.Message msg : session.getMessages()) {
            String roleStr = msg.getRole().toLowerCase();
            String content = msg.getContent();
            
            // Convert tool calls if present
            List<AIAgentService.ToolCall> toolCalls = null;
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                toolCalls = new ArrayList<>();
                for (AgentHistory.ToolCall tc : msg.getToolCalls()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> args = tc.getArguments() instanceof java.util.Map ? 
                        (java.util.Map<String, Object>) tc.getArguments() : new java.util.HashMap<>();
                    toolCalls.add(new AIAgentService.ToolCall(tc.getName(), args, tc.getResult()));
                }
            }
            
            switch (roleStr) {
                case "system":
                    history.add(AIAgentService.ChatMessage.system(content));
                    hasSystemMessage = true;
                    break;
                case "user":
                    history.add(AIAgentService.ChatMessage.user(content));
                    break;
                default:
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        history.add(AIAgentService.ChatMessage.assistantWithTools(content, toolCalls));
                    } else {
                        history.add(AIAgentService.ChatMessage.assistant(content));
                    }
                    break;
            }
        }
        
        // Ensure we have system message
        if (!hasSystemMessage) {
            history.add(0, AIAgentService.ChatMessage.system(SYSTEM_PROMPT));
        }
    }
    
    private void trimHistory() {
        if (history.size() > MAX_HISTORY) {
            int remove = history.size() - MAX_HISTORY;
            // keep system message at index 0
            int idx = 1;
            while (remove > 0 && idx < history.size()) {
                history.remove(idx);
                remove--;
            }
        }
    }
}