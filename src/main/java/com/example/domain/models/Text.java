package com.example.domain.models;

import com.example.domain.values.Position;
import com.example.domain.values.Style;

import java.util.Locale;
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

  public Text(String id, Position position, String content, String fontFamily, float fontSize,
      TextAlign horizontalAlign, TextAlign verticalAlign, Style style, boolean visible,
      int layerOrder) {
    super(id, position, style, visible, layerOrder);
    this.content = Objects.requireNonNull(content, "Text content cannot be null");
    this.fontFamily = fontFamily != null ? fontFamily : "Arial";
    this.fontSize = Math.max(1, fontSize);
    this.horizontalAlign = horizontalAlign != null ? horizontalAlign : TextAlign.LEFT;
    this.verticalAlign = verticalAlign != null ? verticalAlign : TextAlign.TOP;
  }


  public String getContent() {
    return content;
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



  @Override
  public ShapeType getType() {
    return ShapeType.TEXT;
  }

  @Override
  public Text moveTo(Position newPosition) {
    return new Text(id, newPosition, content, fontFamily, fontSize, horizontalAlign, verticalAlign,
        style, visible, layerOrder);
  }

  @Override
  public Text withStyle(Style newStyle) {
    return new Text(id, position, content, fontFamily, fontSize, horizontalAlign, verticalAlign,
        newStyle, visible, layerOrder);
  }

  public Text withContent(String newContent) {
    return new Text(id, position, newContent, fontFamily, fontSize, horizontalAlign, verticalAlign,
        style, visible, layerOrder);
  }

  public Text withFont(String newFontFamily, float newFontSize) {
    return new Text(id, position, content, newFontFamily, newFontSize, horizontalAlign,
        verticalAlign, style, visible, layerOrder);
  }

  public Text withFontSize(float newFontSize) {
    return new Text(id, position, content, fontFamily, newFontSize, horizontalAlign, verticalAlign,
        style, visible, layerOrder);
  }

  public Text withAlignment(TextAlign newHorizontalAlign, TextAlign newVerticalAlign) {
    return new Text(id, position, content, fontFamily, fontSize, newHorizontalAlign,
        newVerticalAlign, style, visible, layerOrder);
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
  protected Shape createWithProperties(String id, Position position, Style style, boolean visible,
      int layerOrder) {
    return new Text(id, position, content, fontFamily, fontSize, horizontalAlign, verticalAlign,
        style, visible, layerOrder);
  }

  @Override
  protected void performDraw(Object canvas) {
    // Drawing is handled by infrastructure layer renderers (TextRenderer)
    // This method is kept for backward compatibility but should not contain logic
    throw new UnsupportedOperationException("Drawing should be handled by TextRenderer in infrastructure layer");
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
    return Float.compare(text.fontSize, fontSize) == 0 && Objects.equals(content, text.content)
        && Objects.equals(fontFamily, text.fontFamily) && horizontalAlign == text.horizontalAlign
        && verticalAlign == text.verticalAlign;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), content, fontFamily, fontSize, horizontalAlign,
        verticalAlign);
  }



  // Enum for text alignment
  public enum TextAlign {
    LEFT, CENTER, RIGHT, TOP, BOTTOM;

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }
  }
}
