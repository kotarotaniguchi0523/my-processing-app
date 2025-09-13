package com.example;


import com.example.presentation.SketchUIRenderer;
import com.example.presentation.SketchEventHandler;
import com.example.presentation.ChatManager;
import com.example.presentation.InputManager;
import com.example.presentation.DiffApprovalRenderer;
import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;
import com.example.infrastructure.ServiceInitializer;

import processing.core.PApplet;
import processing.core.PFont;


public class SimpleSketch extends PApplet {


  // UI Components (refactored into focused classes)
  private SketchUIRenderer uiRenderer;
  private SketchEventHandler eventHandler;
  private ChatManager chatManager;
  private InputManager inputManager;
  private AIAgentService aiAgentService;

  // UI Components
  private PFont font;
  private PFont smallFont;

  public static void main(String[] args) {
    PApplet.main("com.example.SimpleSketch");
  }

  @Override
  public void settings() {
    size(1000, 700);
  }

  @Override
  public void setup() {
    background(240);
    // Ensure color mode is RGB across the app
    colorMode(RGB, 255);
    // Allow window resize per PRD
    try {
      if (getSurface() != null) {
        getSurface().setResizable(true);
      }
    } catch (Exception ignored) {
      // safe to ignore if surface not available
    }

    // Initialize fonts
    setupFonts();



    // Initialize UI components
    initializeUIComponents();

    println("UI Layout initialized - SimpleSketch refactored into focused components");
  }

  private void setupFonts() {
    // Improve text readability: prefer Meiryo/Yu Gothic on Windows, with fallbacks
    try {
      font = chooseReadableFont(
          new String[] {"Meiryo UI", "Meiryo", "Yu Gothic UI", "Yu Gothic", "MS Gothic"}, 18);
      smallFont = chooseReadableFont(
          new String[] {"Meiryo UI", "Meiryo", "Yu Gothic UI", "Yu Gothic", "MS Gothic"}, 16);
      smooth(); // enable anti-aliasing for crisper text
    } catch (Exception e) {
      // フォント作成に失敗した場合のフォールバック
      textSize(18);
    }
  }

  private void initializeUIComponents() {
    // Initialize managers in order of dependency
    chatManager = new ChatManager(this);

    // Initialize AI agent service with error handling for missing API key
    try {
      aiAgentService = new AIAgentService(DrawingState.defaultState());
      // Register with ServiceInitializer for access from other components
      ServiceInitializer.setAIAgentService(aiAgentService);
    } catch (IllegalArgumentException e) {
      // API key is missing, but continue with null AI service
      System.err.println("警告: Gemini APIキーが設定されていません。AI機能は無効です。");
      System.err.println("環境変数 GOOGLE_API_KEY または GEMINI_API_KEY を設定してください。");
      aiAgentService = null;
      ServiceInitializer.setAIAgentService(null);
    }

    uiRenderer = new SketchUIRenderer(this, chatManager, aiAgentService);
    inputManager = new InputManager(this, chatManager, aiAgentService, uiRenderer);
    eventHandler =
        new SketchEventHandler(this, uiRenderer, chatManager, inputManager, aiAgentService);

    // Configure UI renderer
    uiRenderer.setFonts(font, smallFont);
    uiRenderer.calculateLayout();

    // Register ChatManager as AI processing listener
    // This enables real-time display of AI thinking process
    if (aiAgentService != null) {
      aiAgentService.addProcessingListener(chatManager);

      // Set up approval callback for DiffApprovalRenderer
      setupApprovalCallback();
    }
  }

  // Try creating the first available font from candidates
  private PFont chooseReadableFont(String[] candidates, int size) {
    for (String name : candidates) {
      try {
        PFont f = createFont(name, size, true);
        if (f != null) {
          return f;
        }
      } catch (Exception ignored) {
        // try next candidate
      }
    }
    // Fallback to default
    return createFont("SansSerif", size, true);
  }

  @Override
  public void draw() {
    // 背景グラデーション（テーマ）
    uiRenderer.drawBackground();

    // Flush AI processing messages from queue to UI (thread-safe)
    chatManager.flushQueueToUI();

    // Recalculate layout on window resize
    uiRenderer.calculateLayout();
    inputManager.updateJapaneseInputPosition();

    // Draw UI areas through renderer
    uiRenderer.drawCanvasArea();
    uiRenderer.drawChatArea();

    // Draw UI borders for clarity
    uiRenderer.drawUIBorders();

    // Draw diff approval dialog if active
    uiRenderer.drawDiffApproval();

  }

  // Event handlers - delegate to EventHandler
  @Override
  public void mousePressed() {
    // Check if diff approval dialog is handling the click
    if (uiRenderer.getDiffApprovalRenderer().isShowingApproval()) {
      if (uiRenderer.getDiffApprovalRenderer().handleMousePressed(mouseX, mouseY)) {
        return; // Click was handled by diff approval
      }
    }

    eventHandler.handleMousePressed();
    // 送信ボタン押下アニメ
    if (uiRenderer.isInSendButtonArea(mouseX, mouseY)) {
      uiRenderer.setSendPressed(true);
    }
  }


  @Override
  public void keyPressed() {
    eventHandler.handleKeyPressed();
  }

  @Override
  public void keyReleased() {
    eventHandler.handleKeyReleased();
  }

  @Override
  public void mouseReleased() {
    uiRenderer.setSendPressed(false);
  }

  @Override
  public void mouseWheel(processing.event.MouseEvent event) {
    // If diff approval modal is open and wants to consume the wheel, do not propagate
    if (uiRenderer.getDiffApprovalRenderer().isShowingApproval()) {
      boolean handled = uiRenderer.getDiffApprovalRenderer().handleMouseWheel(event);
      if (handled)
        return;
    }
    eventHandler.handleMouseWheel(event);
  }

  private void setupApprovalCallback() {
    // Create approval callback implementation
    AIAgentService.ApprovalCallback approvalCallback = new AIAgentService.ApprovalCallback() {
      @Override
      public void onApprove(DrawingState newState, String message) {
        // Show approval dialog
        DrawingState currentState = aiAgentService.getCurrentState();
        uiRenderer.getDiffApprovalRenderer().showApproval(currentState, newState);

        // Set up the renderer's approval callback
        uiRenderer.getDiffApprovalRenderer()
            .setApprovalCallback(new DiffApprovalRenderer.ApprovalCallback() {
              @Override
              public void onApprove() {
                // Apply the approved state
                aiAgentService.applyApprovedState(newState, message);
                chatManager.addMessage("AI: " + message);
                chatManager.addMessage("System: 変更が承認されました");
              }

              @Override
              public void onReject() {
                // Just add a rejection message
                chatManager.addMessage("System: 変更が拒否されました");
              }
            });
      }

      @Override
      public void onReject() {
        // This is called when AI decides not to make changes
        // Not typically used in our flow
      }
    };

    // Set the callback in AIAgentService
    aiAgentService.setApprovalCallback(approvalCallback);
  }


}
