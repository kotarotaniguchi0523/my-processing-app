package com.example.domain.services;

import com.example.domain.models.DrawingSession;

/**
 * セッション管理サービスのインターフェース
 */
public interface ISessionManager {
    
    /**
     * セッションを保存
     */
    void saveSession(String filename) throws Exception;
    
    /**
     * セッションを読み込み
     */
    DrawingSession loadSession(String filename) throws Exception;
    
    /**
     * 自動保存間隔を設定（秒）
     */
    void setAutoSaveInterval(int seconds);
    
    /**
     * 自動保存を有効/無効化
     */
    void setAutoSaveEnabled(boolean enabled);
    
    /**
     * 利用可能なセッションファイルをリスト
     */
    String[] listAvailableSessions();
    
    /**
     * セッションを削除
     */
    void deleteSession(String filename) throws Exception;
}