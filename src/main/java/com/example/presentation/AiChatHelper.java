package com.example.presentation;

import com.example.application.services.AIAgentService;

import javax.swing.SwingUtilities;

public final class AiChatHelper {
  private AiChatHelper() {}

  public static void sendAndAppendAsync(AIAgentService ai, ChatManager chat, String userText,
      String threadName) {
    if (userText == null || userText.isBlank())
      return;
    chat.addMessage("User: " + userText);
    new Thread(() -> {
      try {
        AIAgentService.AgentReply reply = ai.handleUserPrompt(userText);
        SwingUtilities.invokeLater(() -> chat.addMessage("Assistant: " + reply.message));
      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> chat.addMessage("Assistant: (エラーが発生しました)"));
      }
    }, threadName != null ? threadName : "AI-Call").start();
  }
}
