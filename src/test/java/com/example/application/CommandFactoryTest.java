package com.example.application;

import static org.junit.jupiter.api.Assertions.*;

import com.example.domain.commands.AddShapeCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.models.Circle;
import com.example.domain.models.Shape;
import com.example.domain.tools.ToolCommand;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandFactoryTest {

  @Test
  void createCircleCommand_basicParams_returnsAddShapeCommand() {
    CommandFactory factory = new CommandFactory();
    Map<String, String> params = new HashMap<>();
    params.put("x", "120");
    params.put("y", "140");
    params.put("diameter", "60");
    params.put("color", "red");

    ParsedCommand pc = new ParsedCommand("circle", params, "test");
    DrawCommand cmd = factory.createCommandFromParsed(pc);

    assertNotNull(cmd);
    assertTrue(cmd instanceof AddShapeCommand);
    Shape shape = ((AddShapeCommand) cmd).getShape();
    assertTrue(shape instanceof Circle);
    Circle c = (Circle) shape;
    assertEquals(120.0f, c.getPosition().getX());
    assertEquals(140.0f, c.getPosition().getY());
    assertEquals(30.0f, c.getRadius()); // diameter 60 -> radius 30
  }

  @Test
  void createModifyCommand_positionParams_returnsToolCommand() {
    CommandFactory factory = new CommandFactory();
    Map<String, String> params = new HashMap<>();
    params.put("id", "shape_1");
    params.put("property", "position");
    params.put("x", "50");
    params.put("y", "60");

    ParsedCommand pc = new ParsedCommand("modify", params, "test");
    DrawCommand cmd = factory.createCommandFromParsed(pc);

    assertNotNull(cmd);
    assertTrue(cmd instanceof ToolCommand);
    ToolCommand tc = (ToolCommand) cmd;
    assertEquals("modifyShape", tc.getTool().name());
    assertEquals("shape_1", tc.getParameters().get("id"));
    @SuppressWarnings("unchecked")
    Map<String, Object> pos = (Map<String, Object>) tc.getParameters().get("position");
    assertNotNull(pos);
    assertEquals(50.0f, ((Number) pos.get("x")).floatValue());
    assertEquals(60.0f, ((Number) pos.get("y")).floatValue());
  }

  @Test
  void createRectangleCommand_basicParams_returnsAddShapeCommand() {
    CommandFactory factory = new CommandFactory();
    Map<String, String> params = new HashMap<>();
    params.put("x", "10");
    params.put("y", "20");
    params.put("width", "80");
    params.put("height", "60");
    params.put("color", "#00FF00");

    ParsedCommand pc = new ParsedCommand("rectangle", params, "test");
    DrawCommand cmd = factory.createCommandFromParsed(pc);

    assertNotNull(cmd);
    assertTrue(cmd instanceof AddShapeCommand);
  }

  @Test
  void createModifyCommand_sizeSingle_updatesRadiusAndDimensions() {
    CommandFactory factory = new CommandFactory();
    Map<String, String> params = new HashMap<>();
    params.put("id", "shape_2");
    params.put("property", "size");
    params.put("size", "100");

    ParsedCommand pc = new ParsedCommand("modify", params, "test");
    DrawCommand cmd = factory.createCommandFromParsed(pc);
    assertNotNull(cmd);
    assertTrue(cmd instanceof ToolCommand);
    @SuppressWarnings("unchecked")
    Map<String, Object> props = (Map<String, Object>) ((ToolCommand) cmd).getParameters().get("properties");
    assertEquals(100.0f, ((Number) props.get("radius")).floatValue());
    assertEquals(100.0f, ((Number) props.get("width")).floatValue());
    assertEquals(100.0f, ((Number) props.get("height")).floatValue());
  }

  @Test
  void createModifyCommand_invalidProperty_throws() {
    CommandFactory factory = new CommandFactory();
    Map<String, String> params = new HashMap<>();
    params.put("id", "shape_3");
    params.put("property", "unknown");

    ParsedCommand pc = new ParsedCommand("modify", params, "test");
    assertThrows(IllegalArgumentException.class, () -> factory.createCommandFromParsed(pc));
  }
}
