package com.example.domain.exceptions;

/**
 * Custom exception for workflow execution errors.
 * Provides proper error propagation with context.
 */
public class WorkflowException extends RuntimeException {

    private String workflowStage;
    private Object partialResult;

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkflowException(String message, String workflowStage, Object partialResult) {
        super(message);
        this.workflowStage = workflowStage;
        this.partialResult = partialResult;
    }

    public WorkflowException(String message, Throwable cause, String workflowStage, Object partialResult) {
        super(message, cause);
        this.workflowStage = workflowStage;
        this.partialResult = partialResult;
    }

    public String getWorkflowStage() {
        return workflowStage;
    }

    public Object getPartialResult() {
        return partialResult;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (workflowStage != null) {
            sb.append(" [Stage: ").append(workflowStage).append("]");
        }
        if (partialResult != null) {
            sb.append(" [PartialResult: ").append(partialResult).append("]");
        }
        return sb.toString();
    }
}