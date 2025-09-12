package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Line;
import com.example.domain.models.Text;
import com.example.infrastructure.ai.GeminiClient;
import com.example.infrastructure.services.ToolInvoker;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.tools.ToolRegistry;
import com.example.infrastructure.ai.DrawingFunctions;
import com.example.domain.events.*;
import com.google.genai.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// (Using default safe Yaml loader; avoid explicit constructor for compatibility)
// (Logging removed for minimal initial implementation)

/**
 * AIAgentService orchestrates chat -> LLM -> (optional) tool execution -> new state + reply.
 * Minimum viable implementation: single-turn text prompt, optional single tool call JSON.
 */
public class AIAgentService {
  private final GeminiClient geminiClient;
  private final ToolInvoker toolInvoker;
  private DrawingState currentState;

  // --- Event System ---
  private final List<IAIProcessingListener> listeners = new CopyOnWriteArrayList<>();
  private final String sessionId = UUID.randomUUID().toString();
  private final AtomicLong sequenceCounter = new AtomicLong(0);

  // --- Conversation History (Step 1) ---
  private static final String SYSTEM_PROMPT = String.join("\n", "You are an AI drawing assistant.",
      "User interacts with a vector drawing canvas (shapes: circle, rectangle, line, text).",
      "Keep answers concise. If a drawing action is requested, you may later emit a tool call (not yet implemented).",
      "Ask clarifying questions when parameters are ambiguous.");

  private static final int MAX_HISTORY = 5; // Reduced to minimize prompt size
  private static final Path HISTORY_FILE = Path.of(".agent_history.json");
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private long historyPersistCounter = 0; // persist every N changes
  private static final int PERSIST_INTERVAL = 2;
  private final List<ChatMessage> history = new ArrayList<>();
  // Flags to control history behavior
  private final boolean loadHistoryOnStartup;
  private final boolean persistEnabled;

  /**
   * Default constructor keeps previous behavior: load history on startup and enable persistence.
   */
  public AIAgentService(DrawingState initialState) {
    this(initialState, true, true);
  }

  /**
   * New constructor: control whether to load history on startup and whether to persist history.
   *
   * @param initialState initial drawing state (nullable)
   * @param loadHistoryOnStartup if true, attempt to load `.agent_history.json` on construction
   * @param persistHistory if true, persist history to `.agent_history.json` at intervals
   */
  public AIAgentService(DrawingState initialState, boolean loadHistoryOnStartup,
      boolean persistHistory) {
    this.geminiClient = GeminiClient.getInstance();
    // Initialize DrawingFunctions with UseCaseFacade
    DrawingFunctions.initialize();
    // Get the initialized ToolInvoker from ToolRegistry
    this.toolInvoker = new ToolInvoker(ToolRegistry.getInstance());
    this.currentState = initialState != null ? initialState : DrawingState.defaultState();
    // Seed system message
    history.add(ChatMessage.system(SYSTEM_PROMPT));
    this.loadHistoryOnStartup = loadHistoryOnStartup;
    this.persistEnabled = persistHistory;
    if (this.loadHistoryOnStartup) {
      loadHistoryIfExists();
    }
    // Create initial history file if persistence is enabled and file doesn't exist
    if (this.persistEnabled && !Files.exists(HISTORY_FILE)) {
      persistHistoryImmediately();
    }
  }

  public DrawingState getCurrentState() {
    return currentState;
  }

  // --- Event Listener Management ---

  /**
   * Add a listener for AI processing events.
   */
  public void addProcessingListener(IAIProcessingListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener for AI processing events.
   */
  public void removeProcessingListener(IAIProcessingListener listener) {
    listeners.remove(listener);
  }

  // --- Event Firing Methods ---

  private void fireProcessingStarted(String userPrompt, String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ProcessingStartedEvent event = new ProcessingStartedEvent(meta, userPrompt);
    listeners.forEach(l -> {
      try {
        l.onProcessingStarted(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void firePromptAnalyzing(String analysis, String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    PromptAnalyzingEvent event = new PromptAnalyzingEvent(meta, analysis);
    listeners.forEach(l -> {
      try {
        l.onPromptAnalyzing(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void fireToolSelecting(String toolName, String reason, Map<String, Object> parameters,
      String requestId, UUID toolCallId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ToolSelectingEvent event =
        new ToolSelectingEvent(meta, toolName, reason, parameters, toolCallId);
    listeners.forEach(l -> {
      try {
        l.onToolSelecting(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void fireToolExecuting(String toolName, ToolStatus status, String requestId,
      UUID toolCallId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ToolExecutingEvent event = new ToolExecutingEvent(meta, toolName, status, toolCallId);
    listeners.forEach(l -> {
      try {
        l.onToolExecuting(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void fireProcessingCompleted(String result, DrawingState newState, String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    String stateVersion = "v-" + System.currentTimeMillis();
    UUID toolCallId = null; // Non-tool response
    ProcessingCompletedEvent event =
        new ProcessingCompletedEvent(meta, result, stateVersion, toolCallId);
    listeners.forEach(l -> {
      try {
        l.onProcessingCompleted(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  private void fireProcessingFailed(String errorMessage, FailurePoint failurePoint,
      String requestId) {
    EventMeta meta = new EventMeta(sessionId, requestId, sequenceCounter.incrementAndGet());
    ProcessingFailedEvent event = new ProcessingFailedEvent(meta, errorMessage, failurePoint);
    listeners.forEach(l -> {
      try {
        l.onProcessingFailed(event);
      } catch (Exception e) {
        // Prevent listener exceptions from affecting processing
      }
    });
  }

  /**
   * Execute a user natural language request. Strategy (temporary): 1. Send prompt as-is to Gemini.
   * 2. Return raw model text (no automatic JSON tool calling yet).
   */
  /**
   * Handle user prompt using Gemini Automatic Function Calling (AFC). Allows natural language
   * commands to trigger drawing operations.
   */
  public AgentReply handleUserPrompt(String userPrompt) {
    String requestId = UUID.randomUUID().toString();
    fireProcessingStarted(userPrompt, requestId);

    // Fire analyzing event to show we're working on it
    firePromptAnalyzing("プロンプトを解析中...", requestId);

    // Record user turn
    history.add(ChatMessage.user(userPrompt));
    trimHistory();
    persistHistoryMaybe();

    try {
      // Update DrawingFunctions with current state
      DrawingFunctions.setCurrentState(currentState);

      // Build conversation context
      String contextPrompt = buildContextPrompt(userPrompt);

      // Fire analyzing event
      firePromptAnalyzing("描画関数を準備中...", requestId);

      // Get drawing function methods for automatic execution
      List<java.lang.reflect.Method> methods = getDrawingMethods();

      // Configure with automatic function calling
      GenerateContentConfig config = GenerateContentConfig.builder()
          .tools(Tool.builder().functions(methods.toArray(new java.lang.reflect.Method[0])))
          .build();

      // Call Gemini with Function Calling support
      Optional<GenerateContentResponse> responseOpt =
          geminiClient.generateContentWithTools(contextPrompt, config);

      if (responseOpt.isEmpty()) {
        String error = "申し訳ございません。AIサービスへの接続に失敗しました。";
        fireProcessingFailed(error, FailurePoint.CONNECTION, requestId);
        return new AgentReply(error, currentState, null);
      }

      GenerateContentResponse response = responseOpt.get();

      // Get the text response
      String textResponse = response.text();

      // Check if automatic function calling occurred
      // The SDK handles function execution automatically
      // We just need to update our state after execution
      DrawingState previousState = currentState;
      currentState = DrawingFunctions.getCurrentState();

      // Track tool calls for history
      List<ToolCall> toolCalls = new ArrayList<>();

      // If state changed, we know functions were called
      if (!currentState.equals(previousState)) {
        // Calculate what changed
        int shapesBefore = previousState.getShapes().size();
        int shapesAfter = currentState.getShapes().size();

        if (shapesAfter > shapesBefore) {
          // Infer which function was likely called based on changes
          for (int i = shapesBefore; i < shapesAfter; i++) {
            com.example.domain.models.Shape shape = currentState.getShapes().get(i);
            String toolName = getToolNameFromShape(shape);
            UUID toolCallId = UUID.randomUUID();

            // Create tool call record
            Map<String, Object> args = inferArgumentsFromShape(shape);
            toolCalls.add(new ToolCall(toolName, args, "Success"));

            fireToolSelecting(toolName, "Automatic Function Calling", args, requestId, toolCallId);
            fireToolExecuting(toolName, ToolStatus.COMPLETED, requestId, toolCallId);
          }
        } else if (shapesAfter == 0 && shapesBefore > 0) {
          // Canvas was cleared
          toolCalls.add(new ToolCall("clearCanvas", new java.util.HashMap<>(), "Success"));
          fireToolSelecting("clearCanvas", "Automatic Function Calling", null, requestId,
              UUID.randomUUID());
          fireToolExecuting("clearCanvas", ToolStatus.COMPLETED, requestId, UUID.randomUUID());
        }
      }

      // Use response text or default message
      String finalMessage =
          textResponse != null && !textResponse.isBlank() ? textResponse : "操作を完了しました。";

      // Record assistant response with tool calls if any
      if (!toolCalls.isEmpty()) {
        history.add(ChatMessage.assistantWithTools(finalMessage, toolCalls));
      } else {
        history.add(ChatMessage.assistant(finalMessage));
      }
      trimHistory();
      persistHistoryMaybe();

      fireProcessingCompleted(finalMessage, currentState, requestId);
      return new AgentReply(finalMessage, currentState, null);

    } catch (Exception e) {
      String error = "エラーが発生しました: " + e.getMessage();
      fireProcessingFailed(error, FailurePoint.TOOL_EXECUTION, requestId);
      return new AgentReply(error, currentState, null);
    }
  }

  /**
   * Build conversation context from history.
   */
  private String buildContextPrompt(String currentPrompt) {
    StringBuilder prompt = new StringBuilder();

    // Add system prompt
    prompt.append(SYSTEM_PROMPT).append("\n\n");

    // Add recent conversation history (excluding system message)
    prompt.append("会話履歴:\n");
    for (int i = 1; i < history.size(); i++) { // Skip system message at index 0
      ChatMessage msg = history.get(i);
      if (msg.role == Role.USER) {
        prompt.append("ユーザー: ").append(msg.content).append("\n");
      } else if (msg.role == Role.ASSISTANT) {
        prompt.append("アシスタント: ").append(msg.content).append("\n");
      }
    }

    // Add current prompt
    prompt.append("\n現在のリクエスト: ").append(currentPrompt);

    return prompt.toString();
  }

  /**
   * Get tool name from shape type.
   */
  private String getToolNameFromShape(com.example.domain.models.Shape shape) {
    if (shape instanceof com.example.domain.models.Circle) {
      return "createCircle";
    } else if (shape instanceof com.example.domain.models.Rectangle) {
      return "createRectangle";
    } else if (shape instanceof com.example.domain.models.Line) {
      return "createLine";
    } else if (shape instanceof com.example.domain.models.Text) {
      return "createText";
    }
    return "unknown";
  }

  /**
   * Infer arguments from shape properties.
   */
  private Map<String, Object> inferArgumentsFromShape(com.example.domain.models.Shape shape) {
    Map<String, Object> args = new java.util.HashMap<>();

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
   * Get drawing function methods for automatic function calling.
   */
  private List<java.lang.reflect.Method> getDrawingMethods() {
    List<java.lang.reflect.Method> methods = new ArrayList<>();

    try {
      // Get methods from DrawingFunctions class
      Class<?> clazz = DrawingFunctions.class;

      // Add createRectangle method
      methods.add(clazz.getMethod("createRectangle", int.class, int.class, int.class, int.class,
          String.class));

      // Add createCircle method
      methods.add(clazz.getMethod("createCircle", int.class, int.class, int.class, String.class));

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
      // Log error but continue with available methods
      System.err.println("Failed to load some drawing methods: " + e.getMessage());
    }

    return methods;
  }


  /**
   * Hook for future structured tool call execution.
   */
  public AgentReply executeTool(String toolName, Map<String, Object> params) {
    ToolExecutionResult res = toolInvoker.invokeTool(toolName, params, currentState);
    if (res.isSuccess()) {
      currentState = res.getResultState();
      return new AgentReply("Tool executed: " + toolName, currentState, res.getMetadata());
    }
    return new AgentReply("Tool failed: " + res.getErrorMessage(), currentState, res.getMetadata());
  }

  // --- History helpers ---
  private void trimHistory() {
    if (history.size() > MAX_HISTORY) {
      int remove = history.size() - MAX_HISTORY;
      // keep system message at index 0
      int idx = 1;
      while (remove > 0 && idx < history.size()) {
        history.remove(idx);
        remove--;
      }
    }
  }

  private void persistHistoryMaybe() {
    historyPersistCounter++;
    if (!persistEnabled || historyPersistCounter % PERSIST_INTERVAL != 0) {
      return;
    }
    persistHistoryImmediately();
  }

  /**
   * Persist history immediately to file.
   */
  private void persistHistoryImmediately() {
    if (!persistEnabled) {
      return;
    }
    try {
      List<Map<String, Object>> list = new ArrayList<>();
      for (ChatMessage m : history) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", m.role.name().toLowerCase());
        entry.put("content", m.content);

        // Add tool calls if present
        if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
          List<Map<String, Object>> tools = new ArrayList<>();
          for (ToolCall tc : m.toolCalls) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", tc.name);
            tool.put("arguments", tc.arguments);
            tool.put("result", tc.result);
            tools.add(tool);
          }
          entry.put("toolCalls", tools);
        }

        list.add(entry);
      }
      String json = GSON.toJson(list);
      Files.writeString(HISTORY_FILE, json, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      /* Log error for debugging */
      System.err.println("Failed to persist history: " + e.getMessage());
    }
  }

  /**
   * Clear persisted history file (if exists) and reset in-memory history to seeded system prompt.
   * Best-effort: exceptions are swallowed to avoid affecting agent runtime.
   */
  public void clearHistoryFile() {
    try {
      if (Files.exists(HISTORY_FILE)) {
        Files.delete(HISTORY_FILE);
      }
    } catch (Exception ignored) {
      // best-effort
    }
    resetHistory();
  }

  private void loadHistoryIfExists() {
    if (!Files.exists(HISTORY_FILE))
      return;
    try {
      String json = Files.readString(HISTORY_FILE);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> list = GSON.fromJson(json, List.class);
      if (list == null)
        return;
      // keep the seed system message at index 0, append the rest
      for (Map<String, Object> e : list) {
        Object r = e.get("role");
        Object c = e.get("content");
        if (!(c instanceof String))
          continue;
        String roleStr = r instanceof String ? (String) r : "assistant";
        switch (roleStr.toLowerCase()) {
          case "system":
            // replace initial system with loaded if different
            if (!history.isEmpty() && history.get(0).role == Role.SYSTEM) {
              history.set(0, ChatMessage.system((String) c));
            }
            break;
          case "user":
            history.add(ChatMessage.user((String) c));
            break;
          default:
            history.add(ChatMessage.assistant((String) c));
            break;
        }
      }
      trimHistory();
    } catch (Exception ignored) {
      /* ignore corrupt */ }
  }

  /** Public wrapper to allow UI to trigger history loading on demand. */
  public boolean loadHistoryOnDemand() {
    int before = history.size();
    loadHistoryIfExists();
    return history.size() > before;
  }


  public List<ChatMessage> getHistory() {
    return Collections.unmodifiableList(history);
  }

  public void resetHistory() {
    history.clear();
    history.add(ChatMessage.system(SYSTEM_PROMPT));
  }

  // --- ChatMessage DTO ---
  public static class ChatMessage {
    public final Role role;
    public final String content;
    public final List<ToolCall> toolCalls;

    private ChatMessage(Role role, String content) {
      this(role, content, null);
    }

    private ChatMessage(Role role, String content, List<ToolCall> toolCalls) {
      this.role = role;
      this.content = content;
      this.toolCalls = toolCalls;
    }

    public static ChatMessage system(String c) {
      return new ChatMessage(Role.SYSTEM, c);
    }

    public static ChatMessage user(String c) {
      return new ChatMessage(Role.USER, c);
    }

    public static ChatMessage assistant(String c) {
      return new ChatMessage(Role.ASSISTANT, c);
    }

    public static ChatMessage assistantWithTools(String c, List<ToolCall> tools) {
      return new ChatMessage(Role.ASSISTANT, c, tools);
    }
  }

  // --- ToolCall DTO ---
  public static class ToolCall {
    public final String name;
    public final Map<String, Object> arguments;
    public final String result;

    public ToolCall(String name, Map<String, Object> arguments, String result) {
      this.name = name;
      this.arguments = arguments;
      this.result = result;
    }
  }

  public enum Role {
    SYSTEM("System"), USER("User"), ASSISTANT("Assistant");

    public final String display;

    Role(String d) {
      this.display = d;
    }
  }



  /** Simple immutable reply DTO */
  public static class AgentReply {
    public final String message;
    public final DrawingState state;
    public final Map<String, Object> metadata;

    public AgentReply(String message, DrawingState state, Map<String, Object> metadata) {
      this.message = message;
      this.state = state;
      this.metadata = metadata;
    }
  }
}
