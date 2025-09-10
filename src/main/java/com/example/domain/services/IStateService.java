package com.example.domain.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.DrawingSession;

/**
 * 状態管理サービスのインターフェース
 */
public interface IStateService {
    
    /**
     * 現在の描画状態を取得
     */
    DrawingState getCurrentState();
    
    /**
     * 描画状態を更新
     */
    void updateState(DrawingState newState);
    
    /**
     * セッションを初期化
     */
    void initializeSession(DrawingSession session);
    
    /**
     * 状態をリセット
     */
    void reset();
    
    /**
     * 状態の履歴を取得
     */
    DrawingState[] getStateHistory();
    
    /**
     * 特定の時点の状態を取得
     */
    DrawingState getStateAt(int index);
}