package com.example.domain.tools.impl;

import com.example.domain.commands.ClearCanvasCommand;
import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for clearing the entire canvas
 */
public class ClearCanvasTool implements IAgentTool {

  @Override
  public String name() {
    return "clear_canvas";
  }

  @Override
  public Map<String, Object> schema() {
    // 現状 ClearCanvasCommand は背景色変更機能を持たないため空オブジェクト
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");
    schema.put("properties", Map.of());
    schema.put("required", new String[] {});
    return schema;
  }

  @Override
  public DrawingState execute(Map<String, Object> parameters, DrawingState state) {
    // TODO: まだやらないが、背景色指定機能を実装する場合は ClearCanvasCommand 拡張 or 新Commandを追加
    return new ClearCanvasCommand().execute(state);
  }
}
