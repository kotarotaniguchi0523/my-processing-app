package com.example.infrastructure.agent.tools;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.models.Triangle;
import com.example.domain.tools.IAgentTool;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CreateShapeTool enables the creation of new shapes and adds them to the drawing state. Supports
 * creating circles, rectangles, lines, triangles, and text with customizable properties.
 */
public class CreateShapeTool implements IAgentTool {

  @Override
  public String name() {
    return "createShape";
  }

  @Override
  public Map<String, Object> schema() {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");
    schema.put("description", "Create a new shape and add it to the drawing canvas");

    Map<String, Object> properties = new HashMap<>();

    // Shape type
    Map<String, Object> typeProperty = new HashMap<>();
    typeProperty.put("type", "string");
    typeProperty.put("description", "The type of shape to create");
    typeProperty.put("enum", Arrays.asList("circle", "rectangle", "line", "text", "triangle"));
    properties.put("type", typeProperty);

    // Position
    Map<String, Object> positionProperty = new HashMap<>();
    positionProperty.put("type", "object");
    positionProperty.put("description", "The position of the shape");
    Map<String, Object> positionProps = new HashMap<>();
    positionProps.put("x", createNumberProperty("X coordinate"));
    positionProps.put("y", createNumberProperty("Y coordinate"));
    positionProperty.put("properties", positionProps);
    positionProperty.put("required", Arrays.asList("x", "y"));
    properties.put("position", positionProperty);

    // Optional ID
    Map<String, Object> idProperty = new HashMap<>();
    idProperty.put("type", "string");
    idProperty.put("description",
        "Optional unique ID for the shape. If not provided, one will be generated");
    properties.put("id", idProperty);

    // Shape-specific properties
    Map<String, Object> propertiesProperty = new HashMap<>();
    propertiesProperty.put("type", "object");
    propertiesProperty.put("description", "Shape-specific properties");
    Map<String, Object> shapeProps = new HashMap<>();

    // For circle
    shapeProps.put("radius", createNumberProperty("Radius for circle shapes"));

    // For rectangle
    shapeProps.put("width", createNumberProperty("Width for rectangle shapes"));
    shapeProps.put("height", createNumberProperty("Height for rectangle shapes"));

    // For line
    Map<String, Object> toProperty = new HashMap<>();
    toProperty.put("type", "object");
    toProperty.put("description", "End position for line shapes");
    toProperty.put("properties", positionProps);
    toProperty.put("required", Arrays.asList("x", "y"));
    shapeProps.put("to", toProperty);

    // For text
    shapeProps.put("text", createStringProperty("Text content for text shapes"));
    shapeProps.put("fontSize", createNumberProperty("Font size for text shapes"));

    // For triangle
    Map<String, Object> p1Property = new HashMap<>();
    p1Property.put("type", "object");
    p1Property.put("description", "First vertex for triangle shapes");
    p1Property.put("properties", positionProps);
    p1Property.put("required", Arrays.asList("x", "y"));
    shapeProps.put("p1", p1Property);
    
    Map<String, Object> p2Property = new HashMap<>();
    p2Property.put("type", "object");
    p2Property.put("description", "Second vertex for triangle shapes");
    p2Property.put("properties", positionProps);
    p2Property.put("required", Arrays.asList("x", "y"));
    shapeProps.put("p2", p2Property);
    
    Map<String, Object> p3Property = new HashMap<>();
    p3Property.put("type", "object");
    p3Property.put("description", "Third vertex for triangle shapes");
    p3Property.put("properties", positionProps);
    p3Property.put("required", Arrays.asList("x", "y"));
    shapeProps.put("p3", p3Property);

    propertiesProperty.put("properties", shapeProps);
    properties.put("properties", propertiesProperty);

    // Style properties
    Map<String, Object> styleProperty = new HashMap<>();
    styleProperty.put("type", "object");
    styleProperty.put("description", "Visual styling properties");
    Map<String, Object> styleProps = new HashMap<>();

    // Fill color
    styleProps.put("fillColor", createColorProperty("Fill color"));
    styleProps.put("strokeColor", createColorProperty("Stroke color"));
    styleProps.put("strokeWeight", createNumberProperty("Stroke weight"));
    styleProps.put("hasFill", createBooleanProperty("Whether the shape has a fill"));
    styleProps.put("hasStroke", createBooleanProperty("Whether the shape has a stroke"));

    styleProperty.put("properties", styleProps);
    properties.put("style", styleProperty);

    // Visibility and layer
    properties.put("visible", createBooleanProperty("Whether the shape is visible"));
    properties.put("layerOrder", createNumberProperty("Layer order (higher values are on top)"));

    schema.put("properties", properties);
    schema.put("required", Arrays.asList("type", "position"));

    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    validateParameters(parameters);

    String shapeType = (String) parameters.get("type");
    Map<String, Object> positionData = getMap(parameters, "position");

    // Provide default position at center of canvas area if not specified
    if (positionData == null || positionData.isEmpty()) {
      positionData = new HashMap<>();
      // Default to center of visible canvas area (980x335)
      // Canvas center: x=490, y=167.5
      positionData.put("x", 490.0);
      positionData.put("y", 167.5);
    }

    // Handle both "properties" and direct size parameters from AI
    Map<String, Object> propertiesData = getMap(parameters, "properties", new HashMap<>());
    Map<String, Object> sizeData = getMap(parameters, "size");
    if (sizeData != null) {
      // Convert AI's size format to properties format
      if (sizeData.containsKey("width")) {
        propertiesData.put("width", sizeData.get("width"));
      }
      if (sizeData.containsKey("height")) {
        propertiesData.put("height", sizeData.get("height"));
      }
      // For circles, use width as radius
      if ("circle".equalsIgnoreCase(shapeType) && sizeData.containsKey("width")) {
        propertiesData.put("radius", ((Number) sizeData.get("width")).doubleValue() / 2.0);
      }
    }

    // Handle both "style" and direct color parameter from AI
    Map<String, Object> styleData = getMap(parameters, "style", new HashMap<>());
    String directColor = (String) parameters.get("color");
    if (directColor != null && styleData.isEmpty()) {
      // Convert direct color to style format
      styleData = new HashMap<>();
      Map<String, Object> fillColorData = parseColorString(directColor);
      styleData.put("fillColor", fillColorData);
      styleData.put("hasFill", true);
    }

    // Extract basic properties
    String id = (String) parameters.get("id");
    Position position = parsePosition(positionData);
    Style style = parseStyle(styleData);
    boolean visible = getBoolean(parameters, "visible", true);
    int layerOrder = getInt(parameters, "layerOrder", 0);

    // Create shape based on type
    Shape shape =
        createShapeByType(shapeType, id, position, propertiesData, style, visible, layerOrder);

    // Add shape to state
    return state.withAddedShape(shape);
  }

  private Shape createShapeByType(String type, String id, Position position,
      Map<String, Object> properties, Style style, boolean visible, int layerOrder) {

    // Generate ID if not provided
    if (id == null || id.isBlank()) {
      id = type + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    switch (type.toLowerCase(Locale.ROOT)) {
      case "circle":
        float radius = getFloat(properties, "radius", 60.0f);
        return new Circle(id, position, radius, style, visible, layerOrder);

      case "rectangle":
        float width = getFloat(properties, "width", 300.0f);
        float height = getFloat(properties, "height", 200.0f);
        return new Rectangle(id, position, width, height, style, visible, layerOrder);

      case "line":
        Map<String, Object> toData = getMap(properties, "to");
        if (toData == null) {
          throw new IllegalArgumentException("Line shape requires 'to' position in properties");
        }
        Position toPosition = parsePosition(toData);
        return new Line(id, position, toPosition, style, visible, layerOrder);

      case "text":
        String text = (String) properties.get("text");
        if (text == null || text.isBlank()) {
          throw new IllegalArgumentException("Text shape requires 'text' content in properties");
        }
        float fontSize = getFloat(properties, "fontSize", 12.0f);
        String fontFamily = (String) properties.getOrDefault("fontFamily", "Arial");
        return new Text(id, position, text, fontFamily, fontSize, Text.TextAlign.LEFT,
            Text.TextAlign.TOP, style, visible, layerOrder);

      case "triangle":
        // Handle both explicit vertex points and width/height format
        Map<String, Object> p1Data = getMap(properties, "p1");
        Map<String, Object> p2Data = getMap(properties, "p2");
        Map<String, Object> p3Data = getMap(properties, "p3");
        
        if (p1Data != null && p2Data != null && p3Data != null) {
          // Explicit three points
          Position p1 = parsePosition(p1Data);
          Position p2 = parsePosition(p2Data);
          Position p3 = parsePosition(p3Data);
          return new Triangle(id, position, p1, p2, p3, style, visible, layerOrder);
        } else {
          // Use width/height format (creates an isosceles triangle)
          float triWidth = getFloat(properties, "width", 100.0f);
          float triHeight = getFloat(properties, "height", 80.0f);
          
          // Calculate vertices centered around the position
          // The position is the center bottom of the triangle
          Position p1 = new Position(position.getX() - triWidth/2, position.getY());
          Position p2 = new Position(position.getX() + triWidth/2, position.getY());
          Position p3 = new Position(position.getX(), position.getY() - triHeight);
          
          return new Triangle(id, position, p1, p2, p3, style, visible, layerOrder);
        }

      default:
        throw new IllegalArgumentException("Unsupported shape type: " + type);
    }
  }

  private Position parsePosition(Map<String, Object> positionData) {
    if (positionData == null) {
      throw new IllegalArgumentException("Position is required");
    }

    float x = getFloat(positionData, "x");
    float y = getFloat(positionData, "y");
    return new Position(x, y);
  }

  private Map<String, Object> parseColorString(String colorString) {
    Map<String, Object> colorData = new HashMap<>();

    // Handle named colors
    String lowerColor = colorString.toLowerCase();
    switch (lowerColor) {
      case "red":
        colorData.put("r", 255);
        colorData.put("g", 0);
        colorData.put("b", 0);
        break;
      case "green":
        colorData.put("r", 0);
        colorData.put("g", 255);
        colorData.put("b", 0);
        break;
      case "blue":
        colorData.put("r", 0);
        colorData.put("g", 0);
        colorData.put("b", 255);
        break;
      case "yellow":
        colorData.put("r", 255);
        colorData.put("g", 255);
        colorData.put("b", 0);
        break;
      case "black":
        colorData.put("r", 0);
        colorData.put("g", 0);
        colorData.put("b", 0);
        break;
      case "white":
        colorData.put("r", 255);
        colorData.put("g", 255);
        colorData.put("b", 255);
        break;
      case "orange":
        colorData.put("r", 255);
        colorData.put("g", 165);
        colorData.put("b", 0);
        break;
      case "purple":
        colorData.put("r", 128);
        colorData.put("g", 0);
        colorData.put("b", 128);
        break;
      case "pink":
        colorData.put("r", 255);
        colorData.put("g", 192);
        colorData.put("b", 203);
        break;
      case "gray":
      case "grey":
        colorData.put("r", 128);
        colorData.put("g", 128);
        colorData.put("b", 128);
        break;
      default:
        // Handle hex colors
        if (colorString.startsWith("#")) {
          try {
            String hex = colorString.substring(1);
            if (hex.length() == 6) {
              colorData.put("r", Integer.parseInt(hex.substring(0, 2), 16));
              colorData.put("g", Integer.parseInt(hex.substring(2, 4), 16));
              colorData.put("b", Integer.parseInt(hex.substring(4, 6), 16));
            }
          } catch (NumberFormatException e) {
            // Default to black if parsing fails
            colorData.put("r", 0);
            colorData.put("g", 0);
            colorData.put("b", 0);
          }
        } else {
          // Default to black for unknown colors
          colorData.put("r", 0);
          colorData.put("g", 0);
          colorData.put("b", 0);
        }
    }

    colorData.put("a", 255); // Full opacity by default
    return colorData;
  }

  private Style parseStyle(Map<String, Object> styleData) {
    Style.Builder builder = Style.builder();

    // Parse fill color
    Map<String, Object> fillColorData = getMap(styleData, "fillColor");
    if (fillColorData != null) {
      builder.fillColor(parseColor(fillColorData));
    }

    // Parse stroke color
    Map<String, Object> strokeColorData = getMap(styleData, "strokeColor");
    if (strokeColorData != null) {
      builder.strokeColor(parseColor(strokeColorData));
    }

    // Parse other style properties
    if (styleData.containsKey("strokeWeight")) {
      builder.strokeWeight(getFloat(styleData, "strokeWeight"));
    }

    if (styleData.containsKey("hasFill")) {
      builder.hasFill(getBoolean(styleData, "hasFill", true));
    }

    if (styleData.containsKey("hasStroke")) {
      builder.hasStroke(getBoolean(styleData, "hasStroke", true));
    }

    return builder.build();
  }

  private Color parseColor(Map<String, Object> colorData) {
    // Support different color formats
    if (colorData.containsKey("r") && colorData.containsKey("g") && colorData.containsKey("b")) {
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

    if (!parameters.containsKey("type")) {
      throw new IllegalArgumentException("Shape type is required");
    }

    if (!parameters.containsKey("position")) {
      throw new IllegalArgumentException("Position is required");
    }

    String type = (String) parameters.get("type");
    Set<String> validTypes = Set.of("circle", "rectangle", "line", "text", "triangle");
    if (!validTypes.contains(type.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(
          "Invalid shape type: " + type + ". Valid types: " + validTypes);
    }
  }

  // Utility methods for safe parameter extraction
  private Map<String, Object> getMap(Map<String, Object> params, String key) {
    return getMap(params, key, null);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getMap(Map<String, Object> params, String key,
      Map<String, Object> defaultValue) {
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
