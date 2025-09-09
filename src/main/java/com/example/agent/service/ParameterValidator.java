package com.example.agent.service;

import com.example.domain.tools.IAgentTool;

import java.util.Map;

/**
 * パラメータ検証の抽象。ToolInvokerはこのインターフェースに依存する。
 */
public interface ParameterValidator {
    ValidationResult validateParameters(IAgentTool tool, Map<String, Object> parameters);
}

