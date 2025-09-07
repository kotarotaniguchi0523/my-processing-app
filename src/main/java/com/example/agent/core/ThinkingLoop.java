package com.example.agent.core;

import com.example.agent.tools.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * ThinkingLoop implements a basic reasoning loop for AI agents Provides structured thinking,
 * planning, and execution phases
 */
public class ThinkingLoop {

  private static final String LIT_TYPE = "type";
  private static final String LIT_CIRCLE = "circle";
  private static final String LIT_RECTANGLE = "rectangle";
  private static final String LIT_LINE = "line";

  private final Agent agent;
  private final ThinkingConfig config;
  private final List<ThinkingStep> thoughtProcess;
  private ThinkingState currentState;
  private int currentStepIndex;

  public ThinkingLoop(Agent agent, ThinkingConfig config) {
    this.agent = Objects.requireNonNull(agent, "Agent cannot be null");
    this.config = config != null ? config : ThinkingConfig.defaultConfig();
    this.thoughtProcess = new ArrayList<>();
    this.currentState = ThinkingState.ANALYZING;
    this.currentStepIndex = 0;
  }

  public ThinkingLoop(Agent agent) {
    this(agent, null);
  }

  /** Execute the thinking loop for a given instruction */
  public ThinkingResult executeThinkingLoop(String instruction) {
    reset();

    try {
      // Phase 1: Analyze the instruction and current state
      ThinkingStep analysisStep = analyzeInstruction(instruction);
      thoughtProcess.add(analysisStep);

      if (analysisStep.outcome == StepOutcome.ERROR) {
        return ThinkingResult.error("Failed to analyze instruction: " + analysisStep.reasoning);
      }

      // Phase 2: Generate plan
      currentState = ThinkingState.PLANNING;
      ThinkingStep planningStep = generatePlan(instruction, analysisStep);
      thoughtProcess.add(planningStep);

      if (planningStep.outcome == StepOutcome.ERROR) {
        return ThinkingResult.error("Failed to generate plan: " + planningStep.reasoning);
      }

      // Phase 3: Execute plan
      currentState = ThinkingState.EXECUTING;
      List<ThinkingStep> executionSteps = executePlan(planningStep);
      thoughtProcess.addAll(executionSteps);

      // Phase 4: Reflect on results
      currentState = ThinkingState.REFLECTING;
      ThinkingStep reflectionStep = reflectOnExecution(executionSteps);
      thoughtProcess.add(reflectionStep);

      currentState = ThinkingState.COMPLETE;

      return ThinkingResult.success("Thinking loop completed successfully",
          new ArrayList<>(thoughtProcess), extractToolResults(executionSteps));

    } catch (Exception e) {
      currentState = ThinkingState.ERROR;
      ThinkingStep errorStep =
          ThinkingStep.error("Unexpected error in thinking loop: " + e.getMessage());
      thoughtProcess.add(errorStep);

      return ThinkingResult.error("Thinking loop failed: " + e.getMessage());
    }
  }

  private ThinkingStep analyzeInstruction(String instruction) {
    try {
      // Analyze current state
      Agent.StateAnalysis stateAnalysis = agent.analyzeCurrentState();

      // Determine what the instruction is asking for
      InstructionAnalysis analysis = parseInstruction(instruction);

      Map<String, Object> context = new HashMap<>();
      context.put("stateAnalysis", stateAnalysis);
      context.put("instructionAnalysis", analysis);
      context.put("hasCurrentState", agent.hasCurrentState());
      context.put("hasStateHistory", agent.hasStateHistory());

      String reasoning =
          String.format(
              "Analyzed instruction: '%s'. Intent: %s. Current state: %s shapes."
                  + " Assessment: %s",
              instruction, analysis.intent,
              agent.hasCurrentState() ? agent.getCurrentState().getShapes().size() : 0,
              stateAnalysis.getSummary());

      return ThinkingStep.success(ThinkingPhase.ANALYSIS, reasoning, context);

    } catch (Exception e) {
      return ThinkingStep.error("Analysis failed: " + e.getMessage());
    }
  }

  private ThinkingStep generatePlan(String instruction, ThinkingStep analysisStep) {
    try {
      InstructionAnalysis analysis =
          (InstructionAnalysis) analysisStep.context.get("instructionAnalysis");

      List<Agent.ToolExecution> plannedActions = new ArrayList<>();

      // Generate actions based on instruction analysis
      switch (analysis.intent) {
        case DRAW_SHAPE:
          plannedActions.addAll(planDrawingActions(analysis, instruction));
          break;
        case CLEAR_CANVAS:
          plannedActions.add(Agent.ToolExecution.of("clear_canvas", Map.of()));
          break;
        case GET_STATE:
          plannedActions.add(Agent.ToolExecution.of("get_state", Map.of()));
          break;
        case UNDO_REDO:
          plannedActions.addAll(planUndoRedoActions(analysis, instruction));
          break;
        case UNKNOWN:
        default:
          // Try to extract any drawable elements from the instruction
          plannedActions.addAll(attemptGenericDrawing(instruction));
          break;
      }

      if (plannedActions.isEmpty()) {
        return ThinkingStep.error("Could not generate any actionable plan from instruction");
      }

      Map<String, Object> planContext = new HashMap<>();
      planContext.put("plannedActions", plannedActions);
      planContext.put("actionCount", plannedActions.size());

      String reasoning = String.format("Generated plan with %d actions: %s", plannedActions.size(),
          String.join(", ", plannedActions.stream().map(a -> a.toolName).toList()));

      return ThinkingStep.success(ThinkingPhase.PLANNING, reasoning, planContext);

    } catch (Exception e) {
      return ThinkingStep.error("Planning failed: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private List<ThinkingStep> executePlan(ThinkingStep planningStep) {
    List<ThinkingStep> executionSteps = new ArrayList<>();

    try {
      List<Agent.ToolExecution> plannedActions =
          (List<Agent.ToolExecution>) planningStep.context.get("plannedActions");

      for (int i = 0; i < plannedActions.size(); i++) {
        Agent.ToolExecution execution = plannedActions.get(i);
        currentStepIndex = i;

        try {
          ToolResult result = agent.executeTool(execution.toolName, execution.parameters);

          Map<String, Object> stepContext = new HashMap<>();
          stepContext.put("toolName", execution.toolName);
          stepContext.put("parameters", execution.parameters);
          stepContext.put("result", result);

          String reasoning =
              String.format("Executed %s: %s", execution.toolName, result.getMessage());

          ThinkingStep step = result.isSuccess()
              ? ThinkingStep.success(ThinkingPhase.EXECUTION, reasoning, stepContext)
              : ThinkingStep.error("Tool execution failed: " + result.getMessage());

          executionSteps.add(step);

          // Stop execution if critical tool failed
          if (execution.critical && result.isError()) {
            break;
          }

        } catch (Exception e) {
          Map<String, Object> errorContext = new HashMap<>();
          errorContext.put("toolName", execution.toolName);
          errorContext.put("parameters", execution.parameters);
          errorContext.put("error", e.getMessage());

          ThinkingStep errorStep =
              ThinkingStep.error("Tool execution exception: " + e.getMessage());
          errorStep.context.putAll(errorContext);
          executionSteps.add(errorStep);

          if (execution.critical) {
            break;
          }
        }
      }

    } catch (Exception e) {
      ThinkingStep errorStep = ThinkingStep.error("Execution phase failed: " + e.getMessage());
      executionSteps.add(errorStep);
    }

    return executionSteps;
  }

  private ThinkingStep reflectOnExecution(List<ThinkingStep> executionSteps) {
    try {
      int successfulSteps = (int) executionSteps.stream()
          .mapToInt(step -> step.outcome == StepOutcome.SUCCESS ? 1 : 0).sum();

      int totalSteps = executionSteps.size();
      boolean overallSuccess = successfulSteps > 0 && successfulSteps == totalSteps;

      Map<String, Object> reflectionContext = new HashMap<>();
      reflectionContext.put("totalSteps", totalSteps);
      reflectionContext.put("successfulSteps", successfulSteps);
      reflectionContext.put("overallSuccess", overallSuccess);

      String reasoning =
          String.format("Execution completed: %d/%d steps successful. Overall result: %s",
              successfulSteps, totalSteps, overallSuccess ? "SUCCESS" : "PARTIAL/FAILURE");

      return overallSuccess
          ? ThinkingStep.success(ThinkingPhase.REFLECTION, reasoning, reflectionContext)
          : ThinkingStep.partial(ThinkingPhase.REFLECTION, reasoning, reflectionContext);

    } catch (Exception e) {
      return ThinkingStep.error("Reflection failed: " + e.getMessage());
    }
  }

  // Helper methods for plan generation

  private List<Agent.ToolExecution> planDrawingActions(InstructionAnalysis analysis,
      String instruction) {
    List<Agent.ToolExecution> actions = new ArrayList<>();

    // This is a simplified implementation - in a real system, this would use
    // natural language processing to extract shape parameters
    Map<String, Object> shapeParams = extractShapeParameters(instruction);

    if (!shapeParams.isEmpty()) {
      actions.add(Agent.ToolExecution.of("draw_shape", shapeParams));
    }

    return actions;
  }

  private List<Agent.ToolExecution> planUndoRedoActions(InstructionAnalysis analysis,
      String instruction) {
    List<Agent.ToolExecution> actions = new ArrayList<>();

    if (instruction.toLowerCase(Locale.ROOT).contains("undo")) {
      actions.add(Agent.ToolExecution.of("undo_redo", Map.of("operation", "undo")));
    } else if (instruction.toLowerCase(Locale.ROOT).contains("redo")) {
      actions.add(Agent.ToolExecution.of("undo_redo", Map.of("operation", "redo")));
    }

    return actions;
  }

  private List<Agent.ToolExecution> attemptGenericDrawing(String instruction) {
    List<Agent.ToolExecution> actions = new ArrayList<>();

    // Simple heuristics for common drawing instructions
    String lower = instruction.toLowerCase(Locale.ROOT);
    if (lower.contains(LIT_CIRCLE) || lower.contains("round")) {
      Map<String, Object> params = Map.of(LIT_TYPE, LIT_CIRCLE, "x", 100, "y", 100, "radius", 50);
      actions.add(Agent.ToolExecution.of("draw_shape", params));
    } else if (lower.contains(LIT_RECTANGLE) || lower.contains("square") || lower.contains("box")) {
      Map<String, Object> params =
          Map.of(LIT_TYPE, LIT_RECTANGLE, "x", 100, "y", 100, "width", 100, "height", 100);
      actions.add(Agent.ToolExecution.of("draw_shape", params));
    }

    return actions;
  }

  private Map<String, Object> extractShapeParameters(String instruction) {
    // Simplified parameter extraction - in practice, this would be much more sophisticated
    Map<String, Object> params = new HashMap<>();

    String lower = instruction.toLowerCase(Locale.ROOT);

    if (lower.contains(LIT_CIRCLE)) {
      params.put(LIT_TYPE, LIT_CIRCLE);
      params.put("x", 200);
      params.put("y", 200);
      params.put("radius", 50);
    } else if (lower.contains(LIT_RECTANGLE)) {
      params.put(LIT_TYPE, LIT_RECTANGLE);
      params.put("x", 150);
      params.put("y", 150);
      params.put("width", 100);
      params.put("height", 80);
    } else if (lower.contains(LIT_LINE)) {
      params.put(LIT_TYPE, LIT_LINE);
      params.put("x", 50);
      params.put("y", 50);
      params.put("x2", 200);
      params.put("y2", 200);
    }

    return params;
  }

  private InstructionAnalysis parseInstruction(String instruction) {
    String lower = instruction.toLowerCase(Locale.ROOT);

    if (lower.contains("draw") || lower.contains("create") || lower.contains("add")
        || lower.contains("circle") || lower.contains("rectangle") || lower.contains("line")) {
      return new InstructionAnalysis(InstructionIntent.DRAW_SHAPE, instruction);
    } else if (lower.contains("clear") || lower.contains("clean") || lower.contains("empty")) {
      return new InstructionAnalysis(InstructionIntent.CLEAR_CANVAS, instruction);
    } else if (lower.contains("show") || lower.contains("get") || lower.contains("state")
        || lower.contains("what") || lower.contains("current")) {
      return new InstructionAnalysis(InstructionIntent.GET_STATE, instruction);
    } else if (lower.contains("undo") || lower.contains("redo") || lower.contains("back")) {
      return new InstructionAnalysis(InstructionIntent.UNDO_REDO, instruction);
    } else {
      return new InstructionAnalysis(InstructionIntent.UNKNOWN, instruction);
    }
  }

  private List<ToolResult> extractToolResults(List<ThinkingStep> executionSteps) {
    List<ToolResult> results = new ArrayList<>();
    for (ThinkingStep step : executionSteps) {
      Object result = step.context.get("result");
      if (result instanceof ToolResult) {
        results.add((ToolResult) result);
      }
    }
    return results;
  }

  private void reset() {
    thoughtProcess.clear();
    currentState = ThinkingState.ANALYZING;
    currentStepIndex = 0;
  }

  // Getters

  public ThinkingState getCurrentState() {
    return currentState;
  }

  public List<ThinkingStep> getThoughtProcess() {
    return new ArrayList<>(thoughtProcess);
  }

  public int getCurrentStepIndex() {
    return currentStepIndex;
  }

  public ThinkingConfig getConfig() {
    return config;
  }

  // Enums and inner classes

  public enum ThinkingState {
    ANALYZING, PLANNING, EXECUTING, REFLECTING, COMPLETE, ERROR
  }

  public enum ThinkingPhase {
    ANALYSIS, PLANNING, EXECUTION, REFLECTION
  }

  public enum StepOutcome {
    SUCCESS, PARTIAL, ERROR
  }

  public enum InstructionIntent {
    DRAW_SHAPE, CLEAR_CANVAS, GET_STATE, UNDO_REDO, UNKNOWN
  }

  public static class ThinkingConfig {
    public final int maxExecutionSteps;
    public final boolean stopOnError;
    public final boolean detailedLogging;

    public ThinkingConfig(int maxExecutionSteps, boolean stopOnError, boolean detailedLogging) {
      this.maxExecutionSteps = maxExecutionSteps;
      this.stopOnError = stopOnError;
      this.detailedLogging = detailedLogging;
    }

    public static ThinkingConfig defaultConfig() {
      return new ThinkingConfig(10, false, true);
    }
  }

  public static class ThinkingStep {
    public final ThinkingPhase phase;
    public final String reasoning;
    public final Map<String, Object> context;
    public final StepOutcome outcome;
    public final long timestamp;

    public ThinkingStep(ThinkingPhase phase, String reasoning, Map<String, Object> context,
        StepOutcome outcome) {
      this.phase = phase;
      this.reasoning = reasoning;
      this.context = context != null ? new HashMap<>(context) : new HashMap<>();
      this.outcome = outcome;
      this.timestamp = System.currentTimeMillis();
    }

    public static ThinkingStep success(ThinkingPhase phase, String reasoning,
        Map<String, Object> context) {
      return new ThinkingStep(phase, reasoning, context, StepOutcome.SUCCESS);
    }

    public static ThinkingStep partial(ThinkingPhase phase, String reasoning,
        Map<String, Object> context) {
      return new ThinkingStep(phase, reasoning, context, StepOutcome.PARTIAL);
    }

    public static ThinkingStep error(String reasoning) {
      return new ThinkingStep(ThinkingPhase.ANALYSIS, reasoning, null, StepOutcome.ERROR);
    }

    @Override
    public String toString() {
      return String.format("ThinkingStep(%s:%s - %s)", phase, outcome, reasoning);
    }
  }

  public static class InstructionAnalysis {
    public final InstructionIntent intent;
    public final String originalInstruction;

    public InstructionAnalysis(InstructionIntent intent, String originalInstruction) {
      this.intent = intent;
      this.originalInstruction = originalInstruction;
    }
  }

  public static class ThinkingResult {
    private final boolean success;
    private final String message;
    private final List<ThinkingStep> thoughtProcess;
    private final List<ToolResult> toolResults;

    public ThinkingResult(boolean success, String message, List<ThinkingStep> thoughtProcess,
        List<ToolResult> toolResults) {
      this.success = success;
      this.message = message;
      this.thoughtProcess =
          thoughtProcess != null ? new ArrayList<>(thoughtProcess) : new ArrayList<>();
      this.toolResults = toolResults != null ? new ArrayList<>(toolResults) : new ArrayList<>();
    }

    public static ThinkingResult success(String message, List<ThinkingStep> thoughtProcess,
        List<ToolResult> toolResults) {
      return new ThinkingResult(true, message, thoughtProcess, toolResults);
    }

    public static ThinkingResult error(String message) {
      return new ThinkingResult(false, message, null, null);
    }

    // Getters
    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public List<ThinkingStep> getThoughtProcess() {
      return new ArrayList<>(thoughtProcess);
    }

    public List<ToolResult> getToolResults() {
      return new ArrayList<>(toolResults);
    }

    @Override
    public String toString() {
      return String.format("ThinkingResult(success=%s, steps=%d, tools=%d)", success,
          thoughtProcess.size(), toolResults.size());
    }
  }
}
