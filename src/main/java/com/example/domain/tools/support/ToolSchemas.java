package com.example.domain.tools.support;

import java.util.HashMap;
import java.util.Map;

/** Reusable JSON schema fragments for tool definitions. */
public final class ToolSchemas {
  private ToolSchemas() {}

  public static Map<String,Object> colorSchema() {
    Map<String,Object> color = new HashMap<>();
    color.put("type", "object");
    Map<String,Object> colorProps = new HashMap<>();
    colorProps.put("r", Map.of("type","integer","minimum",0,"maximum",255));
    colorProps.put("g", Map.of("type","integer","minimum",0,"maximum",255));
    colorProps.put("b", Map.of("type","integer","minimum",0,"maximum",255));
    colorProps.put("a", Map.of("type","integer","minimum",0,"maximum",255,"default",255));
    color.put("properties", colorProps);
    return color;
  }
}
