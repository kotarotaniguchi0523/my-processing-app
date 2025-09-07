package com.example.agent;

import com.example.agent.core.Agent;
import com.example.agent.core.AgentContext;
import com.example.agent.manager.ToolManager;
import com.example.agent.tools.ToolResult;
import com.example.agent.tools.impl.ClearCanvasTool;
import com.example.agent.tools.impl.DrawShapeTool;
import com.example.agent.tools.impl.GetStateTool;
import com.example.agent.tools.impl.UndoRedoTool;
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

    /** Create a standard tool manager with all basic tools registered */
    public static ToolManager createStandardToolManager() {
        ToolManager toolManager = new ToolManager();

        // Register all standard tools
        toolManager.registerTools(
                new DrawShapeTool(), new ClearCanvasTool(), new GetStateTool(), new UndoRedoTool());

        return toolManager;
    }

    /** Create a basic drawing agent with standard tools */
    public static DrawingAgent createDrawingAgent() {
        ToolManager toolManager = createStandardToolManager();
        return new DrawingAgent("DrawingAgent", "AI agent for drawing operations", toolManager);
    }

    /** Create an agent context from current application state */
    public static AgentContext createContextFromState(
            DrawingState currentState, StateHistory stateHistory) {
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
    public static AgentSystem createAgentSystem(
            DrawingState currentState, StateHistory stateHistory) {
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
            return String.format(
                    "AgentSystem(agent=%s, hasContext=%s)", agent.getName(), context != null);
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
                } else if (lower.contains("show")
                        || lower.contains("get")
                        || lower.contains("state")) {
                    return getState();
                } else if (lower.contains("undo")) {
                    return performUndo();
                } else {
                    return AgentResponse.error(
                            "I don't understand that instruction. Try 'draw circle', 'draw"
                                    + " rectangle', 'clear canvas', or 'show state'.");
                }

            } catch (Exception e) {
                return AgentResponse.error("Error processing instruction: " + e.getMessage());
            }
        }

        private AgentResponse drawCircle(String instruction) {
            try {
                Map<String, Object> params =
                        Map.of(
                                "type",
                                "circle",
                                "x",
                                200,
                                "y",
                                200,
                                "radius",
                                50,
                                "fillColor",
                                getStringConfiguration("defaultColor", "#000000"));

                ToolResult result = executeTool("draw_shape", params);

                if (result.isSuccess()) {
                    // Update context with new state
                    if (result.hasNewState()) {
                        context = context.withNewCurrentState(result.getNewState());
                    }
                    return AgentResponse.success(result.getMessage(), List.of(result));
                } else {
                    return AgentResponse.error(
                            "Failed to draw circle: " + result.getMessage(), List.of(result));
                }

            } catch (Exception e) {
                return AgentResponse.error("Error drawing circle: " + e.getMessage());
            }
        }

        private AgentResponse drawRectangle(String instruction) {
            try {
                Map<String, Object> params =
                        Map.of(
                                "type",
                                "rectangle",
                                "x",
                                150,
                                "y",
                                150,
                                "width",
                                100,
                                "height",
                                80,
                                "fillColor",
                                getStringConfiguration("defaultColor", "#000000"));

                ToolResult result = executeTool("draw_shape", params);

                if (result.isSuccess()) {
                    if (result.hasNewState()) {
                        context = context.withNewCurrentState(result.getNewState());
                    }
                    return AgentResponse.success(result.getMessage(), List.of(result));
                } else {
                    return AgentResponse.error(
                            "Failed to draw rectangle: " + result.getMessage(), List.of(result));
                }

            } catch (Exception e) {
                return AgentResponse.error("Error drawing rectangle: " + e.getMessage());
            }
        }

        private AgentResponse clearCanvas() {
            try {
                Map<String, Object> params = Map.of("preserveSettings", true);
                ToolResult result = executeTool("clear_canvas", params);

                if (result.isSuccess()) {
                    if (result.hasNewState()) {
                        context = context.withNewCurrentState(result.getNewState());
                    }
                    return AgentResponse.success(result.getMessage(), List.of(result));
                } else {
                    return AgentResponse.error(
                            "Failed to clear canvas: " + result.getMessage(), List.of(result));
                }

            } catch (Exception e) {
                return AgentResponse.error("Error clearing canvas: " + e.getMessage());
            }
        }

        private AgentResponse getState() {
            try {
                Map<String, Object> params =
                        Map.of("includeShapes", true, "includeSettings", true, "summary", false);

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
                    return AgentResponse.error(
                            "Failed to undo: " + result.getMessage(), List.of(result));
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

            Map<String, Object> metrics =
                    Map.of(
                            "shapeCount",
                            shapeCount,
                            "canvasWidth",
                            state.getIntSetting("width", 800),
                            "canvasHeight",
                            state.getIntSetting("height", 600));

            String summary =
                    shapeCount == 0
                            ? "Empty canvas ready for drawing"
                            : "Canvas with " + shapeCount + " shapes";

            return new StateAnalysis(summary, observations, suggestions, metrics);
        }
    }
}
