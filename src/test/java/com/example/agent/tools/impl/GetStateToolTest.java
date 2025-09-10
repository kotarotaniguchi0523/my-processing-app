package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetStateToolTest {

    private GetStateTool tool;
    private AgentContext context;
    private DrawingState testState;

    @BeforeEach
    void setUp() {
        tool = new GetStateTool();
        
        // Create test state with shapes using proper concrete classes
        testState = DrawingState.builder()
                .shape(new Circle(
                    Position.of(100, 100),
                    50,
                    Style.defaultStyle()))
                .shape(new Rectangle(
                    Position.of(200, 200),
                    100, 80,
                    Style.defaultStyle()))
                .build();
        
        context = AgentContext.builder()
                .currentState(testState)
                .build();
    }

    @Test
    void testGetName() {
        assertEquals("get_state", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("state"));
    }

    @Test
    void testExecuteWithDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        
        ToolResult result = tool.execute(params, context);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("shapes=2"));
        assertTrue(result.getMessage().contains("800x600"));
        
        Map<String, Object> data = result.getData();
        assertEquals(2, data.get("shapeCount"));
        assertEquals(800, data.get("width"));
        assertEquals(600, data.get("height"));
    }

    @Test
    void testExecuteWithIncludeYaml() {
        Map<String, Object> params = new HashMap<>();
        params.put("includeYaml", true);
        params.put("summary", false);
        
        ToolResult result = tool.execute(params, context);
        
        assertTrue(result.isSuccess());
        Map<String, Object> data = result.getData();
        assertNotNull(data.get("stateYaml"));
        assertTrue(data.get("stateYaml").toString().contains("shapes:"));
    }

    @Test
    void testExecuteWithNoShapes() {
        Map<String, Object> params = new HashMap<>();
        params.put("includeShapes", false);
        
        ToolResult result = tool.execute(params, context);
        
        assertTrue(result.isSuccess());
        Map<String, Object> data = result.getData();
        assertNull(data.get("shapeCount"));
        assertNotNull(data.get("width"));
    }

    @Test
    void testExecuteWithNoContext() {
        assertThrows(Exception.class, () -> {
            tool.execute(new HashMap<>(), null);
        });
    }

    @Test
    void testExecuteWithEmptyState() {
        AgentContext emptyContext = AgentContext.builder()
                .currentState(DrawingState.empty())
                .build();
        
        Map<String, Object> params = new HashMap<>();
        ToolResult result = tool.execute(params, emptyContext);
        
        assertTrue(result.isSuccess());
        Map<String, Object> data = result.getData();
        assertEquals(0, data.get("shapeCount"));
    }

    @Test
    void testValidateAlwaysReturnsValid() {
        assertTrue(tool.validate(null).isValid());
        assertTrue(tool.validate(new HashMap<>()).isValid());
        
        Map<String, Object> params = new HashMap<>();
        params.put("invalidParam", "value");
        assertTrue(tool.validate(params).isValid());
    }
}