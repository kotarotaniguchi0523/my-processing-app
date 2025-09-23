package com.example.application.services;

import com.example.domain.models.AgentHistory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles conversation history management for AI agent interactions. Responsible for maintaining
 * chat messages, trimming history, and session management.
 */
public class ConversationService {
  private static final String SYSTEM_PROMPT = String.join("\n",
      "あなたは2Dベクタキャンバス用の『構成プランナー兼描画アシスタント』です。",
      "— 目標: ユーザーの要求（例: 秋田犬, 家, ロゴ, アイコンなど）を、基本図形の組み合わせで表現し、",
      "  適切な関数呼び出し（Automatic Function Calling）で描画します。", "— できないと言わず、まず図形分解プランを提案してから描画を行います。",
      "— 必要最小限の質問のみ行い（本当に必須な寸法/色が欠ける場合）、それ以外は妥当な既定値で補完します。", "", "座標/単位:",
      "- 原点はキャンバス(ワークスペース)の左上、xは右、yは下。",
      "- current canvas size (W,H) は文脈に含まれる。必ず [0..W], [0..H] 内に収める。",
      "- 位置は相対的に考える（例: 中央=0.5W, 0.5H / 左寄せ=0.25W 等）→ 整数ピクセルに丸めて関数に渡す。",
      "- 重なり順は『後で描いたものが上』。上に来てほしいパーツ（耳/顔の輪郭/目など）は後に描く。", "", "利用可能な関数（必ずこの名前・引数で呼び出す）:",
      "- createRectangle(x, y, width, height, color)", "- createCircle(x, y, radius, color)",
      "- createEllipse(x, y, radiusX, radiusY, color)",
      "- createTriangle(x1, y1, x2, y2, x3, y3, color)", "- createLine(x1, y1, x2, y2, color)",
      "- moveShape(shapeId, newX, newY)", "- deleteShape(shapeId)",
      "- changeColor(shapeId, newColor)", "- resizeShape(shapeId, newWidth, newHeight, newRadius)",
      "- clearCanvas()", "色指定は 'red', '#RRGGBB', 'rgb(r,g,b)' のいずれか。", "", "作業手順:",
      "1) Design Plan: どの図形をどこにどのサイズ/色で置くかを1〜3行で要約（相対位置/重なり順も明記）。",
      "2) 実行: 上記プランに従い、適切な関数呼び出しを行う（必要な個数だけ呼び出してよい）。", "   - 各図形はキャンバス内に収まるように配置（必要なら少し縮小/移動）。", "",
      "設計ルール（例: 秋田犬などの動物/物体）:",
      "- 顔/頭: circle/ellipse、耳: triangle、胴体: rectangle/ellipse、足: rectangle/line、尻尾: line/小ellipse。",
      "- 形の重なり順は後勝ち（後で描くものが上になる）。耳/目/鼻/尻尾などは胴体や頭より後に描く。",
      "- 既定値: 図形サイズは30〜300px程度を目安、色は必要に応じて 'brown' や '#333333' など。",
      "- 見栄え: パーツのベタ付けを避ける。必要なら胴体/頭のサイズを相対的に調整し、自然な重なりにする。");

  private static final int MAX_HISTORY = 20;
  private final List<MessageConverter.ChatMessage> history = new ArrayList<>();
  private final String sessionId = UUID.randomUUID().toString();

  public ConversationService() {
    // Seed system message
    history.add(MessageConverter.ChatMessage.system(SYSTEM_PROMPT));
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getSystemPrompt() {
    return SYSTEM_PROMPT;
  }

  public void addUserMessage(String content) {
    history.add(MessageConverter.ChatMessage.user(content));
    trimHistory();
  }

  public void addAssistantMessage(String content) {
    history.add(MessageConverter.ChatMessage.assistant(content));
    trimHistory();
  }


  public List<MessageConverter.ChatMessage> getHistory() {
    return Collections.unmodifiableList(history);
  }


  public void loadFromAgentHistory(AgentHistory.Session session) {
    if (session == null)
      return;

    history.clear();
    boolean hasSystemMessage = false;

    for (AgentHistory.Message msg : session.getMessages()) {
      String roleStr = msg.getRole().toLowerCase();
      String content = msg.getContent();

      // Convert tool calls if present
      List<MessageConverter.ToolCall> toolCalls = null;
      if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
        toolCalls = new ArrayList<>();
        for (AgentHistory.ToolCall tc : msg.getToolCalls()) {
          @SuppressWarnings("unchecked")
          Map<String, Object> args =
              tc.getArguments() instanceof Map ? (Map<String, Object>) tc.getArguments()
                  : new HashMap<>();
          toolCalls.add(new MessageConverter.ToolCall(tc.getName(), args, tc.getResult()));
        }
      }

      switch (roleStr) {
        case "system":
          history.add(MessageConverter.ChatMessage.system(content));
          hasSystemMessage = true;
          break;
        case "user":
          history.add(MessageConverter.ChatMessage.user(content));
          break;
        default:
          if (toolCalls != null && !toolCalls.isEmpty()) {
            history.add(MessageConverter.ChatMessage.assistantWithTools(content, toolCalls));
          } else {
            history.add(MessageConverter.ChatMessage.assistant(content));
          }
          break;
      }
    }

    // Ensure we have system message
    if (!hasSystemMessage) {
      history.add(0, MessageConverter.ChatMessage.system(SYSTEM_PROMPT));
    }
  }

  private void trimHistory() {
    if (history.size() > MAX_HISTORY) {
      int remove = history.size() - MAX_HISTORY;
      // keep system message at index 0
      int idx = 1;
      while (remove > 0 && idx < history.size()) {
        history.remove(idx);
        remove--;
      }
    }
  }



}
