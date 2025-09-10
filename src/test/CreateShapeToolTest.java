package com.example.infrastructure.agent.tools;

import com.example.infrastructure.agent.tools.CreateShapeTool;

import com.example.domain.models.Circle;
import com.example.domain.models.DrawingState;
import com.example.domain.models.Line;
import com.example.domain.models.Rectangle;
import com.example.domain.models.Shape;
import com.example.domain.models.Text;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CreateShapeTool following AAA pattern.
 * Test naming convention: test_MethodName_Scenario_ExpectedOutcome
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateShapeTool Tests")
class CreateShapeToolTest {

    private CreateShapeTool createShapeTool;
    
    @Mock
    private DrawingState mockDrawingState;
    
    @Mock
    private DrawingState mockUpdatedState;

    @BeforeEach
    void setUp() {
        createShapeTool = new CreateShapeTool();
    }

    @Nested
    @DisplayName("Valid Shape Creation Tests")
    class ValidShapeCreationTests {

        @Test
        @DisplayName("test_CreateCircle_ValidParameters_CreatesCircleSuccessfully")
        void test_CreateCircle_ValidParameters_CreatesCircleSuccessfully() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 200.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("radius", 50.0);
            parameters.put("properties", properties);
            
            Map<String, Object> style = new HashMap<>();
            Map<String, Object> fillColor = new HashMap<>();
            fillColor.put("r", 255);
            fillColor.put("g", 0);
            fillColor.put("b", 0);
            style.put("fillColor", fillColor);
            parameters.put("style", style);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(mockUpdatedState, result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Circle &&
                    shape.getPosition().getX() == 100.0f &&
                    shape.getPosition().getY() == 200.0f &&
                    ((Circle) shape).getRadius() == 50.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateRectangle_ValidParameters_CreatesRectangleSuccessfully")
        void test_CreateRectangle_ValidParameters_CreatesRectangleSuccessfully() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "rectangle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 50.0);
            position.put("y", 50.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("width", 100.0);
            properties.put("height", 80.0);
            parameters.put("properties", properties);
            
            Map<String, Object> style = new HashMap<>();
            Map<String, Object> strokeColor = new HashMap<>();
            strokeColor.put("r", 0);
            strokeColor.put("g", 0);
            strokeColor.put("b", 255);
            style.put("strokeColor", strokeColor);
            style.put("strokeWeight", 2.0);
            parameters.put("style", style);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(mockUpdatedState, result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Rectangle &&
                    shape.getPosition().getX() == 50.0f &&
                    shape.getPosition().getY() == 50.0f &&
                    ((Rectangle) shape).getWidth() == 100.0f &&
                    ((Rectangle) shape).getHeight() == 80.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateLine_ValidParameters_CreatesLineSuccessfully")
        void test_CreateLine_ValidParameters_CreatesLineSuccessfully() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "line");
            
            Map<String, Object> fromPosition = new HashMap<>();
            fromPosition.put("x", 0.0);
            fromPosition.put("y", 0.0);
            parameters.put("position", fromPosition);
            
            Map<String, Object> properties = new HashMap<>();
            Map<String, Object> toPosition = new HashMap<>();
            toPosition.put("x", 300.0);
            toPosition.put("y", 300.0);
            properties.put("to", toPosition);
            parameters.put("properties", properties);
            
            Map<String, Object> style = new HashMap<>();
            Map<String, Object> strokeColor = new HashMap<>();
            strokeColor.put("hex", "#00FF00");
            style.put("strokeColor", strokeColor);
            parameters.put("style", style);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(mockUpdatedState, result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Line &&
                    shape.getPosition().getX() == 0.0f &&
                    shape.getPosition().getY() == 0.0f &&
                    ((Line) shape).getEndPosition().getX() == 300.0f &&
                    ((Line) shape).getEndPosition().getY() == 300.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateText_ValidParameters_CreatesTextSuccessfully")
        void test_CreateText_ValidParameters_CreatesTextSuccessfully() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "text");
            parameters.put("id", "text_001");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 150.0);
            position.put("y", 250.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("text", "Hello World");
            properties.put("fontSize", 24.0);
            properties.put("fontFamily", "Arial");
            parameters.put("properties", properties);
            
            parameters.put("visible", true);
            parameters.put("layerOrder", 10);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(mockUpdatedState, result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Text &&
                    shape.getId().equals("text_001") &&
                    shape.getPosition().getX() == 150.0f &&
                    shape.getPosition().getY() == 250.0f &&
                    ((Text) shape).getContent().equals("Hello World") &&
                    ((Text) shape).getFontSize() == 24.0f &&
                    shape.isVisible() &&
                    shape.getLayerOrder() == 10
                ))
            );
        }

        @Test
        @DisplayName("test_CreateShape_WithDefaultValues_UsesCorrectDefaults")
        void test_CreateShape_WithDefaultValues_UsesCorrectDefaults() {
            // Arrange - minimal required parameters only
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            // No properties, style, visibility, or layerOrder specified
            parameters.put("properties", new HashMap<>());
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Circle &&
                    ((Circle) shape).getRadius() == 50.0f && // default radius
                    shape.isVisible() && // default visibility is true
                    shape.getLayerOrder() == 0 // default layer order
                ))
            );
        }
    }

    @Nested
    @DisplayName("Invalid Parameter Handling Tests")
    class InvalidParameterHandlingTests {

        @Test
        @DisplayName("test_CreateShape_NullParameters_ThrowsIllegalArgumentException")
        void test_CreateShape_NullParameters_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = null;
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Parameters cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateShape_MissingType_ThrowsIllegalArgumentException")
        void test_CreateShape_MissingType_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            // type is missing
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Shape type is required", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateShape_MissingPosition_ThrowsIllegalArgumentException")
        void test_CreateShape_MissingPosition_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            // position is missing
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Position is required", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateShape_InvalidShapeType_ThrowsIllegalArgumentException")
        void test_CreateShape_InvalidShapeType_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "triangle"); // invalid type
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertTrue(exception.getMessage().contains("Invalid shape type: triangle"));
        }

        @Test
        @DisplayName("test_CreateLine_MissingToPosition_ThrowsIllegalArgumentException")
        void test_CreateLine_MissingToPosition_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "line");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 0.0);
            position.put("y", 0.0);
            parameters.put("position", position);
            
            // properties exist but no 'to' position
            parameters.put("properties", new HashMap<>());
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Line shape requires 'to' position in properties", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateText_MissingTextContent_ThrowsIllegalArgumentException")
        void test_CreateText_MissingTextContent_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "text");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            // properties exist but no text content
            parameters.put("properties", new HashMap<>());
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Text shape requires 'text' content in properties", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateText_EmptyTextContent_ThrowsIllegalArgumentException")
        void test_CreateText_EmptyTextContent_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "text");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("text", ""); // empty text
            parameters.put("properties", properties);
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Text shape requires 'text' content in properties", exception.getMessage());
        }

        @Test
        @DisplayName("test_CreateShape_InvalidColor_ThrowsIllegalArgumentException")
        void test_CreateShape_InvalidColor_ThrowsIllegalArgumentException() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> style = new HashMap<>();
            Map<String, Object> fillColor = new HashMap<>();
            // Invalid color - missing required RGB values or hex
            fillColor.put("invalid", "value");
            style.put("fillColor", fillColor);
            parameters.put("style", style);
            
            parameters.put("properties", new HashMap<>());
            
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createShapeTool.execute(parameters, mockDrawingState)
            );
            
            assertEquals("Color must have either 'r', 'g', 'b' values or 'hex' value", 
                exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Boundary Condition Tests")
    class BoundaryConditionTests {

        @Test
        @DisplayName("test_CreateCircle_ZeroRadius_HandledCorrectly")
        void test_CreateCircle_ZeroRadius_HandledCorrectly() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("radius", 0.0); // zero radius
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Circle &&
                    ((Circle) shape).getRadius() == 0.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateRectangle_ZeroDimensions_HandledCorrectly")
        void test_CreateRectangle_ZeroDimensions_HandledCorrectly() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "rectangle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 50.0);
            position.put("y", 50.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("width", 0.0);
            properties.put("height", 0.0);
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Rectangle &&
                    ((Rectangle) shape).getWidth() == 0.0f &&
                    ((Rectangle) shape).getHeight() == 0.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateShape_ExtremelyLargeValues_HandledCorrectly")
        void test_CreateShape_ExtremelyLargeValues_HandledCorrectly() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", Float.MAX_VALUE);
            position.put("y", Float.MAX_VALUE);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("radius", 10000000.0f); // extremely large radius
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Circle &&
                    shape.getPosition().getX() == Float.MAX_VALUE &&
                    shape.getPosition().getY() == Float.MAX_VALUE &&
                    ((Circle) shape).getRadius() == 10000000.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateShape_NegativeCoordinates_HandledCorrectly")
        void test_CreateShape_NegativeCoordinates_HandledCorrectly() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "rectangle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", -100.0);
            position.put("y", -200.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("width", 50.0);
            properties.put("height", 75.0);
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Rectangle &&
                    shape.getPosition().getX() == -100.0f &&
                    shape.getPosition().getY() == -200.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateLine_SameStartEndPosition_HandledCorrectly")
        void test_CreateLine_SameStartEndPosition_HandledCorrectly() {
            // Arrange - line with same start and end position (zero length)
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "line");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            Map<String, Object> toPosition = new HashMap<>();
            toPosition.put("x", 100.0); // same as from
            toPosition.put("y", 100.0); // same as from
            properties.put("to", toPosition);
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Line &&
                    shape.getPosition().getX() == 100.0f &&
                    shape.getPosition().getY() == 100.0f &&
                    ((Line) shape).getEndPosition().getX() == 100.0f &&
                    ((Line) shape).getEndPosition().getY() == 100.0f
                ))
            );
        }

        @Test
        @DisplayName("test_CreateText_VeryLongText_HandledCorrectly")
        void test_CreateText_VeryLongText_HandledCorrectly() {
            // Arrange
            String veryLongText = "A".repeat(1000); // 1000 character text
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "text");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("text", veryLongText);
            properties.put("fontSize", 12.0);
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Text &&
                    ((Text) shape).getContent().equals(veryLongText) &&
                    ((Text) shape).getContent().length() == 1000
                ))
            );
        }
    }

    @Nested
    @DisplayName("Tool Validation Logic Tests")
    class ToolValidationLogicTests {

        @Test
        @DisplayName("test_ValidateShapeParameters_ValidCircleInput_ReturnsTrue")
        void test_ValidateShapeParameters_ValidCircleInput_ReturnsTrue() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 200.0);
            parameters.put("position", position);
            
            // Act - validation happens during execute
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Should not throw any exception
            assertDoesNotThrow(() -> createShapeTool.execute(parameters, mockDrawingState));
        }

        @Test
        @DisplayName("test_ValidateShapeParameters_CaseInsensitiveType_HandledCorrectly")
        void test_ValidateShapeParameters_CaseInsensitiveType_HandledCorrectly() {
            // Arrange - test case insensitive type handling
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "CIRCLE"); // uppercase
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 200.0);
            parameters.put("position", position);
            
            parameters.put("properties", new HashMap<>());
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> 
                    shape instanceof Circle
                ))
            );
        }

        @Test
        @DisplayName("test_Schema_Method_ReturnsCompleteSchema")
        void test_Schema_Method_ReturnsCompleteSchema() {
            // Arrange & Act
            Map<String, Object> schema = createShapeTool.schema();
            
            // Assert
            assertAll(
                () -> assertNotNull(schema),
                () -> assertEquals("object", schema.get("type")),
                () -> assertNotNull(schema.get("description")),
                () -> assertNotNull(schema.get("properties")),
                () -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                    assertTrue(properties.containsKey("type"));
                    assertTrue(properties.containsKey("position"));
                    assertTrue(properties.containsKey("properties"));
                    assertTrue(properties.containsKey("style"));
                    assertTrue(properties.containsKey("visible"));
                    assertTrue(properties.containsKey("layerOrder"));
                },
                () -> {
                    @SuppressWarnings("unchecked")
                    List<String> required = (List<String>) schema.get("required");
                    assertTrue(required.contains("type"));
                    assertTrue(required.contains("position"));
                }
            );
        }

        @Test
        @DisplayName("test_Name_Method_ReturnsCorrectToolName")
        void test_Name_Method_ReturnsCorrectToolName() {
            // Act & Assert
            assertEquals("createShape", createShapeTool.name());
        }

        @Test
        @DisplayName("test_AutoGeneratedId_WhenNotProvided_GeneratesUniqueId")
        void test_AutoGeneratedId_WhenNotProvided_GeneratesUniqueId() {
            // Arrange - no ID provided
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            parameters.put("properties", new HashMap<>());
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            verify(mockDrawingState).withAddedShape(argThat(shape -> 
                shape.getId() != null &&
                shape.getId().startsWith("circle_") &&
                shape.getId().matches("circle_\\d+_\\d+")
            ));
        }
    }

    @Nested
    @DisplayName("DrawingState Integration Tests")
    class DrawingStateIntegrationTests {

        @Test
        @DisplayName("test_CreateShape_SuccessfulCreation_UpdatesDrawingState")
        void test_CreateShape_SuccessfulCreation_UpdatesDrawingState() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "circle");
            parameters.put("id", "test_circle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 100.0);
            position.put("y", 100.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("radius", 25.0);
            parameters.put("properties", properties);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertEquals(mockUpdatedState, result),
                () -> verify(mockDrawingState, times(1)).withAddedShape(any(Circle.class)),
                () -> verifyNoMoreInteractions(mockDrawingState)
            );
        }

        @Test
        @DisplayName("test_CreateMultipleShapes_MaintainsStateConsistency")
        void test_CreateMultipleShapes_MaintainsStateConsistency() {
            // Arrange - create multiple shapes sequentially
            Map<String, Object> circleParams = new HashMap<>();
            circleParams.put("type", "circle");
            Map<String, Object> circlePos = new HashMap<>();
            circlePos.put("x", 100.0);
            circlePos.put("y", 100.0);
            circleParams.put("position", circlePos);
            circleParams.put("properties", new HashMap<>());
            
            Map<String, Object> rectParams = new HashMap<>();
            rectParams.put("type", "rectangle");
            Map<String, Object> rectPos = new HashMap<>();
            rectPos.put("x", 200.0);
            rectPos.put("y", 200.0);
            rectParams.put("position", rectPos);
            rectParams.put("properties", new HashMap<>());
            
            DrawingState secondState = mock(DrawingState.class);
            DrawingState thirdState = mock(DrawingState.class);
            
            when(mockDrawingState.withAddedShape(any(Circle.class))).thenReturn(secondState);
            when(secondState.withAddedShape(any(Rectangle.class))).thenReturn(thirdState);
            
            // Act
            DrawingState result1 = createShapeTool.execute(circleParams, mockDrawingState);
            DrawingState result2 = createShapeTool.execute(rectParams, result1);
            
            // Assert
            assertAll(
                () -> assertEquals(secondState, result1),
                () -> assertEquals(thirdState, result2),
                () -> verify(mockDrawingState).withAddedShape(any(Circle.class)),
                () -> verify(secondState).withAddedShape(any(Rectangle.class))
            );
        }

        @Test
        @DisplayName("test_CreateShape_WithComplexStyle_AppliesAllStyleProperties")
        void test_CreateShape_WithComplexStyle_AppliesAllStyleProperties() {
            // Arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "rectangle");
            
            Map<String, Object> position = new HashMap<>();
            position.put("x", 50.0);
            position.put("y", 50.0);
            parameters.put("position", position);
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("width", 100.0);
            properties.put("height", 100.0);
            parameters.put("properties", properties);
            
            Map<String, Object> style = new HashMap<>();
            Map<String, Object> fillColor = new HashMap<>();
            fillColor.put("r", 255);
            fillColor.put("g", 128);
            fillColor.put("b", 0);
            fillColor.put("a", 200);
            style.put("fillColor", fillColor);
            
            Map<String, Object> strokeColor = new HashMap<>();
            strokeColor.put("hex", "#0000FF");
            style.put("strokeColor", strokeColor);
            
            style.put("strokeWeight", 3.5);
            style.put("hasFill", true);
            style.put("hasStroke", true);
            parameters.put("style", style);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            DrawingState result = createShapeTool.execute(parameters, mockDrawingState);
            
            // Assert
            assertAll(
                () -> assertNotNull(result),
                () -> verify(mockDrawingState).withAddedShape(argThat(shape -> {
                    Style shapeStyle = shape.getStyle();
                    return shapeStyle.getFillColor().getRed() == 255 &&
                           shapeStyle.getFillColor().getGreen() == 128 &&
                           shapeStyle.getFillColor().getBlue() == 0 &&
                           shapeStyle.getFillColor().getAlpha() == 200 &&
                           shapeStyle.getStrokeWeight() == 3.5f &&
                           shapeStyle.hasFill() &&
                           shapeStyle.hasStroke();
                }))
            );
        }

        @Test
        @DisplayName("test_CreateShape_WithLayerOrder_MaintainsCorrectLayering")
        void test_CreateShape_WithLayerOrder_MaintainsCorrectLayering() {
            // Arrange - create shapes with different layer orders
            Map<String, Object> backParams = new HashMap<>();
            backParams.put("type", "rectangle");
            backParams.put("id", "background");
            Map<String, Object> backPos = new HashMap<>();
            backPos.put("x", 0.0);
            backPos.put("y", 0.0);
            backParams.put("position", backPos);
            backParams.put("properties", new HashMap<>());
            backParams.put("layerOrder", 0);
            
            Map<String, Object> frontParams = new HashMap<>();
            frontParams.put("type", "circle");
            frontParams.put("id", "foreground");
            Map<String, Object> frontPos = new HashMap<>();
            frontPos.put("x", 50.0);
            frontPos.put("y", 50.0);
            frontParams.put("position", frontPos);
            frontParams.put("properties", new HashMap<>());
            frontParams.put("layerOrder", 100);
            
            when(mockDrawingState.withAddedShape(any(Shape.class))).thenReturn(mockUpdatedState);
            
            // Act
            createShapeTool.execute(backParams, mockDrawingState);
            createShapeTool.execute(frontParams, mockDrawingState);
            
            // Assert
            verify(mockDrawingState).withAddedShape(argThat(shape -> 
                shape.getId().equals("background") && shape.getLayerOrder() == 0
            ));
            verify(mockDrawingState).withAddedShape(argThat(shape -> 
                shape.getId().equals("foreground") && shape.getLayerOrder() == 100
            ));
        }
    }
}