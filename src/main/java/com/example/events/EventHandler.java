package com.example.events;

/**
 * Interface for handling events in the system. Implementations should process specific event types
 * and handle them appropriately.
 */
@FunctionalInterface
public interface EventHandler<T extends Event> {

    /**
     * Handle the given event.
     *
     * @param event The event to handle
     * @throws Exception if an error occurs during event handling
     */
    void handle(T event) throws Exception;

    /**
     * Default method to check if this handler can handle the given event type. Override this method
     * if you need custom logic for event type checking.
     *
     * @param eventType The event type to check
     * @return true if this handler can handle the event type, false otherwise
     */
    default boolean canHandle(EventType eventType) {
        return true; // Default implementation handles all events
    }

    /**
     * Get the priority of this handler. Handlers with higher priority are executed first.
     *
     * @return The priority value (default is 0)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this handler should stop event propagation after handling. If true, no other
     * handlers will process this event after this handler.
     *
     * @return true to stop propagation, false to continue (default is false)
     */
    default boolean stopsPropagation() {
        return false;
    }
}
