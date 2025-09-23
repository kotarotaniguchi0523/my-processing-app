package com.example.application.services;

import com.example.domain.models.DrawingState;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Lightweight state change notification system using the observer pattern.
 * Replaces the deleted event system with a simple, focused approach for
 * notifying components about drawing state changes.
 */
public class StateChangeNotifier {
    private static final Logger logger = Logger.getLogger(StateChangeNotifier.class.getName());

    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Interface for components that want to be notified of state changes.
     */
    public interface StateChangeListener {
        /**
         * Called when the drawing state changes.
         *
         * @param newState the new drawing state
         * @param changeDescription description of what changed
         */
        void onStateChanged(DrawingState newState, String changeDescription);
    }

    /**
     * Register a listener to receive state change notifications.
     *
     * @param listener the listener to register
     */
    public void addListener(StateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.info("StateChangeListener registered: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Unregister a listener from receiving state change notifications.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(StateChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            logger.info("StateChangeListener unregistered: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Notify all registered listeners of a state change.
     * This method creates a snapshot of listeners to avoid deadlocks and ensures
     * that failing listeners don't affect other notifications.
     *
     * @param newState the new drawing state
     * @param changeDescription description of what changed
     */
    public void notifyStateChanged(DrawingState newState, String changeDescription) {
        if (newState == null) {
            logger.warning("Attempted to notify with null state");
            return;
        }

        // Create a snapshot of listeners to avoid concurrent modification issues
        // and reduce lock contention
        List<StateChangeListener> listenerSnapshot = new ArrayList<>(listeners);

        if (listenerSnapshot.isEmpty()) {
            logger.fine("No listeners registered for state change notification");
            return;
        }

        logger.info("Notifying " + listenerSnapshot.size() + " listeners of state change: " + changeDescription);

        // Notify listeners outside of any synchronized blocks to prevent deadlocks
        for (StateChangeListener listener : listenerSnapshot) {
            try {
                // Each listener notification is isolated to prevent cascading failures
                listener.onStateChanged(newState, changeDescription);
            } catch (Exception e) {
                logger.severe("Error notifying listener " + listener.getClass().getSimpleName() +
                    ": " + e.getMessage() + " (continuing with other listeners)");
                // Continue notifying other listeners even if one fails
                // This isolation prevents one bad listener from breaking the entire notification chain
            }
        }
    }

    /**
     * Notify listeners with a generic change description.
     *
     * @param newState the new drawing state
     */
    public void notifyStateChanged(DrawingState newState) {
        notifyStateChanged(newState, "Drawing state updated");
    }

    /**
     * Get the number of registered listeners.
     *
     * @return number of listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clear all registered listeners.
     */
    public void clearListeners() {
        int count = listeners.size();
        listeners.clear();
        logger.info("Cleared " + count + " state change listeners");
    }
}