package com.example.application.services;

import java.util.List;
import java.util.Map;

/**
 * Handles message conversions and DTOs for chat communication.
 * Extracted from AIAgentService to follow Single Responsibility Principle.
 * Contains all message-related data structures and conversion logic.
 */
public class MessageConverter {

    /**
     * ChatMessage DTO for representing chat messages
     */
    public static class ChatMessage {
        public final Role role;
        public final String content;
        public final List<ToolCall> toolCalls;

        private ChatMessage(Role role, String content) {
            this(role, content, null);
        }

        private ChatMessage(Role role, String content, List<ToolCall> toolCalls) {
            this.role = role;
            this.content = content;
            this.toolCalls = toolCalls;
        }

        public static ChatMessage system(String c) {
            return new ChatMessage(Role.SYSTEM, c);
        }

        public static ChatMessage user(String c) {
            return new ChatMessage(Role.USER, c);
        }

        public static ChatMessage assistant(String c) {
            return new ChatMessage(Role.ASSISTANT, c);
        }

        public static ChatMessage assistantWithTools(String c, List<ToolCall> tools) {
            return new ChatMessage(Role.ASSISTANT, c, tools);
        }
    }

    /**
     * ToolCall DTO for representing tool invocations
     */
    public static class ToolCall {
        public final String name;
        public final Map<String, Object> arguments;
        public final String result;

        public ToolCall(String name, Map<String, Object> arguments, String result) {
            this.name = name;
            this.arguments = arguments;
            this.result = result;
        }
    }

    /**
     * Role enum for message types
     */
    public enum Role {
        SYSTEM("System"),
        USER("User"),
        ASSISTANT("Assistant");

        public final String display;

        Role(String d) {
            this.display = d;
        }
    }

}