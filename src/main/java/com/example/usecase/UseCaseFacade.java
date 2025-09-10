package com.example.usecase;

import com.example.domain.models.DrawingState;
import com.example.domain.services.*;
import com.example.domain.parsers.ICommandParser;
import com.example.infrastructure.adapters.*;
import com.example.infrastructure.ServiceInitializer;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * ユースケースのファサード
 * すべてのユースケースへの統一されたアクセスポイントを提供
 */
public class UseCaseFacade {
    
    private static final Logger LOGGER = Logger.getLogger(UseCaseFacade.class.getName());
    
    private static UseCaseFacade instance;
    
    private final ProcessUserInputUseCase processUserInputUseCase;
    private final ManageDrawingStateUseCase manageDrawingStateUseCase;
    
    // 統合コールバック
    public interface Callback {
        void onStatusUpdate(String status);
        void onChatMessage(String message);
        void onApprovalRequired(String summary, String diff);
        void onStateChanged(DrawingState newState);
        void onError(String error);
        void onMessage(String message);
    }
    
    private Callback callback;
    
    /**
     * プライベートコンストラクタ（シングルトン）
     */
    private UseCaseFacade() {
        // 初期化
        ServiceInitializer.initialize();
        
        // 依存関係の構築
        IAgentService agentService = new AgentServiceAdapter();
        IDiffService diffService = new DiffServiceAdapter();
        IStateService stateService = new StateServiceAdapter();
        ICommandExecutor commandExecutor = new CommandExecutorAdapter();
        ICommandParser commandParser = new CommandParserAdapter();
        ISessionManager sessionManager = new SessionManagerAdapter();
        
        // ユースケースの初期化
        this.processUserInputUseCase = new ProcessUserInputUseCase(
            agentService, diffService, stateService, commandExecutor, commandParser
        );
        
        this.manageDrawingStateUseCase = new ManageDrawingStateUseCase(
            stateService, sessionManager, commandExecutor
        );
        
        // コールバックの設定
        setupCallbacks();
        
        // 自動保存の有効化（5分間隔）
        manageDrawingStateUseCase.enableAutoSave(300);
        
        LOGGER.info("UseCaseFacade初期化完了");
    }
    
    /**
     * シングルトンインスタンスの取得
     */
    public static synchronized UseCaseFacade getInstance() {
        if (instance == null) {
            instance = new UseCaseFacade();
        }
        return instance;
    }
    
    /**
     * コールバックの設定
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    /**
     * 内部コールバックのセットアップ
     */
    private void setupCallbacks() {
        // ProcessUserInputUseCaseのコールバック
        processUserInputUseCase.setCallback(new ProcessUserInputUseCase.Callback() {
            @Override
            public void onStatusUpdate(String status) {
                if (callback != null) callback.onStatusUpdate(status);
            }
            
            @Override
            public void onChatMessage(String message) {
                if (callback != null) callback.onChatMessage(message);
            }
            
            @Override
            public void onApprovalRequired(String summary, String diff) {
                if (callback != null) callback.onApprovalRequired(summary, diff);
            }
            
            @Override
            public void onStateChanged(DrawingState newState) {
                if (callback != null) callback.onStateChanged(newState);
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
        
        // ManageDrawingStateUseCaseのコールバック
        manageDrawingStateUseCase.setCallback(new ManageDrawingStateUseCase.Callback() {
            @Override
            public void onStateChanged(DrawingState newState) {
                if (callback != null) callback.onStateChanged(newState);
            }
            
            @Override
            public void onMessage(String message) {
                if (callback != null) callback.onMessage(message);
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    // === ユーザー入力処理 ===
    
    /**
     * ユーザー入力を処理
     */
    public CompletableFuture<ProcessUserInputUseCase.Result> processUserInput(String input) {
        return processUserInputUseCase.execute(input);
    }
    
    /**
     * 承認された変更を適用
     */
    public void applyApprovedChanges(DrawingState approvedState) {
        processUserInputUseCase.applyApprovedChanges(approvedState);
    }
    
    /**
     * 変更を拒否
     */
    public void rejectChanges() {
        processUserInputUseCase.rejectChanges();
    }
    
    // === 状態管理 ===
    
    /**
     * Undo操作
     */
    public ManageDrawingStateUseCase.Result undo() {
        return manageDrawingStateUseCase.undo();
    }
    
    /**
     * Redo操作
     */
    public ManageDrawingStateUseCase.Result redo() {
        return manageDrawingStateUseCase.redo();
    }
    
    /**
     * キャンバスをクリア
     */
    public ManageDrawingStateUseCase.Result clearCanvas() {
        return manageDrawingStateUseCase.clearCanvas();
    }
    
    /**
     * 現在の状態を取得
     */
    public DrawingState getCurrentState() {
        return manageDrawingStateUseCase.getCurrentState();
    }
    
    /**
     * セッションを保存
     */
    public ManageDrawingStateUseCase.Result saveSession(String filename) {
        return manageDrawingStateUseCase.saveSession(filename);
    }
    
    /**
     * セッションを読み込み
     */
    public ManageDrawingStateUseCase.Result loadSession(String filename) {
        return manageDrawingStateUseCase.loadSession(filename);
    }
    
    /**
     * アプリケーションをシャットダウン
     */
    public void shutdown() {
        try {
            // 自動保存を実行
            saveSession("autosave_shutdown");
            LOGGER.info("シャットダウン時の自動保存完了");
        } catch (Exception e) {
            LOGGER.warning("シャットダウン時の自動保存失敗: " + e.getMessage());
        }
    }
}