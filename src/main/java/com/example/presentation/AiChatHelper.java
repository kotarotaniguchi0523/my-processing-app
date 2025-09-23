package com.example.presentation;

import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.services.StateManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.logging.Level;

public final class AiChatHelper {
  private static final Logger logger = Logger.getLogger(AiChatHelper.class.getName());
  private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "AI-Processing-Thread");
    t.setDaemon(true); // Don't prevent JVM shutdown
    return t;
  });

  private AiChatHelper() {}

  /**
   * Main thread callback interface for Processing thread-safe updates
   */
  public interface MainThreadCallback {
    void execute();
  }

  /**
   * Send user input and process AI response asynchronously, ensuring all UI updates
   * happen on the main Processing thread via callbacks.
   */
  public static Future<?> sendAndAppendAsync(AIAgentService ai, ChatManager chat, String userText,
      MainThreadCallback onComplete) {
    if (userText == null || userText.isBlank()) {
      return null;
    }

    // Add user message immediately (this should be called from main thread)
    chat.addMessage("User: " + userText);

    // Get current state before processing (thread-safe read)
    final DrawingState stateBefore = ai.getCurrentState();

    return executor.submit(() -> {
      try {
        logger.info("Starting AI processing for user input: " + userText.substring(0, Math.min(50, userText.length())));

        // AI processing happens in background thread
        AIAgentService.AgentReply reply = ai.handleUserPrompt(userText);

        // Prepare messages to be added (but don't add them yet)
        final DrawingState stateAfter = reply.state;

        // Schedule UI updates on main thread via callback mechanism
        // Note: The actual UI update must be called from Processing's draw() loop
        if (onComplete != null) {
          // Store the results for main thread processing
          // This is a simple approach - in a more complex system, you might use a queue
          synchronized (chat) {
            // Add assistant response
            chat.addMessage("Assistant: " + reply.message);

            // Add state change information if state changed
            if (stateAfter != null && !stateAfter.equals(stateBefore)) {
              addStateChangeMessages(chat, stateBefore, stateAfter);
            }
          }

          // Execute callback to notify main thread that processing is complete
          onComplete.execute();
        }

        logger.info("AI processing completed successfully");

      } catch (Exception ex) {
        logger.log(Level.SEVERE, "AI processing failed", ex);

        // Handle errors thread-safely
        synchronized (chat) {
          chat.addMessage("Assistant: (エラーが発生しました: " + ex.getMessage() + ")");
        }

        // Still execute callback to notify completion (even if failed)
        if (onComplete != null) {
          onComplete.execute();
        }
      }
    });
  }

  /**
   * Add state change messages to chat. This method should only be called
   * from within a synchronized block on chat.
   */
  private static void addStateChangeMessages(ChatManager chat, DrawingState stateBefore, DrawingState stateAfter) {
    try {
      // Use StateManager's calculateStateDifference for detailed diff
      StateManager stateManager = new StateManager();
      String diff = stateManager.calculateStateDifference(stateBefore, stateAfter);

      // Show detailed diff
      if (diff != null && !diff.trim().isEmpty()) {
        chat.addMessage("System: [変更内容] " + diff);
      }

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

    } catch (Exception ex) {
      logger.log(Level.WARNING, "Failed to calculate state difference", ex);
      chat.addMessage("System: 図形の状態が変更されました");
    }
  }

  /**
   * Gracefully shutdown the executor service
   */
  public static void shutdown() {
    logger.info("Shutting down AI processing executor");
    executor.shutdown();
  }

  /**
   * Check if there are any pending AI operations
   */
  public static boolean hasPendingOperations() {
    return !executor.isTerminated();
  }
}
