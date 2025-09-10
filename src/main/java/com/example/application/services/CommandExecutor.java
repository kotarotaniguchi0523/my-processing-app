package com.example.application.services;

import com.example.domain.commands.AddShapeCommand;
import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.commands.ModifyShapeCommand;
import com.example.domain.commands.RemoveShapeCommand;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for executing drawing commands against the state service. Implements the Command pattern
 * to execute various drawing operations.
 */
public class CommandExecutor {
  private static final Logger LOGGER = Logger.getLogger(CommandExecutor.class.getName());
  
  private static CommandExecutor instance;

  private final StateService stateService;

  public CommandExecutor(StateService stateService) {
    this.stateService = stateService;
  }
  
  public static synchronized CommandExecutor getInstance() {
    if (instance == null) {
      // Check if StateService is initialized
      try {
        instance = new CommandExecutor(StateService.getInstance());
      } catch (IllegalStateException e) {
        throw new IllegalStateException("CommandExecutor requires StateService to be initialized first", e);
      }
    }
    return instance;
  }

  /** Execute a drawing command. */
    public void execute(DrawCommand command) {
        if (command == null) {
            LOGGER.warning("Attempted to execute null command");
            return;
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Executing command: " + command.getClass().getSimpleName());
        }

        try {
            // Guard: ensure command is executable given current state
            if (!canExecute(command)) {
                LOGGER.warning("Command rejected by canExecute(): " + command.getClass().getSimpleName());
                return;
            }
            if (command instanceof AddShapeCommand) {
                executeAddShape((AddShapeCommand) command);
            } else if (command instanceof RemoveShapeCommand) {
                executeRemoveShape((RemoveShapeCommand) command);
            } else if (command instanceof ModifyShapeCommand) {
        executeModifyShape((ModifyShapeCommand) command);
      } else if (command instanceof ClearCanvasCommand) {
        executeClearCanvas((ClearCanvasCommand) command);
      } else {
        // Try to execute the command using its own execution logic
        DrawingState currentState = stateService.getCurrentState();
        DrawingState newState = command.execute(currentState);
        stateService.updateState(newState);
      }

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Command executed successfully: " + command.getClass().getSimpleName());
      }

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.SEVERE)) {
        LOGGER.log(Level.SEVERE, "Error executing command: " + command.getClass().getSimpleName(),
            e);
      }
      throw new CommandExecutionException("Failed to execute command: " + e.getMessage(), e);
    }
  }

  /** Execute an AddShapeCommand. */
  private void executeAddShape(AddShapeCommand command) {
    Shape shape = command.getShape();
    stateService.addShape(shape);
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Added shape: " + shape.getId() + " (" + shape.getType() + ")");
    }
  }

  /** Execute a RemoveShapeCommand. */
  private void executeRemoveShape(RemoveShapeCommand command) {
    String shapeId = command.getShapeId();
    stateService.removeShape(shapeId);
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Removed shape: " + shapeId);
    }
  }

  /** Execute a ModifyShapeCommand. */
  private void executeModifyShape(ModifyShapeCommand command) {
    DrawingState currentState = stateService.getCurrentState();
    DrawingState newState = command.execute(currentState);
    stateService.updateState(newState);
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Modified shape: " + command.getShapeId());
    }
  }

  /** Execute a ClearCanvasCommand. */
  private void executeClearCanvas(ClearCanvasCommand command) {
    stateService.clearAllShapes();
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Canvas cleared");
    }
  }

  /** Check if a command can be executed. */
  public boolean canExecute(DrawCommand command) {
    if (command == null) {
      return false;
    }

    try {
      // Basic validation - can be extended based on command type
      if (command instanceof AddShapeCommand) {
        AddShapeCommand addCommand = (AddShapeCommand) command;
        return addCommand.getShape() != null;
      } else if (command instanceof RemoveShapeCommand) {
        RemoveShapeCommand removeCommand = (RemoveShapeCommand) command;
        return removeCommand.getShapeId() != null
            && stateService.findShapeById(removeCommand.getShapeId()) != null;
      } else if (command instanceof ModifyShapeCommand) {
        ModifyShapeCommand modifyCommand = (ModifyShapeCommand) command;
        return modifyCommand.getShapeId() != null
            && stateService.findShapeById(modifyCommand.getShapeId()) != null;
      } else if (command instanceof ClearCanvasCommand) {
        return true; // Clear command can always be executed
      }

      return true; // Default to true for other command types

    } catch (Exception e) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(Level.WARNING, "Error checking if command can be executed", e);
      }
      return false;
    }
  }

  /** Get the state service. */
  public StateService getStateService() {
    return stateService;
  }

  /** Exception thrown when command execution fails. */
  public static class CommandExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CommandExecutionException(String message) {
      super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
