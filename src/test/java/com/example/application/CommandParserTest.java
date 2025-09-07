package com.example.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** Unit tests for CommandParser including Japanese command support. */
public class CommandParserTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
    }

    // Test English circle commands
    @Test
    void testEnglishCircleCommands() {
        // Basic circle with diameter
        ParsedCommand result = parser.parse("circle 100,200 diameter 60");
        assertTrue(result.isValid());
        assertEquals("circle", result.getType());
        assertEquals("100", result.getParameters().get("x"));
        assertEquals("200", result.getParameters().get("y"));
        assertEquals("60", result.getParameters().get("diameter"));

        // Circle with color
        ParsedCommand colorResult = parser.parse("red circle at 50,50 size 40");
        assertTrue(colorResult.isValid());
        assertEquals("circle", colorResult.getType());
        assertEquals("50", colorResult.getParameters().get("x"));
        assertEquals("50", colorResult.getParameters().get("y"));
        assertEquals("40", colorResult.getParameters().get("diameter"));
        assertEquals("#FF0000", colorResult.getParameters().get("color"));
    }

    // Test Japanese circle commands (M1 requirement)
    @Test
    void testJapaneseCircleCommands() {
        // M1 example: "赤い円 100,200 直径60"
        ParsedCommand result = parser.parse("赤い円 100,200 直径60");
        assertTrue(result.isValid());
        assertEquals("circle", result.getType());
        assertEquals("100", result.getParameters().get("x"));
        assertEquals("200", result.getParameters().get("y"));
        assertEquals("60", result.getParameters().get("diameter"));
        assertEquals("#FF0000", result.getParameters().get("color"));

        // Circle without color
        ParsedCommand simpleResult = parser.parse("円 50,50 直径40");
        assertTrue(simpleResult.isValid());
        assertEquals("circle", simpleResult.getType());
        assertEquals("50", simpleResult.getParameters().get("x"));
        assertEquals("50", simpleResult.getParameters().get("y"));
        assertEquals("40", simpleResult.getParameters().get("diameter"));
        assertNull(simpleResult.getParameters().get("color"));

        // Circle with radius (should convert to diameter)
        ParsedCommand radiusResult = parser.parse("青い円 75,100 半径25");
        assertTrue(radiusResult.isValid());
        assertEquals("circle", radiusResult.getType());
        assertEquals("75", radiusResult.getParameters().get("x"));
        assertEquals("100", radiusResult.getParameters().get("y"));
        assertEquals(
                "50", radiusResult.getParameters().get("diameter")); // radius 25 * 2 = diameter 50
        assertEquals("#0000FF", radiusResult.getParameters().get("color"));
    }

    // Test Japanese colors
    @Test
    void testJapaneseColors() {
        Map<String, String> testCases =
                Map.of(
                        "赤い円 0,0 直径10", "#FF0000",
                        "青い円 0,0 直径10", "#0000FF",
                        "緑の円 0,0 直径10", "#00FF00",
                        "黄色い円 0,0 直径10", "#FFFF00",
                        "黒い円 0,0 直径10", "#000000",
                        "白い円 0,0 直径10", "#FFFFFF");

        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            ParsedCommand result = parser.parse(testCase.getKey());
            assertTrue(result.isValid(), "Failed to parse: " + testCase.getKey());
            assertEquals(testCase.getValue(), result.getParameters().get("color"));
        }
    }

    // Test Japanese rectangles
    @Test
    void testJapaneseRectangleCommands() {
        // Rectangle with width and height
        ParsedCommand result = parser.parse("赤い四角 100,200 幅80 高さ60");
        assertTrue(result.isValid());
        assertEquals("rectangle", result.getType());
        assertEquals("100", result.getParameters().get("x"));
        assertEquals("200", result.getParameters().get("y"));
        assertEquals("80", result.getParameters().get("width"));
        assertEquals("60", result.getParameters().get("height"));
        assertEquals("#FF0000", result.getParameters().get("color"));

        // Square (width only, height should equal width)
        ParsedCommand squareResult = parser.parse("四角 50,50 幅40");
        assertTrue(squareResult.isValid());
        assertEquals("rectangle", squareResult.getType());
        assertEquals("50", squareResult.getParameters().get("x"));
        assertEquals("50", squareResult.getParameters().get("y"));
        assertEquals("40", squareResult.getParameters().get("width"));
        assertEquals("40", squareResult.getParameters().get("height"));

        // 長方形 (rectangle alternative)
        ParsedCommand rectResult = parser.parse("青い長方形 10,20 幅100 高さ50");
        assertTrue(rectResult.isValid());
        assertEquals("rectangle", rectResult.getType());
        assertEquals("10", rectResult.getParameters().get("x"));
        assertEquals("20", rectResult.getParameters().get("y"));
        assertEquals("100", rectResult.getParameters().get("width"));
        assertEquals("50", rectResult.getParameters().get("height"));
        assertEquals("#0000FF", rectResult.getParameters().get("color"));
    }

    // Test mixed English/Japanese doesn't break
    @Test
    void testMixedLanguageHandling() {
        // English should still work
        ParsedCommand englishResult = parser.parse("red circle 100,100 diameter 50");
        assertTrue(englishResult.isValid());

        // Japanese should work
        ParsedCommand japaneseResult = parser.parse("赤い円 100,100 直径50");
        assertTrue(japaneseResult.isValid());

        // Both should produce equivalent results
        assertEquals(englishResult.getType(), japaneseResult.getType());
        assertEquals(
                englishResult.getParameters().get("color"),
                japaneseResult.getParameters().get("color"));
        assertEquals(
                englishResult.getParameters().get("x"), japaneseResult.getParameters().get("x"));
        assertEquals(
                englishResult.getParameters().get("y"), japaneseResult.getParameters().get("y"));
        assertEquals(
                englishResult.getParameters().get("diameter"),
                japaneseResult.getParameters().get("diameter"));
    }

    // Test looksLikeDrawingCommand with Japanese
    @Test
    void testLooksLikeDrawingCommandWithJapanese() {
        // Japanese keywords should be recognized
        assertTrue(parser.looksLikeDrawingCommand("赤い円 100,100 直径50"));
        assertTrue(parser.looksLikeDrawingCommand("四角 50,50 幅40"));
        assertTrue(parser.looksLikeDrawingCommand("青い長方形 10,20 幅100 高さ50"));

        // Individual Japanese keywords
        assertTrue(parser.looksLikeDrawingCommand("円"));
        assertTrue(parser.looksLikeDrawingCommand("四角"));
        assertTrue(parser.looksLikeDrawingCommand("直径"));
        assertTrue(parser.looksLikeDrawingCommand("半径"));
        assertTrue(parser.looksLikeDrawingCommand("幅"));
        assertTrue(parser.looksLikeDrawingCommand("高さ"));

        // English should still work
        assertTrue(parser.looksLikeDrawingCommand("circle 100,100 diameter 50"));
        assertTrue(parser.looksLikeDrawingCommand("rectangle 50,50 size 40"));

        // Non-drawing commands should return false
        assertFalse(parser.looksLikeDrawingCommand("hello world"));
        assertFalse(parser.looksLikeDrawingCommand("こんにちは"));
    }

    // Test modify commands
    @Test
    void testModifyCommands() {
        // Test color modification
        ParsedCommand colorResult = parser.parse("modify shape1 color blue");
        assertTrue(colorResult.isValid());
        assertEquals("modify", colorResult.getType());
        assertEquals("shape1", colorResult.getParameters().get("id"));
        assertEquals("color", colorResult.getParameters().get("property"));
        assertEquals("#0000FF", colorResult.getParameters().get("color"));

        // Test position modification
        ParsedCommand positionResult = parser.parse("modify circle_123 position 200,300");
        assertTrue(positionResult.isValid());
        assertEquals("modify", positionResult.getType());
        assertEquals("circle_123", positionResult.getParameters().get("id"));
        assertEquals("position", positionResult.getParameters().get("property"));
        assertEquals("200", positionResult.getParameters().get("x"));
        assertEquals("300", positionResult.getParameters().get("y"));

        // Test size modification - single value
        ParsedCommand sizeResult = parser.parse("modify rect1 size 100");
        assertTrue(sizeResult.isValid());
        assertEquals("modify", sizeResult.getType());
        assertEquals("rect1", sizeResult.getParameters().get("id"));
        assertEquals("size", sizeResult.getParameters().get("property"));
        assertEquals("100", sizeResult.getParameters().get("size"));

        // Test size modification - width and height
        ParsedCommand sizeResult2 = parser.parse("modify rect2 size 150x200");
        assertTrue(sizeResult2.isValid());
        assertEquals("modify", sizeResult2.getType());
        assertEquals("rect2", sizeResult2.getParameters().get("id"));
        assertEquals("size", sizeResult2.getParameters().get("property"));
        assertEquals("150", sizeResult2.getParameters().get("width"));
        assertEquals("200", sizeResult2.getParameters().get("height"));
    }

    // Test modify command error cases
    @Test
    void testModifyCommandErrors() {
        // Missing property
        ParsedCommand result1 = parser.parse("modify shape1");
        assertFalse(result1.isValid());

        // Missing value
        ParsedCommand result2 = parser.parse("modify shape1 color");
        assertFalse(result2.isValid());

        // Invalid command structure
        ParsedCommand result3 = parser.parse("modify");
        assertFalse(result3.isValid());

        // Unknown property
        ParsedCommand result4 = parser.parse("modify shape1 unknown value");
        assertTrue(result4.isValid()); // Parser accepts it, validation happens later
        assertEquals("modify", result4.getType());
        assertEquals("shape1", result4.getParameters().get("id"));
        assertEquals("unknown", result4.getParameters().get("property"));
    }

    // Test looksLikeDrawingCommand includes modify
    @Test
    void testLooksLikeDrawingCommandWithModify() {
        assertTrue(parser.looksLikeDrawingCommand("modify shape1 color blue"));
        assertTrue(parser.looksLikeDrawingCommand("modify circle_123 position 200,300"));
        assertTrue(parser.looksLikeDrawingCommand("modify rect1 size 100"));
    }

    // Test invalid commands
    @Test
    void testInvalidCommands() {
        // Incomplete Japanese commands
        ParsedCommand result1 = parser.parse("赤い");
        assertFalse(result1.isValid());

        ParsedCommand result2 = parser.parse("円");
        assertFalse(result2.isValid());

        // Empty command
        ParsedCommand result3 = parser.parse("");
        assertFalse(result3.isValid());

        // Null command
        ParsedCommand result4 = parser.parse(null);
        assertFalse(result4.isValid());
    }
}
