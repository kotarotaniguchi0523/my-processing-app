package com.example.domain.services;

import com.example.domain.commands.DrawCommand;
import com.example.domain.commands.ParsedCommand;

/**
 * コマンド実行サービスのインターフェース
 */
public interface ICommandExecutor {
    
    /**
     * コマンドを実行
     */
    void execute(DrawCommand command);
    
    /**
     * Undoを実行
     */
    void undo();
    
    /**
     * Redoを実行
     */
    void redo();
    
    /**
     * Undo可能かチェック
     */
    boolean canUndo();
    
    /**
     * Redo可能かチェック
     */
    boolean canRedo();
    
    /**
     * 履歴をクリア
     */
    void clearHistory();
    
    /**
     * パース済みコマンドからDrawCommandを作成
     */
    DrawCommand createCommand(ParsedCommand parsedCommand);
}