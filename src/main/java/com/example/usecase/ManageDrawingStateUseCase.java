package com.example.usecase;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IStateService;
import com.example.domain.services.ISessionManager;
import com.example.domain.services.ICommandExecutor;
import com.example.domain.commands.DrawCommand;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 描画状態を管理するユースケース
 * Undo/Redo、状態のクリア、セッションの保存/読み込みを担当
 */
public class ManageDrawingStateUseCase {
    
    private static final Logger LOGGER = Logger.getLogger(ManageDrawingStateUseCase.class.getName());
    
    private final IStateService stateService;
    private final ISessionManager sessionManager;
    private final ICommandExecutor commandExecutor;
    
    public interface Callback {
        void onStateChanged(DrawingState newState);
        void onMessage(String message);
        void onError(String error);
    }
    
    private Callback callback;
    
    public ManageDrawingStateUseCase(
            IStateService stateService,
            ISessionManager sessionManager,
            ICommandExecutor commandExecutor) {
        this.stateService = stateService;
        this.sessionManager = sessionManager;
        this.commandExecutor = commandExecutor;
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    /**
     * Undo操作を実行
     */
    public Result undo() {
        try {
            if (commandExecutor.canUndo()) {
                commandExecutor.undo();
                notifyStateChanged(stateService.getCurrentState());
                return Result.success("Undoを実行しました");
            } else {
                return Result.error("Undo履歴がありません");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Undoエラー", e);
            return Result.error("Undoエラー: " + e.getMessage());
        }
    }
    
    /**
     * Redo操作を実行
     */
    public Result redo() {
        try {
            if (commandExecutor.canRedo()) {
                commandExecutor.redo();
                notifyStateChanged(stateService.getCurrentState());
                return Result.success("Redoを実行しました");
            } else {
                return Result.error("Redo履歴がありません");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Redoエラー", e);
            return Result.error("Redoエラー: " + e.getMessage());
        }
    }
    
    /**
     * キャンバスをクリア
     */
    public Result clearCanvas() {
        try {
            DrawingState emptyState = DrawingState.empty();
            stateService.updateState(emptyState);
            commandExecutor.clearHistory();
            notifyStateChanged(emptyState);
            notifyMessage("キャンバスをクリアしました");
            return Result.success("キャンバスをクリアしました");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "クリアエラー", e);
            return Result.error("クリアエラー: " + e.getMessage());
        }
    }
    
    /**
     * 現在の状態を取得
     */
    public DrawingState getCurrentState() {
        return stateService.getCurrentState();
    }
    
    /**
     * セッションを保存
     */
    public Result saveSession(String filename) {
        try {
            sessionManager.saveSession(filename);
            notifyMessage("セッションを保存しました: " + filename);
            return Result.success("保存完了");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "保存エラー", e);
            notifyError("保存エラー: " + e.getMessage());
            return Result.error("保存エラー: " + e.getMessage());
        }
    }
    
    /**
     * セッションを読み込み
     */
    public Result loadSession(String filename) {
        try {
            var session = sessionManager.loadSession(filename);
            if (session != null) {
                stateService.initializeSession(session);
                notifyStateChanged(stateService.getCurrentState());
                notifyMessage("セッションを読み込みました: " + filename);
                return Result.success("読み込み完了");
            } else {
                return Result.error("セッションファイルが見つかりません");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "読み込みエラー", e);
            notifyError("読み込みエラー: " + e.getMessage());
            return Result.error("読み込みエラー: " + e.getMessage());
        }
    }
    
    /**
     * 自動保存を有効化
     */
    public void enableAutoSave(int intervalSeconds) {
        try {
            sessionManager.setAutoSaveInterval(intervalSeconds);
            sessionManager.setAutoSaveEnabled(true);
            LOGGER.info("自動保存を有効化: " + intervalSeconds + "秒間隔");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "自動保存設定エラー", e);
        }
    }
    
    /**
     * コマンドを直接実行
     */
    public Result executeCommand(DrawCommand command) {
        try {
            commandExecutor.execute(command);
            notifyStateChanged(stateService.getCurrentState());
            return Result.success("コマンド実行完了");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "コマンド実行エラー", e);
            return Result.error("コマンド実行エラー: " + e.getMessage());
        }
    }
    
    private void notifyStateChanged(DrawingState newState) {
        if (callback != null) {
            callback.onStateChanged(newState);
        }
    }
    
    private void notifyMessage(String message) {
        if (callback != null) {
            callback.onMessage(message);
        }
    }
    
    private void notifyError(String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
    
    /**
     * 実行結果
     */
    public static class Result {
        public final boolean success;
        public final String message;
        
        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static Result success(String message) {
            return new Result(true, message);
        }
        
        public static Result error(String message) {
            return new Result(false, message);
        }
    }
}