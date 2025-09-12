package com.example.infrastructure.ai;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.tools.ToolRegistry;
import com.example.infrastructure.services.ToolInvoker;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.values.Color;
import com.example.application.usecase.UseCaseFacade;
import com.example.infrastructure.ServiceInitializer;
import com.example.application.services.AIAgentService;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Drawing functions that can be called by Gemini through Function Calling.
 * These are static methods that will be registered with the Gemini client.
 */
public class DrawingFunctions {
  private static final Logger LOGGER = Logger.getLogger(DrawingFunctions.class.getName());
  private static DrawingState currentState = DrawingState.defaultState();
  private static ToolInvoker toolInvoker;
  
  /**
   * Initialize the DrawingFunctions with UseCaseFacade.
   * This should be called once at application startup.
   */
  public static void initialize() {
    // Ensure services are initialized
    ServiceInitializer.initialize();
    UseCaseFacade useCaseFacade = ServiceInitializer.getUseCaseFacade();
    
    // Initialize ToolRegistry with UseCaseFacade
    ToolRegistry.getInstance().initializeWithPort(useCaseFacade);
    
    // Create tool invoker
    toolInvoker = new ToolInvoker(ToolRegistry.getInstance());
  }
  
  /**
   * Set the current drawing state.
   */
  public static void setCurrentState(DrawingState state) {
    currentState = state;
  }
  
  /**
   * Get the current drawing state.
   */
  public static DrawingState getCurrentState() {
    return currentState;
  }
  
  /**
   * Convert color string to Map object for tool parameters.
   * Supports named colors, hex colors, and rgb format.
   * 
   * @param colorStr The color string (e.g., "red", "#FF0000", "rgb(255,0,0)")
   * @return Map object with r, g, b, a values
   */
  private static Map<String, Object> parseColorString(String colorStr) {
    if (colorStr == null || colorStr.trim().isEmpty()) {
      return null;
    }
    
    colorStr = colorStr.trim().toLowerCase();
    
    try {
      // Handle named colors
      Color color = switch (colorStr) {
        case "red" -> Color.RED;
        case "green" -> Color.GREEN;
        case "blue" -> Color.BLUE;
        case "white" -> Color.WHITE;
        case "black" -> Color.BLACK;
        case "yellow" -> new Color(255, 255, 0);
        case "orange" -> new Color(255, 165, 0);
        case "purple" -> new Color(128, 0, 128);
        case "pink" -> new Color(255, 192, 203);
        case "brown" -> new Color(165, 42, 42);
        case "gray", "grey" -> new Color(128, 128, 128);
        case "cyan" -> new Color(0, 255, 255);
        case "magenta" -> new Color(255, 0, 255);
        default -> {
          // Handle hex colors (with or without #)
          if (colorStr.startsWith("#") || colorStr.matches("[0-9a-f]{6}") || colorStr.matches("[0-9a-f]{8}")) {
            String hex = colorStr.startsWith("#") ? colorStr : "#" + colorStr;
            yield Color.fromHex(hex);
          }
          // Handle rgb format: rgb(r,g,b)
          else if (colorStr.startsWith("rgb(") && colorStr.endsWith(")")) {
            String rgbValues = colorStr.substring(4, colorStr.length() - 1);
            String[] parts = rgbValues.split(",");
            if (parts.length == 3) {
              int r = Integer.parseInt(parts[0].trim());
              int g = Integer.parseInt(parts[1].trim());
              int b = Integer.parseInt(parts[2].trim());
              yield new Color(r, g, b);
            } else {
              throw new IllegalArgumentException("Invalid rgb format");
            }
          }
          // Default to black if unrecognized
          else {
            LOGGER.warning("Unrecognized color format: " + colorStr + ", using black");
            yield Color.BLACK;
          }
        }
      };
      
      // Convert to Map format expected by tools
      Map<String, Object> colorMap = new HashMap<>();
      colorMap.put("r", color.getRed());
      colorMap.put("g", color.getGreen());
      colorMap.put("b", color.getBlue());
      colorMap.put("a", color.getAlpha());
      return colorMap;
      
    } catch (Exception e) {
      LOGGER.warning("Failed to parse color: " + colorStr + ", using black. Error: " + e.getMessage());
      Map<String, Object> colorMap = new HashMap<>();
      colorMap.put("r", 0);
      colorMap.put("g", 0);
      colorMap.put("b", 0);
      colorMap.put("a", 255);
      return colorMap;
    }
  }
  
  /**
   * Create a rectangle on the canvas.
   * 
   * @param x The x-coordinate of the rectangle
   * @param y The y-coordinate of the rectangle  
   * @param width The width of the rectangle
   * @param height The height of the rectangle
   * @param color The color of the rectangle (optional, defaults to black)
   * @return Description of the action taken
   */
  public static String createRectangle(int x, int y, int width, int height, String color) {
    Map<String, Object> args = new HashMap<>();
    Map<String, Object> position = new HashMap<>();
    position.put("x", x);
    position.put("y", y);
    args.put("position", position);
    args.put("width", width);
    args.put("height", height);
    
    // Convert color string to Map object
    Map<String, Object> colorMap = parseColorString(color);
    if (colorMap != null) {
      args.put("fillColor", colorMap);
    }
    
    ToolExecutionResult result = toolInvoker.invokeTool("create_rectangle", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("長方形を作成しました (位置: %d,%d, サイズ: %dx%d)", x, y, width, height);
    } else {
      return "長方形の作成に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Create a circle on the canvas.
   * 
   * @param x The x-coordinate of the circle center
   * @param y The y-coordinate of the circle center
   * @param radius The radius of the circle
   * @param color The color of the circle (optional, defaults to black)
   * @return Description of the action taken
   */
  public static String createCircle(int x, int y, int radius, String color) {
    Map<String, Object> args = new HashMap<>();
    Map<String, Object> center = new HashMap<>();
    center.put("x", x);
    center.put("y", y);
    args.put("center", center);
    args.put("radius", radius);
    
    // Convert color string to Map object
    Map<String, Object> colorMap = parseColorString(color);
    if (colorMap != null) {
      args.put("fillColor", colorMap);
    }
    
    ToolExecutionResult result = toolInvoker.invokeTool("create_circle", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("円を作成しました (中心: %d,%d, 半径: %d)", x, y, radius);
    } else {
      return "円の作成に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Create a line on the canvas.
   * 
   * @param x1 The starting x-coordinate
   * @param y1 The starting y-coordinate
   * @param x2 The ending x-coordinate
   * @param y2 The ending y-coordinate
   * @param color The color of the line (optional, defaults to black)
   * @return Description of the action taken
   */
  public static String createLine(int x1, int y1, int x2, int y2, String color) {
    Map<String, Object> args = new HashMap<>();
    Map<String, Object> start = new HashMap<>();
    start.put("x", x1);
    start.put("y", y1);
    args.put("start", start);
    Map<String, Object> end = new HashMap<>();
    end.put("x", x2);
    end.put("y", y2);
    args.put("end", end);
    
    // Convert color string to Map object
    Map<String, Object> colorMap = parseColorString(color);
    if (colorMap != null) {
      args.put("strokeColor", colorMap);
    }
    
    ToolExecutionResult result = toolInvoker.invokeTool("create_line", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("線を作成しました (始点: %d,%d, 終点: %d,%d)", x1, y1, x2, y2);
    } else {
      return "線の作成に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Move a shape to a new position.
   * 
   * @param shapeId The ID of the shape to move
   * @param newX The new x-coordinate
   * @param newY The new y-coordinate
   * @return Description of the action taken
   */
  public static String moveShape(String shapeId, int newX, int newY) {
    Map<String, Object> args = new HashMap<>();
    args.put("shapeId", shapeId);
    Map<String, Object> newPosition = new HashMap<>();
    newPosition.put("x", newX);
    newPosition.put("y", newY);
    args.put("newPosition", newPosition);
    
    ToolExecutionResult result = toolInvoker.invokeTool("move_shape", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("図形を移動しました (ID: %s, 新位置: %d,%d)", shapeId, newX, newY);
    } else {
      return "図形の移動に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Delete a shape from the canvas.
   * 
   * @param shapeId The ID of the shape to delete
   * @return Description of the action taken
   */
  public static String deleteShape(String shapeId) {
    Map<String, Object> args = new HashMap<>();
    args.put("shapeId", shapeId);
    
    ToolExecutionResult result = toolInvoker.invokeTool("delete_shape", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("図形を削除しました (ID: %s)", shapeId);
    } else {
      return "図形の削除に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Clear all shapes from the canvas.
   * 
   * @return Description of the action taken
   */
  public static String clearCanvas() {
    Map<String, Object> args = new HashMap<>();
    
    ToolExecutionResult result = toolInvoker.invokeTool("clear_canvas", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return "キャンバスをクリアしました";
    } else {
      return "キャンバスのクリアに失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Change the color of a shape.
   * 
   * @param shapeId The ID of the shape
   * @param newColor The new color
   * @return Description of the action taken
   */
  public static String changeColor(String shapeId, String newColor) {
    Map<String, Object> args = new HashMap<>();
    args.put("shapeId", shapeId);
    args.put("newColor", newColor);
    
    ToolExecutionResult result = toolInvoker.invokeTool("change_color", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("色を変更しました (ID: %s, 新色: %s)", shapeId, newColor);
    } else {
      return "色の変更に失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Resize a shape.
   * 
   * @param shapeId The ID of the shape to resize
   * @param newWidth The new width (for rectangles)
   * @param newHeight The new height (for rectangles)
   * @param newRadius The new radius (for circles)
   * @return Description of the action taken
   */
  public static String resizeShape(String shapeId, Integer newWidth, Integer newHeight, Integer newRadius) {
    Map<String, Object> args = new HashMap<>();
    args.put("shapeId", shapeId);
    if (newWidth != null) args.put("newWidth", newWidth);
    if (newHeight != null) args.put("newHeight", newHeight);
    if (newRadius != null) args.put("newRadius", newRadius);
    
    ToolExecutionResult result = toolInvoker.invokeTool("resize_shape", args, currentState);
    if (result.isSuccess()) {
      currentState = result.getResultState();
      return String.format("図形をリサイズしました (ID: %s)", shapeId);
    } else {
      return "図形のリサイズに失敗しました: " + result.getErrorMessage();
    }
  }
  
  /**
   * Load conversation history from the persisted file.
   * This allows the AI to remember previous conversations.
   * 
   * @return Description of the action taken
   */
  public static String loadHistory() {
    try {
      AIAgentService aiService = ServiceInitializer.getAIAgentService();
      if (aiService == null) {
        return "AIサービスが初期化されていません。履歴を読み込めません。";
      }
      boolean loaded = aiService.loadHistoryOnDemand();
      if (loaded) {
        return "履歴を読み込みました。前の会話の内容を思い出しました。";
      } else {
        return "新しい履歴はありません。すでに最新の状態です。";
      }
    } catch (Exception e) {
      System.err.println("Failed to load history: " + e.getMessage());
      return "履歴の読み込みに失敗しました: " + e.getMessage();
    }
  }
}