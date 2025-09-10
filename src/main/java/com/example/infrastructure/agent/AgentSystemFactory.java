package com.example.infrastructure.agent;

import com.example.infrastructure.agent.core.Agent;
import com.example.domain.agent.AgentContext;
import com.example.infrastructure.agent.adapter.IAgentToolAdapter;
import com.example.application.agent.ToolManager;
import com.example.domain.agent.tools.ToolResult;
import com.example.domain.tools.IToolRegistry;
import com.example.application.tools.ToolRegistry;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import com.example.domain.session.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Factory class for creating and configuring the agent system Provides convenient methods to set up
 * agents with standard tool configurations
 */
public final class AgentSystemFactory {

  private AgentSystemFactory() {
    // Utility class
  }

  /** Create a standard tool manager with all basic tools registered (domain registry unified) */
  public static ToolManager createStandardToolManager() {
    ToolManager toolManager = new ToolManager();

    // Register tools from domain registry via adapter to avoid abstraction duplication
    IToolRegistry registry = ToolRegistry.getInstance();
    toolManager.registerFromRegistry(registry, true);

    // Register bridge tools for state querying and undo/redo
    toolManager.registerTool(
        new IAgentToolAdapter(new com.example.infrastructure.agent.tools.GetStateTool()));
    toolManager.registerTool(
        new IAgentToolAdapter(new com.example.infrastructure.agent.tools.UndoRedoTool()));

    return toolManager;
  }

  /** Create a basic drawing agent with standard tools */
  public static DrawingAgent createDrawingAgent() {
    ToolManager toolManager = createStandardToolManager();
    return new DrawingAgent("DrawingAgent", "AI agent for drawing operations", toolManager);
  }

  /** Create an agent context from current application state */
  public static AgentContext createContextFromState(DrawingState currentState,
      StateHistory stateHistory) {
    return AgentContext.builder().currentState(currentState).stateHistory(stateHistory).build();
  }

  /** Create an agent context from a session */
  public static AgentContext createContextFromSession(Session session) {
    return AgentContext.fromSession(session);
  }

  /** Create an agent context with minimal state */
  public static AgentContext createMinimalContext() {
    DrawingState defaultState = DrawingState.defaultState();
    StateHistory history = new StateHistory();
    return createContextFromState(defaultState, history);
  }

  /** Configure an agent with standard settings */
  public static void configureStandardAgent(Agent agent) {
    // Set standard configurations
    agent.setConfiguration("maxToolExecutions", 10);
    agent.setConfiguration("requireApprovalForDrawing", true);
    agent.setConfiguration("enableStateHistory", true);
    agent.setConfiguration("autoSaveState", true);
    agent.setConfiguration("verboseLogging", false);
  }

  /** Create a complete agent system ready for use */
  public static AgentSystem createAgentSystem() {
    DrawingAgent agent = createDrawingAgent();
    configureStandardAgent(agent);

    AgentContext context = createMinimalContext();
    agent.updateContext(context);

    return new AgentSystem(agent, context);
  }

  /** Create an agent system with existing state */
  public static AgentSystem createAgentSystem(DrawingState currentState,
      StateHistory stateHistory) {
    DrawingAgent agent = createDrawingAgent();
    configureStandardAgent(agent);

    AgentContext context = createContextFromState(currentState, stateHistory);
    agent.updateContext(context);

    return new AgentSystem(agent, context);
  }

  /** Container class for agent system components */
  public static class AgentSystem {
    private final DrawingAgent agent;
    private AgentContext context;

    public AgentSystem(DrawingAgent agent, AgentContext context) {
      this.agent = agent;
      this.context = context;
    }

    public DrawingAgent getAgent() {
      return agent;
    }

    public AgentContext getContext() {
      return context;
    }

    public void updateContext(AgentContext newContext) {
      this.context = newContext;
      agent.updateContext(newContext);
    }

    public Agent.AgentResponse processInstruction(String instruction) {
      return agent.processInstruction(instruction);
    }

    public ToolManager getToolManager() {
      return agent.getToolManager();
    }

    @Override
    public String toString() {
      return String.format("AgentSystem(agent=%s, hasContext=%s)", agent.getName(),
          context != null);
    }
  }

  /** Basic implementation of a drawing agent */
  public static class DrawingAgent extends Agent {

    public DrawingAgent(String name, String description, ToolManager toolManager) {
      super(name, description, toolManager);
      initialize();
    }

    @Override
    protected void initialize() {
      // Set default configuration
      setConfiguration("preferredShapeSize", 50);
      setConfiguration("defaultColor", "#000000");
      setConfiguration("enableUndo", true);
    }

    @Override
    public AgentResponse processInstruction(String instruction) {
      if (instruction == null || instruction.isBlank()) {
        return AgentResponse.error("Instruction cannot be empty");
      }

      try {
        // Simple instruction processing - in a real system this would be much more
        // sophisticated
        String lower = instruction.toLowerCase(Locale.ROOT).trim();

        if (lower.contains("draw") && lower.contains("circle")) {
          return drawCircle(instruction);
        } else if (lower.contains("draw") && lower.contains("rectangle")) {
          return drawRectangle(instruction);
        } else if (lower.contains("clear")) {
          return clearCanvas();
        } else if (lower.contains("show") || lower.contains("get") || lower.contains("state")) {
          return getState();
        } else if (lower.contains("undo")) {
          return performUndo();
        } else {
          return AgentResponse.error("I don't understand that instruction. Try 'draw circle', 'draw"
              + " rectangle', 'clear canvas', or 'show state'.");
        }

      } catch (Exception e) {
        return AgentResponse.error("Error processing instruction: " + e.getMessage());
      }
    }

    private AgentResponse drawCircle(String instruction) {
      try {
        // Align to domain tool 'createShape' schema
        Map<String, Object> position = new java.util.HashMap<>();
        position.put("x", 200);
        position.put("y", 200);

        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("radius", 50);

        Map<String, Object> style = new java.util.HashMap<>();
        Map<String, Object> fillColor = new java.util.HashMap<>();
        fillColor.put("hex", getStringConfiguration("defaultColor", "#000000"));
        style.put("fillColor", fillColor);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("type", "circle");
        params.put("position", position);
        params.put("properties", properties);
        params.put("style", style);

        ToolResult result = executeTool("createShape", params);

        if (result.isSuccess()) {
          // Update context with new state
          if (result.hasNewState()) {
            context = context.withNewCurrentState(result.getNewState());
          }
          return AgentResponse.success(result.getMessage(), List.of(result));
        } else {
          return AgentResponse.error("Failed to draw circle: " + result.getMessage(),
              List.of(result));
        }

      } catch (Exception e) {
        return AgentResponse.error("Error drawing circle: " + e.getMessage());
      }
    }

    private AgentResponse drawRectangle(String instruction) {
      try {
        Map<String, Object> position = new java.util.HashMap<>();
        position.put("x", 150);
        position.put("y", 150);

        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("width", 100);
        properties.put("height", 80);

        Map<String, Object> style = new java.util.HashMap<>();
        Map<String, Object> fillColor = new java.util.HashMap<>();
        fillColor.put("hex", getStringConfiguration("defaultColor", "#000000"));
        style.put("fillColor", fillColor);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("type", "rectangle");
        params.put("position", position);
        params.put("properties", properties);
        params.put("style", style);

        ToolResult result = executeTool("createShape", params);

        if (result.isSuccess()) {
          if (result.hasNewState()) {
            context = context.withNewCurrentState(result.getNewState());
          }
          return AgentResponse.success(result.getMessage(), List.of(result));
        } else {
          return AgentResponse.error("Failed to draw rectangle: " + result.getMessage(),
              List.of(result));
        }

      } catch (Exception e) {
        return AgentResponse.error("Error drawing rectangle: " + e.getMessage());
      }
    }

    private AgentResponse clearCanvas() {
      try {
        // Domain tool name: clearCanvas, no parameters required
        Map<String, Object> params = new java.util.HashMap<>();
        ToolResult result = executeTool("clearCanvas", params);

        if (result.isSuccess()) {
          if (result.hasNewState()) {
            context = context.withNewCurrentState(result.getNewState());
          }
          return AgentResponse.success(result.getMessage(), List.of(result));
        } else {
          return AgentResponse.error("Failed to clear canvas: " + result.getMessage(),
              List.of(result));
        }

      } catch (Exception e) {
        return AgentResponse.error("Error clearing canvas: " + e.getMessage());
      }
    }

    private AgentResponse getState() {
      try {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("includeShapes", true);
        params.put("includeSettings", true);
        params.put("summary", false);
        params.put("includeYaml", false);
        ToolResult result = executeTool("get_state", params);
        return AgentResponse.success(result.getMessage(), List.of(result));

      } catch (Exception e) {
        return AgentResponse.error("Error getting state: " + e.getMessage());
      }
    }

    private AgentResponse performUndo() {
      try {
        Map<String, Object> params = Map.of("operation", "undo");
        ToolResult result = executeTool("undo_redo", params);

        if (result.isSuccess()) {
          if (result.hasNewState()) {
            context = context.withNewCurrentState(result.getNewState());
          }
          return AgentResponse.success(result.getMessage(), List.of(result));
        } else {
          return AgentResponse.error("Failed to undo: " + result.getMessage(), List.of(result));
        }

      } catch (Exception e) {
        return AgentResponse.error("Error performing undo: " + e.getMessage());
      }
    }

    @Override
    public StateAnalysis analyzeCurrentState() {
      if (!hasCurrentState()) {
        return StateAnalysis.create("No current drawing state available");
      }

      DrawingState state = getCurrentState();
      int shapeCount = state.getShapes().size();

      List<String> observations = new ArrayList<>();
      List<String> suggestions = new ArrayList<>();

      if (shapeCount == 0) {
        observations.add("Canvas is empty");
        suggestions.add("Consider drawing some shapes");
      } else {
        observations.add("Canvas contains " + shapeCount + " shapes");
        if (shapeCount > 10) {
          suggestions.add("Canvas is getting crowded, consider clearing or organizing");
        }
      }

      Map<String, Object> metrics = Map.of("shapeCount", shapeCount, "canvasWidth",
          state.getIntSetting("width", 800), "canvasHeight", state.getIntSetting("height", 600));

      String summary = shapeCount == 0 ? "Empty canvas ready for drawing"
          : "Canvas with " + shapeCount + " shapes";

      return new StateAnalysis(summary, observations, suggestions, metrics);
    }
  }
}
