package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.infrastructure.ai.GeminiClient;
import com.example.infrastructure.services.ToolInvoker;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;
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

  // --- Conversation History (Step 1) ---
  private static final String SYSTEM_PROMPT = String.join("\n", "You are an AI drawing assistant.",
      "User interacts with a vector drawing canvas (shapes: circle, rectangle, line, text).",
      "Keep answers concise. If a drawing action is requested, you may later emit a tool call (not yet implemented).",
      "Ask clarifying questions when parameters are ambiguous.");

  private static final int MAX_HISTORY = 20; // simple cap to avoid unbounded growth
  private static final Path HISTORY_FILE = Path.of(".agent_history.json");
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private long historyPersistCounter = 0; // persist every N changes
  private static final int PERSIST_INTERVAL = 2;
  private final List<ChatMessage> history = new ArrayList<>();

  public AIAgentService(DrawingState initialState) {
    this.geminiClient = GeminiClient.getInstance();
    this.toolInvoker = new ToolInvoker(ToolRegistry.getInstance());
    this.currentState = initialState != null ? initialState : DrawingState.defaultState();
    // Seed system message
    history.add(ChatMessage.system(SYSTEM_PROMPT));
    loadHistoryIfExists();
  }

  public DrawingState getCurrentState() {
    return currentState;
  }

  /**
   * Execute a user natural language request. Strategy (temporary): 1. Send prompt as-is to Gemini.
   * 2. Return raw model text (no automatic JSON tool calling yet).
   */
  public AgentReply handleUserPrompt(String userPrompt) {
    // Record user turn
    history.add(ChatMessage.user(userPrompt));
    trimHistory();
    persistHistoryMaybe();

    String aggregatedPrompt = buildAggregatedPrompt();
    // Append structured output instruction (YAML delimited)
    aggregatedPrompt += "\n\n" + STRUCTURE_INSTRUCTION;

    Optional<String> ai = geminiClient.generateText(aggregatedPrompt);
    if (ai.isEmpty()) {
      return new AgentReply("(AI応答取得失敗)", currentState, null);
    }
    String raw = ai.get();

    // Try structured parse
    StructuredResult sr = parseStructured(raw);
    if (sr.success && sr.type == StructuredType.TOOL_CALL && sr.toolName != null) {
      // Execute tool
      ToolExecutionResult exec = toolInvoker.invokeTool(sr.toolName, sr.args, currentState);
      if (exec.isSuccess()) {
        currentState = exec.getResultState();
        history.add(ChatMessage.assistant("(tool executed) " + sr.toolName));
        trimHistory();
        persistHistoryMaybe();
        return new AgentReply(sr.message != null ? sr.message : "Tool executed: " + sr.toolName,
            currentState, exec.getMetadata());
      } else {
        history.add(ChatMessage.assistant("(tool failed) " + exec.getErrorMessage()));
        trimHistory();
        persistHistoryMaybe();
        return new AgentReply("Tool failed: " + exec.getErrorMessage(), currentState,
            exec.getMetadata());
      }
    }

    // Fallback: treat as plain answer
    history.add(ChatMessage.assistant(sr.message != null ? sr.message : raw));
    trimHistory();
    persistHistoryMaybe();
    return new AgentReply(sr.message != null ? sr.message : raw, currentState, null);
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
    if (historyPersistCounter % PERSIST_INTERVAL != 0) {
      return;
    }
    try {
      List<Map<String, Object>> list = new ArrayList<>();
      for (ChatMessage m : history) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", m.role.name().toLowerCase());
        entry.put("content", m.content);
        list.add(entry);
      }
      String json = GSON.toJson(list);
      Files.writeString(HISTORY_FILE, json, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception ignored) {
      /* persistence best effort */ }
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

  private String buildAggregatedPrompt() {
    StringBuilder sb = new StringBuilder();
    for (ChatMessage m : history) {
      // Skip system (already included as first line) except keep as is
      if (m.role == Role.SYSTEM) {
        sb.append(m.content).append("\n\n");
      } else {
        sb.append(m.role.display).append(": ").append(m.content).append("\n");
      }
    }
    // Append tool list summary in YAML-compatible hint form (schema keys only)
    sb.append("\nAvailableTools (YAML tool_call例):\n");
    ToolRegistry.getInstance().getAllTools().forEach(t -> {
      Map<String, Object> schema = t.schema();
      sb.append("- name: ").append(t.name()).append("\n");
      if (schema != null && schema.containsKey("properties")) {
        Object propsObj = schema.get("properties");
        if (propsObj instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> props = (Map<String, Object>) propsObj;
          sb.append("  args:");
          props.keySet().forEach(k -> sb.append(" ").append(k));
          sb.append("\n");
        }
      }
    });
    sb.append("\nYAML出力時 tool_call 例:\n");
    sb.append(START).append("\n");
    sb.append("type: tool_call\n");
    sb.append("tool: create_rectangle\n");
    sb.append("args:\n  position:\n    x: 100\n    y: 120\n  width: 200\n  height: 80\n");
    sb.append("message: 長方形を追加します\n");
    sb.append(END).append("\n");
    return sb.toString().trim();
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

    private ChatMessage(Role role, String content) {
      this.role = role;
      this.content = content;
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
  }

  public enum Role {
    SYSTEM("System"), USER("User"), ASSISTANT("Assistant");

    public final String display;

    Role(String d) {
      this.display = d;
    }
  }

  // --- Structured YAML Parsing ---
  private static final String START = "<STRUCTURED_START>";
  private static final String END = "<STRUCTURED_END>";
  private static final Pattern BLOCK_PATTERN =
      Pattern.compile(START + "\\s*(.*?)\\s*" + END, Pattern.DOTALL);

  private static final String STRUCTURE_INSTRUCTION = String.join("\n",
      "出力は必ず以下デリミタで囲まれたYAMLのみ。前後に説明やコードフェンス禁止。", START, "type: answer | tool_call",
      "# tool_call の場合は必須: tool, args", "tool: (省略可)", "args: {}", "message: 応答テキスト", END);

  private StructuredResult parseStructured(String raw) {
    if (raw == null) {
      return StructuredResult.failure("raw null");
    }
    Matcher m = BLOCK_PATTERN.matcher(raw);
    if (!m.find()) {
      return StructuredResult.failure("no block");
    }
    String yamlBlock = m.group(1).trim();
    if (yamlBlock.isEmpty()) {
      return StructuredResult.failure("empty block");
    }
    try {
      // Use default safe constructor
      Yaml yaml = new Yaml();
      Object loaded = yaml.load(yamlBlock);
      if (!(loaded instanceof Map)) {
        return StructuredResult.failure("not map");
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) loaded;
      String typeStr = valueAsString(map.get("type"));
      String message = valueAsString(map.get("message"));
      String tool = valueAsString(map.get("tool"));
      Map<String, Object> args = mapOf(map.get("args"));

      StructuredType st = StructuredType.from(typeStr);
      if (st == StructuredType.TOOL_CALL) {
        if (tool == null || tool.isEmpty()) {
          return StructuredResult.failure("tool_call missing tool");
        }
        if (args == null) {
          args = new LinkedHashMap<>();
        }
        ValidationOutcome vo = validateAndNormalizeToolArgs(tool, args);
        if (!vo.valid) {
          return StructuredResult.failure("arg invalid: " + vo.errorMessage);
        }
        String safeMsg = (message == null || message.isBlank()) ? "(no message)" : message;
        return StructuredResult.successTool(tool, args, safeMsg);
      }
      String safeMsg = (message == null || message.isBlank()) ? "(no message)" : message;
      return StructuredResult.successAnswer(safeMsg);
    } catch (Exception e) {
      return StructuredResult.failure("parse error: " + e.getMessage());
    }
  }

  private static String valueAsString(Object o) {
    return o instanceof String ? (String) o : null;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapOf(Object o) {
    return o instanceof Map ? (Map<String, Object>) o : null;
  }

  private enum StructuredType {
    ANSWER, TOOL_CALL, UNKNOWN;

    static StructuredType from(String s) {
      if (s == null)
        return UNKNOWN;
      switch (s) {
        case "answer":
          return ANSWER;
        case "tool_call":
          return TOOL_CALL;
        default:
          return UNKNOWN;
      }
    }
  }

  // --- ValidationOutcome helper ---
  private static final class ValidationOutcome {
    final boolean valid;
    final String errorMessage;

    ValidationOutcome(boolean v, String e) {
      this.valid = v;
      this.errorMessage = e;
    }

    static ValidationOutcome ok() {
      return new ValidationOutcome(true, null);
    }

    static ValidationOutcome fail(String e) {
      return new ValidationOutcome(false, e);
    }
  }

  // B,C,E: schema-based deep validation + extra key removal + nested numeric checks
  @SuppressWarnings("unchecked")
  private ValidationOutcome validateAndNormalizeToolArgs(String tool, Map<String, Object> args) {
    var toolObj = ToolRegistry.getInstance().getTool(tool);
    if (toolObj == null)
      return ValidationOutcome.fail("unknown tool");
    Map<String, Object> schema = toolObj.schema();
    if (schema == null)
      return ValidationOutcome.ok();
    Object propsObj = schema.get("properties");
    Map<String, Object> properties =
        propsObj instanceof Map ? (Map<String, Object>) propsObj : Map.of();
    // Remove extra keys
    Set<String> allowed = new HashSet<>(properties.keySet());
    List<String> toRemove = new ArrayList<>();
    for (String k : args.keySet())
      if (!allowed.contains(k))
        toRemove.add(k);
    toRemove.forEach(args::remove);

    // Required top-level
    Set<String> required = new HashSet<>();
    Object reqObj = schema.get("required");
    if (reqObj instanceof String[]) {
      for (String s : (String[]) reqObj)
        required.add(s);
    } else if (reqObj instanceof List) {
      for (Object o : (List<?>) reqObj)
        if (o instanceof String)
          required.add((String) o);
    }
    for (String r : required)
      if (!args.containsKey(r))
        return ValidationOutcome.fail("missing required: " + r);

    // Validate each present key
    for (Map.Entry<String, Object> e : args.entrySet()) {
      String key = e.getKey();
      Object val = e.getValue();
      Object propSchema = properties.get(key);
      if (!(propSchema instanceof Map))
        continue; // skip unknown
      Map<String, Object> ps = (Map<String, Object>) propSchema;
      String type = valueAsString(ps.get("type"));
      if ("number".equals(type)) {
        if (!(val instanceof Number))
          return ValidationOutcome.fail("field " + key + " not number");
        // minimum
        Object minObj = ps.get("minimum");
        if (minObj instanceof Number) {
          double min = ((Number) minObj).doubleValue();
          if (((Number) val).doubleValue() < min)
            return ValidationOutcome.fail("field " + key + " < minimum");
        }
      } else if ("object".equals(type) && val instanceof Map) {
        // Nested simple object (e.g., position)
        Map<String, Object> nested = (Map<String, Object>) val;
        Object nestedReq = ps.get("required");
        Set<String> nreq = new HashSet<>();
        if (nestedReq instanceof String[]) {
          for (String s : (String[]) nestedReq)
            nreq.add(s);
        } else if (nestedReq instanceof List) {
          for (Object o : (List<?>) nestedReq)
            if (o instanceof String)
              nreq.add((String) o);
        }
        for (String nr : nreq)
          if (!nested.containsKey(nr))
            return ValidationOutcome.fail("nested missing: " + key + "." + nr);
        Object npropsObj = ps.get("properties");
        if (npropsObj instanceof Map) {
          Map<String, Object> nprops = (Map<String, Object>) npropsObj;
          for (String nk : new ArrayList<>(nested.keySet()))
            if (!nprops.containsKey(nk))
              nested.remove(nk);
          for (Map.Entry<String, Object> en : nested.entrySet()) {
            Object npsObj = nprops.get(en.getKey());
            if (npsObj instanceof Map) {
              Map<String, Object> nps = (Map<String, Object>) npsObj;
              String nt = valueAsString(nps.get("type"));
              if ("number".equals(nt)) {
                if (!(en.getValue() instanceof Number))
                  return ValidationOutcome.fail("nested not number: " + key + "." + en.getKey());
                Object nminObj = nps.get("minimum");
                if (nminObj instanceof Number) {
                  double min = ((Number) nminObj).doubleValue();
                  if (((Number) en.getValue()).doubleValue() < min)
                    return ValidationOutcome.fail("nested minimum: " + key + "." + en.getKey());
                }
              }
            }
          }
        }
      } else if ("boolean".equals(type)) {
        if (!(val instanceof Boolean))
          return ValidationOutcome.fail("field " + key + " not boolean");
      }
    }
    return ValidationOutcome.ok();
  }

  private static final class StructuredResult {
    final boolean success;
    final StructuredType type;
    final String toolName;
    final Map<String, Object> args;
    final String message;
    @SuppressWarnings("unused")
    final String error;

    private StructuredResult(boolean success, StructuredType type, String toolName,
        Map<String, Object> args, String message, String error) {
      this.success = success;
      this.type = type;
      this.toolName = toolName;
      this.args = args;
      this.message = message;
      this.error = error;
    }

    static StructuredResult successAnswer(String message) {
      return new StructuredResult(true, StructuredType.ANSWER, null, null, message, null);
    }

    static StructuredResult successTool(String tool, Map<String, Object> args, String message) {
      return new StructuredResult(true, StructuredType.TOOL_CALL, tool, args, message, null);
    }

    static StructuredResult failure(String err) {
      return new StructuredResult(false, StructuredType.UNKNOWN, null, null, null, err);
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
