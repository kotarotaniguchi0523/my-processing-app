package com.example;

import com.example.usecase.UseCaseFacade;
import com.example.infrastructure.rendering.DrawingEngine;
import com.example.presentation.SketchUIRenderer;
import com.example.presentation.SketchEventHandler;
import com.example.presentation.ChatManager;
import com.example.presentation.InputManager;

import processing.core.PApplet;
import processing.core.PFont;


public class SimpleSketch extends PApplet {

  // Application Integration
  private UseCaseFacade useCaseFacade;
  private DrawingEngine drawingEngine;

  // UI Components (refactored into focused classes)
  private SketchUIRenderer uiRenderer;
  private SketchEventHandler eventHandler;
  private ChatManager chatManager;
  private InputManager inputManager;

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

    // Initialize use case facade
    useCaseFacade = UseCaseFacade.getInstance();
    
    // Initialize drawing engine for domain rendering
    drawingEngine = new DrawingEngine(this);

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
    chatManager = new ChatManager(this, useCaseFacade);
    inputManager = new InputManager(this, chatManager);
    uiRenderer = new SketchUIRenderer(this, chatManager, drawingEngine, useCaseFacade);
    eventHandler = new SketchEventHandler(this, uiRenderer, chatManager, inputManager, useCaseFacade);

    // Configure UI renderer
    uiRenderer.setFonts(font, smallFont);
    uiRenderer.calculateLayout();
    
    // CRITICAL: Setup callbacks for UseCaseFacade to connect business logic to UI
    setupUseCaseFacadeCallback();
  }
  
  private void setupUseCaseFacadeCallback() {
    useCaseFacade.setCallback(new UseCaseFacade.Callback() {
      @Override
      public void onStatusUpdate(String status) {
        println("Status: " + status);
      }
      
      @Override
      public void onChatMessage(String message) {
        chatManager.addMessage(message);
      }
      
      @Override
      public void onApprovalRequired(String summary, String diff, com.example.domain.models.DrawingState pendingState) {
        // Approval functionality removed
        println("Approval required (disabled): " + summary);
      }
      
      @Override
      public void onStateChanged(com.example.domain.models.DrawingState newState) {
        // State change is handled by the drawing engine
        println("State changed");
      }
      
      @Override
      public void onError(String error) {
        chatManager.addMessage("Error: " + error);
        println("Error: " + error);
      }
      
      @Override
      public void onMessage(String message) {
        chatManager.addMessage("System: " + message);
      }
    });
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
    background(245);

    // Recalculate layout on window resize
    uiRenderer.calculateLayout();
    inputManager.updateJapaneseInputPosition();

    // Draw UI areas through renderer
    uiRenderer.drawCanvasArea();
    uiRenderer.drawChatArea();
    // drawStatusArea() disabled - AI status display removed
    // uiRenderer.drawStatusArea();

    // Draw UI borders for clarity
    uiRenderer.drawUIBorders();

    // Approval overlay disabled - approval functionality removed
    // if (approvalManager.isApprovalPending()) {
    //   uiRenderer.drawApprovalOverlay(approvalManager);
    // }
  }

  // Event handlers - delegate to EventHandler
  @Override
  public void mousePressed() {
    eventHandler.handleMousePressed();
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
  public void mouseWheel(processing.event.MouseEvent event) {
    eventHandler.handleMouseWheel(event);
  }

  @Override
  public void exit() {
    // シャットダウン時の処理
    if (useCaseFacade != null) {
      useCaseFacade.shutdown();
    }
    super.exit();
  }
}