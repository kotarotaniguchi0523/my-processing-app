package com.example.application.services;

import com.example.domain.models.DrawingState;
import com.example.infrastructure.ai.GeminiClient;
import com.example.infrastructure.services.ToolInvoker;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.tools.ToolRegistry;
import com.example.domain.events.*;
import com.example.domain.models.AgentHistory;
import com.google.genai.types.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Refactored AIAgentService orchestrates chat -> LLM -> tool execution -> new state + reply.
 * Now uses separate service classes for better separation of concerns.
 */
public class AIAgentService {
  private final GeminiClient geminiClient;
  private final ToolInvoker toolInvoker;
  private DrawingState currentState;

  // --- Separated Service Components ---
  private final ConversationService conversationService;
  private final PromptBuilder promptBuilder;
  private final JsonHistoryRepository historyRepository;
  private final ToolCoordinator toolCoordinator;

  // --- Event System ---
  private final List<IAIProcessingListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicLong sequenceCounter = new AtomicLong(0);
  
  // --- Session Management ---
  private AgentHistory.Session currentSession;
  private final boolean loadHistoryOnStartup;
  
  // --- Diff Approval ---
  private ApprovalCallback approvalCallback;

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
    this.toolInvoker = new ToolInvoker(ToolRegistry.getInstance());
    this.currentState = initialState != null ? initialState : DrawingState.defaultState();
    
    // Initialize separated service components
    this.conversationService = new ConversationService();
    this.promptBuilder = new PromptBuilder(conversationService);
    this.historyRepository = new JsonHistoryRepository(persistHistory);
    this.toolCoordinator = new ToolCoordinator();
    
    this.loadHistoryOnStartup = loadHistoryOnStartup;
    
    if (this.loadHistoryOnStartup) {
      loadHistoryIfExists();
    } else {
      // Create new session if not loading from file
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession.addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }
    
    // Create initial history file if persistence is enabled and file doesn't exist
    if (persistHistory && !historyRepository.historyFileExists()) {
      historyRepository.persistHistoryImmediately(currentSession, currentState);
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
    EventMeta meta = new EventMeta(conversationService.getSessionId(), requestId, sequenceCounter.incrementAndGet());
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
   * Interface for approval/rejection callbacks
   */
  public interface ApprovalCallback {
    void onApprove(DrawingState newState, String message);
    void onReject();
  }
  
  /**
   * Set the approval callback for state changes
   */
  public void setApprovalCallback(ApprovalCallback callback) {
    this.approvalCallback = callback;
  }
  
  /**
   * Apply approved state changes
   */
  public void applyApprovedState(DrawingState newState, String message) {
    this.currentState = newState;
    
    // Record the approved change in conversation history
    conversationService.addAssistantMessage(message);
    if (currentSession != null) {
      currentSession.addMessage(new AgentHistory.Message("assistant", message));
      currentSession.setFinalState(currentState);
    }
    
    // Persist history
    historyRepository.persistHistoryMaybe(currentSession, currentState);
    
    // State has been applied and approved
  }
  
  /**
   * Handle user prompt using Gemini Automatic Function Calling (AFC). Allows natural language
   * commands to trigger drawing operations.
   */
  public AgentReply handleUserPrompt(String userPrompt) {
    String requestId = UUID.randomUUID().toString();
    fireProcessingStarted(userPrompt, requestId);

    // Fire analyzing event to show we're working on it
    firePromptAnalyzing("プロンプトを解析中...", requestId);

    // Record user turn using ConversationService
    conversationService.addUserMessage(userPrompt);
    
    // Also add to AgentHistory session
    if (currentSession != null) {
      currentSession.addMessage(new AgentHistory.Message("user", userPrompt));
    }
    
    // Persist history using JsonHistoryRepository
    historyRepository.persistHistoryMaybe(currentSession, currentState);

    try {
      // Update ToolCoordinator with current state
      toolCoordinator.updateCurrentState(currentState);

      // Build conversation context using PromptBuilder (include canvas size)
      String contextPrompt = promptBuilder.buildContextPrompt(userPrompt, currentState);

      // Fire analyzing event
      firePromptAnalyzing("描画関数を準備中...", requestId);

      // Get drawing function configuration from ToolCoordinator
      GenerateContentConfig config = toolCoordinator.createToolConfig();

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

      // Check if automatic function calling occurred using ToolCoordinator
      DrawingState previousState = currentState;
      DrawingState proposedState = toolCoordinator.getCurrentState();

      // Infer tool calls from state changes using ToolCoordinator
      List<ToolCall> toolCalls = toolCoordinator.inferToolCallsFromStateChange(previousState, proposedState);
      
      // Fire events for detected tool calls
      for (ToolCall tc : toolCalls) {
        UUID toolCallId = UUID.randomUUID();
        fireToolSelecting(tc.name, "Automatic Function Calling", tc.arguments, requestId, toolCallId);
        fireToolExecuting(tc.name, ToolStatus.COMPLETED, requestId, toolCallId);
      }

      // Use response text or default message
      String finalMessage =
          textResponse != null && !textResponse.isBlank() ? textResponse : "操作を完了しました。";

      // Check if state has changed and requires approval
      boolean stateChanged = !previousState.equals(proposedState);
      
      if (stateChanged && approvalCallback != null) {
        // Trigger approval dialog with the proposed state and message
        approvalCallback.onApprove(proposedState, finalMessage);
        
        // Return current state (not the proposed one) until approved
        fireProcessingCompleted("変更の承認待ち...", currentState, requestId);
        return new AgentReply("変更の承認待ち...", currentState, null);
        
      } else {
        // No state change or no approval callback - apply immediately
        currentState = proposedState;
        
        // Record assistant response using ConversationService
        if (!toolCalls.isEmpty()) {
          conversationService.addAssistantMessageWithTools(finalMessage, toolCalls);
          
          // Also add to AgentHistory session
          if (currentSession != null) {
            AgentHistory.Message msg = new AgentHistory.Message("assistant", finalMessage);
            for (ToolCall tc : toolCalls) {
              msg.getToolCalls().add(new AgentHistory.ToolCall(tc.name, tc.arguments, tc.result));
            }
            currentSession.addMessage(msg);
          }
        } else {
          conversationService.addAssistantMessage(finalMessage);
          
          // Also add to AgentHistory session
          if (currentSession != null) {
            currentSession.addMessage(new AgentHistory.Message("assistant", finalMessage));
          }
        }
        
        // Update session's final state and persist using JsonHistoryRepository
        if (currentSession != null) {
          currentSession.setFinalState(currentState);
        }
        
        historyRepository.persistHistoryMaybe(currentSession, currentState);

        fireProcessingCompleted(finalMessage, currentState, requestId);
        System.out.println("AIAgentService returning state with " + currentState.getShapes().size() + " shapes");
        return new AgentReply(finalMessage, currentState, null);
      }

    } catch (Exception e) {
      String error = "エラーが発生しました: " + e.getMessage();
      fireProcessingFailed(error, FailurePoint.TOOL_EXECUTION, requestId);
      return new AgentReply(error, currentState, null);
    }
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


  /**
   * Clear persisted history file (if exists) and reset in-memory history to seeded system prompt.
   * Best-effort: exceptions are swallowed to avoid affecting agent runtime.
   */
  public void clearHistoryFile() {
    historyRepository.clearHistoryFile();
    conversationService.resetHistory();
  }

  private void loadHistoryIfExists() {
    AgentHistory.Session loadedSession = historyRepository.loadHistoryFromFile();
    
    if (loadedSession != null) {
      // Load the session and update conversation service
      this.currentSession = loadedSession;
      conversationService.loadFromAgentHistory(loadedSession);
      
      // Load final state if available
      if (currentSession.getFinalState() != null) {
        currentState = currentSession.getFinalState();
      }
    } else {
      // Create new session if no history loaded
      this.currentSession = new AgentHistory.Session(conversationService.getSessionId());
      this.currentSession.addMessage(new AgentHistory.Message("system", conversationService.getSystemPrompt()));
    }
  }

  /** Public wrapper to allow UI to trigger history loading on demand. */
  public boolean loadHistoryOnDemand() {
    int before = conversationService.getHistory().size();
    loadHistoryIfExists();
    return conversationService.getHistory().size() > before;
  }


  public List<ChatMessage> getHistory() {
    return conversationService.getHistory();
  }

  public void resetHistory() {
    conversationService.resetHistory();
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
