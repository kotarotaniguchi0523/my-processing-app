package com.example.presentation;

import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * DiffApprovalRenderer handles GitHub-style diff visualization with approval/rejection. Shows added
 * (green), removed (red), and modified (orange) shapes with interactive buttons.
 */
public class DiffApprovalRenderer {

  private static final int BUTTON_WIDTH = 120; // minimum width
  private static final int BUTTON_HEIGHT = 36;
  private static final int BUTTON_H_PADDING = 14; // text padding per side
  private static final int ITEM_HEIGHT = 64;
  private static final int HEADER_HEIGHT = 56;
  private static final int FOOTER_HEIGHT = 88; // ボタンと余白分
  private static final int PANEL_MARGIN = 24; // 画面端からの最小余白
  private static final int PANEL_SIDE_PADDING = 20;
  private static final int PANEL_RADIUS = 10;
  private static final int PANEL_MAX_WIDTH = 560;
  private static final int PANEL_MIN_WIDTH = 360;
  private static final int SCROLL_STEP = 24;

  // GitHub-style colors
  private static final int COLOR_ADD = 0xFF22C55E; // Green
  private static final int COLOR_REMOVE = 0xFFEF4444; // Red
  private static final int COLOR_MODIFY = 0xFFFB923C; // Orange
  private static final int COLOR_PANEL_BG = 0xFFF9FAFB; // Light gray
  private static final int COLOR_BORDER = 0xFFE5E7EB; // Border gray

  private final PApplet sketch;
  private PFont font;
  private PFont smallFont;

  // Diff approval state
  private boolean showingApproval = false;
  private DiffSummary pendingDiff;

  // Scroll state for the diff list
  private float scrollOffset = 0f;

  // Callback for approval/rejection
  private ApprovalCallback approvalCallback;

  public interface ApprovalCallback {
    void onApprove();

    void onReject();
  }

  public DiffApprovalRenderer(PApplet sketch, ThemeManager themeManager) {
    this.sketch = sketch;
    // themeManager is available for future use if needed
  }

  public void setFonts(PFont font, PFont smallFont) {
    this.font = font;
    this.smallFont = smallFont;
  }

  public void setApprovalCallback(ApprovalCallback callback) {
    this.approvalCallback = callback;
  }

  /**
   * Show diff approval dialog for state changes
   */
  public void showApproval(DrawingState oldState, DrawingState newState) {
    this.pendingDiff = computeDiffSummary(oldState, newState);
    this.showingApproval = true;
  }

  /**
   * Hide the approval dialog
   */
  public void hideApproval() {
    this.showingApproval = false;
    this.pendingDiff = null;
  }

  /**
   * Check if approval dialog is showing
   */
  public boolean isShowingApproval() {
    return showingApproval;
  }

  /**
   * Render the diff approval panel
   */
  public void render() {
    if (!showingApproval || pendingDiff == null)
      return;

    sketch.pushStyle();
    try {
      // Semi-transparent overlay
      sketch.fill(0, 0, 0, 120);
      sketch.noStroke();
      sketch.rect(0, 0, sketch.width, sketch.height);

      // Calculate dynamic panel layout
      Layout layout = calculateLayout();

      // Draw panel background
      drawPanel(layout.panelX, layout.panelY, layout.panelW, layout.panelH);

      // Header (fixed)
      drawHeader(layout.panelX, layout.panelY);

      // Diff items (scrollable area)
      drawDiffItemsClipped(layout);

      // Footer buttons (fixed)
      drawApprovalButtons(layout);

    } finally {
      sketch.popStyle();
    }
  }

  private void drawPanel(int x, int y, int w, int h) {
    // Shadow
    for (int i = 5; i >= 1; i--) {
      sketch.fill(0, 0, 0, 10 * i);
      sketch.noStroke();
      sketch.rect(x + i, y + i * 2, w, h, PANEL_RADIUS);
    }

    // Panel background
    sketch.fill(COLOR_PANEL_BG);
    sketch.stroke(COLOR_BORDER);
    sketch.strokeWeight(1);
    sketch.rect(x, y, w, h, PANEL_RADIUS);
  }

  private void drawHeader(int x, int y) {
    sketch.fill(40);
    if (font != null)
      sketch.textFont(font);
    sketch.textAlign(PApplet.LEFT, PApplet.TOP);
    sketch.text("Review Changes", x + PANEL_SIDE_PADDING, y + 14);

    // Stats summary
    if (smallFont != null)
      sketch.textFont(smallFont);
    sketch.fill(100);
    String summary = String.format("%d additions, %d deletions, %d modifications",
        pendingDiff.added.size(), pendingDiff.removed.size(), pendingDiff.modified.size());
    sketch.text(summary, x + PANEL_SIDE_PADDING, y + 34);
  }

  private void drawDiffItemsClipped(Layout layout) {
    int listX = layout.panelX + PANEL_SIDE_PADDING;
    int contentStartY = layout.contentY;

    int y = contentStartY - Math.round(scrollOffset);

    // Establish a clip region to prevent overlap with header/footer
    int clipX = layout.panelX + PANEL_SIDE_PADDING - 8; // include left color strip
    int clipY = layout.contentY;
    int clipW = layout.panelW - (PANEL_SIDE_PADDING * 2) + 16;
    int clipH = layout.contentH;

    sketch.push();
    try {
      sketch.clip(clipX, clipY, clipW, clipH);

      // Removed (red)
      for (DiffItem item : pendingDiff.removed) {
        drawDiffItem(layout, listX, y, item, COLOR_REMOVE, "Removed");
        y += ITEM_HEIGHT;
      }
      // Added (green)
      for (DiffItem item : pendingDiff.added) {
        drawDiffItem(layout, listX, y, item, COLOR_ADD, "Added");
        y += ITEM_HEIGHT;
      }
      // Modified (orange)
      for (DiffItem item : pendingDiff.modified) {
        drawDiffItem(layout, listX, y, item, COLOR_MODIFY, "Modified");
        y += ITEM_HEIGHT;
      }

    } finally {
      sketch.noClip();
      sketch.pop();
    }

    // Scrollbar (simple) when overflow
    if (layout.contentTotalH > layout.contentH) {
      float ratio = layout.contentH / (float) layout.contentTotalH;
      int barH = Math.max(24, Math.round(layout.contentH * ratio));
      int maxScroll = layout.contentTotalH - layout.contentH;
      float scrollRatio = maxScroll <= 0 ? 0 : scrollOffset / maxScroll;
      int barY = contentStartY + Math.round((layout.contentH - barH) * scrollRatio);

      int barX = layout.panelX + layout.panelW - PANEL_SIDE_PADDING - 6;
      sketch.noStroke();
      sketch.fill(0, 0, 0, 20);
      sketch.rect(barX, contentStartY, 4, layout.contentH, 2);
      sketch.fill(0, 0, 0, 80);
      sketch.rect(barX, barY, 4, barH, 2);
    }
  }

  private void drawDiffItem(Layout layout, int x, int y, DiffItem item, int color, String kind) {
    int w = layout.panelW - PANEL_SIDE_PADDING * 2;

    // Container background
    sketch.fill(color, 20);
    sketch.noStroke();
    sketch.rect(x - 4, y + 2, w, ITEM_HEIGHT - 8, 6);

    // Left color strip
    sketch.fill(color);
    sketch.rect(x - 6, y + 2, 3, ITEM_HEIGHT - 8, 2);

    // Badge
    int badgeY = y + 8;
    sketch.fill(color);
    sketch.rect(x + 2, badgeY, 64, 18, 9);
    sketch.fill(255);
    if (smallFont != null)
      sketch.textFont(smallFont);
    sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
    sketch.text(kind, x + 2 + 32, badgeY + 9);

    // Texts
    sketch.textAlign(PApplet.LEFT, PApplet.TOP);
    sketch.fill(40);
    if (smallFont != null)
      sketch.textFont(smallFont);
    sketch.text(item.description, x + 74, y + 8);

    sketch.fill(100);
    sketch.text(item.details, x + 74, y + 30);
  }

  private void drawApprovalButtons(Layout layout) {
    int buttonY = layout.buttonY;
    int centerX = layout.panelX + layout.panelW / 2;

    // Measure dynamic widths based on text and font
    int approveW = Math.max(BUTTON_WIDTH, measureButtonWidth("✓ Approve"));
    int rejectW = Math.max(BUTTON_WIDTH, measureButtonWidth("✗ Reject"));

    // Approve (left)
    int approveX = centerX - approveW - 10;
    boolean approveHoverNow =
        isInButtonArea(sketch.mouseX, sketch.mouseY, approveX, buttonY, approveW, BUTTON_HEIGHT);
    drawButton(approveX, buttonY, approveW, BUTTON_HEIGHT, "✓ Approve", COLOR_ADD, approveHoverNow);

    // Reject
    int rejectX = centerX + 10;
    boolean rejectHoverNow =
        isInButtonArea(sketch.mouseX, sketch.mouseY, rejectX, buttonY, rejectW, BUTTON_HEIGHT);
    drawButton(rejectX, buttonY, rejectW, BUTTON_HEIGHT, "✗ Reject", COLOR_REMOVE, rejectHoverNow);
  }

  private void drawButton(int x, int y, int w, int h, String text, int color, boolean hover) {
    // Button background
    if (hover) {
      sketch.fill(color);
    } else {
      sketch.fill(color, 200);
    }
    sketch.noStroke();
    sketch.rect(x, y, w, h, 6);

    // Button text (use smaller font for better fit)
    sketch.fill(255);
    if (smallFont != null) {
      sketch.textFont(smallFont);
    } else if (font != null) {
      sketch.textFont(font);
    }
    sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
    sketch.text(text, x + w / 2, y + h / 2);
  }

  private boolean isInButtonArea(int mx, int my, int x, int y, int w, int h) {
    return mx >= x && mx <= x + w && my >= y && my <= y + h;
  }

  /**
   * Handle mouse clicks for approval/rejection
   */
  public boolean handleMousePressed(int mouseX, int mouseY) {
    if (!showingApproval)
      return false;

    Layout layout = calculateLayout();
    int centerX = layout.panelX + layout.panelW / 2;
    int buttonY = layout.buttonY;

    // Recompute dynamic widths consistent with rendering
    int approveW = Math.max(BUTTON_WIDTH, measureButtonWidth("✓ Approve"));
    int rejectW = Math.max(BUTTON_WIDTH, measureButtonWidth("✗ Reject"));

    // Check approve button
    int approveX = centerX - approveW - 10;
    if (isInButtonArea(mouseX, mouseY, approveX, buttonY, approveW, BUTTON_HEIGHT)) {
      handleApprove();
      return true;
    }

    // Check reject button
    int rejectX = centerX + 10;
    if (isInButtonArea(mouseX, mouseY, rejectX, buttonY, rejectW, BUTTON_HEIGHT)) {
      handleReject();
      return true;
    }

    return false;
  }

  // Measure button width based on current font metrics
  private int measureButtonWidth(String label) {
    // Match the font used in drawButton
    if (smallFont != null) {
      sketch.textFont(smallFont);
    } else if (font != null) {
      sketch.textFont(font);
    }
    float tw = sketch.textWidth(label);
    return Math.round(tw + BUTTON_H_PADDING * 2);
  }

  /**
   * Handle mouse wheel to scroll the diff list when the modal is open.
   */
  public boolean handleMouseWheel(MouseEvent event) {
    if (!showingApproval || pendingDiff == null)
      return false;
    Layout layout = calculateLayout();
    // Only scroll when the cursor is within the content area
    if (event.getX() >= layout.panelX && event.getX() <= layout.panelX + layout.panelW
        && event.getY() >= layout.contentY && event.getY() <= layout.contentY + layout.contentH) {
      int maxScroll = Math.max(0, layout.contentTotalH - layout.contentH);
      float delta = event.getCount() * SCROLL_STEP; // down: positive
      scrollOffset = PApplet.constrain(scrollOffset + delta, 0, maxScroll);
      return true;
    }
    return false;
  }

  private void handleApprove() {
    hideApproval();
    if (approvalCallback != null) {
      approvalCallback.onApprove();
    }
  }

  private void handleReject() {
    hideApproval();
    if (approvalCallback != null) {
      approvalCallback.onReject();
    }
  }

  private DiffSummary computeDiffSummary(DrawingState prev, DrawingState curr) {
    List<Shape> prevShapes = prev != null ? prev.getShapes() : Collections.emptyList();
    List<Shape> currShapes = curr != null ? curr.getShapes() : Collections.emptyList();

    Map<String, Shape> prevMap = new HashMap<>();
    for (Shape s : prevShapes)
      prevMap.put(s.getId(), s);
    Map<String, Shape> currMap = new HashMap<>();
    for (Shape s : currShapes)
      currMap.put(s.getId(), s);

    List<DiffItem> added = new ArrayList<>();
    List<DiffItem> removed = new ArrayList<>();
    List<DiffItem> modified = new ArrayList<>();

    // Find added shapes
    for (Shape s : currShapes) {
      if (!prevMap.containsKey(s.getId())) {
        added.add(createDiffItem(s, "Added"));
      }
    }

    // Find removed and modified shapes
    for (Shape sPrev : prevShapes) {
      Shape sCurr = currMap.get(sPrev.getId());
      if (sCurr == null) {
        removed.add(createDiffItem(sPrev, "Removed"));
      } else if (!sPrev.equals(sCurr)) {
        modified.add(createDiffItem(sCurr, "Modified"));
      }
    }

    return new DiffSummary(added, removed, modified);
  }

  private DiffItem createDiffItem(Shape shape, String action) {
    String description = String.format("%s %s [%s]", action,
        shape.getType().toString().toLowerCase(), shape.getId());

    String details = "";
    if (shape.getBounds() != null) {
      Shape.BoundingBox b = shape.getBounds();
      details = String.format("Position: (%.0f, %.0f), Size: %.0fx%.0f", b.getX(), b.getY(),
          b.getWidth(), b.getHeight());
    }

    return new DiffItem(description, details, shape);
  }

  /**
   * Internal class to hold diff summary data
   */
  private static final class DiffSummary {
    final List<DiffItem> added;
    final List<DiffItem> removed;
    final List<DiffItem> modified;

    DiffSummary(List<DiffItem> added, List<DiffItem> removed, List<DiffItem> modified) {
      this.added = added;
      this.removed = removed;
      this.modified = modified;
    }
  }

  /**
   * Internal class for diff item display
   */
  private static final class DiffItem {
    final String description;
    final String details;
    @SuppressWarnings("unused") // Stored for future detailed diff operations
    final Shape shape;

    DiffItem(String description, String details, Shape shape) {
      this.description = description;
      this.details = details;
      this.shape = shape;
    }
  }

  // ===== Layout calculation helpers =====
  private static final class Layout {
    final int panelX, panelY, panelW, panelH;
    final int contentY, contentH, contentTotalH; // scrollable list area
    final int buttonY;

    Layout(int panelX, int panelY, int panelW, int panelH, int contentY, int contentH,
        int contentTotalH, int buttonY) {
      this.panelX = panelX;
      this.panelY = panelY;
      this.panelW = panelW;
      this.panelH = panelH;
      this.contentY = contentY;
      this.contentH = contentH;
      this.contentTotalH = contentTotalH;
      this.buttonY = buttonY;
    }
  }

  private Layout calculateLayout() {
    int totalItems =
        pendingDiff.added.size() + pendingDiff.removed.size() + pendingDiff.modified.size();

    int panelW =
        Math.min(PANEL_MAX_WIDTH, Math.max(PANEL_MIN_WIDTH, sketch.width - PANEL_MARGIN * 2));
    int maxPanelH =
        Math.max(HEADER_HEIGHT + FOOTER_HEIGHT + ITEM_HEIGHT, sketch.height - PANEL_MARGIN * 2);

    int contentTotalH = totalItems * ITEM_HEIGHT + 10; // intrinsic list height
    int contentMaxH = Math.max(ITEM_HEIGHT, maxPanelH - HEADER_HEIGHT - FOOTER_HEIGHT);
    int contentH = Math.min(contentTotalH, contentMaxH);
    int panelH = HEADER_HEIGHT + contentH + FOOTER_HEIGHT;

    int panelX = (sketch.width - panelW) / 2;
    int panelY = (sketch.height - panelH) / 2;
    int contentY = panelY + HEADER_HEIGHT;

    // Clamp scroll offset within bounds whenever layout changes
    int maxScroll = Math.max(0, contentTotalH - contentH);
    scrollOffset = PApplet.constrain(scrollOffset, 0, maxScroll);

    int buttonY = panelY + HEADER_HEIGHT + contentH + 20;

    return new Layout(panelX, panelY, panelW, panelH, contentY, contentH, contentTotalH, buttonY);
  }
}
