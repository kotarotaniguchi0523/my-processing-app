package com.example.application.agent;

import com.example.infrastructure.agent.ai.AIPromptIntegrator;
import com.example.infrastructure.agent.ai.PromptBuilder;
import com.example.domain.models.DrawingState;
import com.example.domain.services.IAgentService;
import com.example.domain.tools.IAgentTool;
import com.example.domain.tools.IToolRegistry;
import com.example.domain.values.ToolExecutionResult;
import com.example.domain.values.ValidationResult;
import com.example.application.tools.ToolRegistry;
// ServiceResponse is in the same package
import com.example.infrastructure.ai.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AgentService is the main orchestrator for AI-driven drawing operations. It handles user input, AI
 * communication, tool execution, and approval workflows.
 */
public class AgentService implements IAgentService {

  private static final Logger logger = Logger.getLogger(AgentService.class.getName());

  private final AIPromptIntegrator promptIntegrator;
  private final ToolInvoker toolInvoker;
  private final IToolRegistry toolRegistry;
  private final Gson gson;
  private DrawingState currentState;
  // M3 approval wait
  private final java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Boolean>> approvalFutureRef =
      new java.util.concurrent.atomic.AtomicReference<>();

  // Progress listener for streaming user-visible logs (thinking logs)
  private transient IAgentService.ProgressListener progressListener;

  /**
   * Creates a new AgentService with the specified AI client
   *
   * @param aiClient The AI client to use for communication
   */
  public AgentService(AIClient aiClient) {
    this(aiClient, new ToolInvoker(), ToolRegistry.getInstance(), new Gson());
  }

  /**
   * 依存を注入可能な拡張コンストラクタ（DIP対応）。
   */
  public AgentService(AIClient aiClient, ToolInvoker toolInvoker, IToolRegistry toolRegistry,
      Gson gson) {
    this.toolInvoker = toolInvoker;
    this.toolRegistry = toolRegistry;
    this.promptIntegrator = new AIPromptIntegrator(aiClient, new PromptBuilder(toolRegistry));
    this.gson = gson;
    this.currentState = DrawingState.empty();
  }

  // Register a listener to receive progress updates
  @Override
  public void setProgressListener(IAgentService.ProgressListener listener) {
    this.progressListener = listener;
  }

  private void progress(String msg) {
    try {
      if (this.progressListener != null && msg != null && !msg.isEmpty()) {
        this.progressListener.onProgress(msg);
      }
    } catch (Exception ignored) {
      // keep robust even if listener throws
    }
  }

  /**
   * Creates a new AgentService with default AI client
   */
  public AgentService() {
    this(AIClientFactory.createApplicationClient());
  }

  /**
   * Processes a user instruction asynchronously
   *
   * @param instruction The user's instruction
   * @return CompletableFuture with the service response
   */
  @Override
  public CompletableFuture<Object> processInstruction(String instruction) {
    logger.info("Processing instruction: " + instruction);
    progress("ユーザーのリクエストを解析中...");

    return promptIntegrator.processMessage(instruction).thenApply(resp -> {
      progress("AI 応答を受信、内容を解析中...");
      return resp;
    }).thenCompose(this::handleAIResponse).thenApply(response -> (Object) response)
        .exceptionally(throwable -> (Object) handleError(throwable));
  }

  /**
   * Processes a user instruction synchronously
   *
   * @param instruction The user's instruction
   * @return The service response
   */
  @Override
  public Object processInstructionSync(String instruction) {
    try {
      return processInstruction(instruction).get();
    } catch (Exception e) {
      return handleError(e);
    }
  }

  // M3: Await user approval (single pending at a time)
  @Override
  public CompletableFuture<Boolean> awaitApproval() {
    CompletableFuture<Boolean> fut = new CompletableFuture<>();
    if (!approvalFutureRef.compareAndSet(null, fut)) {
      // Already pending; complete exceptionally
      fut.completeExceptionally(new IllegalStateException("Approval already pending"));
      return fut;
    }
    return fut;
  }

  // M3: Complete approval from UI events
  @Override
  public void completeApproval(boolean approved) {
    CompletableFuture<Boolean> fut = approvalFutureRef.getAndSet(null);
    if (fut != null) {
      fut.complete(approved);
    }
  }

  @Override
  public boolean hasPendingApproval() {
    return approvalFutureRef.get() != null;
  }

  /**
   * Handles the AI response and processes any tool calls
   *
   * @param response The processed AI response
   * @return CompletableFuture with the service response
   */
  private CompletableFuture<ServiceResponse> handleAIResponse(
      AIPromptIntegrator.ProcessedResponse response) {
    ServiceResponse serviceResponse = new ServiceResponse();
    progress("AI 応答を処理中...");

    // Validate response is not null
    if (response == null) {
      logger.warning("Received null processed response from AI");
      return CompletableFuture
          .completedFuture(ServiceResponse.error("Received invalid response from AI"));
    }

    // Log response details for debugging
    logger.info(String.format("Processing AI response - has tool calls: %s, has text: %s",
        response.hasToolCalls(), response.hasTextResponse()));

    if (response.hasToolCalls()) {
      // Validate tool calls before processing
      List<AIResponse.ToolCall> validToolCalls = validateToolCalls(response.getToolCalls());

      if (validToolCalls.isEmpty()) {
        logger.warning("No valid tool calls found in response");
        progress("有効なツール呼び出しが見つかりませんでした。テキスト応答を確認中...");
        // Fall back to text response if available
        if (response.hasTextResponse()) {
          serviceResponse.setTextResponse(response.getTextResponse());
          serviceResponse.setSuccess(true);
          return CompletableFuture.completedFuture(serviceResponse);
        } else {
          return CompletableFuture
              .completedFuture(ServiceResponse.error("Invalid tool calls and no text response"));
        }
      }

      // Process valid tool calls SEQUENTIALLY to ensure state is updated step-by-step
      progress(String.format("%d 件のツール呼び出しを順次実行します...", validToolCalls.size()));
      return CompletableFuture.supplyAsync(() -> {
        List<ToolExecutionResult> results = new ArrayList<>();
        DrawingState workingState = this.currentState;

        for (AIResponse.ToolCall toolCall : validToolCalls) {
          try {
            progress("ツール実行中: " + toolCall.getName());
            ToolExecutionResult result =
                toolInvoker.invokeTool(toolCall.getName(), toolCall.getParameters(), workingState);
            results.add(result);
            if (result.isSuccess() && result.getResultState() != null) {
              workingState = result.getResultState(); // chain state
              progress("ツール成功: " + toolCall.getName());
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Tool execution failed: " + toolCall.getName(), e);
            results.add(ToolExecutionResult.error(toolCall.getName(), e.getMessage()));
            progress("ツール失敗: " + toolCall.getName() + " - " + e.getMessage());
          }
        }

        // Update agent's current state to the final state after all tools
        this.currentState = workingState;

        serviceResponse.setToolExecutionResults(results);
        serviceResponse.setRequiresApproval(checkIfApprovalRequired(results));

        boolean hasSuccess = results.stream().anyMatch(ToolExecutionResult::isSuccess);
        serviceResponse.setSuccess(hasSuccess);

        if (response.hasTextResponse() && !hasSuccess) {
          serviceResponse
              .setTextResponse("Tool execution failed. AI response: " + response.getTextResponse());
        }

        addToolResultsToConversation(results);
        return serviceResponse;
      });

    } else if (response.hasTextResponse()) {
      // Handle text-only response
      String textResponse = response.getTextResponse();
      logger.info("Processing text-only response: "
          + (textResponse.length() > 100 ? textResponse.substring(0, 100) + "..." : textResponse));
      progress("テキスト応答を生成しました。");

      serviceResponse.setTextResponse(textResponse);
      serviceResponse.setSuccess(true);
      return CompletableFuture.completedFuture(serviceResponse);

    } else {
      // No valid response - this should be rare
      logger.warning("AI response contains neither tool calls nor text response");
      serviceResponse.setErrorMessage("AI provided an empty or malformed response");
      serviceResponse.setSuccess(false);
      return CompletableFuture.completedFuture(serviceResponse);
    }
  }

  /**
   * Validates a list of tool calls, filtering out invalid ones
   *
   * @param toolCalls The tool calls to validate
   * @return List of valid tool calls
   */
  private List<AIResponse.ToolCall> validateToolCalls(List<AIResponse.ToolCall> toolCalls) {
    List<AIResponse.ToolCall> validCalls = new ArrayList<>();

    if (toolCalls == null) {
      return validCalls;
    }

    for (AIResponse.ToolCall toolCall : toolCalls) {
      AIResponse.ToolCall processedCall = processToolCall(toolCall);
      if (processedCall != null) {
        validCalls.add(processedCall);
      } else {
        String name = toolCall != null ? toolCall.getName() : "null";
        logger.warning(String.format("Skipping invalid tool call: %s", name));
        // Add validation feedback into conversation context
        try {
          if (toolCall != null) {
            ValidationResult vr =
                toolInvoker.validateToolParameters(toolCall.getName(), toolCall.getParameters());
            if (!vr.isValid()) {
              promptIntegrator.addToolResult(toolCall.getName(),
                  "Validation failed: " + vr.getErrorMessage());
            }
          }
        } catch (Exception ignored) {
          // keep robust
        }
      }
    }

    logger.info(String.format("Validated tool calls: %d valid out of %d total", validCalls.size(),
        toolCalls.size()));

    return validCalls;
  }

  /**
   * Process and validate a tool call, adding defaults if needed
   *
   * @param toolCall The tool call to process
   * @return Processed tool call or null if invalid
   */
  private AIResponse.ToolCall processToolCall(AIResponse.ToolCall toolCall) {
    if (toolCall == null) {
      return null;
    }

    if (toolCall.getName() == null || toolCall.getName().trim().isEmpty()) {
      return null;
    }

    if (!toolRegistry.hasTool(toolCall.getName())) {
      logger.warning("Tool not found in registry: " + toolCall.getName());
      return null;
    }

    // Special handling for createShape - add default position if missing
    Map<String, Object> params = toolCall.getParameters();
    if ("createShape".equals(toolCall.getName())) {
      if (params == null) {
        params = new HashMap<>();
      } else {
        // Create a mutable copy of the parameters
        params = new HashMap<>(params);
      }
      // Add default position if not provided (center of canvas area)
      if (!params.containsKey("position")) {
        Map<String, Object> defaultPos = new HashMap<>();
        // Canvas center: x=500 (center of 1000px width), y=211 (center of canvas area)
        defaultPos.put("x", 500.0);
        defaultPos.put("y", 211.0);
        params.put("position", defaultPos);
        logger.info("Added default position for createShape: (500, 211) - center of canvas");
      }
      // Create new ToolCall with updated parameters
      toolCall = new AIResponse.ToolCall(toolCall.getId(), toolCall.getName(), params);
    }

    // Validate parameters against schema
    ValidationResult vr = toolInvoker.validateToolParameters(toolCall.getName(), params);
    if (!vr.isValid()) {
      logger.warning("Tool parameter validation failed for '" + toolCall.getName() + "': "
          + vr.getErrorMessage());
      return null;
    }
    return toolCall;
  }


  /**
   * Adds tool execution results to the conversation with better formatting
   *
   * @param results The tool execution results
   */
  private void addToolResultsToConversation(List<ToolExecutionResult> results) {
    for (ToolExecutionResult result : results) {
      String toolName = result.getToolName() != null ? result.getToolName() : "unknown";
      String formattedResult;

      if (result.isSuccess()) {
        formattedResult = String.format("Tool '%s' executed successfully", toolName);
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
          formattedResult += " with metadata: " + gson.toJson(result.getMetadata());
        }
      } else {
        formattedResult = String.format("Tool '%s' failed: %s", toolName,
            result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error");
      }

      promptIntegrator.addToolResult(toolName, formattedResult);
    }
  }



  /**
   * Checks if the tool execution results require user approval
   *
   * @param results The tool execution results
   * @return true if approval is required
   */
  private boolean checkIfApprovalRequired(List<ToolExecutionResult> results) {
    // Approval required if any significant change occurs (create/modify/delete/clear),
    // or if a clear operation is requested. This makes the approval UI reliably appear
    // whenever the AI proposes a state change.

    boolean anySignificant = results.stream().filter(ToolExecutionResult::isSuccess)
        .map(ToolExecutionResult::getToolName).filter(java.util.Objects::nonNull)
        .anyMatch(this::isSignificantChange);

    boolean anyClear = results.stream().anyMatch(r -> {
      String n = r.getToolName() != null ? r.getToolName() : "";
      String nl = n.toLowerCase(java.util.Locale.ROOT);
      return nl.equals("clearcanvas") || nl.contains("clear");
    });

    return anySignificant || anyClear;
  }

  /**
   * Determines if a tool represents a significant change
   *
   * @param toolName The name of the tool
   * @return true if the tool makes significant changes
   */
  private boolean isSignificantChange(String toolName) {
    return toolName.contains("create") || toolName.contains("delete") || toolName.contains("clear")
        || toolName.contains("modify");
  }

  /**
   * Handles errors during processing
   *
   * @param throwable The error that occurred
   * @return Service response with error information
   */
  private ServiceResponse handleError(Throwable throwable) {
    logger.log(Level.SEVERE, "Error processing instruction", throwable);

    ServiceResponse response = new ServiceResponse();
    response.setSuccess(false);
    response.setErrorMessage("Error: " + throwable.getMessage());

    // Try to recover with a helpful message
    String recoveryPrompt =
        "An error occurred: " + throwable.getMessage() + ". Please try rephrasing your request.";
    response.setTextResponse(recoveryPrompt);

    return response;
  }



  /**
   * Gets the current drawing state
   *
   * @return The current drawing state
   */
  @Override
  public DrawingState getCurrentState() {
    return currentState;
  }

  /**
   * Sets the current drawing state
   *
   * @param state The new drawing state
   */
  @Override
  public void setCurrentState(DrawingState state) {
    this.currentState = state;
  }

  /**
   * Clears the conversation history
   */
  @Override
  public void clearConversation() {
    promptIntegrator.clearConversation();
  }

  /**
   * Gets the conversation history as a formatted string
   *
   * @return The conversation history
   */
  @Override
  public String getConversationHistory() {
    return promptIntegrator.getConversationHistory().export();
  }

  /**
   * Validates that a JSON string represents a valid tool call
   *
   * @param jsonString The JSON string to validate
   * @return true if valid, false otherwise
   */
  public boolean validateToolCallJson(String jsonString) {
    try {
      if (jsonString == null || jsonString.trim().isEmpty()) {
        return false;
      }

      JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

      // Check for tool name with multiple possible field names
      String toolName = null;
      if (json.has("tool")) {
        toolName = json.get("tool").getAsString();
      } else if (json.has("name")) {
        toolName = json.get("name").getAsString();
      } else if (json.has("function")) {
        toolName = json.get("function").getAsString();
      }

      if (toolName == null || toolName.trim().isEmpty()) {
        return false;
      }

      // Check if tool exists
      if (!toolRegistry.hasTool(toolName)) {
        logger.warning("Tool validation failed: tool '" + toolName + "' not found in registry");
        return false;
      }

      // Check for parameters field (with multiple possible field names)
      boolean hasParameters = json.has("parameters") || json.has("args") || json.has("arguments");

      if (!hasParameters) {
        logger.info("Tool call validation: no parameters field found, assuming empty parameters");
      }

      // Validate parameters against schema if present
      IAgentTool tool = toolRegistry.getTool(toolName);
      Map<String, Object> schema = tool.schema();

      if (schema != null && hasParameters) {
        Map<String, Object> parameters = null;
        if (json.has("parameters")) {
          @SuppressWarnings("unchecked")
          Map<String, Object> params = gson.fromJson(json.get("parameters"), Map.class);
          parameters = params;
        } else if (json.has("args")) {
          @SuppressWarnings("unchecked")
          Map<String, Object> params = gson.fromJson(json.get("args"), Map.class);
          parameters = params;
        } else if (json.has("arguments")) {
          @SuppressWarnings("unchecked")
          Map<String, Object> params = gson.fromJson(json.get("arguments"), Map.class);
          parameters = params;
        }

        // Use ToolInvoker's validation
        if (parameters != null && !toolInvoker.canInvokeTool(toolName, parameters)) {
          logger.warning(
              "Tool validation failed: parameters do not match schema for tool '" + toolName + "'");
          return false;
        }
      }

      logger.info("Tool call JSON validation passed for tool: " + toolName);
      return true;

    } catch (JsonSyntaxException e) {
      logger.warning("Tool call JSON validation failed: invalid JSON syntax - " + e.getMessage());
      return false;
    } catch (IllegalStateException e) {
      logger.warning("Tool call JSON validation failed: illegal JSON state - " + e.getMessage());
      return false;
    } catch (Exception e) {
      logger.warning("Tool call JSON validation failed: unexpected error - " + e.getMessage());
      return false;
    }
  }

  /**
   * Parses and executes tool calls from raw text content
   *
   * @param rawContent The raw AI response content
   * @param currentState The current drawing state
   * @return CompletableFuture with service response
   */
  public CompletableFuture<ServiceResponse> parseAndExecuteToolCalls(String rawContent,
      DrawingState currentState) {
    try {
      // Create a mock processed response for the raw content
      AIPromptIntegrator.ProcessedResponse mockResponse =
          new AIPromptIntegrator.ProcessedResponse();
      mockResponse.setTextResponse(rawContent);

      // Use the prompt integrator to extract tool calls
      return promptIntegrator.processMessage("").thenCompose(this::handleAIResponse);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to parse and execute tool calls from raw content", e);
      return CompletableFuture
          .completedFuture(ServiceResponse.error("Failed to parse tool calls: " + e.getMessage()));
    }
  }

  /**
   * Gets detailed information about available tools for debugging
   *
   * @return A formatted string with tool information
   */
  @Override
  public String getToolRegistryInfo() {
    StringBuilder info = new StringBuilder();
    info.append("Available Tools (Unified):\n");

    // Use tool registry for tool info
    for (IAgentTool tool : toolRegistry.getAllTools()) {
      info.append("- ").append(tool.name());
      // Description is not available in IAgentTool interface
      info.append("\n");
    }

    return info.toString();
  }


}
