package com.example.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent conversation history model. Supports multiple sessions with up to 20 messages per
 * session.
 */
public class AgentHistory {
  private static final int MAX_SESSIONS = 10;
  private static final int MAX_MESSAGES_PER_SESSION = 20;

  private List<Session> sessions;

  public AgentHistory() {
    this.sessions = new ArrayList<>();
  }

  /**
   * Represents a conversation session
   */
  public static class Session {
    private String sessionId;
    private LocalDateTime lastAccessed;
    private List<Message> messages;
    private DrawingState finalState;

    public Session(String sessionId) {
      this.sessionId = sessionId;
      this.lastAccessed = LocalDateTime.now();
      this.messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
      if (messages.size() >= MAX_MESSAGES_PER_SESSION) {
        // Remove oldest message to maintain limit
        messages.remove(0);
      }
      messages.add(message);
      this.lastAccessed = LocalDateTime.now();
    }

    // Getters and Setters
    public String getSessionId() {
      return sessionId;
    }


    public LocalDateTime getLastAccessed() {
      return lastAccessed;
    }

    public List<Message> getMessages() {
      return messages;
    }


    public DrawingState getFinalState() {
      return finalState;
    }

    public void setFinalState(DrawingState finalState) {
      this.finalState = finalState;
    }
  }

  /**
   * Represents a single message in the conversation
   */
  public static class Message {
    private String role; // "system", "user", "assistant"
    private String content;
    private List<ToolCall> toolCalls;

    public Message(String role, String content) {
      this.role = role;
      this.content = content;
      this.toolCalls = new ArrayList<>();
    }

    // Getters and Setters
    public String getRole() {
      return role;
    }

    public String getContent() {
      return content;
    }


    public List<ToolCall> getToolCalls() {
      return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
      this.toolCalls = toolCalls;
    }

  }

  /**
   * Represents a tool call made by the assistant
   */
  public static class ToolCall {
    private String name;
    private Object arguments;
    private String result;

    public ToolCall(String name, Object arguments, String result) {
      this.name = name;
      this.arguments = arguments;
      this.result = result;
    }

    // Getters and Setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Object getArguments() {
      return arguments;
    }


    public String getResult() {
      return result;
    }

  }

  /**
   * Add or update a session
   */
  public void addOrUpdateSession(Session session) {
    // Check if session already exists
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).getSessionId().equals(session.getSessionId())) {
        sessions.set(i, session);
        return;
      }
    }

    // Add new session, maintaining max limit
    if (sessions.size() >= MAX_SESSIONS) {
      // Remove oldest session
      Session oldest = sessions.get(0);
      for (Session s : sessions) {
        if (s.getLastAccessed().isBefore(oldest.getLastAccessed())) {
          oldest = s;
        }
      }
      sessions.remove(oldest);
    }

    sessions.add(session);
  }


  /**
   * Get the most recent session
   */
  public Session getMostRecentSession() {
    if (sessions.isEmpty()) {
      return null;
    }

    Session mostRecent = sessions.get(0);
    for (Session session : sessions) {
      if (session.getLastAccessed().isAfter(mostRecent.getLastAccessed())) {
        mostRecent = session;
      }
    }
    return mostRecent;
  }

  // Getters and Setters
  public List<Session> getSessions() {
    return sessions;
  }
}
