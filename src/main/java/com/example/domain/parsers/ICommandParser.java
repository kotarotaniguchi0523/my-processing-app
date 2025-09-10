package com.example.domain.parsers;

import com.example.domain.commands.ParsedCommand;

/**
 * コマンドパーサーのインターフェース
 */
public interface ICommandParser {
    
    /**
     * 入力文字列をコマンドとしてパース
     * @param input ユーザー入力
     * @return パース済みコマンド、パースできない場合はnull
     */
    ParsedCommand parse(String input);
    
    /**
     * 入力がコマンドかどうかチェック
     */
    boolean isCommand(String input);
}