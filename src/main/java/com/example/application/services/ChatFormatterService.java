package com.example.application.services;

import com.example.domain.services.ITextWrapStrategy;
import com.example.domain.values.BubbleSize;
import com.example.domain.values.MessageFormat;
import com.example.infrastructure.text.JapaneseTextWrapper;
import com.example.infrastructure.text.EnglishTextWrapper;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for formatting chat messages with text wrapping and bubble sizing. Applies text wrapping
 * strategies and calculates appropriate bubble dimensions.
 */
public class ChatFormatterService {
  private static final float DEFAULT_MAX_WIDTH = 300f;

  private final PGraphics graphics;
  private final List<ITextWrapStrategy> strategies;
  private final float maxBubbleWidth;

  /**
   * Creates a ChatFormatterService with default max width.
   *
   * @param graphics Processing graphics context
   */
  public ChatFormatterService(PGraphics graphics) {
    this(graphics, DEFAULT_MAX_WIDTH);
  }

  /**
   * Creates a ChatFormatterService with specified max width.
   *
   * @param graphics Processing graphics context
   * @param maxBubbleWidth Maximum bubble width in pixels
   */
  public ChatFormatterService(PGraphics graphics, float maxBubbleWidth) {
    this.graphics = graphics;
    this.maxBubbleWidth = maxBubbleWidth;

    // Initialize text wrapping strategies
    this.strategies = new ArrayList<>();
    this.strategies.add(new JapaneseTextWrapper(graphics));
    this.strategies.add(new EnglishTextWrapper(graphics));
  }

  /**
   * Formats a message with text wrapping and calculates bubble size.
   *
   * @param text Message text to format
   * @param font Font for text rendering
   * @return FormattedMessage containing wrapped text and bubble dimensions
   */
  public FormattedMessage formatMessage(String text, PFont font) {
    // Delegate to overload with configured default max width
    return formatMessage(text, font, this.maxBubbleWidth);
  }

  /**
   * Formats a message with text wrapping and calculates bubble size using a specific max content
   * width. This allows callers (UI) to adapt wrapping to the available layout width dynamically.
   *
   * @param text Message text to format
   * @param font Font for text rendering
   * @param contentMaxWidth Maximum width available for the text content (without padding)
   * @return FormattedMessage containing wrapped text and bubble dimensions
   */
  public FormattedMessage formatMessage(String text, PFont font, float contentMaxWidth) {
    if (text == null || text.isEmpty()) {
      return new FormattedMessage(new MessageFormat(new String[] {""}, 0, 0),
          new BubbleSize(50, 20));
    }

    // Select appropriate text wrapping strategy
    ITextWrapStrategy selectedStrategy = selectStrategy(text);

    // Apply text wrapping - use appropriate width based on text length
    graphics.textFont(font);
    float textNaturalWidth = graphics.textWidth(text);
    float effectiveMaxWidth = contentMaxWidth > 0 ? contentMaxWidth : maxBubbleWidth;

    // For short messages, use only the necessary width with minimal padding
    // Don't artificially expand short messages to fill available space
    float wrapWidth;
    // Use a fixed threshold for short messages (e.g., 200 pixels)
    if (textNaturalWidth < 200f) {
      // Short message - use actual text width, no artificial expansion
      wrapWidth = textNaturalWidth;
    } else {
      // Longer message - use the full available width for wrapping
      wrapWidth = effectiveMaxWidth;
    }

    String[] lines = selectedStrategy.wrapText(text, wrapWidth, font);

    // Calculate dimensions
    graphics.textFont(font);
    float maxLineWidth = 0;
    for (String line : lines) {
      float lineWidth = graphics.textWidth(line);
      if (lineWidth > maxLineWidth) {
        maxLineWidth = lineWidth;
      }
    }

    // Calculate height based on actual font metrics for consistent rendering
    float lineHeight = graphics.textAscent() + graphics.textDescent();
    float totalHeight = lineHeight * lines.length;

    // Create MessageFormat
    MessageFormat messageFormat = new MessageFormat(lines, lines.length, maxLineWidth);

    // Provide raw content dimensions; renderer will add visual padding
    BubbleSize bubbleSize = new BubbleSize(maxLineWidth, totalHeight, 0);

    return new FormattedMessage(messageFormat, bubbleSize);
  }

  /**
   * Selects the appropriate text wrapping strategy based on text content.
   *
   * @param text Text to analyze
   * @return Appropriate text wrapping strategy
   */
  private ITextWrapStrategy selectStrategy(String text) {
    // Check each strategy's canHandle method
    for (ITextWrapStrategy strategy : strategies) {
      if (strategy.canHandle(text)) {
        return strategy;
      }
    }

    // Default to first strategy (Japanese) if no match
    return strategies.get(0);
  }

  /**
   * Container for formatted message data. Holds both the formatted text lines and calculated bubble
   * dimensions.
   */
  public static class FormattedMessage {
    private final MessageFormat messageFormat;
    private final BubbleSize bubbleSize;

    public FormattedMessage(MessageFormat messageFormat, BubbleSize bubbleSize) {
      this.messageFormat = messageFormat;
      this.bubbleSize = bubbleSize;
    }

    public MessageFormat getMessageFormat() {
      return messageFormat;
    }

    public BubbleSize getBubbleSize() {
      return bubbleSize;
    }

    public String[] getLines() {
      return messageFormat.lines();
    }

    public int getLineCount() {
      return messageFormat.lineCount();
    }

    public float getTotalWidth() {
      return bubbleSize.getTotalWidth();
    }

    public float getTotalHeight() {
      return bubbleSize.getTotalHeight();
    }
  }
}
