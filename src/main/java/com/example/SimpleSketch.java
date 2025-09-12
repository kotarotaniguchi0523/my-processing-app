package com.example;


import com.example.presentation.SketchUIRenderer;
import com.example.presentation.SketchEventHandler;
import com.example.presentation.ChatManager;
import com.example.presentation.InputManager;
import com.example.application.services.AIAgentService;
import com.example.domain.models.DrawingState;

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
    // Initialize AI agent service with default state
    aiAgentService = new AIAgentService(DrawingState.defaultState());
    inputManager = new InputManager(this, chatManager, aiAgentService);
    uiRenderer = new SketchUIRenderer(this, chatManager, aiAgentService);
    eventHandler =
        new SketchEventHandler(this, uiRenderer, chatManager, inputManager, aiAgentService);

    // Configure UI renderer
    uiRenderer.setFonts(font, smallFont);
    uiRenderer.calculateLayout();


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

    // Draw UI borders for clarity
    uiRenderer.drawUIBorders();

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


}
