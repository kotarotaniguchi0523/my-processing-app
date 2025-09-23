package com.example.domain.agents.adk;

import com.example.domain.models.DrawingState;
import java.util.List;

/**
 * Result of workflow execution.
 * Single responsibility: Encapsulate workflow execution results.
 */
public class WorkflowResult {
    private final boolean success;
    private final DrawingState state;
    private final String message;
    private final List<String> progressLog;

    public WorkflowResult(boolean success, DrawingState state, String message, List<String> progressLog) {
        this.success = success;
        this.state = state;
        this.message = message;
        this.progressLog = progressLog;
    }

    public boolean isSuccess() {
        return success;
    }

    public DrawingState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getProgressLog() {
        return progressLog;
    }

    @Override
    public String toString() {
        return "WorkflowResult{success=" + success + ", message='" + message + "', logSize=" +
               (progressLog != null ? progressLog.size() : 0) + "}";
    }
}