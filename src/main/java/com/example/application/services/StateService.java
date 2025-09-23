package com.example.application.services;

import com.example.domain.models.DrawingSession;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.domain.services.IStateManager;
import com.example.domain.services.IHistoryService;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for managing drawing state and session operations. Provides a high-level interface for
 * state management operations.
 */
public class StateService {
  private static final Logger LOGGER = Logger.getLogger(StateService.class.getName());

  private static StateService instance;

  private DrawingSession currentSession;
  private final IStateManager stateManager;
  private final IHistoryService historyService;

  public StateService() {
    // These will be injected via setter or constructor
    this.stateManager = null;
    this.historyService = null;
  }

  public StateService(IStateManager stateManager, IHistoryService historyService) {
    this.stateManager = stateManager;
    this.historyService = historyService;
  }

  public static synchronized StateService getInstance() {
    if (instance == null) {
      // This needs to be properly initialized with dependencies
      throw new IllegalStateException("StateService must be initialized with dependencies");
    }
    return instance;
  }

  public static synchronized void initialize(IStateManager stateManager,
      IHistoryService historyService) {
    instance = new StateService(stateManager, historyService);
  }

  /** Initialize a new drawing session. */
  public void initializeSession(DrawingSession session) {
    this.currentSession = session;
    stateManager.updateState(session.getCurrentState());
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("Drawing session initialized: " + session.getSessionId());
    }
  }

  /** Get the current drawing session. */
  public DrawingSession getCurrentSession() {
    return currentSession;
  }

  /** Get the current drawing state. */
  public DrawingState getCurrentState() {
    // Ensure we have a session
    if (currentSession == null) {
      initializeDefaultSession();
    }
    return currentSession != null ? currentSession.getCurrentState() : DrawingState.defaultState();
  }

  /** Update the current state. */
  public void updateState(DrawingState newState) {
    // Ensure we have a session
    if (currentSession == null) {
      initializeDefaultSession();
    }
    
    if (currentSession != null) {
      // Validate new state before applying
      boolean valid = stateManager.validateState(newState);
      if (!valid) {
        LOGGER.warning("Rejected state update due to validation failure");
        return;
      }
      // Save current state to history before updating
      historyService.saveState(getCurrentState());

      currentSession.updateState(newState);
      stateManager.updateState(newState);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("State updated for session: " + currentSession.getSessionId());
      }
    }
  }
  
  /** Initialize a default session if none exists. */
  private void initializeDefaultSession() {
    if (currentSession == null && stateManager != null) {
      DrawingSession defaultSession = new DrawingSession();
      initializeSession(defaultSession);
      LOGGER.info("Initialized default drawing session");
    }
  }

  /** Add a shape to the current session. */
  public void addShape(Shape shape) {
    if (currentSession != null) {
      historyService.saveState(getCurrentState());
      currentSession.addShape(shape);
      stateManager.updateState(currentSession.getCurrentState());

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Shape added: " + shape.getId() + " (" + shape.getType() + ")");
      }
    }
  }

  /** Remove a shape from the current session. */
  public void removeShape(String shapeId) {
    if (currentSession != null) {
      historyService.saveState(getCurrentState());
      currentSession.removeShape(shapeId);
      stateManager.updateState(currentSession.getCurrentState());

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("Shape removed: " + shapeId);
      }
    }
  }

  /** Clear all shapes from the current session. */
  public void clearAllShapes() {
    if (currentSession != null) {
      historyService.saveState(getCurrentState());
      currentSession.clearAllShapes();
      stateManager.updateState(currentSession.getCurrentState());

      if (LOGGER.isLoggable(Level.INFO)) {
        LOGGER.info("All shapes cleared from session: " + currentSession.getSessionId());
      }
    }
  }

  /** Get all shapes in the current session. */
  public List<Shape> getAllShapes() {
    return currentSession != null ? currentSession.getAllShapes() : List.of();
  }

  /** Find a shape by its ID. */
  public Shape findShapeById(String shapeId) {
    return currentSession != null ? currentSession.findShapeById(shapeId) : null;
  }

  /** Undo the last operation. */
  public boolean undo() {
    if (currentSession != null && historyService.canUndo()) {
      boolean undoSuccess = historyService.undo();
      if (undoSuccess) {
        // Get the current state from state manager after undo
        DrawingState currentState = stateManager.getCurrentState();
        currentSession.updateState(currentState);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info("Undo operation performed");
        }
        return true;
      }
    }
    return false;
  }

  /** Redo the last undone operation. */
  public boolean redo() {
    if (currentSession != null && historyService.canRedo()) {
      boolean redoSuccess = historyService.redo();
      if (redoSuccess) {
        // Get the current state from state manager after redo
        DrawingState currentState = stateManager.getCurrentState();
        currentSession.updateState(currentState);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.info("Redo operation performed");
        }
        return true;
      }
    }
    return false;
  }

  /** Check if undo is available. */
  public boolean canUndo() {
    return historyService.canUndo();
  }

  /** Check if redo is available. */
  public boolean canRedo() {
    return historyService.canRedo();
  }

  /** Clear the history. */
  public void clearHistory() {
    historyService.clear();
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.info("History cleared");
    }
  }

  /** Get the state manager. */
  public IStateManager getStateManager() {
    return stateManager;
  }

  /** Get the history service. */
  public IHistoryService getHistoryService() {
    return historyService;
  }
}
