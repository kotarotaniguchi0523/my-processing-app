package com.example.presentation;

import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;

import javax.swing.SwingUtilities;

public final class AiChatHelper {
  private AiChatHelper() {}

  public static void sendAndAppendAsync(AIAgentService ai, ChatManager chat, String userText,
      String threadName) {
    if (userText == null || userText.isBlank())
      return;
    chat.addMessage("User: " + userText);
    
    // Get current state before processing
    DrawingState stateBefore = ai.getCurrentState();
    
    new Thread(() -> {
      try {
        // AI will process and fire events that ChatManager will display
        AIAgentService.AgentReply reply = ai.handleUserPrompt(userText);
        
        // Show the assistant's response
        SwingUtilities.invokeLater(() -> {
          chat.addMessage("Assistant: " + reply.message);
          
          // Check if state changed and show diff
          DrawingState stateAfter = reply.state;
          if (stateAfter != null && !stateAfter.equals(stateBefore)) {
            // Show what changed
            int shapesBefore = stateBefore.getShapes().size();
            int shapesAfter = stateAfter.getShapes().size();
            
            if (shapesAfter > shapesBefore) {
              chat.addMessage("System: 図形が" + (shapesAfter - shapesBefore) + "個追加されました");
            } else if (shapesAfter < shapesBefore) {
              chat.addMessage("System: 図形が" + (shapesBefore - shapesAfter) + "個削除されました");
            } else {
              chat.addMessage("System: 図形が変更されました");
            }
          }
        });
      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          chat.addMessage("Assistant: (エラーが発生しました)");
          ex.printStackTrace();
        });
      }
    }, threadName != null ? threadName : "AI-Call").start();
  }
}
