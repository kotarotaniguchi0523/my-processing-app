package com.example.application;

import com.example.agent.AgentSystemFactory;
import com.example.agent.core.Agent;
import com.example.domain.commands.AddShapeCommand;
import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.model.DrawingSession;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.services.CommandExecutor;
import com.example.domain.services.StateService;
import com.example.domain.tools.ToolInitializer;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.events.DomainEvent;
import com.example.events.EventDispatcher;
import com.example.events.EventType;
import com.example.events.SystemEvent;
import com.example.events.UIEvent;
import java.util.HashMap;
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

  private final EventDispatcher eventDispatcher;
  private final StateService stateService;
  private final CommandExecutor commandExecutor;
  private final Agent agent;
  private final CommandParser commandParser;

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

    // Initialize agent system
    this.agent = AgentSystemFactory.createDrawingAgent();

    // Initialize command parser
    this.commandParser = new CommandParser();

    // Initialize domain tools
    ToolInitializer.initialize();
    LOGGER.info("Domain tools initialized with " + ToolInitializer.getRegisteredToolCount() + " tools");

    // Set up event handlers
    setupEventHandlers();

    LOGGER.info("ApplicationController initialized");
  }

  /**
   * Set the UI update callback for communicating with the UI layer.
   *
   * @param callback The UI update callback
   */
  public void setUICallback(UIUpdateCallback callback) {
    this.uiCallback = callback;
    LOGGER.fine("UI callback registered");
  }

  /** Start the application controller. */
  public void start() {
    LOGGER.info("ApplicationController starting...");

    // Initialize drawing session
    DrawingSession session = new DrawingSession();
    stateService.initializeSession(session);

    // Update UI status
    updateUIStatus("Ready");

    LOGGER.info("ApplicationController started successfully");
  }

  /** Shutdown the application controller. */
  public void shutdown() {
    LOGGER.info("ApplicationController shutting down...");
    eventDispatcher.shutdown();
    LOGGER.info("ApplicationController shutdown complete");
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

    LOGGER.fine("Event handlers set up");
  }

  /** Handle chat input from the user. */
  private void handleChatInput(UIEvent event) {
    try {
      String inputText = event.getInputText();
      if (inputText == null || inputText.trim().isEmpty()) {
        return;
      }

      LOGGER.info("Processing chat input: " + inputText);
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
      LOGGER.log(Level.SEVERE, "Error handling chat input", e);
      handleError("Error processing your message: " + e.getMessage());
    }
  }

  /** Handle canvas clear request. */
  private void handleCanvasClear(UIEvent event) {
    try {
      LOGGER.info("Clearing canvas");

      ClearCanvasCommand command = new ClearCanvasCommand();
      commandExecutor.execute(command);

      // Dispatch domain event
      DomainEvent domainEvent =
          new DomainEvent(EventType.DOMAIN_CANVAS_CLEARED, "ClearCanvasCommand", "canvas");
      eventDispatcher.dispatchDomainEvent(domainEvent);

      addChatMessage("System: Canvas cleared");
      updateUIStatus("Canvas cleared");

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error clearing canvas", e);
      handleError("Error clearing canvas: " + e.getMessage());
    }
  }

  /** Handle mouse pressed events (for drawing). */
  private void handleMousePressed(UIEvent event) {
    // For now, just log the event
    // Future implementation could handle direct drawing
    LOGGER.fine("Mouse pressed at: " + event.getMouseX() + ", " + event.getMouseY());
  }

  /** Handle mouse released events. */
  private void handleMouseReleased(UIEvent event) {
    LOGGER.fine("Mouse released at: " + event.getMouseX() + ", " + event.getMouseY());
  }

  /** Handle mouse dragged events. */
  private void handleMouseDragged(UIEvent event) {
    LOGGER.fine("Mouse dragged to: " + event.getMouseX() + ", " + event.getMouseY());
  }

  /** Handle domain state changes. */
  private void handleStateChanged(DomainEvent event) {
    LOGGER.fine("Domain state changed: " + event.getEntityId());
    refreshUI();
  }

  /** Handle command execution events. */
  private void handleCommandExecuted(DomainEvent event) {
    String commandType = event.getCommandType();
    LOGGER.info("Command executed: " + commandType);

    addChatMessage("System: Executed " + commandType);
    updateUIStatus("Ready");
    refreshUI();
  }

  /** Handle system errors. */
  private void handleSystemError(SystemEvent event) {
    String errorMessage = event.getErrorMessage();
    LOGGER.severe("System error occurred: " + errorMessage);
    handleError("System error: " + errorMessage);
  }

  /** Execute a parsed command. */
  private void executeCommand(ParsedCommand parsedCommand) {
    try {
      DrawCommand command = createCommandFromParsed(parsedCommand);
      if (command != null) {
        commandExecutor.execute(command);

        // Dispatch domain event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("command", parsedCommand);
        DomainEvent domainEvent =
            new DomainEvent(
                EventType.DOMAIN_COMMAND_EXECUTED,
                "command",
                command.getClass().getSimpleName(),
                eventData,
                null,
                null);
        eventDispatcher.dispatchDomainEvent(domainEvent);
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error executing command", e);
      handleError("Error executing command: " + e.getMessage());
    }
  }

  /** Process input with the AI agent. */
  private void processWithAgent(String input) {
    // Run agent processing asynchronously to avoid blocking UI
    CompletableFuture.runAsync(
        () -> {
          try {
            updateUIStatus("AI processing...");

            // This is a placeholder for actual agent processing
            // In the real implementation, this would call the agent system
            String response =
                "AI: I received your message: \"" + input + "\". I'm ready to help with drawing!";

            addChatMessage(response);
            updateUIStatus("Ready");

          } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing with agent", e);
            handleError("AI processing error: " + e.getMessage());
          }
        });
  }

  /** Create a draw command from parsed command data. */
  private DrawCommand createCommandFromParsed(ParsedCommand parsedCommand) {
    switch (parsedCommand.getType()) {
      case "circle":
        return createCircleCommand(parsedCommand);
      case "rectangle":
        return createRectangleCommand(parsedCommand);
      case "clear":
        return new ClearCanvasCommand();
      default:
        LOGGER.warning("Unknown command type: " + parsedCommand.getType());
        return null;
    }
  }

  /** Create a circle command from parsed data. */
  private AddShapeCommand createCircleCommand(ParsedCommand parsedCommand) {
    try {
      float x = parsedCommand.getFloatParam("x", 100.0f);
      float y = parsedCommand.getFloatParam("y", 100.0f);
      float diameter = parsedCommand.getFloatParam("diameter", 50.0f);
      String color = parsedCommand.getStringParam("color", "black");

      Position position = new Position(x, y);
      Color fillColor = parseColor(color);
      Style style = Style.builder().fillColor(fillColor).build();
      Circle circle = new Circle(position, diameter / 2, style); // diameter to radius
      return new AddShapeCommand(circle);

    } catch (Exception e) {
      LOGGER.warning("Error creating circle command: " + e.getMessage());
      return null;
    }
  }

  /** Create a rectangle command from parsed data. */
  private AddShapeCommand createRectangleCommand(ParsedCommand parsedCommand) {
    try {
      float x = parsedCommand.getFloatParam("x", 100.0f);
      float y = parsedCommand.getFloatParam("y", 100.0f);
      float width = parsedCommand.getFloatParam("width", 100.0f);
      float height = parsedCommand.getFloatParam("height", 50.0f);
      String color = parsedCommand.getStringParam("color", "black");

      Position position = new Position(x, y);
      Color fillColor = parseColor(color);
      Style style = Style.builder().fillColor(fillColor).build();
      Rectangle rectangle = new Rectangle(position, width, height, style);
      return new AddShapeCommand(rectangle);

    } catch (Exception e) {
      LOGGER.warning("Error creating rectangle command: " + e.getMessage());
      return null;
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

  /**
   * Parse a color string into a Color object. Supports hex colors (#FF0000) and basic color names.
   */
  private Color parseColor(String colorString) {
    if (colorString == null || colorString.trim().isEmpty()) {
      return Color.BLACK;
    }

    String color = colorString.trim().toLowerCase();

    // Try hex color first
    if (color.startsWith("#")) {
      try {
        return Color.fromHex(color);
      } catch (Exception e) {
        LOGGER.warning("Invalid hex color: " + color + ", using black instead");
        return Color.BLACK;
      }
    }

    // Basic color names
    switch (color) {
      case "black":
        return Color.BLACK;
      case "white":
        return Color.WHITE;
      case "red":
        return Color.RED;
      case "green":
        return Color.GREEN;
      case "blue":
        return Color.BLUE;
      case "yellow":
        return new Color(255, 255, 0);
      case "purple":
        return new Color(128, 0, 128);
      case "orange":
        return new Color(255, 165, 0);
      case "pink":
        return new Color(255, 192, 203);
      case "brown":
        return new Color(165, 42, 42);
      case "gray":
      case "grey":
        return new Color(128, 128, 128);
      default:
        LOGGER.warning("Unknown color: " + color + ", using black instead");
        return Color.BLACK;
    }
  }
}
