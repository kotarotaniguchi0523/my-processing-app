package com.example.infrastructure.adapters;

import com.example.domain.models.DrawingState;
import com.example.domain.services.IHistoryService;
import com.example.infrastructure.services.HistoryService;
import com.example.infrastructure.services.StateManager;

import java.util.List;

/**
 * Adapter for HistoryService to implement IHistoryService interface.
 */
public class HistoryServiceAdapter implements IHistoryService {
    
    private final HistoryService historyService;
    
    public HistoryServiceAdapter(StateManager stateManager) {
        this.historyService = new HistoryService(stateManager);
    }
    
    public HistoryServiceAdapter(HistoryService historyService) {
        this.historyService = historyService;
    }
    
    @Override
    public void saveState(DrawingState state) {
        historyService.saveState(state);
    }
    
    @Override
    public void addState(DrawingState state) {
        historyService.addState(state);
    }
    
    @Override
    public boolean canUndo() {
        return historyService.canUndo();
    }
    
    @Override
    public boolean canRedo() {
        return historyService.canRedo();
    }
    
    @Override
    public boolean undo() {
        return historyService.undo();
    }
    
    @Override
    public boolean redo() {
        return historyService.redo();
    }
    
    @Override
    public void clear() {
        historyService.clear();
    }
    
    @Override
    public void clearHistory() {
        historyService.clearHistory();
    }
    
    @Override
    public int getHistorySize() {
        return historyService.getHistorySize();
    }
    
    @Override
    public int getCurrentIndex() {
        return historyService.getCurrentIndex();
    }
    
    @Override
    public List<DrawingState> getHistoryCopy() {
        return historyService.getHistoryCopy();
    }
}