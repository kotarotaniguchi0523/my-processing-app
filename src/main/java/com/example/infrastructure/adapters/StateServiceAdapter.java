package com.example.infrastructure.adapters;

import com.example.domain.models.DrawingState;
import com.example.domain.models.DrawingSession;
import com.example.domain.services.IStateService;
import com.example.application.services.StateService;

import java.util.logging.Logger;

/**
 * StateServiceのアダプター
 * ドメインのIStateServiceインタフェースを実装
 */
public class StateServiceAdapter implements IStateService {
    
    private final com.example.application.services.StateService stateService;
    
    public StateServiceAdapter() {
        this.stateService = com.example.application.services.StateService.getInstance();
    }
    
    public StateServiceAdapter(com.example.application.services.StateService stateService) {
        this.stateService = stateService;
    }
    
    @Override
    public DrawingState getCurrentState() {
        return stateService.getCurrentState();
    }
    
    @Override
    public void updateState(DrawingState newState) {
        stateService.updateState(newState);
    }
    
    @Override
    public void initializeSession(com.example.domain.models.DrawingSession session) {
        // StateService doesn't have this method, so we initialize with empty state
        if (session != null) {
            stateService.updateState(DrawingState.empty());
        }
    }
    
    @Override
    public void reset() {
        stateService.updateState(DrawingState.empty());
    }
    
    @Override
    public DrawingState[] getStateHistory() {
        // StateService doesn't maintain history, return current state only
        return new DrawingState[] { stateService.getCurrentState() };
    }
    
    @Override
    public DrawingState getStateAt(int index) {
        // StateService doesn't maintain history, return current state for index 0
        if (index == 0) {
            return stateService.getCurrentState();
        }
        return null;
    }
    
    /**
     * Get session (backward compatibility)
     */
    public com.example.domain.models.DrawingSession getSession() {
        // StateServiceはDrawingSessionを直接扱わないため、
        // 現在の状態から新しいDrawingSessionを作成
        com.example.domain.models.DrawingSession session = new com.example.domain.models.DrawingSession();
        DrawingState currentState = stateService.getCurrentState();
        // DrawingSessionに現在の状態を設定する処理が必要
        return session;
    }
}