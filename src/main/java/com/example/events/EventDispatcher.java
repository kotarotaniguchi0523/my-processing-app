package com.example.events;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Central dispatcher for routing events to appropriate handlers. Provides a facade over the
 * EventBus for easier event management.
 */
public class EventDispatcher {
  private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class.getName());

  private final EventBus eventBus;
  private boolean enabled;

  /** Create a new EventDispatcher with synchronous event bus. */
  public EventDispatcher() {
    this(false);
  }

  /**
   * Create a new EventDispatcher with specified async capability.
   *
   * @param enableAsync Enable asynchronous event processing
   */
  public EventDispatcher(boolean enableAsync) {
    this.eventBus = new EventBus(enableAsync);
    this.enabled = true;
    LOGGER.info("EventDispatcher created with async=" + enableAsync);
  }

  /**
   * Dispatch a UI event.
   *
   * @param event The UI event to dispatch
   */
  public void dispatchUIEvent(UIEvent event) {
    if (!enabled) {
      LOGGER.fine("EventDispatcher is disabled, ignoring UI event: " + event.getType());
      return;
    }

    LOGGER.fine("Dispatching UI event: " + event.getType());
    eventBus.publish(event);
  }

  /**
   * Dispatch a domain event.
   *
   * @param event The domain event to dispatch
   */
  public void dispatchDomainEvent(DomainEvent event) {
    if (!enabled) {
      LOGGER.fine("EventDispatcher is disabled, ignoring domain event: " + event.getType());
      return;
    }

    LOGGER.fine("Dispatching domain event: " + event.getType());
    eventBus.publish(event);
  }

  /**
   * Dispatch a system event.
   *
   * @param event The system event to dispatch
   */
  public void dispatchSystemEvent(SystemEvent event) {
    if (!enabled) {
      LOGGER.fine("EventDispatcher is disabled, ignoring system event: " + event.getType());
      return;
    }

    LOGGER.fine("Dispatching system event: " + event.getType());
    eventBus.publish(event);
  }

  /**
   * Dispatch any event.
   *
   * @param event The event to dispatch
   */
  public void dispatch(Event event) {
    if (!enabled) {
      LOGGER.fine("EventDispatcher is disabled, ignoring event: " + event.getType());
      return;
    }

    if (event instanceof UIEvent) {
      dispatchUIEvent((UIEvent) event);
    } else if (event instanceof DomainEvent) {
      dispatchDomainEvent((DomainEvent) event);
    } else if (event instanceof SystemEvent) {
      dispatchSystemEvent((SystemEvent) event);
    } else {
      LOGGER.fine("Dispatching generic event: " + event.getType());
      eventBus.publish(event);
    }
  }

  /**
   * Dispatch an event asynchronously.
   *
   * @param event The event to dispatch
   * @return CompletableFuture that completes when processing is done
   */
  public CompletableFuture<Void> dispatchAsync(Event event) {
    if (!enabled) {
      LOGGER.fine("EventDispatcher is disabled, ignoring async event: " + event.getType());
      return CompletableFuture.completedFuture(null);
    }

    return eventBus.publishAsync(event);
  }

  /**
   * Subscribe a handler to a specific event type.
   *
   * @param eventType The event type to subscribe to
   * @param handler The handler to subscribe
   */
  public <T extends Event> void subscribe(EventType eventType, EventHandler<T> handler) {
    eventBus.subscribe(eventType, handler);
    LOGGER.fine("Subscribed handler for event type: " + eventType);
  }

  /**
   * Unsubscribe a handler from a specific event type.
   *
   * @param eventType The event type to unsubscribe from
   * @param handler The handler to unsubscribe
   */
  public <T extends Event> void unsubscribe(EventType eventType, EventHandler<T> handler) {
    eventBus.unsubscribe(eventType, handler);
    LOGGER.fine("Unsubscribed handler from event type: " + eventType);
  }

  /**
   * Enable or disable the event dispatcher. When disabled, all events are ignored.
   *
   * @param enabled True to enable, false to disable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("EventDispatcher " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Check if the event dispatcher is enabled.
   *
   * @return True if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Get the number of handlers registered for a specific event type.
   *
   * @param eventType The event type to check
   * @return Number of handlers registered
   */
  public int getHandlerCount(EventType eventType) {
    return eventBus.getHandlerCount(eventType);
  }

  /**
   * Clear all handlers for a specific event type.
   *
   * @param eventType The event type to clear
   */
  public void clearHandlers(EventType eventType) {
    eventBus.clearHandlers(eventType);
  }

  /** Clear all handlers from the dispatcher. */
  public void clearAllHandlers() {
    eventBus.clearAllHandlers();
  }

  /** Shutdown the event dispatcher and clean up resources. */
  public void shutdown() {
    enabled = false;
    eventBus.shutdown();
    LOGGER.info("EventDispatcher shut down");
  }
}
