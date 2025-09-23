package com.example.application.services;

import com.example.domain.models.AgentHistory;
import com.example.domain.models.DrawingState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles JSON-based persistence of agent history and conversation data. Manages reading from and
 * writing to .agent_history.json file.
 */
public class JsonHistoryRepository {
  private static final Path HISTORY_FILE = Path.of(".agent_history.json");
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final int PERSIST_INTERVAL = 2;

  private final boolean persistEnabled;
  private long persistCounter = 0;
  private AgentHistory agentHistory;

  public JsonHistoryRepository(boolean persistEnabled) {
    this.persistEnabled = persistEnabled;
    this.agentHistory = new AgentHistory();
  }

  public boolean historyFileExists() {
    return Files.exists(HISTORY_FILE);
  }

  /**
   * Load history from file if it exists. Returns the most recent session or null if no sessions
   * found.
   */
  public AgentHistory.Session loadHistoryFromFile() {
    if (!historyFileExists()) {
      return null;
    }

    try {
      String json = Files.readString(HISTORY_FILE);

      // Try to load as new AgentHistory format first
      try {
        agentHistory = GSON.fromJson(json, AgentHistory.class);
        if (agentHistory != null && agentHistory.getSessions() != null) {
          return agentHistory.getMostRecentSession();
        }
      } catch (Exception e) {
        // Fall back to old format
        return loadOldFormat(json);
      }

    } catch (Exception e) {
      System.err.println("Failed to load history: " + e.getMessage());
      // Initialize fresh if loading fails
      agentHistory = new AgentHistory();
    }

    return null;
  }

  /**
   * Handle legacy format conversion
   */
  private AgentHistory.Session loadOldFormat(String json) {
    try {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> list = GSON.fromJson(json, List.class);
      if (list != null) {
        // Initialize new AgentHistory and session
        agentHistory = new AgentHistory();
        AgentHistory.Session session = new AgentHistory.Session(UUID.randomUUID().toString());

        // Process old format messages
        for (Map<String, Object> e : list) {
          Object r = e.get("role");
          Object c = e.get("content");
          Object toolCallsObj = e.get("toolCalls");

          if (!(c instanceof String))
            continue;
          String roleStr = r instanceof String ? (String) r : "assistant";

          // Skip system messages as they'll be added by ConversationService
          if (roleStr.equalsIgnoreCase("system"))
            continue;

          // Parse tool calls if present
          List<AgentHistory.ToolCall> agentToolCalls = new ArrayList<>();
          if (toolCallsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCallsList = (List<Map<String, Object>>) toolCallsObj;
            for (Map<String, Object> tc : toolCallsList) {
              String name = (String) tc.get("name");
              @SuppressWarnings("unchecked")
              Map<String, Object> args = (Map<String, Object>) tc.get("arguments");
              String result = (String) tc.get("result");
              if (name != null) {
                agentToolCalls.add(new AgentHistory.ToolCall(name, args, result));
              }
            }
          }

          AgentHistory.Message msg = new AgentHistory.Message(roleStr, (String) c);
          msg.setToolCalls(agentToolCalls);
          session.addMessage(msg);
        }

        agentHistory.addOrUpdateSession(session);
        return session;
      }
    } catch (Exception e) {
      System.err.println("Failed to parse old format: " + e.getMessage());
    }
    return null;
  }

  /**
   * Persist history conditionally based on counter.
   */
  public void persistHistoryMaybe(AgentHistory.Session currentSession, DrawingState currentState) {
    persistCounter++;
    if (!persistEnabled || persistCounter % PERSIST_INTERVAL != 0) {
      return;
    }
    persistHistoryImmediately(currentSession, currentState);
  }

  /**
   * Persist history immediately to file using AgentHistory structure.
   */
  public void persistHistoryImmediately(AgentHistory.Session currentSession,
      DrawingState currentState) {
    if (!persistEnabled) {
      return;
    }

    try {
      // Update current session with final state
      if (currentSession != null) {
        currentSession.setFinalState(currentState);
        // Add or update session in agent history
        agentHistory.addOrUpdateSession(currentSession);
      }

      // Serialize AgentHistory to JSON
      String json = GSON.toJson(agentHistory);
      Files.writeString(HISTORY_FILE, json, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      System.err.println("Failed to persist history: " + e.getMessage());
    }
  }

  /**
   * Clear persisted history file and reset in-memory history.
   */
  public void clearHistoryFile() {
    try {
      if (Files.exists(HISTORY_FILE)) {
        Files.delete(HISTORY_FILE);
      }
    } catch (Exception ignored) {
      // best-effort
    }
    agentHistory = new AgentHistory();
  }
}
