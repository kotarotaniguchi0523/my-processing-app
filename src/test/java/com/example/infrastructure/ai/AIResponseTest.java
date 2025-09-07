package com.example.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIResponse and its nested classes (Usage, ToolCall)
 */
class AIResponseTest {

    @Test
    void builder_DefaultValues_ShouldBeNull() {
        // Execute
        AIResponse response = AIResponse.builder().build();
        
        // Verify
        assertNull(response.getContent());
        assertNull(response.getModel());
        assertNull(response.getId());
        assertNull(response.getUsage());
        assertNull(response.getToolCalls());
        assertNull(response.getMetadata());
    }

    @Test
    void builder_SetContent_ShouldSetCorrectly() {
        // Execute
        AIResponse response = AIResponse.builder()
            .content("Test response content")
            .build();
        
        // Verify
        assertEquals("Test response content", response.getContent());
    }

    @Test
    void builder_SetModel_ShouldSetCorrectly() {
        // Execute
        AIResponse response = AIResponse.builder()
            .model("gpt-4-turbo")
            .build();
        
        // Verify
        assertEquals("gpt-4-turbo", response.getModel());
    }

    @Test
    void builder_SetId_ShouldSetCorrectly() {
        // Execute
        AIResponse response = AIResponse.builder()
            .id("response-12345")
            .build();
        
        // Verify
        assertEquals("response-12345", response.getId());
    }

    @Test
    void builder_SetUsage_ShouldSetCorrectly() {
        // Setup
        AIResponse.Usage usage = new AIResponse.Usage(100, 50, 150);
        
        // Execute
        AIResponse response = AIResponse.builder()
            .usage(usage)
            .build();
        
        // Verify
        assertEquals(usage, response.getUsage());
        assertEquals(100, response.getUsage().getPromptTokens());
        assertEquals(50, response.getUsage().getCompletionTokens());
        assertEquals(150, response.getUsage().getTotalTokens());
    }

    @Test
    void builder_SetToolCalls_ShouldSetCorrectly() {
        // Setup
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall(
            "call_123", 
            "get_weather", 
            Map.of("location", "New York")
        );
        List<AIResponse.ToolCall> toolCalls = List.of(toolCall);
        
        // Execute
        AIResponse response = AIResponse.builder()
            .toolCalls(toolCalls)
            .build();
        
        // Verify
        assertEquals(toolCalls, response.getToolCalls());
        assertEquals(1, response.getToolCalls().size());
        assertEquals(toolCall, response.getToolCalls().get(0));
    }

    @Test
    void builder_SetMetadata_ShouldSetCorrectly() {
        // Setup
        Map<String, Object> metadata = Map.of(
            "request_id", "req_123",
            "processing_time_ms", 250,
            "model_version", "v2.1"
        );
        
        // Execute
        AIResponse response = AIResponse.builder()
            .metadata(metadata)
            .build();
        
        // Verify
        assertEquals(metadata, response.getMetadata());
    }

    @Test
    void builder_AllFields_ShouldSetCorrectly() {
        // Setup
        AIResponse.Usage usage = new AIResponse.Usage(200, 100, 300);
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_456", "calculate", Map.of("x", 5, "y", 3));
        List<AIResponse.ToolCall> toolCalls = List.of(toolCall);
        Map<String, Object> metadata = Map.of("source", "test");
        
        // Execute
        AIResponse response = AIResponse.builder()
            .content("Complete response")
            .model("claude-3-haiku")
            .id("resp_789")
            .usage(usage)
            .toolCalls(toolCalls)
            .metadata(metadata)
            .build();
        
        // Verify
        assertEquals("Complete response", response.getContent());
        assertEquals("claude-3-haiku", response.getModel());
        assertEquals("resp_789", response.getId());
        assertEquals(usage, response.getUsage());
        assertEquals(toolCalls, response.getToolCalls());
        assertEquals(metadata, response.getMetadata());
    }

    @Test
    void hasToolCalls_NoToolCalls_ShouldReturnFalse() {
        // Execute
        AIResponse response = AIResponse.builder().build();
        
        // Verify
        assertFalse(response.hasToolCalls());
    }

    @Test
    void hasToolCalls_EmptyToolCalls_ShouldReturnFalse() {
        // Execute
        AIResponse response = AIResponse.builder()
            .toolCalls(List.of())
            .build();
        
        // Verify
        assertFalse(response.hasToolCalls());
    }

    @Test
    void hasToolCalls_WithToolCalls_ShouldReturnTrue() {
        // Setup
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_1", "test_tool", Map.of());
        
        // Execute
        AIResponse response = AIResponse.builder()
            .toolCalls(List.of(toolCall))
            .build();
        
        // Verify
        assertTrue(response.hasToolCalls());
    }

    @Test
    void builder_FluentInterface_ShouldReturnSameBuilderInstance() {
        // Execute
        AIResponse.Builder builder = AIResponse.builder();
        
        // Verify fluent interface
        assertSame(builder, builder.content("test"));
        assertSame(builder, builder.model("test-model"));
        assertSame(builder, builder.id("test-id"));
        assertSame(builder, builder.usage(new AIResponse.Usage(1, 1, 2)));
        assertSame(builder, builder.toolCalls(List.of()));
        assertSame(builder, builder.metadata(Map.of()));
    }

    // Usage class tests
    @Test
    void usage_Constructor_ShouldSetFieldsCorrectly() {
        // Execute
        AIResponse.Usage usage = new AIResponse.Usage(150, 75, 225);
        
        // Verify
        assertEquals(150, usage.getPromptTokens());
        assertEquals(75, usage.getCompletionTokens());
        assertEquals(225, usage.getTotalTokens());
    }

    @Test
    void usage_ZeroValues_ShouldBeAllowed() {
        // Execute
        AIResponse.Usage usage = new AIResponse.Usage(0, 0, 0);
        
        // Verify
        assertEquals(0, usage.getPromptTokens());
        assertEquals(0, usage.getCompletionTokens());
        assertEquals(0, usage.getTotalTokens());
    }

    @Test
    void usage_LargeValues_ShouldBeHandled() {
        // Execute
        AIResponse.Usage usage = new AIResponse.Usage(
            Integer.MAX_VALUE, 
            Integer.MAX_VALUE / 2, 
            Integer.MAX_VALUE
        );
        
        // Verify
        assertEquals(Integer.MAX_VALUE, usage.getPromptTokens());
        assertEquals(Integer.MAX_VALUE / 2, usage.getCompletionTokens());
        assertEquals(Integer.MAX_VALUE, usage.getTotalTokens());
    }

    // ToolCall class tests
    @Test
    void toolCall_Constructor_ShouldSetFieldsCorrectly() {
        // Setup
        Map<String, Object> parameters = Map.of(
            "query", "weather in Paris",
            "units", "celsius"
        );
        
        // Execute
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_abc", "get_weather", parameters);
        
        // Verify
        assertEquals("call_abc", toolCall.getId());
        assertEquals("get_weather", toolCall.getName());
        assertEquals(parameters, toolCall.getParameters());
    }

    @Test
    void toolCall_NullParameters_ShouldBeAllowed() {
        // Execute
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_def", "no_params_tool", null);
        
        // Verify
        assertEquals("call_def", toolCall.getId());
        assertEquals("no_params_tool", toolCall.getName());
        assertNull(toolCall.getParameters());
    }

    @Test
    void toolCall_EmptyParameters_ShouldBeAllowed() {
        // Execute
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_ghi", "empty_params_tool", Map.of());
        
        // Verify
        assertEquals("call_ghi", toolCall.getId());
        assertEquals("empty_params_tool", toolCall.getName());
        assertEquals(Map.of(), toolCall.getParameters());
    }

    @Test
    void toolCall_ComplexParameters_ShouldBeHandled() {
        // Setup
        Map<String, Object> parameters = Map.of(
            "string_param", "value",
            "number_param", 42,
            "boolean_param", true,
            "list_param", List.of("a", "b", "c"),
            "nested_object", Map.of("key", "nested_value")
        );
        
        // Execute
        AIResponse.ToolCall toolCall = new AIResponse.ToolCall("call_jkl", "complex_tool", parameters);
        
        // Verify
        assertEquals("call_jkl", toolCall.getId());
        assertEquals("complex_tool", toolCall.getName());
        assertEquals(parameters, toolCall.getParameters());
        
        // Verify complex parameter access
        assertEquals("value", toolCall.getParameters().get("string_param"));
        assertEquals(42, toolCall.getParameters().get("number_param"));
        assertTrue((Boolean) toolCall.getParameters().get("boolean_param"));
        assertEquals(List.of("a", "b", "c"), toolCall.getParameters().get("list_param"));
        assertEquals(Map.of("key", "nested_value"), toolCall.getParameters().get("nested_object"));
    }

    @Test
    void builder_MultipleBuilds_ShouldCreateIndependentInstances() {
        // Setup
        AIResponse.Builder builder = AIResponse.builder()
            .content("shared content")
            .model("shared model");
        
        // Execute
        AIResponse response1 = builder.build();
        AIResponse response2 = builder.build();
        
        // Verify
        assertNotSame(response1, response2);
        assertEquals(response1.getContent(), response2.getContent());
        assertEquals(response1.getModel(), response2.getModel());
    }

    @Test
    void response_NullFields_ShouldNotCauseIssues() {
        // Execute
        AIResponse response = AIResponse.builder()
            .content(null)
            .model(null)
            .id(null)
            .usage(null)
            .toolCalls(null)
            .metadata(null)
            .build();
        
        // Verify
        assertNull(response.getContent());
        assertNull(response.getModel());
        assertNull(response.getId());
        assertNull(response.getUsage());
        assertNull(response.getToolCalls());
        assertNull(response.getMetadata());
        assertFalse(response.hasToolCalls()); // Should handle null gracefully
    }

    @Test
    void response_EmptyStringFields_ShouldBeAllowed() {
        // Execute
        AIResponse response = AIResponse.builder()
            .content("")
            .model("")
            .id("")
            .build();
        
        // Verify
        assertEquals("", response.getContent());
        assertEquals("", response.getModel());
        assertEquals("", response.getId());
    }
}