package com.example.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all events in the system. Provides common functionality like timestamps, event
 * IDs, and metadata.
 */
public abstract class Event {
  private final String id;
  private final LocalDateTime timestamp;
  private final EventType type;
  private Object source;
  private boolean handled;

  protected Event(EventType type) {
    this.id = UUID.randomUUID().toString();
    this.timestamp = LocalDateTime.now();
    this.type = type;
    this.handled = false;
  }

  protected Event(EventType type, Object source) {
    this(type);
    this.source = source;
  }

  // Getters
  public String getId() {
    return id;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public EventType getType() {
    return type;
  }

  public Object getSource() {
    return source;
  }

  public boolean isHandled() {
    return handled;
  }

  // Mark event as handled to prevent further processing
  public void markHandled() {
    this.handled = true;
  }

  @Override
  public String toString() {
    return String.format(
        "%s[id=%s, type=%s, timestamp=%s, handled=%s]",
        getClass().getSimpleName(), id, type, timestamp, handled);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Event event = (Event) obj;
    return id.equals(event.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
