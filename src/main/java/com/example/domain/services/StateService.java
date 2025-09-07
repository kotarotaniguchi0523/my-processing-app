package com.example.domain.services;

import com.example.domain.model.DrawingSession;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import com.example.services.HistoryService;
import com.example.services.StateManager;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for managing drawing state and session operations. Provides a high-level interface for
 * state management operations.
 */
public class StateService {
  private static final Logger LOGGER = Logger.getLogger(StateService.class.getName());

  private DrawingSession currentSession;
  private final StateManager stateManager;
  private final HistoryService historyService;

  public StateService() {
    this.stateManager = new StateManager();
    this.historyService = new HistoryService(stateManager);
  }

  public StateService(StateManager stateManager, HistoryService historyService) {
    this.stateManager = stateManager;
    this.historyService = historyService;
  }

  /** Initialize a new drawing session. */
  public void initializeSession(DrawingSession session) {
    this.currentSession = session;
    stateManager.updateState(session.getCurrentState());
    LOGGER.info("Drawing session initialized: " + session.getSessionId());
  }

  /** Get the current drawing session. */
  public DrawingSession getCurrentSession() {
    return currentSession;
  }

  /** Get the current drawing state. */
  public DrawingState getCurrentState() {
    return currentSession != null ? currentSession.getCurrentState() : DrawingState.defaultState();
  }

  /** Update the current state. */
  public void updateState(DrawingState newState) {
    if (currentSession != null) {
      // Save current state to history before updating
      historyService.addState(getCurrentState());

      currentSession.updateState(newState);
      stateManager.updateState(newState);

      LOGGER.fine("State updated for session: " + currentSession.getSessionId());
    }
  }

  /** Add a shape to the current session. */
  public void addShape(Shape shape) {
    if (currentSession != null) {
      historyService.addState(getCurrentState());
      currentSession.addShape(shape);
      stateManager.updateState(currentSession.getCurrentState());

      LOGGER.info("Shape added: " + shape.getId() + " (" + shape.getType() + ")");
    }
  }

  /** Remove a shape from the current session. */
  public void removeShape(String shapeId) {
    if (currentSession != null) {
      historyService.addState(getCurrentState());
      currentSession.removeShape(shapeId);
      stateManager.updateState(currentSession.getCurrentState());

      LOGGER.info("Shape removed: " + shapeId);
    }
  }

  /** Clear all shapes from the current session. */
  public void clearAllShapes() {
    if (currentSession != null) {
      historyService.addState(getCurrentState());
      currentSession.clearAllShapes();
      stateManager.updateState(currentSession.getCurrentState());

      LOGGER.info("All shapes cleared from session: " + currentSession.getSessionId());
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
      if (historyService.undo()) {
        // HistoryService already updated the StateManager
        DrawingState newState = stateManager.getCurrentState();
        currentSession.updateState(newState);

        LOGGER.info("Undo operation performed");
        return true;
      }
    }
    return false;
  }

  /** Redo the last undone operation. */
  public boolean redo() {
    if (currentSession != null && historyService.canRedo()) {
      if (historyService.redo()) {
        // HistoryService already updated the StateManager
        DrawingState newState = stateManager.getCurrentState();
        currentSession.updateState(newState);

        LOGGER.info("Redo operation performed");
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
    historyService.clearHistory();
    LOGGER.info("History cleared");
  }

  /** Get the state manager. */
  public StateManager getStateManager() {
    return stateManager;
  }

  /** Get the history service. */
  public HistoryService getHistoryService() {
    return historyService;
  }
}
