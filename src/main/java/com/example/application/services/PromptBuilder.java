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
     * Build conversation context from history with a compact tooling reminder.
     * Only include recent messages to avoid exceeding API limits.
     */
    public String buildContextPrompt(String currentPrompt) {
        StringBuilder prompt = new StringBuilder();
        List<AIAgentService.ChatMessage> history = conversationService.getHistory();

        // Add system prompt
        prompt.append(conversationService.getSystemPrompt()).append("\n\n");

        // Compact tooling reminder (kept short; repeated each turn for reliability)
        prompt.append(String.join("\n",
                "[Tooling Reminder]",
                "関数は: createRectangle(x,y,width,height,color), createCircle(x,y,radius,color),",
                "createEllipse(x,y,radiusX,radiusY,color), createTriangle(x1,y1,x2,y2,x3,y3,color),",
                "createLine(x1,y1,x2,y2,color), moveShape(id,x,y), deleteShape(id),",
                "changeColor(id,color), resizeShape(id,newWidth,newHeight,newRadius), clearCanvas().",
                "まずDesign Planを1〜3行で書き、その後に必要な関数を呼び出す。",
                "整数座標、20..980/20..620付近に収める。",
                "")).append('\n');

        // Only include last 5 messages to avoid prompt size issues
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

    /**
     * Overload that additionally embeds current canvas size for better relative placement.
     */
    public String buildContextPrompt(String currentPrompt, com.example.domain.models.DrawingState state) {
        String base = buildContextPrompt(currentPrompt);
        int w = state != null ? state.getIntSetting("width", 800) : 800;
        int h = state != null ? state.getIntSetting("height", 600) : 600;
        return base + "\n\n[Canvas] width=" + w + ", height=" + h + " (相対配置の基準)";
    }
}
