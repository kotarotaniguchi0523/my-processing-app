package com.example.infrastructure.adapters;

import com.example.domain.models.DrawingSession;
import com.example.domain.services.ISessionManager;
import com.example.infrastructure.services.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SessionManagerのアダプター
 * ドメインのISessionManagerインタフェースを実装
 */
public class SessionManagerAdapter implements ISessionManager {
    
    private static final Logger logger = Logger.getLogger(SessionManagerAdapter.class.getName());
    
    private final com.example.infrastructure.services.SessionManager sessionManager;
    
    public SessionManagerAdapter() {
        this.sessionManager = com.example.infrastructure.services.SessionManager.getInstance();
    }
    
    public SessionManagerAdapter(com.example.infrastructure.services.SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    @Override
    public void saveSession(String filename) throws Exception {
        try {
            sessionManager.saveSessionAs(filename);
        } catch (Exception e) {
            throw new Exception("Failed to save session: " + e.getMessage(), e);
        }
    }
    
    @Override
    public com.example.domain.models.DrawingSession loadSession(String filename) throws Exception {
        try {
            sessionManager.loadSession(filename);
            // SessionManager doesn't return DrawingSession, so we create a placeholder
            // The actual state is managed internally by SessionManager
            return new com.example.domain.models.DrawingSession();
        } catch (Exception e) {
            throw new Exception("Failed to load session: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void setAutoSaveInterval(int seconds) {
        sessionManager.setAutoSaveInterval(seconds);
    }
    
    @Override
    public void setAutoSaveEnabled(boolean enabled) {
        sessionManager.setAutoSaveEnabled(enabled);
    }
    
    @Override
    public String[] listAvailableSessions() {
        try {
            File sessionsDir = new File("sessions");
            if (!sessionsDir.exists()) {
                return new String[0];
            }
            
            File[] files = sessionsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) {
                return new String[0];
            }
            
            List<String> sessionNames = new ArrayList<>();
            for (File file : files) {
                sessionNames.add(file.getName().replace(".json", ""));
            }
            
            return sessionNames.toArray(new String[0]);
        } catch (Exception e) {
            logger.warning("セッションリスト取得エラー: " + e.getMessage());
            return new String[0];
        }
    }
    
    @Override
    public void deleteSession(String filename) throws Exception {
        File file = new File("sessions", filename + ".json");
        if (file.exists()) {
            if (!file.delete()) {
                throw new Exception("セッションファイルの削除に失敗しました: " + filename);
            }
        }
    }
}