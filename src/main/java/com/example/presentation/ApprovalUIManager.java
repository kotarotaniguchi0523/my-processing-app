package com.example.presentation;

import com.example.domain.models.DrawingState;
import com.example.usecase.UseCaseFacade;

/**
 * ApprovalUIManager handles the approval workflow UI state and interactions.
 * This class follows the Single Responsibility Principle by focusing solely on approval management.
 */
public class ApprovalUIManager {
    private final UseCaseFacade useCaseFacade;

    // Approval overlay state
    private boolean approvalPending;
    private String approvalSummary = "";
    private String approvalDiff = "";
    private int approvalScrollOffset;
    private DrawingState pendingApprovalState;

    // Button positions (set by UI renderer)
    private int approveBtnX, approveBtnY, approveBtnW, approveBtnH;
    private int rejectBtnX, rejectBtnY, rejectBtnW, rejectBtnH;

    public ApprovalUIManager(UseCaseFacade useCaseFacade) {
        this.useCaseFacade = useCaseFacade;
    }

    public void showApproval(String summary, String diff, DrawingState pendingState) {
        this.approvalPending = true;
        this.approvalSummary = summary;
        this.approvalDiff = diff;
        this.pendingApprovalState = pendingState;
        this.approvalScrollOffset = 0; // Reset scroll when new approval shown
    }

    public void approve() {
        if (pendingApprovalState != null) {
            useCaseFacade.applyApprovedChanges(pendingApprovalState);
            clearApproval();
        }
    }

    public void reject() {
        useCaseFacade.rejectChanges();
        clearApproval();
    }

    public void clearApproval() {
        approvalPending = false;
        approvalSummary = "";
        approvalDiff = "";
        pendingApprovalState = null;
        approvalScrollOffset = 0;
    }

    public void scrollUp() {
        approvalScrollOffset = Math.max(0, approvalScrollOffset - 20);
    }

    public void scrollDown() {
        approvalScrollOffset += 20;
    }

    public boolean isInApproveButton(int mouseX, int mouseY) {
        return isInRect(mouseX, mouseY, approveBtnX, approveBtnY, approveBtnW, approveBtnH);
    }

    public boolean isInRejectButton(int mouseX, int mouseY) {
        return isInRect(mouseX, mouseY, rejectBtnX, rejectBtnY, rejectBtnW, rejectBtnH);
    }

    private boolean isInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    public void setButtonPositions(int approveBtnX, int approveBtnY, int rejectBtnX, int rejectBtnY, 
                                  int buttonW, int buttonH) {
        this.approveBtnX = approveBtnX;
        this.approveBtnY = approveBtnY;
        this.rejectBtnX = rejectBtnX;
        this.rejectBtnY = rejectBtnY;
        this.approveBtnW = buttonW;
        this.approveBtnH = buttonH;
        this.rejectBtnW = buttonW;
        this.rejectBtnH = buttonH;
    }

    // Getters
    public boolean isApprovalPending() {
        return approvalPending;
    }

    public String getApprovalSummary() {
        return approvalSummary;
    }

    public String getApprovalDiff() {
        return approvalDiff;
    }

    public int getApprovalScrollOffset() {
        return approvalScrollOffset;
    }

    public DrawingState getPendingApprovalState() {
        return pendingApprovalState;
    }

    // Button position getters
    public int getApproveBtnX() { return approveBtnX; }
    public int getApproveBtnY() { return approveBtnY; }
    public int getApproveBtnW() { return approveBtnW; }
    public int getApproveBtnH() { return approveBtnH; }
    
    public int getRejectBtnX() { return rejectBtnX; }
    public int getRejectBtnY() { return rejectBtnY; }
    public int getRejectBtnW() { return rejectBtnW; }
    public int getRejectBtnH() { return rejectBtnH; }
}