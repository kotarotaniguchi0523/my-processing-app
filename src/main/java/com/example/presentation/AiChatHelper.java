package com.example.presentation;

import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.services.StateManager;

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
        
        // Show the assistant's response (thread-safe)
        // Use synchronized block to ensure all messages are added atomically
        synchronized (chat) {
          chat.addMessage("Assistant: " + reply.message);
          
          // Check if state changed and show diff
          DrawingState stateAfter = reply.state;
          
          // Debug logging
          System.out.println("State Before: " + (stateBefore != null ? stateBefore.getShapes().size() : "null") + " shapes");
          System.out.println("State After: " + (stateAfter != null ? stateAfter.getShapes().size() : "null") + " shapes");
          System.out.println("States equal: " + (stateAfter != null && stateAfter.equals(stateBefore)));
          
          if (stateAfter != null && !stateAfter.equals(stateBefore)) {
            // Use StateManager's calculateStateDifference for detailed diff
            StateManager stateManager = new StateManager();
            String diff = stateManager.calculateStateDifference(stateBefore, stateAfter);
            
            // Debug logging for diff
            System.out.println("Diff calculated: " + diff);
            
            // Show detailed diff
            chat.addMessage("System: [変更内容] " + diff);
            System.out.println("Diff message added to chat");
            
            // Also show simple count-based message
            int shapesBefore = stateBefore != null ? stateBefore.getShapes().size() : 0;
            int shapesAfter = stateAfter.getShapes().size();
            
            if (shapesAfter > shapesBefore) {
              chat.addMessage("System: ✅ 図形が" + (shapesAfter - shapesBefore) + "個追加されました");
            } else if (shapesAfter < shapesBefore) {
              chat.addMessage("System: ❌ 図形が" + (shapesBefore - shapesAfter) + "個削除されました");  
            } else if (shapesAfter > 0) {
              chat.addMessage("System: ✏️ 図形が変更されました");
            }
            
            // Debug: Print all messages after adding
            System.out.println("Current chat messages after diff:");
            for (String msg : chat.getChatMessages()) {
              System.out.println("  - " + msg);
            }
          }
        }
      } catch (Exception ex) {
        // Handle errors thread-safely
        synchronized (chat) {
          chat.addMessage("Assistant: (エラーが発生しました)");
        }
        ex.printStackTrace();
      }
    }, threadName != null ? threadName : "AI-Call").start();
  }
}
