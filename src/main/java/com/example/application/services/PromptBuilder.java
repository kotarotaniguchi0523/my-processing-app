package com.example.application.services;

import java.util.List;

/**
 * Responsible for building context prompts for AI interactions.
 * Combines system prompts, conversation history, and current requests.
 */
public class PromptBuilder {
    
    private final ConversationService conversationService;
    
    public PromptBuilder(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    /**
     * Build conversation context from history.
     * Only include recent messages to avoid exceeding API limits.
     */
    public String buildContextPrompt(String currentPrompt) {
        StringBuilder prompt = new StringBuilder();
        List<AIAgentService.ChatMessage> history = conversationService.getHistory();

        // Add system prompt
        prompt.append(conversationService.getSystemPrompt()).append("\n\n");

        // Only include last 5 messages to avoid prompt size issues
        // Even though we store 20 messages, we only send recent 5 to API
        int startIndex = Math.max(1, history.size() - 5);
        
        if (startIndex < history.size()) {
            prompt.append("会話履歴:\n");
            for (int i = startIndex; i < history.size(); i++) {
                AIAgentService.ChatMessage msg = history.get(i);
                if (msg.role == AIAgentService.Role.USER) {
                    prompt.append("ユーザー: ").append(msg.content).append("\n");
                } else if (msg.role == AIAgentService.Role.ASSISTANT) {
                    // Truncate long assistant messages to avoid prompt size issues
                    String content = msg.content;
                    if (content.length() > 200) {
                        content = content.substring(0, 200) + "...";
                    }
                    prompt.append("アシスタント: ").append(content).append("\n");
                }
            }
        }

        // Add current prompt
        prompt.append("\n現在のリクエスト: ").append(currentPrompt);

        return prompt.toString();
    }
}