package com.example.application;

import com.example.domain.commands.AddShapeCommand;
import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.model.DrawingSession;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.services.CommandExecutor;
import com.example.domain.services.StateService;
import com.example.domain.tools.ToolCommand;
import com.example.domain.tools.ToolInitializer;
import com.example.domain.tools.impl.ModifyShapeTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.events.DomainEvent;
import com.example.events.EventDispatcher;
import com.example.events.EventType;
import com.example.events.SystemEvent;
import com.example.events.UIEvent;
import com.example.services.FileStateRepository;
import com.example.services.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
  private static final String COLOR = "color";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  private static final String SIZE = "size";
  private static final String POSITION = "position";
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

  /** Create a draw command from parsed command data. */
  private DrawCommand createCommandFromParsed(ParsedCommand parsedCommand) {
    switch (parsedCommand.getType()) {
      case "circle":
        return createCircleCommand(parsedCommand);
      case "rectangle":
        return createRectangleCommand(parsedCommand);
      case "clear":
        return new ClearCanvasCommand();
      case "modify":
        return createModifyCommand(parsedCommand);
      default:
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Unknown command type: " + parsedCommand.getType());
        }
        return null;
    }
  }

  /** Create a circle command from parsed data. */
  private AddShapeCommand createCircleCommand(ParsedCommand parsedCommand) {
    try {
      float x = parsedCommand.getFloatParam("x", 100.0f);
      float y = parsedCommand.getFloatParam("y", 100.0f);
      float diameter = parsedCommand.getFloatParam("diameter", 50.0f);
      String color = parsedCommand.getStringParam(COLOR, "black");

      Position position = new Position(x, y);
      Color fillColor = parseColor(color);
      Style style = Style.builder().fillColor(fillColor).build();
      Circle circle = new Circle(position, diameter / 2, style); // diameter to radius
      return new AddShapeCommand(circle);

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Error creating circle command: " + e.getMessage());
      }
      return null;
    }
  }

  /** Create a rectangle command from parsed data. */
  private AddShapeCommand createRectangleCommand(ParsedCommand parsedCommand) {
    try {
      float x = parsedCommand.getFloatParam("x", 100.0f);
      float y = parsedCommand.getFloatParam("y", 100.0f);
      float width = parsedCommand.getFloatParam(WIDTH, 100.0f);
      float height = parsedCommand.getFloatParam(HEIGHT, 50.0f);
      String color = parsedCommand.getStringParam(COLOR, "black");

      Position position = new Position(x, y);
      Color fillColor = parseColor(color);
      Style style = Style.builder().fillColor(fillColor).build();
      Rectangle rectangle = new Rectangle(position, width, height, style);
      return new AddShapeCommand(rectangle);

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Error creating rectangle command: " + e.getMessage());
      }
      return null;
    }
  }

  /** Create a modify command from parsed data. */
  private ToolCommand createModifyCommand(ParsedCommand parsedCommand) {
    try {
      // Validate required parameters
      String shapeId = parsedCommand.getStringParam("id");
      String property = parsedCommand.getStringParam("property");

      if (shapeId == null || shapeId.isBlank()) {
        throw new IllegalArgumentException("Shape ID is required for modify command");
      }

      if (property == null || property.isBlank()) {
        throw new IllegalArgumentException("Property type is required for modify command");
      }

      // Validate property type
      if (!isValidProperty(property)) {
        throw new IllegalArgumentException(
            "Invalid property type: " + property + ". Valid properties are: color, position, size");
      }

      // Build parameters map for ModifyShapeTool
      Map<String, Object> parameters = buildModifyParameters(parsedCommand);

      // Create the modify tool and tool command
      ModifyShapeTool modifyTool = new ModifyShapeTool();
      return new ToolCommand(modifyTool, parameters);

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.warning("Error creating modify command: " + e.getMessage());
      }
      handleError("Error with modify command: " + e.getMessage());
      return null;
    }
  }

  /** Check if a property type is valid for modification. */
  private boolean isValidProperty(String property) {
    return COLOR.equals(property) || POSITION.equals(property) || SIZE.equals(property);
  }

  /** Build parameters for ModifyShapeTool from parsed command data. */
  private Map<String, Object> buildModifyParameters(ParsedCommand parsedCommand) {
    Map<String, Object> parameters = new HashMap<>();

    // Add shape identifier
    String shapeId = parsedCommand.getStringParam("id");
    if (shapeId != null) {
      parameters.put("id", shapeId);
    }

    // Get the property being modified
    String property = parsedCommand.getStringParam("property");

    switch (property) {
      case COLOR:
        // Build style modification
        Map<String, Object> style = new HashMap<>();
        String colorValue = parsedCommand.getStringParam(COLOR);
        if (colorValue != null) {
          Color color = parseColor(colorValue);
          Map<String, Object> colorMap = new HashMap<>();
          colorMap.put("r", color.getRed());
          colorMap.put("g", color.getGreen());
          colorMap.put("b", color.getBlue());
          colorMap.put("a", color.getAlpha());
          style.put("fillColor", colorMap);
        }
        parameters.put("style", style);
        break;

      case POSITION:
        // Build position modification
        if (parsedCommand.hasParam("x") && parsedCommand.hasParam("y")) {
          Map<String, Object> position = new HashMap<>();
          position.put("x", parsedCommand.getFloatParam("x", 0));
          position.put("y", parsedCommand.getFloatParam("y", 0));
          parameters.put("position", position);
        } else {
          throw new IllegalArgumentException(
              "Position modification requires both x and y coordinates");
        }
        break;

      case SIZE:
        // Build properties modification for size
        Map<String, Object> properties = new HashMap<>();

        if (parsedCommand.hasParam(SIZE)) {
          // Single size value - could be radius for circle or width/height for rectangle
          float size = parsedCommand.getFloatParam(SIZE, 0);
          if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
          }
          properties.put("radius", size); // For circles
          properties.put(WIDTH, size); // For rectangles/squares
          properties.put(HEIGHT, size); // For rectangles/squares
        } else {
          // Separate width and height
          boolean hasSize = false;
          if (parsedCommand.hasParam(WIDTH)) {
            float width = parsedCommand.getFloatParam(WIDTH, 0);
            if (width <= 0) {
              throw new IllegalArgumentException("Width must be greater than 0");
            }
            properties.put(WIDTH, width);
            hasSize = true;
          }
          if (parsedCommand.hasParam(HEIGHT)) {
            float height = parsedCommand.getFloatParam(HEIGHT, 0);
            if (height <= 0) {
              throw new IllegalArgumentException("Height must be greater than 0");
            }
            properties.put(HEIGHT, height);
            hasSize = true;
          }

          if (!hasSize) {
            throw new IllegalArgumentException(
                "Size modification requires either a size value or width/height" + " values");
          }
        }

        parameters.put("properties", properties);
        break;

      default:
        throw new IllegalArgumentException("Unknown property: " + property);
    }

    return parameters;
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
    if (colorString == null || colorString.isBlank()) {
      return Color.BLACK;
    }

    String color = colorString.trim().toLowerCase(Locale.ROOT);

    // Try hex color first
    if (color.startsWith("#")) {
      try {
        return Color.fromHex(color);
      } catch (Exception e) {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Invalid hex color: " + color + ", using black instead");
        }
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
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.warning("Unknown color: " + color + ", using black instead");
        }
        return Color.BLACK;
    }
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
