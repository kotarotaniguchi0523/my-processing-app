package com.example.domain.session;

import com.example.domain.commands.DrawCommand;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SessionContext represents the complete context of a drawing session Includes session info,
 * current drawing state, history, and command queue
 */
public final class SessionContext {
  private final Session session;
  private final DrawingState currentState;
  private final StateHistory stateHistory;
  private final List<DrawCommand> commandQueue;
  private final LocalDateTime lastModified;
  private final boolean isDirty;

  public SessionContext(
      Session session,
      DrawingState currentState,
      StateHistory stateHistory,
      List<DrawCommand> commandQueue,
      LocalDateTime lastModified,
      boolean isDirty) {
    this.session = Objects.requireNonNull(session, "Session cannot be null");
    this.currentState = Objects.requireNonNull(currentState, "Current state cannot be null");
    this.stateHistory = Objects.requireNonNull(stateHistory, "State history cannot be null");
    this.commandQueue = commandQueue != null ? new ArrayList<>(commandQueue) : new ArrayList<>();
    this.lastModified = Objects.requireNonNull(lastModified, "Last modified cannot be null");
    this.isDirty = isDirty;
  }

  // Factory method for new session context
  public static SessionContext newContext(Session session) {
    DrawingState initialState = DrawingState.defaultState();
    StateHistory history = new StateHistory(session.getSettings().getMaxUndoHistory());
    return new SessionContext(
        session, initialState, history, new ArrayList<>(), LocalDateTime.now(), false);
  }

  // Factory method for new session context with custom state
  public static SessionContext newContext(Session session, DrawingState initialState) {
    StateHistory history = new StateHistory(session.getSettings().getMaxUndoHistory());
    return new SessionContext(
        session, initialState, history, new ArrayList<>(), LocalDateTime.now(), false);
  }

  // Getters
  public Session getSession() {
    return session;
  }

  public DrawingState getCurrentState() {
    return currentState;
  }

  public StateHistory getStateHistory() {
    return stateHistory;
  }

  public List<DrawCommand> getCommandQueue() {
    return new ArrayList<>(commandQueue);
  }

  public LocalDateTime getLastModified() {
    return lastModified;
  }

  public boolean isDirty() {
    return isDirty;
  }

  // Execute a command and update context
  public SessionContext executeCommand(DrawCommand command) {
    if (!command.canExecute(currentState)) {
      return this; // Command cannot be executed, return unchanged
    }

    DrawingState newState = command.execute(currentState);
    StateHistory newHistory =
        stateHistory.addSnapshot(
            com.example.domain.models.StateSnapshot.createFromDraw(
                newState, command.getDescription()));

    Session updatedSession = session.updateActivity();

    return new SessionContext(
        updatedSession, newState, newHistory, commandQueue, LocalDateTime.now(), true);
  }

  // Queue a command for later execution
  public SessionContext queueCommand(DrawCommand command) {
    List<DrawCommand> newQueue = new ArrayList<>(commandQueue);
    newQueue.add(command);

    return new SessionContext(session, currentState, stateHistory, newQueue, lastModified, isDirty);
  }

  // Execute all queued commands
  public SessionContext executeQueuedCommands() {
    SessionContext context = this;
    List<DrawCommand> currentQueue = new ArrayList<>(commandQueue);

    for (DrawCommand command : currentQueue) {
      context = context.executeCommand(command);
    }

    // Clear the queue after execution
    return new SessionContext(
        context.session,
        context.currentState,
        context.stateHistory,
        new ArrayList<>(),
        context.lastModified,
        context.isDirty);
  }

  // Clear command queue
  public SessionContext clearCommandQueue() {
    if (commandQueue.isEmpty()) {
      return this;
    }

    return new SessionContext(
        session, currentState, stateHistory, new ArrayList<>(), lastModified, isDirty);
  }

  // Undo last action
  public SessionContext undo() {
    if (!stateHistory.canUndo()) {
      return this;
    }

    StateHistory newHistory = stateHistory.undo();
    DrawingState previousState = newHistory.getCurrentState();
    Session updatedSession = session.updateActivity();

    return new SessionContext(
        updatedSession, previousState, newHistory, commandQueue, LocalDateTime.now(), true);
  }

  // Redo last undone action
  public SessionContext redo() {
    if (!stateHistory.canRedo()) {
      return this;
    }

    StateHistory newHistory = stateHistory.redo();
    DrawingState nextState = newHistory.getCurrentState();
    Session updatedSession = session.updateActivity();

    return new SessionContext(
        updatedSession, nextState, newHistory, commandQueue, LocalDateTime.now(), true);
  }

  // Update the drawing state directly (use with caution)
  public SessionContext withState(DrawingState newState) {
    StateHistory newHistory =
        stateHistory.addSnapshot(
            com.example.domain.models.StateSnapshot.createFromModify(
                newState, "Direct state update"));
    Session updatedSession = session.updateActivity();

    return new SessionContext(
        updatedSession, newState, newHistory, commandQueue, LocalDateTime.now(), true);
  }

  // Update session information
  public SessionContext withSession(Session newSession) {
    return new SessionContext(
        newSession, currentState, stateHistory, commandQueue, lastModified, isDirty);
  }

  // Mark as saved (clear dirty flag)
  public SessionContext markAsSaved() {
    if (!isDirty) {
      return this;
    }

    return new SessionContext(
        session, currentState, stateHistory, commandQueue, lastModified, false);
  }

  // Update session activity
  public SessionContext updateActivity() {
    Session updatedSession = session.updateActivity();
    return new SessionContext(
        updatedSession, currentState, stateHistory, commandQueue, LocalDateTime.now(), isDirty);
  }

  // Pause the session
  public SessionContext pauseSession() {
    Session pausedSession = session.withStatus(Session.SessionStatus.PAUSED);
    return new SessionContext(
        pausedSession, currentState, stateHistory, commandQueue, lastModified, isDirty);
  }

  // Resume the session
  public SessionContext resumeSession() {
    Session resumedSession = session.withStatus(Session.SessionStatus.ACTIVE).updateActivity();
    return new SessionContext(
        resumedSession, currentState, stateHistory, commandQueue, LocalDateTime.now(), isDirty);
  }

  // End the session
  public SessionContext endSession() {
    Session endedSession = session.withStatus(Session.SessionStatus.ENDED);
    return new SessionContext(
        endedSession, currentState, stateHistory, commandQueue, lastModified, isDirty);
  }

  // Query methods
  public boolean canUndo() {
    return stateHistory.canUndo();
  }

  public boolean canRedo() {
    return stateHistory.canRedo();
  }

  public boolean hasQueuedCommands() {
    return !commandQueue.isEmpty();
  }

  public int getQueuedCommandCount() {
    return commandQueue.size();
  }

  public boolean isActive() {
    return session.isActive();
  }

  public boolean isPaused() {
    return session.isPaused();
  }

  public boolean isEnded() {
    return session.isEnded();
  }

  public boolean needsAutoSave() {
    if (!session.getSettings().isAutoSave() || !isDirty) {
      return false;
    }

    LocalDateTime nextAutoSave =
        lastModified.plusMinutes(session.getSettings().getAutoSaveIntervalMinutes());

    return LocalDateTime.now().isAfter(nextAutoSave);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SessionContext)) return false;
    SessionContext context = (SessionContext) obj;
    return isDirty == context.isDirty
        && Objects.equals(session, context.session)
        && Objects.equals(currentState, context.currentState)
        && Objects.equals(stateHistory, context.stateHistory)
        && Objects.equals(commandQueue, context.commandQueue)
        && Objects.equals(lastModified, context.lastModified);
  }

  @Override
  public int hashCode() {
    return Objects.hash(session, currentState, stateHistory, commandQueue, lastModified, isDirty);
  }

  @Override
  public String toString() {
    return String.format(
        "SessionContext(session=%s, shapes=%d, dirty=%s, queued=%d)",
        session.getSessionId(), currentState.getShapes().size(), isDirty, commandQueue.size());
  }
}
