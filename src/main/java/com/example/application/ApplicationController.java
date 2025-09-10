package com.example.application;

import com.example.agent.service.AgentService;
import com.example.agent.service.ServiceResponse;
import com.example.agent.service.ToolExecutionResult;
import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.model.DrawingSession;
import com.example.domain.models.DrawingState;
import com.example.domain.services.CommandExecutor;
import com.example.domain.services.StateService;
import com.example.domain.tools.ToolInitializer;
import com.example.events.DomainEvent;
import com.example.events.EventDispatcher;
import com.example.events.EventType;
import com.example.events.SystemEvent;
import com.example.events.UIEvent;
import com.example.services.FileStateRepository;
import com.example.services.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central controller that bridges UI events with domain logic and agent services. Handles the main
 * application workflow and coordinates between different layers.
 */
public class ApplicationController {
  private static final Logger LOGGER = Logger.getLogger(ApplicationController.class.getName());
  private static final String READY = "Ready";
  private static final String CMD_SAVE = "save";
  private static final String CMD_LOAD = "load";
  private static final String CMD_LIST = "list";

  private final EventDispatcher eventDispatcher;
  private final StateService stateService;
  private final CommandExecutor commandExecutor;
  private final CommandParser commandParser;
  private final CommandFactory commandFactory;
  private final SessionManager sessionManager;
  private AgentService agentService; // M2: AI integration
  // M3: Approval workflow state
  private DrawingState pendingApprovalState;
  private String pendingApprovalSummary;
  private String pendingApprovalDiff;

  // UI Callback interface for updating the UI
  public interface UIUpdateCallback {
    void updateStatus(String status);

    void addChatMessage(String message);

    void refreshCanvas();

    void showError(String error);

    // M3: Approval overlay controls
    void showApproval(String summary, String diffText);

    void clearApproval();
  }

  private UIUpdateCallback uiCallback;

  /** Initialize the application controller with necessary services. */
  public ApplicationController() {
    // Initialize event system
    this.eventDispatcher = new EventDispatcher(true); // Enable async processing

    // Initialize domain services
    this.stateService = new StateService();
    this.commandExecutor = new CommandExecutor(stateService);

    // Initialize command parser and factory
    this.commandParser = new CommandParser();
    this.commandFactory = new CommandFactory();

    // Initialize session manager (handles repository init internally)
    FileStateRepository repository = new FileStateRepository();
    this.sessionManager =
        new SessionManager(stateService.getStateManager(), stateService.getHistoryService(), repository);

    // Initialize domain tools
    ToolInitializer.initialize();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(
          "Domain tools initialized with " + ToolInitializer.getRegisteredToolCount() + " tools");
    }

    // Initialize AgentService (if API keys configured)
    try {
      this.agentService = new AgentService();
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("AgentService initialized (AI client available)");
      }
    } catch (Exception e) {
      // AI unavailable is OK for offline use; fall back to parser/commands only
      this.agentService = null;
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "AgentService initialization failed (AI disabled): " + e.getMessage());
      }
    }

    // Set up event handlers
    setupEventHandlers();

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("ApplicationController initialized");
    }
  }

  /**
   * Set the UI update callback for communicating with the UI layer.
   *
   * @param callback The UI update callback
   */
  public void setUICallback(UIUpdateCallback callback) {
    this.uiCallback = callback;
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("UI callback registered");
    }
  }

  /** Start the application controller. */
  public void start() {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("ApplicationController starting...");
    }

    // Initialize drawing session
    DrawingSession session = new DrawingSession();
    stateService.initializeSession(session);

    // Enable auto-save per M4 (5 minutes)
    try {
      sessionManager.setAutoSaveInterval(300);
      sessionManager.setAutoSaveEnabled(true);
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Failed to enable auto-save: " + e.getMessage(), e);
      }
    }

    // Update UI status
    updateUIStatus(READY);
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("ApplicationController started successfully");
    }
  }

  /** Shutdown the application controller. */
  public void shutdown() {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("ApplicationController shutting down...");
    }
    eventDispatcher.shutdown();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("ApplicationController shutdown complete");
    }
  }

  /** Setup event handlers for different event types. */
  private void setupEventHandlers() {
    // UI Event Handlers
    eventDispatcher.subscribe(EventType.UI_CHAT_INPUT_SUBMITTED, this::handleChatInput);
    eventDispatcher.subscribe(EventType.UI_CANVAS_CLEARED, this::handleCanvasClear);
    eventDispatcher.subscribe(EventType.UI_MOUSE_PRESSED, this::handleMousePressed);
    eventDispatcher.subscribe(EventType.UI_MOUSE_RELEASED, this::handleMouseReleased);
    eventDispatcher.subscribe(EventType.UI_MOUSE_DRAGGED, this::handleMouseDragged);
    // Approval UI events (M3)
    eventDispatcher.subscribe(EventType.UI_APPROVAL_ACCEPTED, this::handleApprovalAccepted);
    eventDispatcher.subscribe(EventType.UI_APPROVAL_REJECTED, this::handleApprovalRejected);

    // Domain Event Handlers
    eventDispatcher.subscribe(EventType.DOMAIN_STATE_CHANGED, this::handleStateChanged);
    eventDispatcher.subscribe(EventType.DOMAIN_COMMAND_EXECUTED, this::handleCommandExecuted);

    // System Event Handlers
    eventDispatcher.subscribe(EventType.SYSTEM_ERROR_OCCURRED, this::handleSystemError);

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Event handlers set up");
    }
  }

  /** Handle chat input from the user. */
  private void handleChatInput(UIEvent event) {
    try {
      String inputText = event.getInputText();
      if (inputText == null || inputText.isBlank()) {
        return;
      }

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Processing chat input: " + inputText);
      }
      updateUIStatus("Processing...");
      addChatMessage("User: " + inputText);

      // Parse the command
      ParsedCommand parsedCommand = commandParser.parse(inputText.trim());

      if (parsedCommand.isValid()) {
        // Execute the parsed command
        executeCommand(parsedCommand);
      } else {
        // Send to AI agent for processing (M2)
        processWithAgent(inputText);
      }

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error handling chat input", e);
      }
      handleError("Error processing your message: " + e.getMessage());
    }
  }

  /** Handle canvas clear request. */
  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void handleCanvasClear(UIEvent event) {
    try {
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Clearing canvas");
      }

      ClearCanvasCommand command = new ClearCanvasCommand();
      commandExecutor.execute(command);

      // Dispatch domain event
      DomainEvent domainEvent =
          new DomainEvent(EventType.DOMAIN_CANVAS_CLEARED, "ClearCanvasCommand", "canvas");
      eventDispatcher.dispatchDomainEvent(domainEvent);

      addChatMessage("System: Canvas cleared");
      updateUIStatus("Canvas cleared");
      refreshUI();

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error clearing canvas", e);
      }
      handleError("Error clearing canvas: " + e.getMessage());
    }
  }

  /** Handle mouse pressed events (for drawing). */
  private void handleMousePressed(UIEvent event) {
    // For now, just log the event
    // Future implementation could handle direct drawing
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Mouse pressed at: " + event.getMouseX() + ", " + event.getMouseY());
    }
  }

  /** Handle mouse released events. */
  private void handleMouseReleased(UIEvent event) {
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Mouse released at: " + event.getMouseX() + ", " + event.getMouseY());
    }
  }

  /** Handle mouse dragged events. */
  private void handleMouseDragged(UIEvent event) {
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Mouse dragged to: " + event.getMouseX() + ", " + event.getMouseY());
    }
  }

  /** Handle domain state changes. */
  private void handleStateChanged(DomainEvent event) {
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Domain state changed: " + event.getEntityId());
    }
    refreshUI();
  }

  /** Handle command execution events. */
  private void handleCommandExecuted(DomainEvent event) {
    String commandType = event.getCommandType();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Command executed: " + commandType);
    }

    addChatMessage("System: Executed " + commandType);
    updateUIStatus(READY);
    refreshUI();
  }

  /** Handle system errors. */
  private void handleSystemError(SystemEvent event) {
    String errorMessage = event.getErrorMessage();
    if (LOGGER.isLoggable(Level.SEVERE)) {
      LOGGER.severe("System error occurred: " + errorMessage);
    }
    handleError("System error: " + errorMessage);
  }

  /** Execute a parsed command. */
  private void executeCommand(ParsedCommand parsedCommand) {
    try {
      String commandType = parsedCommand.getType();

      // Handle file operations separately from drawing commands
      if (CMD_SAVE.equals(commandType)) {
        executeSaveCommand(parsedCommand);
        return;
      } else if (CMD_LOAD.equals(commandType)) {
        executeLoadCommand(parsedCommand);
        return;
      } else if (CMD_LIST.equals(commandType)) {
        executeListCommand(parsedCommand);
        return;
      }

      // Handle drawing commands via factory
      DrawCommand command = commandFactory.createCommandFromParsed(parsedCommand);
      if (command != null) {
        commandExecutor.execute(command);

        // Dispatch domain event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("command", parsedCommand);
        DomainEvent domainEvent = new DomainEvent(EventType.DOMAIN_COMMAND_EXECUTED, "command",
            command.getClass().getSimpleName(), eventData, null, null);
        eventDispatcher.dispatchDomainEvent(domainEvent);
      }
    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error executing command", e);
      }
      handleError("Error executing command: " + e.getMessage());
    }
  }

  /** Process input with the AI agent. */
  private void processWithAgent(String input) {
    try {
      updateUIStatus("AI processing...");

      if (agentService == null) {
        // Fallback: AI unavailable, guide user
        addChatMessage("AI: 現在AI連携は無効です。.envにAPIキーを設定してください。");
        updateUIStatus(READY);
        return;
      }

      // Keep agent's working state in sync with domain
      agentService.setCurrentState(stateService.getCurrentState());

      agentService
          .processInstruction(input)
          .thenAccept(this::handleAgentResponse)
          .exceptionally(
              e -> {
                if (LOGGER.isLoggable(Level.WARNING)) {
                  LOGGER.log(Level.WARNING, "Error processing with agent", e);
                }
                handleError("AI processing error: " + e.getMessage());
                return null;
              });

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error starting agent processing", e);
      }
      handleError("AI start error: " + e.getMessage());
    }
  }

  // Apply AgentService response to domain/UI (M2)
  private void handleAgentResponse(ServiceResponse response) {
    try {
      if (response == null) {
        handleError("Empty response from agent");
        return;
      }

      // Text feedback from AI
      if (response.hasTextResponse()) {
        addChatMessage("AI: " + response.getTextResponse());
      }

      // Apply tool results to domain state
      if (response.hasToolExecutionResults()) {
        DrawingState lastApplied = null;
        for (ToolExecutionResult result : response.getToolExecutionResults()) {
          if (result.isSuccess() && result.hasResultState()) {
            lastApplied = result.getResultState();
          } else if (result.hasErrorMessage()) {
            addChatMessage("System: Tool '" + result.getToolName() + "' failed - " + result.getErrorMessage());
          }
        }
        if (lastApplied != null) {
          if (response.requiresApproval()) {
            // Prepare approval request (M3)
            prepareApproval(stateService.getCurrentState(), lastApplied);
          } else {
            // Immediate apply
            stateService.updateState(lastApplied);
            if (agentService != null) {
              agentService.setCurrentState(lastApplied);
            }
            refreshUI();
            addChatMessage("System: Applied AI tool results");
          }
        }
      }

      updateUIStatus(READY);

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error handling agent response", e);
      }
      handleError("AI response handling error: " + e.getMessage());
    }
  }

  // M3: Build diff and show approval overlay
  private void prepareApproval(DrawingState current, DrawingState proposed) {
    try {
      com.example.agent.diff.DiffService diffService = new com.example.agent.diff.DiffService();
      var diff = diffService.diff(current, proposed);

      this.pendingApprovalState = proposed;
      this.pendingApprovalSummary = diff.summary;
      this.pendingApprovalDiff = diff.unifiedDiff;

      updateUIStatus("Approval required");
      if (uiCallback != null) {
        uiCallback.showApproval(pendingApprovalSummary, pendingApprovalDiff);
      }
      addChatMessage("System: Approval required - " + pendingApprovalSummary);

      // Await user approval asynchronously (M3)
      if (agentService != null) {
        agentService
            .awaitApproval()
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .thenAccept(approved -> {
              if (approved) {
                try {
                  stateService.updateState(pendingApprovalState);
                  agentService.setCurrentState(pendingApprovalState);
                  refreshUI();
                  addChatMessage("System: Changes approved and applied");
                } catch (Exception e) {
                  handleError("Failed to apply approved changes: " + e.getMessage());
                }
              } else {
                addChatMessage("System: Changes rejected");
              }
              clearApprovalState();
            })
            .exceptionally(e -> {
              addChatMessage("System: Approval timed out - changes discarded");
              clearApprovalState();
              return null;
            });
      }
    } catch (Exception e) {
      handleError("Failed to prepare approval: " + e.getMessage());
    }
  }

  // M3: Handle approval accept
  private void handleApprovalAccepted(UIEvent event) {
    if (agentService != null) {
      agentService.completeApproval(true);
    }
  }

  // M3: Handle approval reject
  private void handleApprovalRejected(UIEvent event) {
    if (agentService != null) {
      agentService.completeApproval(false);
    }
  }

  private void clearApprovalState() {
    this.pendingApprovalState = null;
    this.pendingApprovalSummary = null;
    this.pendingApprovalDiff = null;
    updateUIStatus(READY);
    if (uiCallback != null) {
      uiCallback.clearApproval();
    }
  }

  

  

  

  

  

  /** Update UI status through callback. */
  private void updateUIStatus(String status) {
    if (uiCallback != null) {
      uiCallback.updateStatus(status);
    }
  }

  /** Add chat message through callback. */
  private void addChatMessage(String message) {
    if (uiCallback != null) {
      uiCallback.addChatMessage(message);
    }
  }

  /** Refresh UI through callback. */
  private void refreshUI() {
    if (uiCallback != null) {
      uiCallback.refreshCanvas();
    }
  }

  /** Handle errors and update UI. */
  private void handleError(String errorMessage) {
    updateUIStatus("Error occurred");
    if (uiCallback != null) {
      uiCallback.showError(errorMessage);
    }
  }

  /** Get the event dispatcher for external access. */
  public EventDispatcher getEventDispatcher() {
    return eventDispatcher;
  }

  /** Get the current drawing session state. */
  public DrawingSession getCurrentSession() {
    return stateService.getCurrentSession();
  }

  /** Get the state service for direct access to undo/redo operations. */
  public StateService getStateService() {
    return stateService;
  }

  

  /** Execute save command. */
  private void executeSaveCommand(ParsedCommand parsedCommand) {
    try {
      String filename = parsedCommand.getStringParam("filename");
      if (filename == null || filename.isBlank()) {
        handleError("Save command requires a filename");
        return;
      }

      updateUIStatus("Saving session...");

      // Save via SessionManager (session-based persistence)
      sessionManager.saveSessionAs(filename);

      addChatMessage("System: Session saved as '" + filename + "'");
      updateUIStatus(READY);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Session saved successfully: " + filename);
      }

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error saving session", e);
      }
      handleError("Failed to save session: " + e.getMessage());
    }
  }

  /** Execute load command. */
  private void executeLoadCommand(ParsedCommand parsedCommand) {
    try {
      String filename = parsedCommand.getStringParam("filename");
      if (filename == null || filename.isBlank()) {
        handleError("Load command requires a filename");
        return;
      }

      updateUIStatus("Loading session...");

      // Load session via SessionManager
      sessionManager.loadSession(filename);

      // Trigger UI update
      refreshUI();

      int shapeCount = stateService.getCurrentState().getShapes().size();
      addChatMessage(
          "System: Session loaded: '" + filename + "' (" + shapeCount + " shapes)");
      updateUIStatus(READY);

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Session loaded successfully: " + filename);
      }

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error loading session", e);
      }
      handleError("Failed to load session: " + e.getMessage());
    }
  }

  /** Execute list command. */
  @SuppressWarnings("PMD.UnusedFormalParameter")
  private void executeListCommand(ParsedCommand parsedCommand) {
    try {
      updateUIStatus("Listing saved sessions...");

      // Get list of saved sessions
      List<String> savedFiles = sessionManager.listSessions();

      if (savedFiles.isEmpty()) {
        addChatMessage("System: No saved sessions found");
      } else {
        StringBuilder listMessage = new StringBuilder("System: Saved sessions (");
        listMessage.append(savedFiles.size()).append("):");
        for (String filename : savedFiles) {
          listMessage.append("\n  - ").append(filename);
        }
        addChatMessage(listMessage.toString());
      }

      updateUIStatus(READY);
      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Listed " + savedFiles.size() + " saved sessions");
      }

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error listing sessions", e);
      }
      handleError("Failed to list sessions: " + e.getMessage());
    }
  }
}
