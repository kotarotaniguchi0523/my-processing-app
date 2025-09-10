package com.example.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIClientConfig and its builder pattern
 */
class AIClientConfigTest {

    @Test
    void defaultConfig_ShouldHaveCorrectDefaults() {
        // Execute
        AIClientConfig config = AIClientConfig.defaultConfig();
        
        // Verify
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(3, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(1), config.getRetryDelay());
        assertTrue(config.isLoggingEnabled());
        assertFalse(config.isStreamingEnabled());
        assertNull(config.getBaseUrl());
        assertNull(config.getModelName());
    }

    @Test
    void builder_DefaultValues_ShouldMatchDefaults() {
        // Execute
        AIClientConfig config = AIClientConfig.builder().build();
        
        // Verify
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(3, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(1), config.getRetryDelay());
        assertTrue(config.isLoggingEnabled());
        assertFalse(config.isStreamingEnabled());
        assertNull(config.getBaseUrl());
        assertNull(config.getModelName());
    }

    @Test
    void builder_CustomTimeout_ShouldSetCorrectly() {
        // Setup
        Duration customTimeout = Duration.ofMinutes(2);
        
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .timeout(customTimeout)
            .build();
        
        // Verify
        assertEquals(customTimeout, config.getTimeout());
    }

    @Test
    void builder_CustomMaxRetries_ShouldSetCorrectly() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .maxRetries(5)
            .build();
        
        // Verify
        assertEquals(5, config.getMaxRetries());
    }

    @Test
    void builder_NegativeMaxRetries_ShouldThrowException() {
        // Execute & Verify
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            AIClientConfig.builder().maxRetries(-1));
        
        assertEquals("Max retries must be non-negative", exception.getMessage());
    }

    @Test
    void builder_ZeroMaxRetries_ShouldBeAllowed() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .maxRetries(0)
            .build();
        
        // Verify
        assertEquals(0, config.getMaxRetries());
    }

    @Test
    void builder_CustomRetryDelay_ShouldSetCorrectly() {
        // Setup
        Duration customDelay = Duration.ofSeconds(5);
        
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .retryDelay(customDelay)
            .build();
        
        // Verify
        assertEquals(customDelay, config.getRetryDelay());
    }

    @Test
    void builder_DisableLogging_ShouldSetCorrectly() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .enableLogging(false)
            .build();
        
        // Verify
        assertFalse(config.isLoggingEnabled());
    }

    @Test
    void builder_EnableStreaming_ShouldSetCorrectly() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .enableStreaming(true)
            .build();
        
        // Verify
        assertTrue(config.isStreamingEnabled());
    }

    @Test
    void builder_CustomBaseUrl_ShouldSetCorrectly() {
        // Setup
        String customUrl = "https://api.custom.ai/v1";
        
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .baseUrl(customUrl)
            .build();
        
        // Verify
        assertEquals(customUrl, config.getBaseUrl());
    }

    @Test
    void builder_CustomModelName_ShouldSetCorrectly() {
        // Setup
        String customModel = "gpt-4-turbo";
        
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .modelName(customModel)
            .build();
        
        // Verify
        assertEquals(customModel, config.getModelName());
    }

    @Test
    void builder_AllCustomValues_ShouldSetCorrectly() {
        // Setup
        Duration timeout = Duration.ofMinutes(5);
        Duration retryDelay = Duration.ofSeconds(10);
        String baseUrl = "https://custom.api.com";
        String modelName = "custom-model";
        
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .timeout(timeout)
            .maxRetries(10)
            .retryDelay(retryDelay)
            .enableLogging(false)
            .enableStreaming(true)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .build();
        
        // Verify
        assertEquals(timeout, config.getTimeout());
        assertEquals(10, config.getMaxRetries());
        assertEquals(retryDelay, config.getRetryDelay());
        assertFalse(config.isLoggingEnabled());
        assertTrue(config.isStreamingEnabled());
        assertEquals(baseUrl, config.getBaseUrl());
        assertEquals(modelName, config.getModelName());
    }

    @Test
    void builder_FluentInterface_ShouldReturnSameBuilderInstance() {
        // Execute
        AIClientConfig.Builder builder = AIClientConfig.builder();
        
        // Verify fluent interface
        assertSame(builder, builder.timeout(Duration.ofSeconds(30)));
        assertSame(builder, builder.maxRetries(5));
        assertSame(builder, builder.retryDelay(Duration.ofSeconds(2)));
        assertSame(builder, builder.enableLogging(true));
        assertSame(builder, builder.enableStreaming(false));
        assertSame(builder, builder.baseUrl("https://example.com"));
        assertSame(builder, builder.modelName("test-model"));
    }

    @Test
    void builder_MultipleBuilds_ShouldCreateIndependentInstances() {
        // Setup
        AIClientConfig.Builder builder = AIClientConfig.builder()
            .timeout(Duration.ofSeconds(60))
            .maxRetries(5);
        
        // Execute
        AIClientConfig config1 = builder.build();
        AIClientConfig config2 = builder.build();
        
        // Verify
        assertNotSame(config1, config2);
        assertEquals(config1.getTimeout(), config2.getTimeout());
        assertEquals(config1.getMaxRetries(), config2.getMaxRetries());
    }

    @Test
    void builder_NullValues_ShouldBeAllowed() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .timeout(null)
            .retryDelay(null)
            .baseUrl(null)
            .modelName(null)
            .build();
        
        // Verify
        assertNull(config.getTimeout());
        assertNull(config.getRetryDelay());
        assertNull(config.getBaseUrl());
        assertNull(config.getModelName());
    }

    @Test
    void getters_ShouldReturnCorrectValues() {
        // Setup
        Duration timeout = Duration.ofMinutes(1);
        Duration retryDelay = Duration.ofSeconds(3);
        String baseUrl = "https://test.api.com";
        String modelName = "test-model-v2";
        
        AIClientConfig config = AIClientConfig.builder()
            .timeout(timeout)
            .maxRetries(7)
            .retryDelay(retryDelay)
            .enableLogging(false)
            .enableStreaming(true)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .build();
        
        // Verify all getters
        assertEquals(timeout, config.getTimeout());
        assertEquals(7, config.getMaxRetries());
        assertEquals(retryDelay, config.getRetryDelay());
        assertFalse(config.isLoggingEnabled());
        assertTrue(config.isStreamingEnabled());
        assertEquals(baseUrl, config.getBaseUrl());
        assertEquals(modelName, config.getModelName());
    }

    @Test
    void defaultConfig_MultipleCalls_ShouldReturnDifferentInstances() {
        // Execute
        AIClientConfig config1 = AIClientConfig.defaultConfig();
        AIClientConfig config2 = AIClientConfig.defaultConfig();
        
        // Verify
        assertNotSame(config1, config2);
        assertEquals(config1.getTimeout(), config2.getTimeout());
        assertEquals(config1.getMaxRetries(), config2.getMaxRetries());
    }

    @Test
    void builder_EdgeCaseValues_ShouldHandleCorrectly() {
        // Execute
        AIClientConfig config = AIClientConfig.builder()
            .timeout(Duration.ZERO)
            .maxRetries(Integer.MAX_VALUE)
            .retryDelay(Duration.ofNanos(1))
            .baseUrl("")
            .modelName("")
            .build();
        
        // Verify
        assertEquals(Duration.ZERO, config.getTimeout());
        assertEquals(Integer.MAX_VALUE, config.getMaxRetries());
        assertEquals(Duration.ofNanos(1), config.getRetryDelay());
        assertEquals("", config.getBaseUrl());
        assertEquals("", config.getModelName());
    }
}