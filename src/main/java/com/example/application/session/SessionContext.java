package com.example.application.session;

import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import com.example.domain.session.Session;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * SessionContext (moved from com.example.domain.session) はアプリケーション層の協調コンテキスト。
 */
public final class SessionContext {
  private final Session session;
  private final DrawingState currentState;
  private final StateHistory stateHistory;
  private final LocalDateTime lastModified;
  private final boolean isDirty;

  public SessionContext(Session session, DrawingState currentState, StateHistory stateHistory,
      LocalDateTime lastModified, boolean isDirty) {
    this.session = Objects.requireNonNull(session, "Session cannot be null");
    this.currentState = Objects.requireNonNull(currentState, "Current state cannot be null");
    this.stateHistory = Objects.requireNonNull(stateHistory, "State history cannot be null");
    this.lastModified = Objects.requireNonNull(lastModified, "Last modified cannot be null");
    this.isDirty = isDirty;
  }

  public static SessionContext newContext(Session session) {
    DrawingState initialState = DrawingState.defaultState();
    StateHistory history = new StateHistory(session.getSettings().getMaxUndoHistory());
    return new SessionContext(session, initialState, history, LocalDateTime.now(), false);
  }

  public static SessionContext newContext(Session session, DrawingState initialState) {
    StateHistory history = new StateHistory(session.getSettings().getMaxUndoHistory());
    return new SessionContext(session, initialState, history, LocalDateTime.now(), false);
  }

  public Session getSession() {
    return session;
  }

  public DrawingState getCurrentState() {
    return currentState;
  }

  public StateHistory getStateHistory() {
    return stateHistory;
  }

  public LocalDateTime getLastModified() {
    return lastModified;
  }

  public boolean isDirty() {
    return isDirty;
  }

  // Command execution methods removed.

  public SessionContext undo() {
    if (!stateHistory.canUndo()) {
      return this;
    }
    StateHistory newHistory = stateHistory.undo();
    DrawingState previousState = newHistory.getCurrentState();
    Session updatedSession = session.updateActivity();
    return new SessionContext(updatedSession, previousState, newHistory, LocalDateTime.now(), true);
  }

  public SessionContext redo() {
    if (!stateHistory.canRedo()) {
      return this;
    }
    StateHistory newHistory = stateHistory.redo();
    DrawingState nextState = newHistory.getCurrentState();
    Session updatedSession = session.updateActivity();
    return new SessionContext(updatedSession, nextState, newHistory, LocalDateTime.now(), true);
  }

  public SessionContext withState(DrawingState newState) {
    StateHistory newHistory = stateHistory.addSnapshot(
        com.example.domain.models.StateSnapshot.createFromModify(newState, "Direct state update"));
    Session updatedSession = session.updateActivity();
    return new SessionContext(updatedSession, newState, newHistory, LocalDateTime.now(), true);
  }

  public SessionContext withSession(Session newSession) {
    return new SessionContext(newSession, currentState, stateHistory, lastModified, isDirty);
  }

  public SessionContext markAsSaved() {
    if (!isDirty) {
      return this;
    }
    return new SessionContext(session, currentState, stateHistory, lastModified, false);
  }

  public SessionContext updateActivity() {
    Session updatedSession = session.updateActivity();
    return new SessionContext(updatedSession, currentState, stateHistory, LocalDateTime.now(),
        isDirty);
  }

  public SessionContext pauseSession() {
    Session pausedSession = session.withStatus(Session.SessionStatus.PAUSED);
    return new SessionContext(pausedSession, currentState, stateHistory, lastModified, isDirty);
  }

  public SessionContext resumeSession() {
    Session resumedSession = session.withStatus(Session.SessionStatus.ACTIVE).updateActivity();
    return new SessionContext(resumedSession, currentState, stateHistory, LocalDateTime.now(),
        isDirty);
  }

  public SessionContext endSession() {
    Session endedSession = session.withStatus(Session.SessionStatus.ENDED);
    return new SessionContext(endedSession, currentState, stateHistory, lastModified, isDirty);
  }

  public boolean canUndo() {
    return stateHistory.canUndo();
  }

  public boolean canRedo() {
    return stateHistory.canRedo();
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
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SessionContext)) {
      return false;
    }
    SessionContext context = (SessionContext) obj;
    return isDirty == context.isDirty && Objects.equals(session, context.session)
        && Objects.equals(currentState, context.currentState)
        && Objects.equals(stateHistory, context.stateHistory)
        && Objects.equals(lastModified, context.lastModified);
  }

  @Override
  public int hashCode() {
    return Objects.hash(session, currentState, stateHistory, lastModified, isDirty);
  }

  @Override
  public String toString() {
    return String.format("SessionContext(session=%s, shapes=%d, dirty=%s)", session.getSessionId(),
        currentState.getShapes().size(), isDirty);
  }
}
