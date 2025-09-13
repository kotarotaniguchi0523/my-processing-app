package com.example.application.services;

import com.example.domain.models.*;
import com.example.infrastructure.ai.DrawingFunctions;
import com.google.genai.types.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Coordinates tool selection, execution, and result interpretation. Handles drawing function
 * management and automatic function calling integration.
 */
public class ToolCoordinator {

  public ToolCoordinator() {
    // Initialize DrawingFunctions
    DrawingFunctions.initialize();
  }

  /**
   * Update DrawingFunctions with current state before tool execution.
   */
  public void updateCurrentState(DrawingState state) {
    DrawingFunctions.setCurrentState(state);
  }

  /**
   * Get current state from DrawingFunctions after tool execution.
   */
  public DrawingState getCurrentState() {
    return DrawingFunctions.getCurrentState();
  }

  /**
   * Get drawing function methods for automatic function calling.
   */
  public List<Method> getDrawingMethods() {
    List<Method> methods = new ArrayList<>();

    try {
      Class<?> clazz = DrawingFunctions.class;

      // Add createRectangle method
      methods.add(clazz.getMethod("createRectangle", int.class, int.class, int.class, int.class,
          String.class));

      // Add createCircle method
      methods.add(clazz.getMethod("createCircle", int.class, int.class, int.class, String.class));

      // Add createEllipse method (ENABLE AFC TO CALL ELLIPSE)
      methods.add(clazz.getMethod("createEllipse", int.class, int.class, int.class, int.class,
          String.class));

      // Add createTriangle method
      methods.add(clazz.getMethod("createTriangle", int.class, int.class, int.class, int.class,
          int.class, int.class, String.class));

      // Add createLine method
      methods.add(
          clazz.getMethod("createLine", int.class, int.class, int.class, int.class, String.class));

      // Add clearCanvas method
      methods.add(clazz.getMethod("clearCanvas"));

      // Add moveShape method
      methods.add(clazz.getMethod("moveShape", String.class, int.class, int.class));

      // Add deleteShape method
      methods.add(clazz.getMethod("deleteShape", String.class));

      // Add changeColor method
      methods.add(clazz.getMethod("changeColor", String.class, String.class));

      // Add resizeShape method
      methods.add(clazz.getMethod("resizeShape", String.class, Integer.class, Integer.class,
          Integer.class));

      // Add loadHistory method
      methods.add(clazz.getMethod("loadHistory"));

    } catch (NoSuchMethodException e) {
      System.err.println("Failed to load some drawing methods: " + e.getMessage());
    }

    return methods;
  }

  /**
   * Analyze state changes and infer tool calls that occurred.
   */
  public List<AIAgentService.ToolCall> inferToolCallsFromStateChange(DrawingState previousState,
      DrawingState newState) {

    List<AIAgentService.ToolCall> toolCalls = new ArrayList<>();

    if (newState.equals(previousState)) {
      return toolCalls; // No changes
    }

    int shapesBefore = previousState.getShapes().size();
    int shapesAfter = newState.getShapes().size();

    if (shapesAfter > shapesBefore) {
      // New shapes were added
      for (int i = shapesBefore; i < shapesAfter; i++) {
        Shape shape = newState.getShapes().get(i);
        String toolName = getToolNameFromShape(shape);
        Map<String, Object> args = inferArgumentsFromShape(shape);
        toolCalls.add(new AIAgentService.ToolCall(toolName, args, "Success"));
      }
    } else if (shapesAfter == 0 && shapesBefore > 0) {
      // Canvas was cleared
      toolCalls.add(new AIAgentService.ToolCall("clearCanvas", new HashMap<>(), "Success"));
    }
    
    // Future enhancement: Add sophisticated detection for shape modifications, deletions, 
    // and other operations by comparing shape properties, positions, and attributes.
    // This would enable more granular tool call inference for better user feedback.

    return toolCalls;
  }

  /**
   * Get tool name from shape type.
   */
  private String getToolNameFromShape(Shape shape) {
    if (shape instanceof Circle) {
      return "createCircle";
    } else if (shape instanceof Rectangle) {
      return "createRectangle";
    } else if (shape instanceof Line) {
      return "createLine";
    } else if (shape instanceof Ellipse) {
      return "createEllipse";
    } else if (shape instanceof Triangle) {
      return "createTriangle";
    } else if (shape instanceof Text) {
      return "createText";
    }
    return "unknown";
  }

  /**
   * Infer arguments from shape properties.
   */
  private Map<String, Object> inferArgumentsFromShape(Shape shape) {
    Map<String, Object> args = new HashMap<>();

    if (shape instanceof Circle) {
      Circle circle = (Circle) shape;
      args.put("x", (int) circle.getPosition().getX());
      args.put("y", (int) circle.getPosition().getY());
      args.put("radius", (int) circle.getRadius());
      if (circle.getStyle() != null && circle.getStyle().getFillColor() != null) {
        args.put("color", circle.getStyle().getFillColor().toHex());
      }
    } else if (shape instanceof Rectangle) {
      Rectangle rect = (Rectangle) shape;
      args.put("x", (int) rect.getPosition().getX());
      args.put("y", (int) rect.getPosition().getY());
      args.put("width", (int) rect.getWidth());
      args.put("height", (int) rect.getHeight());
      if (rect.getStyle() != null && rect.getStyle().getFillColor() != null) {
        args.put("color", rect.getStyle().getFillColor().toHex());
      }
    } else if (shape instanceof Ellipse) {
      Ellipse e = (Ellipse) shape;
      args.put("x", (int) e.getPosition().getX());
      args.put("y", (int) e.getPosition().getY());
      args.put("radiusX", (int) e.getRadiusX());
      args.put("radiusY", (int) e.getRadiusY());
      if (e.getStyle() != null && e.getStyle().getFillColor() != null) {
        args.put("color", e.getStyle().getFillColor().toHex());
      }
    } else if (shape instanceof Triangle) {
      Triangle tri = (Triangle) shape;
      args.put("x1", (int) tri.getP1().getX());
      args.put("y1", (int) tri.getP1().getY());
      args.put("x2", (int) tri.getP2().getX());
      args.put("y2", (int) tri.getP2().getY());
      args.put("x3", (int) tri.getP3().getX());
      args.put("y3", (int) tri.getP3().getY());
      if (tri.getStyle() != null && tri.getStyle().getFillColor() != null) {
        args.put("color", tri.getStyle().getFillColor().toHex());
      }
    } else if (shape instanceof Line) {
      Line line = (Line) shape;
      args.put("x1", (int) line.getStartPosition().getX());
      args.put("y1", (int) line.getStartPosition().getY());
      args.put("x2", (int) line.getEndPosition().getX());
      args.put("y2", (int) line.getEndPosition().getY());
      if (line.getStyle() != null && line.getStyle().getStrokeColor() != null) {
        args.put("color", line.getStyle().getStrokeColor().toHex());
      }
    } else if (shape instanceof Text) {
      Text text = (Text) shape;
      args.put("x", (int) text.getPosition().getX());
      args.put("y", (int) text.getPosition().getY());
      args.put("text", text.getContent());
      args.put("size", text.getFontSize());
    }

    return args;
  }

  /**
   * Configure GenerateContentConfig for automatic function calling.
   */
  public GenerateContentConfig createToolConfig() {
    List<Method> methods = getDrawingMethods();
    return GenerateContentConfig.builder()
        .tools(Tool.builder().functions(methods.toArray(new Method[0]))).build();
  }
}
