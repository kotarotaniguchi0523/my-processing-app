package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.StateHistory;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Line;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UndoRedoToolTest {

    private UndoRedoTool tool;
    private AgentContext context;
    private StateHistory history;

    @BeforeEach
    void setUp() {
        tool = new UndoRedoTool();
        
        // Create state history with multiple states
        DrawingState state1 = DrawingState.builder()
                .shape(new Circle(Position.of(100, 100), 50, Style.defaultStyle()))
                .build();
                
        DrawingState state2 = DrawingState.builder()
                .shape(new Circle(Position.of(100, 100), 50, Style.defaultStyle()))
                .shape(new Rectangle(Position.of(200, 200), 100, 80, Style.defaultStyle()))
                .build();
                
        DrawingState state3 = DrawingState.builder()
                .shape(new Circle(Position.of(100, 100), 50, Style.defaultStyle()))
                .shape(new Rectangle(Position.of(200, 200), 100, 80, Style.defaultStyle()))
                .shape(new Line(Position.of(0, 0), Position.of(100, 100), Style.defaultStyle()))
                .build();
        
        history = new StateHistory();
        history = history.addState(state1);
        history = history.addState(state2);
        history = history.addState(state3);
        
        context = AgentContext.builder()
                .currentState(state3)
                .stateHistory(history)
                .build();
    }

    @Test
    void testGetName() {
        assertEquals("undo_redo", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().toLowerCase().contains("undo"));
        assertTrue(tool.getDescription().toLowerCase().contains("redo"));
    }

    @Test
    void testExecuteUndo() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "undo");
        
        ToolResult result = tool.execute(params, context);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("undo"));
        
        DrawingState newState = result.getNewState();
        assertNotNull(newState);
        assertEquals(2, newState.getShapes().size()); // After undo, should have 2 shapes
        
        Map<String, Object> data = result.getData();
        assertEquals("undo", data.get("operation"));
        assertTrue((Boolean) data.get("canUndo"));
        assertTrue((Boolean) data.get("canRedo"));
    }

    @Test
    void testExecuteRedo() {
        // First undo to have something to redo
        history = history.undo();
        context = AgentContext.builder()
                .currentState(history.getCurrentState())
                .stateHistory(history)
                .build();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "redo");
        
        ToolResult result = tool.execute(params, context);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("redo"));
        
        DrawingState newState = result.getNewState();
        assertNotNull(newState);
        assertEquals(3, newState.getShapes().size()); // After redo, should have 3 shapes
    }

    @Test
    void testExecuteWithInvalidOperation() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "invalid");
        
        assertThrows(Exception.class, () -> {
            tool.execute(params, context);
        });
    }

    @Test
    void testExecuteWithNullOperation() {
        Map<String, Object> params = new HashMap<>();
        
        assertThrows(Exception.class, () -> {
            tool.execute(params, context);
        });
    }

    @Test
    void testExecuteWithNoContext() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "undo");
        
        assertThrows(Exception.class, () -> {
            tool.execute(params, null);
        });
    }

    @Test
    void testExecuteUndoAtBeginning() {
        // Create history with only one state
        StateHistory singleHistory = new StateHistory();
        DrawingState singleState = DrawingState.empty();
        singleHistory = singleHistory.addState(singleState);
        
        AgentContext singleContext = AgentContext.builder()
                .currentState(singleState)
                .stateHistory(singleHistory)
                .build();
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "undo");
        
        ToolResult result = tool.execute(params, singleContext);
        
        // Should handle gracefully when can't undo
        assertNotNull(result);
    }

    @Test
    void testExecuteRedoAtEnd() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "redo");
        
        ToolResult result = tool.execute(params, context);
        
        // Should handle gracefully when can't redo (already at latest state)
        assertNotNull(result);
    }

    @Test
    void testValidateAlwaysReturnsValid() {
        assertTrue(tool.validate(null).isValid());
        assertTrue(tool.validate(new HashMap<>()).isValid());
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "undo");
        assertTrue(tool.validate(params).isValid());
    }

    @Test
    void testGetParameters() {
        Map<String, String> params = tool.getParameters();
        assertNotNull(params);
        assertTrue(params.containsKey("operation"));
        assertTrue(params.get("operation").toLowerCase().contains("undo"));
        assertTrue(params.get("operation").toLowerCase().contains("redo"));
    }
}