package com.example.domain.values;

import com.example.domain.serialization.YamlUtils;

import java.util.Map;

/**
 * Style value object representing visual styling properties Immutable value object for styling
 * information
 */
public final class Style {
  private final Color fillColor;
  private final Color strokeColor;
  private final float strokeWeight;
  private final boolean hasFill;
  private final boolean hasStroke;

  public Style(Color fillColor, Color strokeColor, float strokeWeight, boolean hasFill,
      boolean hasStroke) {
    this.fillColor = fillColor;
    this.strokeColor = strokeColor;
    this.strokeWeight = Math.max(0, strokeWeight);
    this.hasFill = hasFill;
    this.hasStroke = hasStroke;
  }

  // Builder pattern for convenience
  public static class Builder {
    private Color fillColor = Color.WHITE;
    private Color strokeColor = Color.BLACK;
    private float strokeWeight = 1.0f;
    private boolean hasFill = true;
    private boolean hasStroke = true;

    public Builder fillColor(Color fillColor) {
      this.fillColor = fillColor;
      return this;
    }

    public Builder strokeColor(Color strokeColor) {
      this.strokeColor = strokeColor;
      return this;
    }

    public Builder strokeWeight(float strokeWeight) {
      this.strokeWeight = strokeWeight;
      return this;
    }

    public Builder hasFill(boolean hasFill) {
      this.hasFill = hasFill;
      return this;
    }

    public Builder hasStroke(boolean hasStroke) {
      this.hasStroke = hasStroke;
      return this;
    }

    public Builder noFill() {
      this.hasFill = false;
      return this;
    }

    public Builder noStroke() {
      this.hasStroke = false;
      return this;
    }

    public Style build() {
      return new Style(fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Color getFillColor() {
    return fillColor;
  }

  public Color getStrokeColor() {
    return strokeColor;
  }

  public float getStrokeWeight() {
    return strokeWeight;
  }

  public boolean hasFill() {
    return hasFill;
  }

  public boolean hasStroke() {
    return hasStroke;
  }

  public Style withFillColor(Color fillColor) {
    return new Style(fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
  }

  public Style withStrokeColor(Color strokeColor) {
    return new Style(fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
  }

  public Style withStrokeWeight(float strokeWeight) {
    return new Style(fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Style)) {
      return false;
    }
    Style style = (Style) obj;
    return Float.compare(style.strokeWeight, strokeWeight) == 0 && hasFill == style.hasFill
        && hasStroke == style.hasStroke
        && (fillColor != null ? fillColor.equals(style.fillColor) : style.fillColor == null)
        && (strokeColor != null ? strokeColor.equals(style.strokeColor)
            : style.strokeColor == null);
  }

  @Override
  public int hashCode() {
    int result = fillColor != null ? fillColor.hashCode() : 0;
    result = 31 * result + (strokeColor != null ? strokeColor.hashCode() : 0);
    result = 31 * result + (strokeWeight != +0.0f ? Float.floatToIntBits(strokeWeight) : 0);
    result = 31 * result + (hasFill ? 1 : 0);
    result = 31 * result + (hasStroke ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("Style(fill=%s, stroke=%s, weight=%.2f, hasFill=%s, hasStroke=%s)",
        fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
  }

  // Default styles
  public static final Style DEFAULT = new Style(Color.WHITE, Color.BLACK, 1.0f, true, true);
  public static final Style NO_FILL = new Style(Color.TRANSPARENT, Color.BLACK, 1.0f, false, true);
  public static final Style NO_STROKE =
      new Style(Color.WHITE, Color.TRANSPARENT, 0.0f, true, false);

  /**
   * デフォルトスタイルを返すメソッド
   */
  public static Style defaultStyle() {
    return DEFAULT;
  }

  // YAML Serialization

  public Map<String, Object> toYamlMap() {
    Map<String, Object> map = YamlUtils.createYamlMap(getYamlType());
    YamlUtils.putIfNotNull(map, "fillColor", hasFill ? fillColor.toYamlMap() : null);
    YamlUtils.putIfNotNull(map, "strokeColor", hasStroke ? strokeColor.toYamlMap() : null);
    map.put("strokeWeight", strokeWeight);
    map.put("hasFill", hasFill);
    map.put("hasStroke", hasStroke);
    return map;
  }


  public String getYamlType() {
    return "style";
  }

  public static Style fromYamlMap(Map<String, Object> yamlMap) {
    YamlUtils.validateYamlType(yamlMap, "style");

    Color fillColor = Color.WHITE;
    Color strokeColor = Color.BLACK;

    Map<String, Object> fillColorMap = YamlUtils.getMap(yamlMap, "fillColor", null);
    if (fillColorMap != null) {
      fillColor = Color.fromYamlMap(fillColorMap);
    }

    Map<String, Object> strokeColorMap = YamlUtils.getMap(yamlMap, "strokeColor", null);
    if (strokeColorMap != null) {
      strokeColor = Color.fromYamlMap(strokeColorMap);
    }

    float strokeWeight = YamlUtils.getFloat(yamlMap, "strokeWeight", 1.0f);
    boolean hasFill = YamlUtils.getBoolean(yamlMap, "hasFill", true);
    boolean hasStroke = YamlUtils.getBoolean(yamlMap, "hasStroke", true);

    return new Style(fillColor, strokeColor, strokeWeight, hasFill, hasStroke);
  }
}
