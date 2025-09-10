package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.example.domain.serialization.YamlUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Text shape implementation Represents text with position, content, font properties, and styling
 */
public final class Text extends Shape {
    private final String content;
    private final String fontFamily;
    private final float fontSize;
    private final TextAlign horizontalAlign;
    private final TextAlign verticalAlign;

    public Text(
            String id,
            Position position,
            String content,
            String fontFamily,
            float fontSize,
            TextAlign horizontalAlign,
            TextAlign verticalAlign,
            Style style,
            boolean visible,
            int layerOrder) {
        super(id, position, style, visible, layerOrder);
        this.content = Objects.requireNonNull(content, "Text content cannot be null");
        this.fontFamily = fontFamily != null ? fontFamily : "Arial";
        this.fontSize = Math.max(1, fontSize);
        this.horizontalAlign = horizontalAlign != null ? horizontalAlign : TextAlign.LEFT;
        this.verticalAlign = verticalAlign != null ? verticalAlign : TextAlign.TOP;
    }

    public Text(
            String id,
            Position position,
            String content,
            String fontFamily,
            float fontSize,
            Style style) {
        this(
                id,
                position,
                content,
                fontFamily,
                fontSize,
                TextAlign.LEFT,
                TextAlign.TOP,
                style,
                true,
                0);
    }

    public Text(Position position, String content, String fontFamily, float fontSize, Style style) {
        this(generateId("text"), position, content, fontFamily, fontSize, style);
    }

    public Text(Position position, String content, float fontSize) {
        this(position, content, "Arial", fontSize, Style.NO_STROKE);
    }

    public Text(Position position, String content) {
        this(position, content, 12);
    }

    public String getContent() {
        return content;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public float getFontSize() {
        return fontSize;
    }

    public TextAlign getHorizontalAlign() {
        return horizontalAlign;
    }

    public TextAlign getVerticalAlign() {
        return verticalAlign;
    }

    public boolean isEmpty() {
        return content == null || content.isBlank();
    }

    public int getLength() {
        return content.length();
    }

    public int getLineCount() {
        if (isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    @Override
    public ShapeType getType() {
        return ShapeType.TEXT;
    }

    @Override
    public Text moveTo(Position newPosition) {
        return new Text(
                id,
                newPosition,
                content,
                fontFamily,
                fontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    @Override
    public Text withStyle(Style newStyle) {
        return new Text(
                id,
                position,
                content,
                fontFamily,
                fontSize,
                horizontalAlign,
                verticalAlign,
                newStyle,
                visible,
                layerOrder);
    }

    public Text withContent(String newContent) {
        return new Text(
                id,
                position,
                newContent,
                fontFamily,
                fontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    public Text withFont(String newFontFamily, float newFontSize) {
        return new Text(
                id,
                position,
                content,
                newFontFamily,
                newFontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    public Text withFontSize(float newFontSize) {
        return new Text(
                id,
                position,
                content,
                fontFamily,
                newFontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    public Text withAlignment(TextAlign newHorizontalAlign, TextAlign newVerticalAlign) {
        return new Text(
                id,
                position,
                content,
                fontFamily,
                fontSize,
                newHorizontalAlign,
                newVerticalAlign,
                style,
                visible,
                layerOrder);
    }

    @Override
    public boolean contains(Position point) {
        // For text, we approximate the bounds and check if point is within
        BoundingBox bounds = getBounds();
        return bounds.contains(point);
    }

    @Override
    public BoundingBox getBounds() {
        // Approximate text bounds - in a real implementation, this would
        // use actual font metrics
        float approximateCharWidth = fontSize * 0.6f;
        float lineHeight = fontSize * 1.2f;

        String[] lines = content.split("\n");
        float maxLineWidth = 0;
        for (String line : lines) {
            float lineWidth = line.length() * approximateCharWidth;
            maxLineWidth = Math.max(maxLineWidth, lineWidth);
        }

        float totalHeight = lines.length * lineHeight;

        // Adjust position based on alignment
        float x = position.getX();
        float y = position.getY();

        if (horizontalAlign == TextAlign.CENTER) {
            x -= maxLineWidth / 2;
        } else if (horizontalAlign == TextAlign.RIGHT) {
            x -= maxLineWidth;
        } else {
            // LEFT (default): no adjustment
        }

        if (verticalAlign == TextAlign.CENTER) {
            y -= totalHeight / 2;
        } else if (verticalAlign == TextAlign.BOTTOM) {
            y -= totalHeight;
        } else {
            // TOP (default): no adjustment
        }

        return new BoundingBox(x, y, maxLineWidth, totalHeight);
    }

    @Override
    protected Shape createWithProperties(
            String id, Position position, Style style, boolean visible, int layerOrder) {
        return new Text(
                id,
                position,
                content,
                fontFamily,
                fontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    @Override
    protected void performDraw(Object canvas) {
        // This would be implemented based on the specific rendering system
        // For now, it's a placeholder that would work with Processing
        if (canvas instanceof processing.core.PGraphics) {
            processing.core.PGraphics pg = (processing.core.PGraphics) canvas;

            // Apply text style
            if (style.hasFill()) {
                pg.fill(style.getFillColor().toProcessingColor());
            } else {
                pg.noFill();
            }

            if (style.hasStroke()) {
                pg.stroke(style.getStrokeColor().toProcessingColor());
                pg.strokeWeight(style.getStrokeWeight());
            } else {
                pg.noStroke();
            }

            // Set text properties
            pg.textSize(fontSize);

            // Convert alignment to Processing constants
            int hAlign = processing.core.PConstants.LEFT;
            switch (horizontalAlign) {
                case LEFT:
                    hAlign = processing.core.PConstants.LEFT;
                    break;
                case CENTER:
                    hAlign = processing.core.PConstants.CENTER;
                    break;
                case RIGHT:
                    hAlign = processing.core.PConstants.RIGHT;
                    break;
                case TOP: // map to LEFT for horizontal alignment
                case BOTTOM: // map to LEFT for horizontal alignment
                default:
                    hAlign = processing.core.PConstants.LEFT;
                    break;
            }

            int vAlign = processing.core.PConstants.TOP;
            switch (verticalAlign) {
                case TOP:
                    vAlign = processing.core.PConstants.TOP;
                    break;
                case CENTER:
                    vAlign = processing.core.PConstants.CENTER;
                    break;
                case BOTTOM:
                    vAlign = processing.core.PConstants.BOTTOM;
                    break;
                case LEFT: // map to TOP for vertical alignment
                case RIGHT: // map to TOP for vertical alignment
                default:
                    vAlign = processing.core.PConstants.TOP;
                    break;
            }

            pg.textAlign(hAlign, vAlign);

            // Draw text
            pg.text(content, position.getX(), position.getY());
        }
    }

    // Append text to existing content
    public Text append(String additionalText) {
        return withContent(content + additionalText);
    }

    // Prepend text to existing content
    public Text prepend(String prefixText) {
        return withContent(prefixText + content);
    }

    // Replace part of the text
    public Text replace(String target, String replacement) {
        return withContent(content.replace(target, replacement));
    }

    // Get a substring as a new Text object
    public Text substring(int start, int end) {
        if (start < 0 || end > content.length() || start > end) {
            throw new IllegalArgumentException("Invalid substring bounds");
        }
        return withContent(content.substring(start, end));
    }

    // Convert text to uppercase
    public Text toUpperCase() {
        return withContent(content.toUpperCase(Locale.ROOT));
    }

    // Convert text to lowercase
    public Text toLowerCase() {
        return withContent(content.toLowerCase(Locale.ROOT));
    }

    // Trim whitespace
    public Text trim() {
        return withContent(content.trim());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Text)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        Text text = (Text) obj;
        return Float.compare(text.fontSize, fontSize) == 0
                && Objects.equals(content, text.content)
                && Objects.equals(fontFamily, text.fontFamily)
                && horizontalAlign == text.horizontalAlign
                && verticalAlign == text.verticalAlign;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), content, fontFamily, fontSize, horizontalAlign, verticalAlign);
    }

    @Override
    public String toString() {
        return String.format(
                "Text(id=%s, content='%s', font=%s/%.1f, position=%s, visible=%s)",
                id,
                content.length() > 20 ? content.substring(0, 20) + "..." : content,
                fontFamily,
                fontSize,
                position,
                visible);
    }

    // YAML Serialization
    @Override
    public Map<String, Object> toYamlMap() {
        Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
        map.put("id", id);
        map.put("position", position.toYamlMap());
        map.put("content", content);
        map.put("fontFamily", fontFamily);
        map.put("fontSize", fontSize);
        map.put("horizontalAlign", horizontalAlign.name());
        map.put("verticalAlign", verticalAlign.name());
        map.put("style", style.toYamlMap());
        map.put("visible", visible);
        map.put("layerOrder", layerOrder);
        return map;
    }

    @Override
    public String getYamlType() {
        return "text";
    }

    public static Text fromYamlMap(Map<String, Object> yamlMap) {
        YamlUtils.validateYamlType(yamlMap, "text");
        YamlUtils.validateRequiredFields(yamlMap, "id", "position", "content");

        String id = YamlUtils.getRequiredString(yamlMap, "id");
        Position position = Position.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "position"));
        String content = YamlUtils.getRequiredString(yamlMap, "content");
        String fontFamily = YamlUtils.getString(yamlMap, "fontFamily", "Arial");
        float fontSize = YamlUtils.getFloat(yamlMap, "fontSize", 12.0f);

        TextAlign horizontalAlign = YamlUtils.getEnum(yamlMap, "horizontalAlign", TextAlign.class, TextAlign.LEFT);
        TextAlign verticalAlign = YamlUtils.getEnum(yamlMap, "verticalAlign", TextAlign.class, TextAlign.TOP);

        Style style = YamlUtils.getMap(yamlMap, "style", null) != null
                        ? Style.fromYamlMap(YamlUtils.getRequiredMap(yamlMap, "style"))
                        : Style.NO_STROKE;
        boolean visible = YamlUtils.getBoolean(yamlMap, "visible", true);
        int layerOrder = YamlUtils.getInt(yamlMap, "layerOrder", 0);

        return new Text(
                id,
                position,
                content,
                fontFamily,
                fontSize,
                horizontalAlign,
                verticalAlign,
                style,
                visible,
                layerOrder);
    }

    // Enum for text alignment
    public enum TextAlign {
        LEFT,
        CENTER,
        RIGHT,
        TOP,
        BOTTOM;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
