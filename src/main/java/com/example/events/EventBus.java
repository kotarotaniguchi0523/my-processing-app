package com.example.events;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event bus for publishing and subscribing to events. Supports both synchronous and asynchronous
 * event processing.
 */
public class EventBus {
    private static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());

    private final Map<EventType, List<EventHandlerWrapper>> handlers;
    private final ExecutorService executorService;
    private final boolean allowAsyncProcessing;

    /** Create a new EventBus with default settings (synchronous processing). */
    public EventBus() {
        this(false);
    }

    /**
     * Create a new EventBus with specified async processing capability.
     *
     * @param allowAsyncProcessing If true, supports async event publishing
     */
    public EventBus(boolean allowAsyncProcessing) {
        this.handlers = new ConcurrentHashMap<>();
        this.allowAsyncProcessing = allowAsyncProcessing;
        this.executorService = allowAsyncProcessing ? Executors.newFixedThreadPool(4) : null;
    }

    /**
     * Subscribe an event handler to a specific event type.
     *
     * @param eventType The event type to subscribe to
     * @param handler The handler to subscribe
     */
    public <T extends Event> void subscribe(EventType eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new EventHandlerWrapper(handler));

        // Sort handlers by priority (higher priority first)
        handlers.get(eventType)
                .sort((h1, h2) -> Integer.compare(h2.getPriority(), h1.getPriority()));

        LOGGER.fine("Subscribed handler to event type: " + eventType);
    }

    /**
     * Unsubscribe an event handler from a specific event type.
     *
     * @param eventType The event type to unsubscribe from
     * @param handler The handler to unsubscribe
     */
    public <T extends Event> void unsubscribe(EventType eventType, EventHandler<T> handler) {
        List<EventHandlerWrapper> typeHandlers = handlers.get(eventType);
        if (typeHandlers != null) {
            typeHandlers.removeIf(wrapper -> wrapper.getHandler().equals(handler));
            LOGGER.fine("Unsubscribed handler from event type: " + eventType);
        }
    }

    /**
     * Publish an event synchronously to all registered handlers.
     *
     * @param event The event to publish
     */
    @SuppressWarnings("unchecked")
    public void publish(Event event) {
        if (event == null) {
            LOGGER.warning("Attempted to publish null event");
            return;
        }

        List<EventHandlerWrapper> typeHandlers = handlers.get(event.getType());
        if (typeHandlers == null || typeHandlers.isEmpty()) {
            LOGGER.fine("No handlers registered for event type: " + event.getType());
            return;
        }

        LOGGER.fine("Publishing event: " + event);

        for (EventHandlerWrapper wrapper : typeHandlers) {
            if (event.isHandled() && wrapper.stopsPropagation()) {
                LOGGER.fine("Event propagation stopped by handler");
                break;
            }

            if (!wrapper.canHandle(event.getType())) {
                continue;
            }

            try {
                wrapper.handle(event);
                LOGGER.fine("Event handled by: " + wrapper.getHandler().getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error handling event: " + event, e);
                // Continue processing other handlers unless the event is marked as handled
            }
        }
    }

    /**
     * Publish an event asynchronously to all registered handlers. Only available if async
     * processing is enabled.
     *
     * @param event The event to publish
     * @return CompletableFuture that completes when all handlers finish
     */
    public CompletableFuture<Void> publishAsync(Event event) {
        if (!allowAsyncProcessing) {
            throw new UnsupportedOperationException(
                    "Async processing not enabled for this EventBus");
        }

        return CompletableFuture.runAsync(() -> publish(event), executorService);
    }

    /**
     * Get the number of handlers registered for a specific event type.
     *
     * @param eventType The event type to check
     * @return Number of handlers registered
     */
    public int getHandlerCount(EventType eventType) {
        List<EventHandlerWrapper> typeHandlers = handlers.get(eventType);
        return typeHandlers != null ? typeHandlers.size() : 0;
    }

    /**
     * Get all registered event types.
     *
     * @return Set of event types that have registered handlers
     */
    public Set<EventType> getRegisteredEventTypes() {
        return new HashSet<>(handlers.keySet());
    }

    /**
     * Clear all handlers for a specific event type.
     *
     * @param eventType The event type to clear
     */
    public void clearHandlers(EventType eventType) {
        handlers.remove(eventType);
        LOGGER.info("Cleared all handlers for event type: " + eventType);
    }

    /** Clear all handlers from the event bus. */
    public void clearAllHandlers() {
        handlers.clear();
        LOGGER.info("Cleared all event handlers");
    }

    /** Shutdown the event bus and clean up resources. */
    public void shutdown() {
        clearAllHandlers();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            LOGGER.info("EventBus shut down");
        }
    }

    /** Wrapper class for event handlers to provide additional functionality. */
    private static class EventHandlerWrapper {
        private final EventHandler<?> handler;

        public EventHandlerWrapper(EventHandler<?> handler) {
            this.handler = handler;
        }

        public EventHandler<?> getHandler() {
            return handler;
        }

        public boolean canHandle(EventType eventType) {
            return handler.canHandle(eventType);
        }

        public int getPriority() {
            return handler.getPriority();
        }

        public boolean stopsPropagation() {
            return handler.stopsPropagation();
        }

        @SuppressWarnings("unchecked")
        public void handle(Event event) throws Exception {
            ((EventHandler<Event>) handler).handle(event);
        }
    }
}
