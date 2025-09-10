package com.example.infrastructure.adapters;

import com.example.domain.services.ICommandExecutor;

/**
 * CommandExecutorのアダプター
 * ドメインのICommandExecutorインタフェースを実装
 */
public class CommandExecutorAdapter implements ICommandExecutor {
    
    private final com.example.application.services.CommandExecutor commandExecutor;
    
    /**
     * デフォルトコンストラクタ
     * 標準のCommandExecutorインスタンスを作成
     */
    public CommandExecutorAdapter() {
        this.commandExecutor = com.example.application.services.CommandExecutor.getInstance();
    }
    
    /**
     * 依存性注入用コンストラクタ
     * @param commandExecutor CommandExecutorインスタンス
     */
    public CommandExecutorAdapter(com.example.application.services.CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public void execute(com.example.domain.commands.DrawCommand command) {
        commandExecutor.execute(command);
    }
    
    @Override
    public void undo() {
        // CommandExecutor doesn't have undo/redo functionality yet
        throw new UnsupportedOperationException("Undo not yet implemented");
    }
    
    @Override
    public void redo() {
        // CommandExecutor doesn't have undo/redo functionality yet
        throw new UnsupportedOperationException("Redo not yet implemented");
    }
    
    @Override
    public boolean canUndo() {
        return false; // Not implemented yet
    }
    
    @Override
    public boolean canRedo() {
        return false; // Not implemented yet
    }
    
    @Override
    public void clearHistory() {
        // No history to clear in current implementation
    }
    
    @Override
    public com.example.domain.commands.DrawCommand createCommand(com.example.domain.commands.ParsedCommand parsedCommand) {
        // Not implemented - CommandFactory was deleted
        return null;
    }
    
}