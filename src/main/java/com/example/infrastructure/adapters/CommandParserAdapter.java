package com.example.infrastructure.adapters;

import com.example.domain.parsers.ICommandParser;

/**
 * CommandParserのアダプター実装
 */
public class CommandParserAdapter implements ICommandParser {

  private final com.example.application.CommandParser commandParser;

  /**
   * デフォルトコンストラクタ
   */
  public CommandParserAdapter() {
    this.commandParser = new com.example.application.CommandParser();
  }

  /**
   * 依存性注入用コンストラクタ
   */
  public CommandParserAdapter(com.example.application.CommandParser commandParser) {
    this.commandParser = commandParser;
  }

  @Override
  public com.example.domain.commands.ParsedCommand parse(String input) {
    return commandParser.parse(input);
  }

  @Override
  public boolean isCommand(String input) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }

    // Check if input starts with a command prefix
    String trimmed = input.trim();
    return trimmed.startsWith("/") || trimmed.startsWith("add ") || trimmed.startsWith("remove ")
        || trimmed.startsWith("modify ") || trimmed.startsWith("clear")
        || trimmed.startsWith("save") || trimmed.startsWith("load") || trimmed.startsWith("list");
  }
}
