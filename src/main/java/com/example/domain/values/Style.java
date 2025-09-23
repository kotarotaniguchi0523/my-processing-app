package com.example.domain.values;


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


}
