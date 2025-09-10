package com.example.infrastructure.adapters;

import com.example.domain.services.ICommandExecutor;

/**
 * CommandExecutorのアダプター
 * ドメインのICommandExecutorインタフェースを実装
 */
public class CommandExecutorAdapter implements ICommandExecutor {
    
    private final com.example.application.services.CommandExecutor commandExecutor;
    private final com.example.application.CommandFactory commandFactory;
    
    /**
     * デフォルトコンストラクタ
     * 標準のCommandExecutorインスタンスを作成
     */
    public CommandExecutorAdapter() {
        this.commandExecutor = com.example.application.services.CommandExecutor.getInstance();
        this.commandFactory = new com.example.application.CommandFactory();
    }
    
    /**
     * 依存性注入用コンストラクタ
     * @param commandExecutor CommandExecutorインスタンス
     */
    public CommandExecutorAdapter(com.example.application.services.CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.commandFactory = new com.example.application.CommandFactory();
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
        if (parsedCommand == null) {
            return null;
        }
        
        // Use CommandFactory to create the appropriate command
        return commandFactory.createCommandFromParsed(parsedCommand);
    }
    
    /**
     * コマンド文字列を実行 (backward compatibility)
     * @param command コマンド文字列
     * @return 実行結果
     */
    public boolean execute(String command) {
        try {
            // Parse the command string first
            com.example.application.CommandParser parser = new com.example.application.CommandParser();
            com.example.domain.commands.ParsedCommand parsed = parser.parse(command);
            if (parsed != null) {
                com.example.domain.commands.DrawCommand drawCommand = createCommand(parsed);
                if (drawCommand != null) {
                    execute(drawCommand);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * パース済みコマンドを実行 (backward compatibility)
     * @param command パース済みコマンド
     * @return 実行結果
     */
    public boolean execute(com.example.domain.commands.ParsedCommand command) {
        if (command == null) {
            return false;
        }
        
        try {
            com.example.domain.commands.DrawCommand drawCommand = createCommand(command);
            if (drawCommand != null) {
                execute(drawCommand);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error executing parsed command: " + e.getMessage());
            return false;
        }
    }
}