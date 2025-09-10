package com.example.usecase;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IAgentService;
import com.example.domain.services.IDiffService;
import com.example.domain.services.IStateService;
import com.example.domain.services.ICommandExecutor;
import com.example.domain.parsers.ICommandParser;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ユーザー入力を処理するユースケース
 * すべてのユーザー入力処理のビジネスロジックを管理
 */
public class ProcessUserInputUseCase {
    
    private static final Logger LOGGER = Logger.getLogger(ProcessUserInputUseCase.class.getName());
    
    private final IAgentService agentService;
    private final IDiffService diffService;
    private final IStateService stateService;
    private final ICommandExecutor commandExecutor;
    private final ICommandParser commandParser;
    
    // コールバックインターフェース
    public interface Callback {
        void onStatusUpdate(String status);
        void onChatMessage(String message);
        void onApprovalRequired(String summary, String diff);
        void onStateChanged(DrawingState newState);
        void onError(String error);
    }
    
    private Callback callback;
    
    public ProcessUserInputUseCase(
            IAgentService agentService,
            IDiffService diffService,
            IStateService stateService,
            ICommandExecutor commandExecutor,
            ICommandParser commandParser) {
        this.agentService = agentService;
        this.diffService = diffService;
        this.stateService = stateService;
        this.commandExecutor = commandExecutor;
        this.commandParser = commandParser;
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    /**
     * ユーザー入力を処理
     * @param input ユーザーからの入力文字列
     * @return 処理結果
     */
    public CompletableFuture<Result> execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                Result.error("入力が空です")
            );
        }
        
        updateStatus("処理中...");
        
        // コマンドとして解析を試みる
        if (input.startsWith("/")) {
            return processAsCommand(input);
        }
        
        // AIで処理
        return processWithAI(input);
    }
    
    private CompletableFuture<Result> processAsCommand(String input) {
        try {
            var parsedCommand = commandParser.parse(input);
            if (parsedCommand != null) {
                var command = commandExecutor.createCommand(parsedCommand);
                if (command != null) {
                    commandExecutor.execute(command);
                    notifyStateChanged(stateService.getCurrentState());
                    return CompletableFuture.completedFuture(
                        Result.success("コマンドを実行しました")
                    );
                }
            }
            return CompletableFuture.completedFuture(
                Result.error("無効なコマンドです")
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "コマンド実行エラー", e);
            return CompletableFuture.completedFuture(
                Result.error("コマンド実行エラー: " + e.getMessage())
            );
        }
    }
    
    private CompletableFuture<Result> processWithAI(String input) {
        if (agentService == null) {
            addChatMessage("AI: 現在AI連携は無効です。.envにAPIキーを設定してください。");
            return CompletableFuture.completedFuture(
                Result.error("AI連携が無効です")
            );
        }
        
        updateStatus("AI処理中...");
        
        // AIの現在の状態を同期
        agentService.setCurrentState(stateService.getCurrentState());
        
        return agentService.processInstruction(input)
            .thenApply(responseObj -> {
                // Cast to ServiceResponse (the actual implementation type)
                com.example.application.agent.ServiceResponse response = 
                    (com.example.application.agent.ServiceResponse) responseObj;
                // テキストレスポンスを表示
                if (response.hasTextResponse()) {
                    addChatMessage("AI: " + response.getTextResponse());
                }
                
                // 状態変更がある場合
                DrawingState newState = null;
                if (response.hasToolExecutionResults()) {
                    // Find the latest successful tool execution with a result state
                    for (var result : response.getToolExecutionResults()) {
                        if (result.isSuccess() && result.hasResultState()) {
                            newState = result.getResultState();
                        }
                    }
                }
                
                if (newState != null) {
                    if (response.requiresApproval()) {
                        // 承認が必要な場合
                        prepareApproval(stateService.getCurrentState(), newState);
                        return Result.pendingApproval("承認待ち", newState);
                    } else {
                        // 即座に適用
                        applyState(newState);
                        return Result.success("AI処理完了");
                    }
                }
                
                // エラーがある場合
                if (response.hasErrorMessage()) {
                    return Result.error(response.getError());
                }
                
                return Result.success("AI処理完了");
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "AI処理エラー", throwable);
                notifyError("AI処理エラー: " + throwable.getMessage());
                return Result.error(throwable.getMessage());
            })
            .whenComplete((result, throwable) -> {
                updateStatus("準備完了");
            });
    }
    
    /**
     * 承認された変更を適用
     */
    public void applyApprovedChanges(DrawingState approvedState) {
        if (approvedState != null) {
            applyState(approvedState);
            addChatMessage("System: 変更を適用しました");
        }
    }
    
    /**
     * 承認を拒否
     */
    public void rejectChanges() {
        addChatMessage("System: 変更を拒否しました");
        updateStatus("準備完了");
    }
    
    private void applyState(DrawingState newState) {
        stateService.updateState(newState);
        if (agentService != null) {
            // Agent service might need to know about state changes
            // but IAgentService doesn't have setCurrentState method
            // This is handled through the state service instead
        }
        notifyStateChanged(newState);
    }
    
    private void prepareApproval(DrawingState current, DrawingState proposed) {
        var diffResult = diffService.generateDiff(current, proposed);
        if (callback != null) {
            callback.onApprovalRequired(diffResult.summary, diffResult.unifiedDiff);
        }
    }
    
    private void updateStatus(String status) {
        if (callback != null) {
            callback.onStatusUpdate(status);
        }
    }
    
    private void addChatMessage(String message) {
        if (callback != null) {
            callback.onChatMessage(message);
        }
    }
    
    private void notifyStateChanged(DrawingState newState) {
        if (callback != null) {
            callback.onStateChanged(newState);
        }
    }
    
    private void notifyError(String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
    
    /**
     * ユースケースの実行結果
     */
    public static class Result {
        public final boolean success;
        public final String message;
        public final DrawingState pendingState;
        public final boolean requiresApproval;
        
        private Result(boolean success, String message, DrawingState pendingState, boolean requiresApproval) {
            this.success = success;
            this.message = message;
            this.pendingState = pendingState;
            this.requiresApproval = requiresApproval;
        }
        
        public static Result success(String message) {
            return new Result(true, message, null, false);
        }
        
        public static Result error(String message) {
            return new Result(false, message, null, false);
        }
        
        public static Result pendingApproval(String message, DrawingState state) {
            return new Result(true, message, state, true);
        }
    }
}