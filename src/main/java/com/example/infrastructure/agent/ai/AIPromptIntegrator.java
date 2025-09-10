package com.example.infrastructure.agent.ai;

import com.example.infrastructure.ai.*;
import com.example.infrastructure.agent.conversation.ConversationHistory;
import com.example.infrastructure.agent.conversation.ConversationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Integrates the PromptBuilder with AI clients to facilitate tool-aware conversations. Handles the
 * conversion between prompt formats and AI client request/response formats.
 */
public class AIPromptIntegrator {


  private final AIClient aiClient;
  private final PromptBuilder promptBuilder;

  public AIPromptIntegrator(AIClient aiClient) {
    this(aiClient, new PromptBuilder());
  }

  public AIPromptIntegrator(AIClient aiClient, PromptBuilder promptBuilder) {
    this.aiClient = aiClient;
    this.promptBuilder = promptBuilder;
  }

  /**
   * Sends a user message to the AI and gets a response
   *
   * @param userMessage The user's message
   * @return The AI's response
   */
  public CompletableFuture<AIResponse> sendMessage(String userMessage) {
    // Add user message to history
    promptBuilder.addUserMessage(userMessage);

    // Build chat request with tool definitions
    ChatRequest request = buildChatRequestWithTools();

    // Send to AI with tool support
    return aiClient.chatCompletion(request).thenApply(response -> {
      // Add assistant response to history
      if (response.getContent() != null) {
        promptBuilder.addAssistantResponse(response.getContent());
      }
      return response;
    });
  }

  /**
   * Processes a user message and handles tool calls if present
   *
   * @param userMessage The user's message
   * @return ProcessedResponse containing the result and any tool calls
   */
  public CompletableFuture<ProcessedResponse> processMessage(String userMessage) {
    return sendMessage(userMessage).thenApply(this::processAIResponse);
  }

  /**
   * Builds a ChatRequest from the current conversation state
   *
   * @return The chat request
   */

  /**
   * Builds a ChatRequest with tool definitions for function calling
   *
   * @return The chat request with tools
   */
  private ChatRequest buildChatRequestWithTools() {
    ChatRequest.Builder requestBuilder = ChatRequest.builder();

    // Build simple system prompt (tools will be handled separately)
    String systemPrompt =
        "You are a drawing AI that creates visual compositions using tool calls.\n"
            + "\nCanvas dimensions: 980x335 pixels (center at x=490, y=167)\n"
            + "\nYour task: Interpret user requests and create drawings using ONLY tool calls.\n"
            + "\nAvailable tool: createShape\n" + "Parameters:\n"
            + "- type: 'circle', 'rectangle', 'triangle', 'line', or 'text'\n"
            + "- position: {x: number, y: number}\n"
            + "- size: {width: number, height: number} for rect/triangle, {radius: number} for circle\n"
            + "- color: color name ('red', 'blue', etc.) or hex code\n"
            + "\nExample: To draw a red circle at center:\n"
            + "Use createShape with: type='circle', position={x:490, y:167}, size={radius:50}, color='red'\n"
            + "\nRules:\n" + "1. ALWAYS use tool calls, never plain text responses\n"
            + "2. Break complex objects into multiple simple shapes\n"
            + "3. Calculate positions to arrange shapes properly\n"
            + "4. Use multiple tool calls for composite drawings";
    requestBuilder.addSystemMessage(systemPrompt);

    // Add conversation history
    ConversationHistory history = promptBuilder.getConversationHistory();
    for (ConversationMessage msg : history.getMessages()) {
      switch (msg.getRole()) {
        case USER:
          requestBuilder.addUserMessage(msg.getContent());
          break;
        case ASSISTANT:
          requestBuilder.addAssistantMessage(msg.getContent());
          break;
        case SYSTEM:
          requestBuilder.addSystemMessage(msg.getContent());
          break;
        case TOOL_RESULT:
          requestBuilder.addUserMessage("[Tool Result] " + msg.getContent());
          break;
      }
    }

    // Add tool definitions for function calling
    addToolDefinitions(requestBuilder);

    // Set parameters
    requestBuilder.temperature(0.7);
    requestBuilder.maxTokens(2000);

    // Use a model that supports function calling
    requestBuilder.model("gemini-2.5-flash");

    return requestBuilder.build();
  }

  /**
   * Add tool definitions to the chat request
   *
   * @param requestBuilder The request builder to add tools to
   */
  private void addToolDefinitions(ChatRequest.Builder requestBuilder) {
    // Note: Tool definitions are handled at the AIClient level
    // The ChatRequest doesn't directly support tools - this is
    // implemented in the AI client

    // We don't need to add tool descriptions as they're already in the system prompt
    // The actual tool definitions are handled by the AI client
  }

  /**
   * Get description for a tool
   */


  /**
   * Processes an AI response to extract tool calls
   *
   * @param response The AI response
   * @return Processed response with extracted information
   */
  private ProcessedResponse processAIResponse(AIResponse response) {
    ProcessedResponse processed = new ProcessedResponse();
    processed.setRawResponse(response);

    if (response.hasToolCalls()) {
      // Handle explicit tool calls from AI
      processed.setToolCalls(response.getToolCalls());
    } else if (response.getContent() != null) {
      // Don't try to extract tool calls from content - Gemini already handles this
      // Just set the text response
      processed.setTextResponse(response.getContent());
    }

    return processed;
  }

  /**
   * Attempts to extract tool calls from text content (JSON format)
   *
   * @param content The response content
   * @return List of extracted tool calls
   */


  /**
   * Extracts tool calls from JSON code blocks (```json ... ```)
   */


  /** Parses a single tool call object */



  /**
   * Adds a tool execution result to the conversation
   *
   * @param toolName The executed tool name
   * @param result The execution result
   */
  public void addToolResult(String toolName, String result) {
    promptBuilder.addToolResult(toolName, result);
  }

  /**
   * Clears the conversation history
   */
  public void clearConversation() {
    promptBuilder.clearHistory();
  }

  /**
   * Gets the current conversation history
   *
   * @return The conversation history
   */
  public ConversationHistory getConversationHistory() {
    return promptBuilder.getConversationHistory();
  }

  /**
   * Gets the prompt builder
   *
   * @return The prompt builder instance
   */
  public PromptBuilder getPromptBuilder() {
    return promptBuilder;
  }

  /**
   * Represents a processed AI response with extracted tool calls
   */
  public static class ProcessedResponse {
    private AIResponse rawResponse;
    private String textResponse;
    private List<AIResponse.ToolCall> toolCalls;

    public ProcessedResponse() {
      this.toolCalls = new ArrayList<>();
    }

    public AIResponse getRawResponse() {
      return rawResponse;
    }

    public void setRawResponse(AIResponse rawResponse) {
      this.rawResponse = rawResponse;
    }

    public String getTextResponse() {
      return textResponse;
    }

    public void setTextResponse(String textResponse) {
      this.textResponse = textResponse;
    }

    public List<AIResponse.ToolCall> getToolCalls() {
      return toolCalls;
    }

    public void setToolCalls(List<AIResponse.ToolCall> toolCalls) {
      this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
    }

    public boolean hasToolCalls() {
      return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean hasTextResponse() {
      return textResponse != null && !textResponse.trim().isEmpty();
    }
  }
}
