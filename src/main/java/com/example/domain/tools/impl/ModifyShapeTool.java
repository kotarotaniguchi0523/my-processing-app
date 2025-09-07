package com.example.domain.tools.impl;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ModifyShapeTool enables modification of existing shapes in the drawing state. Can modify
 * position, size, style, visibility, and other shape properties.
 */
public class ModifyShapeTool implements IAgentTool {

    @Override
    public String name() {
        return "modifyShape";
    }

    @Override
    public Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("description", "Modify an existing shape's properties");

        Map<String, Object> properties = new HashMap<>();

        // Shape identifier - either ID or index
        Map<String, Object> idProperty = new HashMap<>();
        idProperty.put("type", "string");
        idProperty.put("description", "The ID of the shape to modify");
        properties.put("id", idProperty);

        Map<String, Object> indexProperty = new HashMap<>();
        indexProperty.put("type", "integer");
        indexProperty.put("description", "The index of the shape to modify (alternative to ID)");
        indexProperty.put("minimum", 0);
        properties.put("index", indexProperty);

        // Position modification
        Map<String, Object> positionProperty = new HashMap<>();
        positionProperty.put("type", "object");
        positionProperty.put("description", "New position for the shape");
        Map<String, Object> positionProps = new HashMap<>();
        positionProps.put("x", createNumberProperty("X coordinate"));
        positionProps.put("y", createNumberProperty("Y coordinate"));
        positionProperty.put("properties", positionProps);
        positionProperty.put("required", Arrays.asList("x", "y"));
        properties.put("position", positionProperty);

        // Shape-specific properties
        Map<String, Object> propertiesProperty = new HashMap<>();
        propertiesProperty.put("type", "object");
        propertiesProperty.put("description", "Shape-specific properties to modify");
        Map<String, Object> shapeProps = new HashMap<>();

        // For circle
        shapeProps.put("radius", createNumberProperty("New radius for circle shapes"));

        // For rectangle
        shapeProps.put("width", createNumberProperty("New width for rectangle shapes"));
        shapeProps.put("height", createNumberProperty("New height for rectangle shapes"));

        // For line
        Map<String, Object> toProperty = new HashMap<>();
        toProperty.put("type", "object");
        toProperty.put("description", "New end position for line shapes");
        toProperty.put("properties", positionProps);
        toProperty.put("required", Arrays.asList("x", "y"));
        shapeProps.put("to", toProperty);

        // For text
        shapeProps.put("text", createStringProperty("New text content for text shapes"));
        shapeProps.put("fontSize", createNumberProperty("New font size for text shapes"));

        propertiesProperty.put("properties", shapeProps);
        properties.put("properties", propertiesProperty);

        // Style modifications
        Map<String, Object> styleProperty = new HashMap<>();
        styleProperty.put("type", "object");
        styleProperty.put("description", "Style properties to modify");
        Map<String, Object> styleProps = new HashMap<>();

        styleProps.put("fillColor", createColorProperty("New fill color"));
        styleProps.put("strokeColor", createColorProperty("New stroke color"));
        styleProps.put("strokeWeight", createNumberProperty("New stroke weight"));
        styleProps.put("hasFill", createBooleanProperty("Whether the shape should have a fill"));
        styleProps.put(
                "hasStroke", createBooleanProperty("Whether the shape should have a stroke"));

        styleProperty.put("properties", styleProps);
        properties.put("style", styleProperty);

        // Visibility and layer
        properties.put("visible", createBooleanProperty("New visibility state"));
        properties.put(
                "layerOrder", createNumberProperty("New layer order (higher values are on top)"));

        schema.put("properties", properties);

        // At least one of id or index is required
        Map<String, Object> anyOf = new HashMap<>();
        anyOf.put(
                "anyOf",
                Arrays.asList(
                        Map.of("required", Arrays.asList("id")),
                        Map.of("required", Arrays.asList("index"))));
        schema.putAll(anyOf);

        return schema;
    }

    @Override
    public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
        validateParameters(parameters);

        // Find the shape to modify
        ShapeWithIndex shapeWithIndex = findShape(parameters, state);
        if (shapeWithIndex == null) {
            throw new IllegalArgumentException("Shape not found");
        }

        Shape originalShape = shapeWithIndex.shape;
        int shapeIndex = shapeWithIndex.index;

        // Apply modifications
        Shape modifiedShape = applyModifications(originalShape, parameters);

        // Replace the shape in the state
        return state.withReplacedShape(shapeIndex, modifiedShape);
    }

    private static class ShapeWithIndex {
        final Shape shape;
        final int index;

        ShapeWithIndex(Shape shape, int index) {
            this.shape = shape;
            this.index = index;
        }
    }

    private ShapeWithIndex findShape(Map<String, Object> parameters, DrawingState state) {
        List<Shape> shapes = state.getShapes();

        // Find by ID if provided
        if (parameters.containsKey("id")) {
            String targetId = (String) parameters.get("id");
            for (int i = 0; i < shapes.size(); i++) {
                Shape shape = shapes.get(i);
                if (targetId.equals(shape.getId())) {
                    return new ShapeWithIndex(shape, i);
                }
            }
            return null;
        }

        // Find by index if provided
        if (parameters.containsKey("index")) {
            int index = getInt(parameters, "index");
            if (index >= 0 && index < shapes.size()) {
                return new ShapeWithIndex(shapes.get(index), index);
            }
            return null;
        }

        return null;
    }

    private Shape applyModifications(Shape originalShape, Map<String, Object> parameters) {
        Shape modifiedShape = originalShape;

        // Apply position modification
        if (parameters.containsKey("position")) {
            Map<String, Object> positionData = getMap(parameters, "position");
            Position newPosition = parsePosition(positionData);
            modifiedShape = modifiedShape.moveTo(newPosition);
        }

        // Apply style modifications
        if (parameters.containsKey("style")) {
            Map<String, Object> styleData = getMap(parameters, "style");
            Style newStyle = mergeStyle(modifiedShape.getStyle(), styleData);
            modifiedShape = modifiedShape.withStyle(newStyle);
        }

        // Apply visibility modification
        if (parameters.containsKey("visible")) {
            boolean visible = getBoolean(parameters, "visible", true);
            modifiedShape = modifiedShape.withVisibility(visible);
        }

        // Apply layer order modification
        if (parameters.containsKey("layerOrder")) {
            int layerOrder = getInt(parameters, "layerOrder");
            modifiedShape = modifiedShape.withLayerOrder(layerOrder);
        }

        // Apply shape-specific modifications
        if (parameters.containsKey("properties")) {
            Map<String, Object> propertiesData = getMap(parameters, "properties");
            modifiedShape = applyShapeSpecificModifications(modifiedShape, propertiesData);
        }

        return modifiedShape;
    }

    private Shape applyShapeSpecificModifications(Shape shape, Map<String, Object> properties) {
        switch (shape.getType()) {
            case CIRCLE:
                return applyCircleModifications((Circle) shape, properties);
            case RECTANGLE:
                return applyRectangleModifications((Rectangle) shape, properties);
            case LINE:
                return applyLineModifications((Line) shape, properties);
            case TEXT:
                return applyTextModifications((Text) shape, properties);
            default:
                return shape; // No modifications for unsupported types
        }
    }

    private Circle applyCircleModifications(Circle circle, Map<String, Object> properties) {
        if (properties.containsKey("radius")) {
            float newRadius = getFloat(properties, "radius");
            return circle.withRadius(newRadius);
        }
        return circle;
    }

    private Rectangle applyRectangleModifications(
            Rectangle rectangle, Map<String, Object> properties) {
        if (properties.containsKey("width") || properties.containsKey("height")) {
            float newWidth = getFloat(properties, "width", rectangle.getWidth());
            float newHeight = getFloat(properties, "height", rectangle.getHeight());
            return rectangle.withDimensions(newWidth, newHeight);
        }
        return rectangle;
    }

    private Line applyLineModifications(Line line, Map<String, Object> properties) {
        if (properties.containsKey("to")) {
            Map<String, Object> toData = getMap(properties, "to");
            Position newEndPosition = parsePosition(toData);
            return line.withEndPosition(newEndPosition);
        }
        return line;
    }

    private Text applyTextModifications(Text text, Map<String, Object> properties) {
        Text modifiedText = text;

        if (properties.containsKey("text")) {
            String newTextContent = (String) properties.get("text");
            modifiedText = modifiedText.withContent(newTextContent);
        }

        if (properties.containsKey("fontSize")) {
            float newFontSize = getFloat(properties, "fontSize");
            modifiedText = modifiedText.withFontSize(newFontSize);
        }

        return modifiedText;
    }

    private Style mergeStyle(Style originalStyle, Map<String, Object> styleModifications) {
        Style.Builder builder =
                Style.builder()
                        .fillColor(originalStyle.getFillColor())
                        .strokeColor(originalStyle.getStrokeColor())
                        .strokeWeight(originalStyle.getStrokeWeight())
                        .hasFill(originalStyle.hasFill())
                        .hasStroke(originalStyle.hasStroke());

        // Apply style modifications
        if (styleModifications.containsKey("fillColor")) {
            Map<String, Object> fillColorData = getMap(styleModifications, "fillColor");
            builder.fillColor(parseColor(fillColorData));
        }

        if (styleModifications.containsKey("strokeColor")) {
            Map<String, Object> strokeColorData = getMap(styleModifications, "strokeColor");
            builder.strokeColor(parseColor(strokeColorData));
        }

        if (styleModifications.containsKey("strokeWeight")) {
            builder.strokeWeight(getFloat(styleModifications, "strokeWeight"));
        }

        if (styleModifications.containsKey("hasFill")) {
            builder.hasFill(getBoolean(styleModifications, "hasFill", true));
        }

        if (styleModifications.containsKey("hasStroke")) {
            builder.hasStroke(getBoolean(styleModifications, "hasStroke", true));
        }

        return builder.build();
    }

    private Position parsePosition(Map<String, Object> positionData) {
        if (positionData == null) {
            throw new IllegalArgumentException("Position data cannot be null");
        }

        float x = getFloat(positionData, "x");
        float y = getFloat(positionData, "y");
        return new Position(x, y);
    }

    private Color parseColor(Map<String, Object> colorData) {
        // Support different color formats
        if (colorData.containsKey("r")
                && colorData.containsKey("g")
                && colorData.containsKey("b")) {
            int r = getInt(colorData, "r");
            int g = getInt(colorData, "g");
            int b = getInt(colorData, "b");
            int a = getInt(colorData, "a", 255);
            return new Color(r, g, b, a);
        } else if (colorData.containsKey("hex")) {
            String hex = (String) colorData.get("hex");
            return Color.fromHex(hex);
        } else {
            throw new IllegalArgumentException(
                    "Color must have either 'r', 'g', 'b' values or 'hex' value");
        }
    }

    private void validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        // Must have either ID or index
        if (!parameters.containsKey("id") && !parameters.containsKey("index")) {
            throw new IllegalArgumentException(
                    "Either 'id' or 'index' must be provided to identify the shape");
        }

        // Must have at least one modification
        Set<String> modificationKeys =
                Set.of("position", "style", "properties", "visible", "layerOrder");
        boolean hasModification = parameters.keySet().stream().anyMatch(modificationKeys::contains);

        if (!hasModification) {
            throw new IllegalArgumentException(
                    "At least one modification must be specified: " + modificationKeys);
        }
    }

    // Utility methods for safe parameter extraction
    private Map<String, Object> getMap(Map<String, Object> params, String key) {
        return getMap(params, key, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(
            Map<String, Object> params, String key, Map<String, Object> defaultValue) {
        Object value = params.get(key);
        return value instanceof Map ? (Map<String, Object>) value : defaultValue;
    }

    private float getFloat(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter missing: " + key);
        }
        return ((Number) value).floatValue();
    }

    private float getFloat(Map<String, Object> params, String key, float defaultValue) {
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    private int getInt(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter missing: " + key);
        }
        return ((Number) value).intValue();
    }

    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    // Schema helper methods
    private Map<String, Object> createNumberProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "number");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createStringProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createBooleanProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "boolean");
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> createColorProperty(String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "object");
        prop.put("description", description);

        Map<String, Object> props = new HashMap<>();
        props.put("r", createNumberProperty("Red component (0-255)"));
        props.put("g", createNumberProperty("Green component (0-255)"));
        props.put("b", createNumberProperty("Blue component (0-255)"));
        props.put("a", createNumberProperty("Alpha component (0-255, optional)"));
        props.put("hex", createStringProperty("Hex color string (alternative to RGB)"));

        prop.put("properties", props);
        return prop;
    }
}
