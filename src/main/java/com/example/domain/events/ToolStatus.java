package com.example.domain.events;

/**
 * Enumeration of tool execution statuses.
 * Provides type-safe status tracking for tool operations.
 */
public enum ToolStatus {
    INITIALIZING("初期化中"),
    VALIDATING("検証中"),
    RUNNING("実行中"),
    COMPLETED("完了"),
    FAILED("失敗"),
    CANCELLED("キャンセル");
    
    private final String displayName;
    
    ToolStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}