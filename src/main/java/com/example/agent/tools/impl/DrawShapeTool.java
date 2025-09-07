package com.example.agent.tools.impl;

import com.example.agent.core.AgentContext;
import com.example.agent.tools.AbstractTool;
import com.example.agent.tools.ToolExecutionException;
import com.example.agent.tools.ToolResult;
import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Map;

/**
 * DrawShapeTool allows the AI agent to add shapes to the drawing canvas Supports creating circles,
 * rectangles, lines, and text elements
 */
public class DrawShapeTool extends AbstractTool {

    public DrawShapeTool() {
        super(
                "draw_shape",
                "Add a new shape to the drawing canvas",
                "drawing",
                true, // requires
                // approval
                true); // state modifying
    }

    @Override
    protected void defineParameters() {
        addParameter("type", String.class, "Shape type (circle, rectangle, line, text)", true);
        addParameter("x", Number.class, "X coordinate of the shape", true);
        addParameter("y", Number.class, "Y coordinate of the shape", true);

        // Shape-specific parameters
        addParameter("width", Number.class, "Width (for rectangle)", false);
        addParameter("height", Number.class, "Height (for rectangle)", false);
        addParameter("radius", Number.class, "Radius (for circle)", false);
        addParameter("x2", Number.class, "End X coordinate (for line)", false);
        addParameter("y2", Number.class, "End Y coordinate (for line)", false);
        addParameter("text", String.class, "Text content (for text shape)", false);

        // Style parameters
        addParameter(
                "fillColor",
                String.class,
                "Fill color (hex format, e.g., #FF0000)",
                false,
                "#000000");
        addParameter("strokeColor", String.class, "Stroke color (hex format)", false, "#000000");
        addParameter("strokeWidth", Number.class, "Stroke width", false, 1.0f);
        addParameter("fontSize", Number.class, "Font size (for text)", false, 12);

        // Optional parameters
        addParameter("id", String.class, "Custom shape ID", false);
        addParameter("visible", Boolean.class, "Shape visibility", false, true);
        addParameter("layerOrder", Number.class, "Layer order (z-index)", false, 0);
    }

    private Circle createCircle(
            String id,
            Position position,
            Style style,
            boolean visible,
            int layerOrder,
            Map<String, Object> parameters)
            throws ToolExecutionException {
        Number radiusNum = (Number) parameters.get("radius");
        if (radiusNum == null) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Radius is required for circle shapes");
        }

        float radius = radiusNum.floatValue();
        if (radius <= 0) {
            throw ToolExecutionException.invalidParameters(getName(), "Radius must be positive");
        }

        if (id == null) {
            id = "circle_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        }

        return new Circle(id, position, radius, style, visible, layerOrder);
    }

    private Rectangle createRectangle(
            String id,
            Position position,
            Style style,
            boolean visible,
            int layerOrder,
            Map<String, Object> parameters)
            throws ToolExecutionException {
        Number widthNum = (Number) parameters.get("width");
        Number heightNum = (Number) parameters.get("height");

        if (widthNum == null || heightNum == null) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Width and height are required for rectangle shapes");
        }

        float width = widthNum.floatValue();
        float height = heightNum.floatValue();

        if (width <= 0 || height <= 0) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Width and height must be positive");
        }

        if (id == null) {
            id = "rectangle_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        }

        return new Rectangle(id, position, width, height, style, visible, layerOrder);
    }

    private Line createLine(
            String id,
            Position position,
            Style style,
            boolean visible,
            int layerOrder,
            Map<String, Object> parameters)
            throws ToolExecutionException {
        Number x2Num = (Number) parameters.get("x2");
        Number y2Num = (Number) parameters.get("y2");

        if (x2Num == null || y2Num == null) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "x2 and y2 coordinates are required for line shapes");
        }

        Position endPosition = new Position(x2Num.floatValue(), y2Num.floatValue());

        if (id == null) {
            id = "line_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        }

        return new Line(id, position, endPosition, style, visible, layerOrder);
    }

    private Text createText(
            String id,
            Position position,
            Style style,
            boolean visible,
            int layerOrder,
            Map<String, Object> parameters)
            throws ToolExecutionException {
        String textContent = (String) parameters.get("text");
        if (textContent == null || textContent.isBlank()) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Text content is required for text shapes");
        }

        // Extract font size if provided
        Number fontSizeNum = (Number) parameters.get("fontSize");
        float fontSize = fontSizeNum != null ? fontSizeNum.floatValue() : 12.0f;

        if (fontSize <= 0) {
            throw ToolExecutionException.invalidParameters(getName(), "Font size must be positive");
        }

        if (id == null) {
            id = "text_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        }

        return new Text(id, position, textContent, "Arial", fontSize, style);
    }

    private Style createStyle(
            String fillColorHex,
            String strokeColorHex,
            float strokeWidth,
            Map<String, Object> parameters)
            throws ToolExecutionException {
        try {
            Color fillColor = Color.fromHex(fillColorHex);
            Color strokeColor = Color.fromHex(strokeColorHex);

            return Style.builder()
                    .fillColor(fillColor)
                    .strokeColor(strokeColor)
                    .strokeWeight(strokeWidth)
                    .build();
        } catch (IllegalArgumentException e) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Invalid color format: " + e.getMessage());
        }
    }

    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {

        // Validate that we have a current state to work with
        DrawingState currentState = requireCurrentState(context);

        // Validate canvas bounds if available
        int canvasWidth = context.getCanvasWidth();
        int canvasHeight = context.getCanvasHeight();

        float x = getRequiredParameter(parameters, "x", Number.class).floatValue();
        float y = getRequiredParameter(parameters, "y", Number.class).floatValue();

        if (x < 0 || x > canvasWidth || y < 0 || y > canvasHeight) {
            return ToolResult.builder()
                    .status(ToolResult.Status.ERROR)
                    .message(
                            String.format(
                                    "Position (%.1f, %.1f) is outside canvas bounds (0, 0, %d, %d)",
                                    x, y, canvasWidth, canvasHeight))
                    .errorType("BOUNDS_ERROR")
                    .build();
        }

        return executeInternalImpl(parameters, context);
    }

    private ToolResult executeInternalImpl(Map<String, Object> parameters, AgentContext context)
            throws ToolExecutionException {

        DrawingState currentState = requireCurrentState(context);

        // Extract basic parameters
        String shapeType =
                getRequiredParameter(parameters, "type", String.class)
                        .toLowerCase(java.util.Locale.ROOT);
        float x = getRequiredParameter(parameters, "x", Number.class).floatValue();
        float y = getRequiredParameter(parameters, "y", Number.class).floatValue();

        // Extract style parameters
        String fillColorHex =
                getOptionalParameter(parameters, "fillColor", String.class, "#000000");
        String strokeColorHex =
                getOptionalParameter(parameters, "strokeColor", String.class, "#000000");
        float strokeWidth =
                getOptionalParameter(parameters, "strokeWidth", Number.class, 1.0f).floatValue();

        // Extract optional parameters
        String customId = getOptionalParameter(parameters, "id", String.class, null);
        boolean visible = getOptionalParameter(parameters, "visible", Boolean.class, true);
        int layerOrder = getOptionalParameter(parameters, "layerOrder", Number.class, 0).intValue();

        // Create style
        Style style = createStyle(fillColorHex, strokeColorHex, strokeWidth, parameters);
        Position position = new Position(x, y);

        // Create the shape based on type
        Shape shape;
        try {
            switch (shapeType) {
                case "circle":
                    shape =
                            createCircle(
                                    customId, position, style, visible, layerOrder, parameters);
                    break;
                case "rectangle":
                case "rect":
                    shape =
                            createRectangle(
                                    customId, position, style, visible, layerOrder, parameters);
                    break;
                case "line":
                    shape = createLine(customId, position, style, visible, layerOrder, parameters);
                    break;
                case "text":
                    shape = createText(customId, position, style, visible, layerOrder, parameters);
                    break;
                default:
                    throw ToolExecutionException.invalidParameters(
                            getName(),
                            "Unsupported shape type: "
                                    + shapeType
                                    + ". Supported types: circle, rectangle, line, text");
            }
        } catch (IllegalArgumentException e) {
            throw ToolExecutionException.invalidParameters(
                    getName(), "Invalid parameters for " + shapeType + ": " + e.getMessage());
        }

        // Add the shape to the current state
        DrawingState newState = currentState.withAddedShape(shape);

        return ToolResult.success(
                        String.format(
                                "Added %s shape '%s' at position (%.1f, %.1f)",
                                shapeType, shape.getId(), x, y),
                        newState)
                .withData("shapeId", shape.getId())
                .withData("shapeType", shapeType)
                .withData("position", String.format("(%.1f, %.1f)", x, y));
    }
}
