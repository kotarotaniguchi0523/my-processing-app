package com.example.adapter.tools;

import java.util.Map;
import java.util.function.Function;

/**
 * Parameter extraction helper used by agent tools (adapter layer). Centralizes runtime validation
 * to reduce duplication inside each tool.
 */
public final class Parameters {
  private Parameters() {}

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obj(Map<String, Object> src, String key, boolean required) {
    Object v = src.get(key);
    if (v == null) {
      if (required)
        throw new IllegalArgumentException("Missing object: " + key);
      return null;
    }
    if (v instanceof Map)
      return (Map<String, Object>) v;
    throw new IllegalArgumentException("Field '" + key + "' must be object");
  }

  public static Number num(Map<String, Object> src, String key, boolean required) {
    Object v = src.get(key);
    if (v == null) {
      if (required)
        throw new IllegalArgumentException("Missing number: " + key);
      return null;
    }
    if (v instanceof Number)
      return (Number) v;
    throw new IllegalArgumentException("Field '" + key + "' must be number");
  }

  public static Boolean bool(Map<String, Object> src, String key) {
    Object v = src.get(key);
    if (v == null)
      return null;
    if (v instanceof Boolean)
      return (Boolean) v;
    throw new IllegalArgumentException("Field '" + key + "' must be boolean");
  }

  public static <T> T map(Map<String, Object> src, String key, boolean required,
      Function<Map<String, Object>, T> mapper) {
    Map<String, Object> m = obj(src, key, required);
    if (m == null)
      return null;
    return mapper.apply(m);
  }
}
