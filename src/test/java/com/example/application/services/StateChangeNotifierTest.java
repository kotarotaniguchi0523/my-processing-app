package com.example.application.services;

import com.example.domain.models.DrawingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateChangeNotifier to ensure proper observer pattern functionality.
 */
public class StateChangeNotifierTest {

    private StateChangeNotifier notifier;
    private TestStateChangeListener listener1;
    private TestStateChangeListener listener2;

    @BeforeEach
    void setUp() {
        notifier = new StateChangeNotifier();
        listener1 = new TestStateChangeListener("Listener1");
        listener2 = new TestStateChangeListener("Listener2");
    }

    @Test
    void testAddListener() {
        notifier.addListener(listener1);
        assertEquals(1, notifier.getListenerCount());

        notifier.addListener(listener2);
        assertEquals(2, notifier.getListenerCount());

        // Adding same listener twice should not increase count
        notifier.addListener(listener1);
        assertEquals(2, notifier.getListenerCount());
    }

    @Test
    void testRemoveListener() {
        notifier.addListener(listener1);
        notifier.addListener(listener2);
        assertEquals(2, notifier.getListenerCount());

        notifier.removeListener(listener1);
        assertEquals(1, notifier.getListenerCount());

        notifier.removeListener(listener2);
        assertEquals(0, notifier.getListenerCount());

        // Removing non-existent listener should not cause issues
        notifier.removeListener(listener1);
        assertEquals(0, notifier.getListenerCount());
    }

    @Test
    void testNotifyStateChanged() {
        notifier.addListener(listener1);
        notifier.addListener(listener2);

        DrawingState testState = DrawingState.defaultState();
        String changeDescription = "Test change";

        notifier.notifyStateChanged(testState, changeDescription);

        // Both listeners should have been notified
        assertTrue(listener1.wasNotified);
        assertTrue(listener2.wasNotified);
        assertEquals(testState, listener1.lastState);
        assertEquals(testState, listener2.lastState);
        assertEquals(changeDescription, listener1.lastDescription);
        assertEquals(changeDescription, listener2.lastDescription);
    }

    @Test
    void testNotifyStateChangedWithDefaultDescription() {
        notifier.addListener(listener1);

        DrawingState testState = DrawingState.defaultState();
        notifier.notifyStateChanged(testState);

        assertTrue(listener1.wasNotified);
        assertEquals(testState, listener1.lastState);
        assertEquals("Drawing state updated", listener1.lastDescription);
    }

    @Test
    void testNotifyWithNullState() {
        notifier.addListener(listener1);

        // Should not crash, but listener should not be notified
        notifier.notifyStateChanged(null, "Test");

        assertFalse(listener1.wasNotified);
    }

    @Test
    void testNotifyWithNoListeners() {
        DrawingState testState = DrawingState.defaultState();

        // Should not crash when no listeners are registered
        assertDoesNotThrow(() -> notifier.notifyStateChanged(testState, "Test"));
    }

    @Test
    void testClearListeners() {
        notifier.addListener(listener1);
        notifier.addListener(listener2);
        assertEquals(2, notifier.getListenerCount());

        notifier.clearListeners();
        assertEquals(0, notifier.getListenerCount());

        // Notifying after clear should not reach any listeners
        DrawingState testState = DrawingState.defaultState();
        notifier.notifyStateChanged(testState, "Test");

        assertFalse(listener1.wasNotified);
        assertFalse(listener2.wasNotified);
    }

    @Test
    void testListenerException() {
        // Add a listener that throws an exception
        StateChangeNotifier.StateChangeListener faultyListener = new StateChangeNotifier.StateChangeListener() {
            @Override
            public void onStateChanged(DrawingState newState, String changeDescription) {
                throw new RuntimeException("Test exception");
            }
        };

        notifier.addListener(faultyListener);
        notifier.addListener(listener1);

        DrawingState testState = DrawingState.defaultState();

        // Should not crash and should still notify other listeners
        assertDoesNotThrow(() -> notifier.notifyStateChanged(testState, "Test"));

        // listener1 should still have been notified despite the exception in faultyListener
        assertTrue(listener1.wasNotified);
    }

    /**
     * Test implementation of StateChangeListener for testing purposes.
     */
    private static class TestStateChangeListener implements StateChangeNotifier.StateChangeListener {
        private final String name;
        boolean wasNotified = false;
        DrawingState lastState = null;
        String lastDescription = null;

        TestStateChangeListener(String name) {
            this.name = name;
        }

        @Override
        public void onStateChanged(DrawingState newState, String changeDescription) {
            wasNotified = true;
            lastState = newState;
            lastDescription = changeDescription;
        }

        @Override
        public String toString() {
            return "TestStateChangeListener{" + name + "}";
        }
    }
}