package com.example.domain.events;

/**
 * Enumeration of failure points in AI processing.
 * Helps identify where in the pipeline a failure occurred.
 */
public enum FailurePoint {
    CONNECTION("接続エラー"),
    PROMPT_PARSING("プロンプト解析"),
    TOOL_SELECTION("ツール選択"),
    TOOL_VALIDATION("ツール検証"),
    TOOL_EXECUTION("ツール実行"),
    RESPONSE_GENERATION("応答生成"),
    STATE_UPDATE("状態更新"),
    TIMEOUT("タイムアウト"),
    CANCELLED("キャンセル"),
    UNKNOWN("不明");
    
    private final String displayName;
    
    FailurePoint(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}