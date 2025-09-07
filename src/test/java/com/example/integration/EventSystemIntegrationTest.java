package com.example.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.example.application.ApplicationController;
import com.example.domain.models.Shape;
import com.example.events.EventType;
import com.example.events.UIEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration test for the complete event system from chat input to canvas update. Tests the full
 * flow: UI Event → ApplicationController → CommandParser → AgentService → StateService → UI Update
 * callback.
 */
public class EventSystemIntegrationTest {

    private ApplicationController appController;
    private AtomicReference<String> lastStatus = new AtomicReference<>("Not Set");
    private AtomicReference<String> lastChatMessage = new AtomicReference<>("Not Set");
    private AtomicReference<String> lastError = new AtomicReference<>("Not Set");
    private AtomicBoolean canvasRefreshed = new AtomicBoolean(false);
    private CountDownLatch callbackLatch;

    @BeforeEach
    void setUp() {
        appController = new ApplicationController();
        appController.setUICallback(new TestUICallback());
        appController.start();

        // Reset test state
        lastStatus.set("Not Set");
        lastChatMessage.set("Not Set");
        lastError.set("Not Set");
        canvasRefreshed.set(false);
        callbackLatch = new CountDownLatch(1);
    }

    @AfterEach
    void tearDown() {
        // ApplicationController doesn't need explicit cleanup
        // Resources will be cleaned up by JVM
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCompleteCircleCommandFlow() throws InterruptedException {
        // Test English circle command
        String command = "red circle at 100,200 diameter 50";

        // Send chat input event
        UIEvent chatEvent = new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, command, "chat");
        appController.getEventDispatcher().dispatchUIEvent(chatEvent);

        // Wait for processing to complete
        assertTrue(
                callbackLatch.await(5, TimeUnit.SECONDS),
                "Callback should be invoked within timeout");

        // Verify state was updated
        assertNotNull(appController.getCurrentSession(), "Session should be initialized");
        List<Shape> shapes = appController.getCurrentSession().getAllShapes();

        // Should have one shape
        assertEquals(1, shapes.size(), "Should have exactly one shape after circle command");
        Shape shape = shapes.get(0);
        assertEquals(Shape.ShapeType.CIRCLE, shape.getType(), "Shape should be a circle");

        // Verify canvas refresh was called
        assertTrue(canvasRefreshed.get(), "Canvas should have been refreshed");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testJapaneseCircleCommandFlow() throws InterruptedException {
        callbackLatch = new CountDownLatch(1);

        // Test Japanese circle command
        String command = "赤い円 150,250 直径80";

        UIEvent chatEvent = new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, command, "chat");
        appController.getEventDispatcher().dispatchUIEvent(chatEvent);

        assertTrue(
                callbackLatch.await(5, TimeUnit.SECONDS),
                "Callback should be invoked within timeout");

        // Verify shape was created
        List<Shape> shapes = appController.getCurrentSession().getAllShapes();
        assertEquals(1, shapes.size(), "Should have one shape after Japanese command");

        Shape shape = shapes.get(0);
        assertEquals(Shape.ShapeType.CIRCLE, shape.getType(), "Shape should be a circle");
        assertTrue(canvasRefreshed.get(), "Canvas should have been refreshed");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testRectangleCommandFlow() throws InterruptedException {
        callbackLatch = new CountDownLatch(1);

        String command = "draw rectangle at 50,100 size 80x60 in blue";

        UIEvent chatEvent = new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, command, "chat");
        appController.getEventDispatcher().dispatchUIEvent(chatEvent);

        assertTrue(
                callbackLatch.await(5, TimeUnit.SECONDS),
                "Callback should be invoked within timeout");

        List<Shape> shapes = appController.getCurrentSession().getAllShapes();
        assertEquals(1, shapes.size(), "Should have one shape after rectangle command");

        Shape shape = shapes.get(0);
        assertEquals(Shape.ShapeType.RECTANGLE, shape.getType(), "Shape should be a rectangle");
        assertTrue(canvasRefreshed.get(), "Canvas should have been refreshed");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleShapeCommands() throws InterruptedException {
        // Add first shape
        callbackLatch = new CountDownLatch(1);
        UIEvent event1 =
                new UIEvent(
                        EventType.UI_CHAT_INPUT_SUBMITTED,
                        "red circle at 100,100 diameter 40",
                        "chat");
        appController.getEventDispatcher().dispatchUIEvent(event1);
        assertTrue(callbackLatch.await(3, TimeUnit.SECONDS), "First callback should complete");

        // Add second shape
        callbackLatch = new CountDownLatch(1);
        canvasRefreshed.set(false);
        UIEvent event2 =
                new UIEvent(
                        EventType.UI_CHAT_INPUT_SUBMITTED,
                        "blue rectangle 200,150 size 60x80",
                        "chat");
        appController.getEventDispatcher().dispatchUIEvent(event2);
        assertTrue(callbackLatch.await(3, TimeUnit.SECONDS), "Second callback should complete");

        // Verify both shapes exist
        List<Shape> shapes = appController.getCurrentSession().getAllShapes();
        assertEquals(2, shapes.size(), "Should have two shapes after multiple commands");

        // Verify canvas was refreshed for both operations
        assertTrue(canvasRefreshed.get(), "Canvas should have been refreshed for second shape");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCanvasClearFlow() throws InterruptedException {
        // Add a shape first
        callbackLatch = new CountDownLatch(1);
        UIEvent addEvent =
                new UIEvent(
                        EventType.UI_CHAT_INPUT_SUBMITTED,
                        "green circle 150,150 diameter 50",
                        "chat");
        appController.getEventDispatcher().dispatchUIEvent(addEvent);
        assertTrue(callbackLatch.await(3, TimeUnit.SECONDS), "Add shape callback should complete");

        assertEquals(
                1,
                appController.getCurrentSession().getAllShapes().size(),
                "Should have one shape before clear");

        // Clear canvas
        callbackLatch = new CountDownLatch(1);
        canvasRefreshed.set(false);
        UIEvent clearEvent = new UIEvent(EventType.UI_CANVAS_CLEARED, "canvas");
        appController.getEventDispatcher().dispatchUIEvent(clearEvent);
        assertTrue(callbackLatch.await(3, TimeUnit.SECONDS), "Clear callback should complete");

        // Verify canvas was cleared
        assertEquals(
                0,
                appController.getCurrentSession().getAllShapes().size(),
                "Shapes should be cleared");
        assertTrue(canvasRefreshed.get(), "Canvas should have been refreshed after clear");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testUndoRedoFlow() throws InterruptedException {
        // Add a shape
        callbackLatch = new CountDownLatch(1);
        UIEvent addEvent =
                new UIEvent(
                        EventType.UI_CHAT_INPUT_SUBMITTED,
                        "yellow circle 200,200 diameter 60",
                        "chat");
        appController.getEventDispatcher().dispatchUIEvent(addEvent);
        assertTrue(callbackLatch.await(3, TimeUnit.SECONDS), "Add shape callback should complete");

        assertEquals(
                1,
                appController.getCurrentSession().getAllShapes().size(),
                "Should have one shape");

        // Test undo
        boolean undone = appController.getStateService().undo();
        assertTrue(undone, "Undo should succeed");
        assertEquals(
                0,
                appController.getCurrentSession().getAllShapes().size(),
                "Shape should be undone");

        // Test redo
        boolean redone = appController.getStateService().redo();
        assertTrue(redone, "Redo should succeed");
        assertEquals(
                1,
                appController.getCurrentSession().getAllShapes().size(),
                "Shape should be restored");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testInvalidCommandHandling() throws InterruptedException {
        callbackLatch = new CountDownLatch(1);

        // Send invalid command
        String invalidCommand = "draw impossible shape xyz";
        UIEvent invalidEvent =
                new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, invalidCommand, "chat");
        appController.getEventDispatcher().dispatchUIEvent(invalidEvent);

        assertTrue(
                callbackLatch.await(5, TimeUnit.SECONDS),
                "Invalid command callback should complete");

        // Should have no shapes created
        assertEquals(
                0,
                appController.getCurrentSession().getAllShapes().size(),
                "No shapes should be created from invalid command");

        // Should have received some response (either error or info message)
        assertNotEquals("Not Set", lastChatMessage.get(), "Should have received a chat response");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testAsyncProcessingPerformance() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // Send multiple commands quickly to test async processing
        CountDownLatch multipleCommandsLatch = new CountDownLatch(5);

        String[] commands = {
            "red circle 50,50 diameter 30",
            "blue rectangle 100,100 size 40x50",
            "green circle 200,150 diameter 25",
            "yellow rectangle 250,200 size 60x40",
            "red circle 300,100 diameter 35"
        };

        // Override callback to count multiple operations
        appController.setUICallback(
                new ApplicationController.UIUpdateCallback() {
                    @Override
                    public void updateStatus(String status) {
                        lastStatus.set(status);
                    }

                    @Override
                    public void addChatMessage(String message) {
                        lastChatMessage.set(message);
                        multipleCommandsLatch.countDown(); // Count down for each processed command
                    }

                    @Override
                    public void refreshCanvas() {
                        canvasRefreshed.set(true);
                    }

                    @Override
                    public void showError(String error) {
                        lastError.set(error);
                        multipleCommandsLatch.countDown();
                    }
                });

        // Send all commands
        for (String cmd : commands) {
            UIEvent event = new UIEvent(EventType.UI_CHAT_INPUT_SUBMITTED, cmd, "chat");
            appController.getEventDispatcher().dispatchUIEvent(event);
        }

        assertTrue(
                multipleCommandsLatch.await(10, TimeUnit.SECONDS),
                "All commands should be processed within reasonable time");

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // Verify all shapes were created
        assertEquals(
                5,
                appController.getCurrentSession().getAllShapes().size(),
                "All 5 shapes should be created");

        // Verify performance (should complete within 10 seconds for 5 commands)
        assertTrue(
                processingTime < 10000,
                "Processing should complete within 10 seconds, took: " + processingTime + "ms");

        System.out.println("Async processing of 5 commands completed in: " + processingTime + "ms");
    }

    /** Test implementation of UIUpdateCallback for integration testing */
    private class TestUICallback implements ApplicationController.UIUpdateCallback {
        @Override
        public void updateStatus(String status) {
            lastStatus.set(status);
            System.out.println("Status updated: " + status);
        }

        @Override
        public void addChatMessage(String message) {
            lastChatMessage.set(message);
            System.out.println("Chat message: " + message);
            callbackLatch.countDown(); // Signal that processing completed
        }

        @Override
        public void refreshCanvas() {
            canvasRefreshed.set(true);
            System.out.println("Canvas refreshed");
        }

        @Override
        public void showError(String error) {
            lastError.set(error);
            System.out.println("Error: " + error);
            callbackLatch.countDown(); // Signal that error processing completed
        }
    }
}
