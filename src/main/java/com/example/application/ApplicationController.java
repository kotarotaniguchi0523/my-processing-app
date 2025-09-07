package com.example.application;

import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.model.DrawingSession;
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

  // UI Callback interface for updating the UI
  public interface UIUpdateCallback {
    void updateStatus(String status);

    void addChatMessage(String message);

    void refreshCanvas();

    void showError(String error);
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
        // Send to AI agent for processing
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
    // Run agent processing asynchronously to avoid blocking UI
    CompletableFuture.runAsync(() -> {
      try {
        updateUIStatus("AI processing...");

        // This is a placeholder for actual agent processing
        // In the real implementation, this would call the agent system
        String response =
            "AI: I received your message: \"" + input + "\". I'm ready to help with drawing!";

        addChatMessage(response);
        updateUIStatus("Ready");

      } catch (Exception e) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.log(Level.WARNING, "Error processing with agent", e);
        }
        handleError("AI processing error: " + e.getMessage());
      }
    });
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
