package com.example.application;

import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.commands.ParsedCommand;
import com.example.domain.models.DrawingSession;
import com.example.domain.models.DrawingState;
import com.example.application.services.CommandExecutor;
import com.example.domain.services.IAIService;
import com.example.domain.services.IDiffService;
import com.example.application.services.StateService;
import com.example.infrastructure.adapters.AIServiceAdapter;
import com.example.infrastructure.adapters.DiffServiceAdapter;
import com.example.application.tools.ToolInitializer;
import com.example.domain.events.DomainEvent;
import com.example.infrastructure.events.EventDispatcher;
import com.example.domain.events.EventType;
import com.example.domain.events.SystemEvent;
import com.example.domain.events.UIEvent;
import com.example.infrastructure.services.FileStateRepository;
import com.example.infrastructure.services.SessionManager;

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
  private IAIService aiService; // M2: AI integration
  private IDiffService diffService; // M3: Diff generation
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
    // Initialize infrastructure
    com.example.infrastructure.services.StateManager stateManager = new com.example.infrastructure.services.StateManager();
    com.example.infrastructure.services.HistoryService historyService = new com.example.infrastructure.services.HistoryService(stateManager);
    
    // Initialize domain services with infrastructure implementations
    StateService.initialize(
        new com.example.infrastructure.adapters.StateManagerAdapter(stateManager),
        new com.example.infrastructure.adapters.HistoryServiceAdapter(historyService)
    );
    
    this.stateService = StateService.getInstance();
    this.commandExecutor = new CommandExecutor(stateService);

    // Initialize command parser and factory
    this.commandParser = new CommandParser();
    this.commandFactory = new CommandFactory();

    // Initialize session manager (handles repository init internally)
    FileStateRepository repository = new FileStateRepository();
    this.sessionManager = new SessionManager(stateManager, historyService, repository);

    // Initialize domain tools
    ToolInitializer.initialize();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info(
          "Domain tools initialized with " + ToolInitializer.getRegisteredToolCount() + " tools");
    }

    // Initialize AI Service (if API keys configured)
    try {
      AIServiceAdapter aiAdapter = new AIServiceAdapter();
      if (aiAdapter.isAvailable()) {
        this.aiService = aiAdapter;
        // Stream AI thinking logs into the chat interface
        this.aiService.setProgressListener(message -> {
          try {
            addChatMessage("AI (thinking): " + message);
          } catch (Exception ignored) {
            // keep robust if UI not ready yet
          }
        });
        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info("AI Service initialized (AI client available)");
        }
      } else {
        this.aiService = null;
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.log(Level.WARNING, "AI Service unavailable (offline mode)");
        }
      }
    } catch (Exception e) {
      // AI unavailable is OK for offline use; fall back to parser/commands only
      this.aiService = null;
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "AI Service initialization failed (AI disabled): " + e.getMessage());
      }
    }
    
    // Initialize Diff Service
    this.diffService = new DiffServiceAdapter();

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

      if (aiService == null) {
        // Fallback: AI unavailable, guide user
        addChatMessage("AI: 現在AI連携は無効です。.envにAPIキーを設定してください。");
        updateUIStatus(READY);
        return;
      }

      // Keep agent's working state in sync with domain
      aiService.setCurrentState(stateService.getCurrentState());

      aiService
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

  // Apply AI service response to domain/UI (M2)
  private void handleAgentResponse(IAIService.AIServiceResponse response) {
    try {
      if (response == null) {
        handleError("Empty response from agent");
        return;
      }

      // Text feedback from AI
      if (response.hasTextResponse()) {
        addChatMessage("AI: " + response.getTextResponse());
      }

      // Apply result state from AI response
      if (response.hasResultState()) {
        DrawingState resultState = response.getResultState();
        if (response.requiresApproval()) {
          // Prepare approval request (M3)
          prepareApproval(stateService.getCurrentState(), resultState);
        } else {
          // Immediate apply
          stateService.updateState(resultState);
          if (aiService != null) {
            aiService.setCurrentState(resultState);
          }
          refreshUI();
          addChatMessage("System: Applied AI tool results");
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
      // Use diff service to generate code diff
      var diffResult = diffService.generateDiff(current, proposed);

      this.pendingApprovalState = proposed;
      this.pendingApprovalSummary = diffResult.summary;
      this.pendingApprovalDiff = diffResult.unifiedDiff;

      updateUIStatus("Approval required");
      if (uiCallback != null) {
        uiCallback.showApproval(pendingApprovalSummary, pendingApprovalDiff);
      }
      addChatMessage("System: Approval required - " + pendingApprovalSummary);

      // Await user approval asynchronously (M3)
      if (aiService != null) {
        aiService
            .awaitApproval()
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .thenAccept(approved -> {
              if (approved) {
                try {
                  stateService.updateState(pendingApprovalState);
                  aiService.setCurrentState(pendingApprovalState);
                  
                  // Log the state after approval for debugging
                  DrawingState updatedState = stateService.getCurrentState();
                  int shapeCount = updatedState.getShapes().size();
                  LOGGER.info("After approval - Shape count: " + shapeCount);
                  for (var shape : updatedState.getShapes()) {
                    LOGGER.info("  Shape: " + shape.getId() + " type=" + shape.getType() + 
                               " pos=" + shape.getPosition());
                  }
                  
                  refreshUI();
                  addChatMessage("System: Changes approved and applied (" + shapeCount + " shapes)");
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
    if (aiService != null) {
      aiService.completeApproval(true);
    }
  }

  // M3: Handle approval reject
  private void handleApprovalRejected(UIEvent event) {
    if (aiService != null) {
      aiService.completeApproval(false);
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
