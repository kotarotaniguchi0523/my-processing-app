package com.example.application;

import com.example.domain.commands.AddShapeCommand;
import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.commands.DrawCommand;
import com.example.domain.models.Circle;
import com.example.domain.models.Rectangle;
import com.example.domain.tools.ToolCommand;
import com.example.domain.tools.impl.ModifyShapeTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CommandFactory converts ParsedCommand into domain DrawCommand/ToolCommand.
 * Keeps ApplicationController lean by isolating command creation logic.
 */
public class CommandFactory {

    public DrawCommand createCommandFromParsed(ParsedCommand parsedCommand) {
        switch (parsedCommand.getType()) {
            case "circle":
                return createCircleCommand(parsedCommand);
            case "rectangle":
                return createRectangleCommand(parsedCommand);
            case "clear":
                return new ClearCanvasCommand();
            case "modify":
                return createModifyCommand(parsedCommand);
            default:
                return null;
        }
    }

    private AddShapeCommand createCircleCommand(ParsedCommand parsedCommand) {
        float x = parsedCommand.getFloatParam("x", 100.0f);
        float y = parsedCommand.getFloatParam("y", 100.0f);
        float diameter = parsedCommand.getFloatParam("diameter", 50.0f);
        String color = parsedCommand.getStringParam("color", "black");

        Position position = new Position(x, y);
        Color fillColor = parseColor(color);
        Style style = Style.builder().fillColor(fillColor).build();
        Circle circle = new Circle(position, diameter / 2, style); // diameter -> radius
        return new AddShapeCommand(circle);
    }

    private AddShapeCommand createRectangleCommand(ParsedCommand parsedCommand) {
        float x = parsedCommand.getFloatParam("x", 100.0f);
        float y = parsedCommand.getFloatParam("y", 100.0f);
        float width = parsedCommand.getFloatParam("width", 100.0f);
        float height = parsedCommand.getFloatParam("height", 50.0f);
        String color = parsedCommand.getStringParam("color", "black");

        Position position = new Position(x, y);
        Color fillColor = parseColor(color);
        Style style = Style.builder().fillColor(fillColor).build();
        Rectangle rectangle = new Rectangle(position, width, height, style);
        return new AddShapeCommand(rectangle);
    }

    private ToolCommand createModifyCommand(ParsedCommand parsedCommand) {
        // Validate required parameters
        String shapeId = parsedCommand.getStringParam("id");
        String property = parsedCommand.getStringParam("property");
        if (shapeId == null || shapeId.isBlank()) {
            throw new IllegalArgumentException("Shape ID is required for modify command");
        }
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("Property type is required for modify command");
        }

        // Build parameters map for ModifyShapeTool
        Map<String, Object> parameters = buildModifyParameters(parsedCommand);
        ModifyShapeTool modifyTool = new ModifyShapeTool();
        return new ToolCommand(modifyTool, parameters);
    }

    private Map<String, Object> buildModifyParameters(ParsedCommand parsedCommand) {
        Map<String, Object> parameters = new HashMap<>();
        String shapeId = parsedCommand.getStringParam("id");
        if (shapeId != null) {
            parameters.put("id", shapeId);
        }
        String property = parsedCommand.getStringParam("property");
        switch (property) {
            case "color":
                Map<String, Object> style = new HashMap<>();
                String colorValue = parsedCommand.getStringParam("color");
                if (colorValue != null) {
                    Color color = parseColor(colorValue);
                    Map<String, Object> colorMap = new HashMap<>();
                    colorMap.put("r", color.getRed());
                    colorMap.put("g", color.getGreen());
                    colorMap.put("b", color.getBlue());
                    colorMap.put("a", color.getAlpha());
                    style.put("fillColor", colorMap);
                }
                parameters.put("style", style);
                break;
            case "position":
                if (parsedCommand.hasParam("x") && parsedCommand.hasParam("y")) {
                    Map<String, Object> position = new HashMap<>();
                    position.put("x", parsedCommand.getFloatParam("x", 0));
                    position.put("y", parsedCommand.getFloatParam("y", 0));
                    parameters.put("position", position);
                } else {
                    throw new IllegalArgumentException(
                            "Position modification requires both x and y coordinates");
                }
                break;
            case "size":
                Map<String, Object> props = new HashMap<>();
                if (parsedCommand.hasParam("size")) {
                    float size = parsedCommand.getFloatParam("size", 0);
                    if (size <= 0) {
                        throw new IllegalArgumentException("Size must be greater than 0");
                    }
                    props.put("radius", size);
                    props.put("width", size);
                    props.put("height", size);
                } else {
                    boolean hasSize = false;
                    if (parsedCommand.hasParam("width")) {
                        float w = parsedCommand.getFloatParam("width", 0);
                        if (w <= 0) {
                            throw new IllegalArgumentException("Width must be greater than 0");
                        }
                        props.put("width", w);
                        hasSize = true;
                    }
                    if (parsedCommand.hasParam("height")) {
                        float h = parsedCommand.getFloatParam("height", 0);
                        if (h <= 0) {
                            throw new IllegalArgumentException("Height must be greater than 0");
                        }
                        props.put("height", h);
                        hasSize = true;
                    }
                    if (!hasSize) {
                        throw new IllegalArgumentException(
                                "Size modification requires either a size value or width/height values");
                    }
                }
                parameters.put("properties", props);
                break;
            default:
                throw new IllegalArgumentException("Unknown property: " + property);
        }
        return parameters;
    }

    private Color parseColor(String colorString) {
        if (colorString == null || colorString.isBlank()) {
            return Color.BLACK;
        }
        String color = colorString.trim().toLowerCase(Locale.ROOT);
        if (color.startsWith("#")) {
            try {
                return Color.fromHex(color);
            } catch (Exception e) {
                return Color.BLACK;
            }
        }
        switch (color) {
            case "black":
                return Color.BLACK;
            case "white":
                return Color.WHITE;
            case "red":
                return Color.RED;
            case "green":
                return Color.GREEN;
            case "blue":
                return Color.BLUE;
            case "yellow":
                return new Color(255, 255, 0);
            case "purple":
                return new Color(128, 0, 128);
            case "orange":
                return new Color(255, 165, 0);
            case "pink":
                return new Color(255, 192, 203);
            case "brown":
                return new Color(165, 42, 42);
            case "gray":
            case "grey":
                return new Color(128, 128, 128);
            default:
                return Color.BLACK;
        }
    }
}

