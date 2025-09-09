package com.example.domain.tools;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IToolRegistry defines the abstraction for tool discovery and schema access.
 * Application層はこのインターフェースのみに依存し、実装詳細（シングルトン等）に依存しない。
 */
public interface IToolRegistry {

    boolean hasTool(String name);

    IAgentTool getTool(String name);

    Set<String> getToolNames();

    List<IAgentTool> getAllTools();

    /**
     * OpenAI Functions 互換のツールスキーマ一覧を返す。
     */
    List<Map<String, Object>> generateToolSchemas();
}

