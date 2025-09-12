package com.example.domain.tools.impl;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.IAgentTool;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool for clearing the entire canvas
 */
public class ClearCanvasTool implements IAgentTool {

  // Feature flag removed: always facade
  private com.example.application.usecase.UseCaseFacade facade;

  public ClearCanvasTool withFacade(com.example.application.usecase.UseCaseFacade facade) {
    this.facade = facade;
    return this;
  }

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
    if (facade == null) {
      throw new IllegalStateException("UseCaseFacade not injected");
    }
    var result = facade.clearCanvas();
    return result.success() ? facade.getCurrentState() : state;
  }
}
