package com.example.domain.tools.support;

import com.example.domain.values.Color;
import com.example.domain.values.Style;
import java.util.Map;

/** Common color/style extraction used across creation & mutation tools. */
public final class ToolStyleUtil {
  private ToolStyleUtil() {}

  public static Color extractColor(Map<String, Object> map) {
    if (map == null) return new Color(0,0,0,255);
    Number r = num(map, "r", 0);
    Number g = num(map, "g", 0);
    Number b = num(map, "b", 0);
    Number a = num(map, "a", 255);
    return new Color(r.intValue(), g.intValue(), b.intValue(), a.intValue());
  }

  private static Number num(Map<String, Object> src, String key, int def) {
    Object v = src.get(key);
    if (v == null) return def;
    if (v instanceof Number n) return n;
    throw new IllegalArgumentException("Field '" + key + "' must be number");
  }

  /** Apply optional stroke/fill related common params: strokeColor, fillColor, strokeWeight, filled. */
  public static void applyOptionalStrokeFill(Map<String, Object> params, Style.Builder builder) {
    @SuppressWarnings("unchecked") Map<String,Object> strokeColor = (Map<String,Object>) params.get("strokeColor");
    if (strokeColor != null) builder.strokeColor(extractColor(strokeColor));
    @SuppressWarnings("unchecked") Map<String,Object> fillColor = (Map<String,Object>) params.get("fillColor");
    if (fillColor != null) builder.fillColor(extractColor(fillColor));
    Object sw = params.get("strokeWeight");
    if (sw instanceof Number n) builder.strokeWeight(n.floatValue());
    Object filled = params.get("filled");
    if (filled instanceof Boolean b) {
      if (b) builder.hasFill(true); else builder.noFill();
    }
  }
}
