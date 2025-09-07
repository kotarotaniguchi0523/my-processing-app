package com.example.agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test class for the enhanced ToolValidator functionality
 * Tests comprehensive JSON schema validation including types, nested objects, arrays, etc.
 */
class ToolValidatorTest {

    private ToolInvoker toolInvoker;
    private TestTool testTool;

    @BeforeEach
    void setUp() {
        toolInvoker = new ToolInvoker();
        testTool = new TestTool();
    }

    @Test
    void testValidStringParameter() {
        Map<String, Object> parameters = Map.of("name", "test");
        
        // Create a mock validation result using reflection to access the inner validator
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Valid string parameter should pass validation");
    }

    @Test
    void testInvalidStringParameter() {
        Map<String, Object> parameters = Map.of("name", 123);
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Invalid string parameter should fail validation");
    }

    @Test
    void testMissingRequiredParameter() {
        Map<String, Object> parameters = Map.of("optional", "value");
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Missing required parameter should fail validation");
    }

    @Test
    void testValidNumberParameter() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "age", 25
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Valid number parameter should pass validation");
    }

    @Test
    void testValidArrayParameter() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "tags", Arrays.asList("tag1", "tag2", "tag3")
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Valid array parameter should pass validation");
    }

    @Test
    void testInvalidArrayItemType() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "tags", Arrays.asList("tag1", 123, "tag3")
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Array with invalid item types should fail validation");
    }

    @Test
    void testValidNestedObject() {
        Map<String, Object> address = Map.of(
            "street", "123 Main St",
            "city", "Anytown"
        );
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "address", address
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Valid nested object should pass validation");
    }

    @Test
    void testInvalidNestedObjectMissingField() {
        Map<String, Object> address = Map.of(
            "street", "123 Main St"
            // missing required "city" field
        );
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "address", address
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Nested object missing required field should fail validation");
    }

    @Test
    void testEnumValidation() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "status", "active"
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Valid enum value should pass validation");
    }

    @Test
    void testInvalidEnumValue() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "status", "invalid"
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Invalid enum value should fail validation");
    }

    @Test
    void testNumberRangeValidation() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "score", 85
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertTrue(isValid, "Number within valid range should pass validation");
    }

    @Test
    void testNumberOutOfRange() {
        Map<String, Object> parameters = Map.of(
            "name", "test",
            "score", 150  // max is 100
        );
        
        boolean isValid = validateParameters(testTool, parameters);
        assertFalse(isValid, "Number out of range should fail validation");
    }

    /**
     * Helper method to validate parameters using reflection to access the private validator
     */
    private boolean validateParameters(TestTool tool, Map<String, Object> parameters) {
        try {
            // Create a new ToolInvoker to access the validator
            ToolInvoker invoker = new ToolInvoker();
            return invoker.canInvokeTool(tool.name(), parameters);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Test tool implementation with comprehensive schema
     */
    private static class TestTool implements com.example.domain.tools.IAgentTool {
        
        @Override
        public String name() {
            return "testTool";
        }

        @Override
        public Map<String, Object> schema() {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            schema.put("description", "Test tool for validation");
            
            Map<String, Object> properties = new HashMap<>();
            
            // String property
            Map<String, Object> nameProperty = new HashMap<>();
            nameProperty.put("type", "string");
            nameProperty.put("description", "Name parameter");
            nameProperty.put("minLength", 1);
            nameProperty.put("maxLength", 50);
            properties.put("name", nameProperty);
            
            // Number property with range
            Map<String, Object> ageProperty = new HashMap<>();
            ageProperty.put("type", "number");
            ageProperty.put("description", "Age parameter");
            ageProperty.put("minimum", 0);
            ageProperty.put("maximum", 150);
            properties.put("age", ageProperty);
            
            // Integer property with range
            Map<String, Object> scoreProperty = new HashMap<>();
            scoreProperty.put("type", "integer");
            scoreProperty.put("description", "Score parameter");
            scoreProperty.put("minimum", 0);
            scoreProperty.put("maximum", 100);
            properties.put("score", scoreProperty);
            
            // Boolean property
            Map<String, Object> activeProperty = new HashMap<>();
            activeProperty.put("type", "boolean");
            activeProperty.put("description", "Active status");
            properties.put("active", activeProperty);
            
            // Array property with string items
            Map<String, Object> tagsProperty = new HashMap<>();
            tagsProperty.put("type", "array");
            tagsProperty.put("description", "Tags array");
            tagsProperty.put("minItems", 1);
            tagsProperty.put("maxItems", 10);
            Map<String, Object> tagItems = new HashMap<>();
            tagItems.put("type", "string");
            tagsProperty.put("items", tagItems);
            properties.put("tags", tagsProperty);
            
            // Nested object property
            Map<String, Object> addressProperty = new HashMap<>();
            addressProperty.put("type", "object");
            addressProperty.put("description", "Address object");
            Map<String, Object> addressProperties = new HashMap<>();
            
            Map<String, Object> streetProperty = new HashMap<>();
            streetProperty.put("type", "string");
            streetProperty.put("description", "Street address");
            addressProperties.put("street", streetProperty);
            
            Map<String, Object> cityProperty = new HashMap<>();
            cityProperty.put("type", "string");
            cityProperty.put("description", "City name");
            addressProperties.put("city", cityProperty);
            
            addressProperty.put("properties", addressProperties);
            addressProperty.put("required", Arrays.asList("street", "city"));
            properties.put("address", addressProperty);
            
            // Enum string property
            Map<String, Object> statusProperty = new HashMap<>();
            statusProperty.put("type", "string");
            statusProperty.put("description", "Status value");
            statusProperty.put("enum", Arrays.asList("active", "inactive", "pending"));
            properties.put("status", statusProperty);
            
            schema.put("properties", properties);
            schema.put("required", Arrays.asList("name"));
            schema.put("additionalProperties", false);
            
            return schema;
        }

        @Override
        public com.example.domain.models.DrawingState execute(Map<String, Object> parameters, 
                com.example.domain.models.DrawingState state) {
            return state; // No-op for testing
        }
    }
}