package com.example.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatRequest and Message classes
 */
class ChatRequestTest {

    @Test
    void builder_DefaultValues_ShouldSetCorrectly() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("test message")
            .build();
        
        // Verify
        assertEquals(1, request.getMessages().size());
        assertNull(request.getModel());
        assertEquals(0.7, request.getTemperature());
        assertEquals(1000, request.getMaxTokens());
        assertNull(request.getAdditionalParams());
    }

    @Test
    void builder_CustomModel_ShouldSetCorrectly() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("test")
            .model("gpt-4")
            .build();
        
        // Verify
        assertEquals("gpt-4", request.getModel());
    }

    @Test
    void builder_CustomTemperature_ShouldSetCorrectly() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("test")
            .temperature(0.9)
            .build();
        
        // Verify
        assertEquals(0.9, request.getTemperature());
    }

    @Test
    void builder_InvalidTemperatureTooLow_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ChatRequest.builder().temperature(-0.1));
        
        assertEquals("Temperature must be between 0 and 2", exception.getMessage());
    }

    @Test
    void builder_InvalidTemperatureTooHigh_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ChatRequest.builder().temperature(2.1));
        
        assertEquals("Temperature must be between 0 and 2", exception.getMessage());
    }

    @Test
    void builder_ValidTemperatureBoundaries_ShouldAccept() {
        // Execute - lower boundary
        ChatRequest request1 = ChatRequest.builder()
            .addUserMessage("test")
            .temperature(0.0)
            .build();
        
        // Execute - upper boundary
        ChatRequest request2 = ChatRequest.builder()
            .addUserMessage("test")
            .temperature(2.0)
            .build();
        
        // Verify
        assertEquals(0.0, request1.getTemperature());
        assertEquals(2.0, request2.getTemperature());
    }

    @Test
    void builder_CustomMaxTokens_ShouldSetCorrectly() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("test")
            .maxTokens(2000)
            .build();
        
        // Verify
        assertEquals(2000, request.getMaxTokens());
    }

    @Test
    void builder_InvalidMaxTokens_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ChatRequest.builder().maxTokens(0));
        
        assertEquals("Max tokens must be positive", exception.getMessage());
    }

    @Test
    void builder_NegativeMaxTokens_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ChatRequest.builder().maxTokens(-1));
        
        assertEquals("Max tokens must be positive", exception.getMessage());
    }

    @Test
    void builder_AdditionalParams_ShouldSetCorrectly() {
        // Setup
        Map<String, Object> params = Map.of("custom_param", "value", "number_param", 42);
        
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("test")
            .additionalParams(params)
            .build();
        
        // Verify
        assertEquals(params, request.getAdditionalParams());
    }

    @Test
    void builder_AddUserMessage_ShouldAddCorrectMessage() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello world")
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(1, messages.size());
        assertEquals(ChatRequest.Role.USER, messages.get(0).getRole());
        assertEquals("Hello world", messages.get(0).getContent());
    }

    @Test
    void builder_AddAssistantMessage_ShouldAddCorrectMessage() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Hello")
            .addAssistantMessage("Hi there!")
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(2, messages.size());
        assertEquals(ChatRequest.Role.ASSISTANT, messages.get(1).getRole());
        assertEquals("Hi there!", messages.get(1).getContent());
    }

    @Test
    void builder_AddSystemMessage_ShouldAddCorrectMessage() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addSystemMessage("You are a helpful assistant")
            .addUserMessage("Hello")
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(2, messages.size());
        assertEquals(ChatRequest.Role.SYSTEM, messages.get(0).getRole());
        assertEquals("You are a helpful assistant", messages.get(0).getContent());
    }

    @Test
    void builder_AddMessage_ShouldAddCorrectMessage() {
        // Setup
        ChatRequest.Message customMessage = new ChatRequest.Message(ChatRequest.Role.USER, "Custom message");
        
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addMessage(customMessage)
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(1, messages.size());
        assertEquals(ChatRequest.Role.USER, messages.get(0).getRole());
        assertEquals("Custom message", messages.get(0).getContent());
    }

    @Test
    void builder_SetMessages_ShouldReplaceExistingMessages() {
        // Setup
        List<ChatRequest.Message> newMessages = List.of(
            ChatRequest.Message.system("System prompt"),
            ChatRequest.Message.user("User query"),
            ChatRequest.Message.assistant("Assistant response")
        );
        
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("This will be replaced")
            .messages(newMessages)
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(3, messages.size());
        assertEquals("System prompt", messages.get(0).getContent());
        assertEquals("User query", messages.get(1).getContent());
        assertEquals("Assistant response", messages.get(2).getContent());
    }

    @Test
    void builder_NoMessages_ShouldThrowException() {
        // Execute & Verify
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            ChatRequest.builder().build());
        
        assertEquals("At least one message is required", exception.getMessage());
    }

    @Test
    void getMessages_ShouldReturnCopyNotReference() {
        // Setup
        ChatRequest request = ChatRequest.builder()
            .addUserMessage("Original message")
            .build();
        
        // Execute
        List<ChatRequest.Message> messages1 = request.getMessages();
        List<ChatRequest.Message> messages2 = request.getMessages();
        
        // Verify
        assertNotSame(messages1, messages2);
        assertEquals(messages1.size(), messages2.size());
        assertEquals(messages1.get(0).getContent(), messages2.get(0).getContent());
    }

    @Test
    void message_StaticFactoryMethods_ShouldCreateCorrectMessages() {
        // Execute
        ChatRequest.Message userMsg = ChatRequest.Message.user("User content");
        ChatRequest.Message assistantMsg = ChatRequest.Message.assistant("Assistant content");
        ChatRequest.Message systemMsg = ChatRequest.Message.system("System content");
        
        // Verify
        assertEquals(ChatRequest.Role.USER, userMsg.getRole());
        assertEquals("User content", userMsg.getContent());
        
        assertEquals(ChatRequest.Role.ASSISTANT, assistantMsg.getRole());
        assertEquals("Assistant content", assistantMsg.getContent());
        
        assertEquals(ChatRequest.Role.SYSTEM, systemMsg.getRole());
        assertEquals("System content", systemMsg.getContent());
    }

    @Test
    void message_Constructor_ShouldSetFieldsCorrectly() {
        // Execute
        ChatRequest.Message message = new ChatRequest.Message(ChatRequest.Role.USER, "Test content");
        
        // Verify
        assertEquals(ChatRequest.Role.USER, message.getRole());
        assertEquals("Test content", message.getContent());
    }

    @Test
    void role_EnumValues_ShouldHaveCorrectValues() {
        // Verify
        assertEquals("system", ChatRequest.Role.SYSTEM.getValue());
        assertEquals("user", ChatRequest.Role.USER.getValue());
        assertEquals("assistant", ChatRequest.Role.ASSISTANT.getValue());
    }

    @Test
    void builder_FluentInterface_ShouldReturnSameBuilderInstance() {
        // Execute
        ChatRequest.Builder builder = ChatRequest.builder();
        
        // Verify fluent interface
        assertSame(builder, builder.addUserMessage("test"));
        assertSame(builder, builder.addAssistantMessage("test"));
        assertSame(builder, builder.addSystemMessage("test"));
        assertSame(builder, builder.model("test-model"));
        assertSame(builder, builder.temperature(1.0));
        assertSame(builder, builder.maxTokens(500));
        assertSame(builder, builder.additionalParams(Map.of()));
    }

    @Test
    void builder_ComplexConversation_ShouldMaintainOrder() {
        // Execute
        ChatRequest request = ChatRequest.builder()
            .addSystemMessage("You are helpful")
            .addUserMessage("Hello")
            .addAssistantMessage("Hi there!")
            .addUserMessage("How are you?")
            .addAssistantMessage("I'm doing well, thanks!")
            .build();
        
        // Verify
        List<ChatRequest.Message> messages = request.getMessages();
        assertEquals(5, messages.size());
        
        assertEquals(ChatRequest.Role.SYSTEM, messages.get(0).getRole());
        assertEquals("You are helpful", messages.get(0).getContent());
        
        assertEquals(ChatRequest.Role.USER, messages.get(1).getRole());
        assertEquals("Hello", messages.get(1).getContent());
        
        assertEquals(ChatRequest.Role.ASSISTANT, messages.get(2).getRole());
        assertEquals("Hi there!", messages.get(2).getContent());
        
        assertEquals(ChatRequest.Role.USER, messages.get(3).getRole());
        assertEquals("How are you?", messages.get(3).getContent());
        
        assertEquals(ChatRequest.Role.ASSISTANT, messages.get(4).getRole());
        assertEquals("I'm doing well, thanks!", messages.get(4).getContent());
    }

    @Test
    void builder_AllParameters_ShouldSetCorrectly() {
        // Setup
        Map<String, Object> params = Map.of("stream", true);
        List<ChatRequest.Message> messages = List.of(ChatRequest.Message.user("Test"));
        
        // Execute
        ChatRequest request = ChatRequest.builder()
            .messages(messages)
            .model("custom-model")
            .temperature(1.5)
            .maxTokens(500)
            .additionalParams(params)
            .build();
        
        // Verify all fields
        assertEquals(1, request.getMessages().size());
        assertEquals("custom-model", request.getModel());
        assertEquals(1.5, request.getTemperature());
        assertEquals(500, request.getMaxTokens());
        assertEquals(params, request.getAdditionalParams());
    }
}