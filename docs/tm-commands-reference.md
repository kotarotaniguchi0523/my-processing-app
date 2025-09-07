# Taskmaster/Claude TM コマンド詳細リファレンス

このドキュメントは、`.claude/commands/tm/` ディレクトリにある全コマンドガイドを一つにまとめた実務向けリファレンスです。対応する MCP ツール・CLI、主用途、主要オプション、使用例、注意点を簡潔に整理し、最後に「Workflows」を詳説します。

- 想定環境: Windows PowerShell (pwsh)
- 併用体系: MCP ツール(推奨) と `task-master` CLI(フォールバック)
- タグ管理: 全コマンドはタグ付きタスクリストをサポート。`--tag <name>` を併用可能

## 目次
- 初期化・セットアップ: init, setup, models
- タスク表示・ナビゲーション: help, tm-main, list, next, show, status, complexity-report
- タスク作成・更新: add-task, update, set-status, remove-task
- サブタスク管理: add-subtask, expand, clear-subtasks, remove-subtask
- 依存関係: add-dependency, remove-dependency, validate-dependencies, fix-dependencies
- 生成・同期: generate, sync-readme
- 解析・ユーティリティ: analyze-complexity, utils/analyze-project
- PRD: parse-prd
- ワークフロー: workflows/auto-implement-tasks, command-pipeline, smart-workflow (詳細)

---

## 初期化・セットアップ

### setup (setup/install-taskmaster.md, quick-install-taskmaster.md)
- 目的: CLI インストール・環境準備
- CLI: インストール
  ```pwsh
  npm install -g task-master-ai
  ```
- 注意: API キーは `.env`(CLI) または `.vscode/mcp.json`(MCP) に設定

### init (init/init-project.md, init-project-quick.md)
- MCP: initialize_project
- CLI: `task-master init [--name <name>] [--description <text>] [-y] [--rules vscode,windsurf]`
- 用途: `.taskmaster/` 構成の初期化
- 注意: 初期化後は PRD を `parse-prd` で取り込み

### models (models/setup-models.md, view-models.md)
- MCP/CLI: `models`, `task-master models --setup`
- 用途: main/research/fallback モデル設定
- 例:
  ```pwsh
  task-master models --setup
  task-master models --set-main claude-3-5-sonnet
  ```

---

## タスク表示・ナビゲーション

### help (help.md)
- CLI: `task-master help`
- 用途: コマンド一覧の確認

### tm-main (tm-main.md)
- 概要: プロジェクトダッシュボード表示のガイド
- 実体は `status`/`list` 系の活用

### list (list/*.md)
- MCP: get_tasks
- CLI: `task-master list [--status pending,done] [--with-subtasks] [--tag <name>]`
- 用途: タスク一覧

### next (next/next-task.md)
- MCP: next_task
- CLI: `task-master next [--tag <name>]`
- 用途: 次に着手可能なタスク提案

### show (show/show-task.md)
- MCP: get_task
- CLI: `task-master show <id>` (複数ID可)
- 用途: 特定タスク詳細

### status (status/project-status.md)
- CLI: `task-master status [--tag <name>]`
- 用途: 進捗サマリー

### complexity-report (complexity-report/complexity-report.md)
- MCP: complexity_report
- CLI: `task-master complexity-report [--tag <name>]`
- 用途: 複雑度分析レポート表示

---

## タスク作成・更新

### add-task (add-task/add-task.md)
- MCP: add_task
- CLI: `task-master add-task --prompt "説明" [--priority high] [--dependencies 1,2] [--research]`
- 用途: 新規タスク生成(LLM)

### update (update/*.md)
- MCP: update, update_task
- CLI: 
  - まとめ更新: `task-master update --from <id> --prompt "..." [--research]`
  - 単一更新: `task-master update-task --id <id> --prompt "..." [--append] [--research]`

### set-status (set-status/to-*.md)
- MCP: set_task_status
- CLI: `task-master set-status --id <id[,id2]> --status done|in-progress|pending|deferred|review|cancelled`

### remove-task (remove-task/remove-task.md)
- MCP: remove_task
- CLI: `task-master remove-task --id <id> [-y]`

---

## サブタスク管理

### add-subtask (add-subtask/*.md)
- MCP: add_subtask
- CLI: `task-master add-subtask --parent <id> --title "..." [--description "..."] [--status pending]`
- 既存タスクをサブタスク化: `--task-id <id>`

### expand (expand/*.md)
- MCP: expand_task / expand_all
- CLI: 
  - 単一: `task-master expand --id <id> [--num 5] [--research] [--force]`
  - 全体: `task-master expand --all [--num 5] [--research] [--force]`

### clear-subtasks (clear-subtasks/*.md)
- MCP: clear_subtasks
- CLI: `task-master clear-subtasks --id <id[,id2]>` または `--all`

### remove-subtask (remove-subtask/remove-subtask.md)
- MCP: remove_subtask
- CLI: `task-master remove-subtask --id <parent.subId> [--convert]`

---

## 依存関係

### add-dependency (add-dependency/add-dependency.md)
- MCP: add_dependency
- CLI: `task-master add-dependency --id <id> --depends-on <id>`

### remove-dependency (remove-dependency/remove-dependency.md)
- MCP: remove_dependency
- CLI: `task-master remove-dependency --id <id> --depends-on <id>`

### validate-dependencies (validate-dependencies/validate-dependencies.md)
- MCP: validate_dependencies
- CLI: `task-master validate-dependencies`

### fix-dependencies (fix-dependencies/fix-dependencies.md)
- MCP: fix_dependencies
- CLI: `task-master fix-dependencies`

---

## 生成・同期

### generate (generate/generate-tasks.md)
- MCP: generate
- CLI: `task-master generate [--output tasks] [--tag <name>]`
- 用途: `tasks.json` を Markdown ファイル群に展開

### sync-readme (sync-readme/sync-readme.md)
- CLI: `task-master sync-readme [--status done] [--with-subtasks] [--tag <name>]`
- 用途: タスクリストを README.md へ同期 (実験的)

---

## 解析・ユーティリティ

### analyze-complexity (analyze-complexity/analyze-complexity.md)
- MCP: analyze_project_complexity
- CLI: `task-master analyze-complexity [--research] [--threshold 7] [--tag <name>]`

### utils/analyze-project (utils/analyze-project.md)
- 目的: コードベース分析/調査のガイド (MCP research 併用)

---

## PRD

### parse-prd (parse-prd/*.md)
- MCP: parse_prd
- CLI: `task-master parse-prd <file> [-n 10] [--force] [--tag <name>] [--research]`
- 用途: PRD/仕様からタスク自動生成

---

## Workflows (詳細) — `.claude/commands/tm/workflows/*`

### 1) auto-implement-tasks.md: 自動タスク実装ループ
- ゴール: 次タスク選定→分解→実装計画の記録→実装→進捗ログ→完了の反復を自動化
- 最小ループ:
  1. 次タスクの抽出
     ```pwsh
     task-master next
     ```
  2. 必要に応じて分解
     ```pwsh
     task-master expand --id <id> --research
     ```
  3. 実装計画の記録 (サブタスク単位)
     ```pwsh
     task-master update-subtask --id <id.sub> --prompt "計画/変更箇所/検証方法 を詳細に"
     ```
  4. 実装を開始・進捗の追記
     ```pwsh
     task-master set-status --id <id.sub> --status in-progress
     task-master update-subtask --id <id.sub> --prompt "やったこと/うまくいったこと/ダメだったこと"
     ```
  5. 完了の確定
     ```pwsh
     task-master set-status --id <id.sub> --status done
     ```
- ポイント: `update-subtask` は時系列ログ用途。設計/検証結果を細かく蓄積し、後続の意思決定・再現性を高める

### 2) command-pipeline.md: 初期化〜文書化の一括パイプライン
- ゴール: PRD 駆動の初期展開〜文書生成を一括実行
- 例(タグ分離・研究あり):
  ```pwsh
  task-master init -y
  task-master parse-prd requirement/prd.md --tag feature-ui --num 12 --research
  task-master analyze-complexity --tag feature-ui --research
  task-master expand --all --tag feature-ui --research
  task-master generate --tag feature-ui
  task-master complexity-report --tag feature-ui
  ```
- 追加検証:
  ```pwsh
  task-master validate-dependencies --tag feature-ui
  task-master fix-dependencies --tag feature-ui
  ```

### 3) smart-workflow.md: タグ/PRD 駆動の高度化フロー
- ねらい: ブランチ/実験/大型機能に応じてタグを適切に分離し、マスターを高レベルに保つ
- 典型パターン:
  - Feature ブランチ検知→タグ作成
    ```pwsh
    task-master add-tag feature-user-auth --from-branch
    task-master use-tag feature-user-auth
    ```
  - PRD を専用タグへ投入し、分析/展開
    ```pwsh
    task-master parse-prd .taskmaster/docs/feature-auth-prd.txt --tag feature-user-auth
    task-master analyze-complexity --tag feature-user-auth --research
    task-master expand --all --tag feature-user-auth --research
    ```
  - マスターには高レベルの参照タスクのみを維持 (詳細作業はタグ側)

---

## よくあるオプションと注意点
- `--tag <name>`: タスクコンテキストを切替。チーム作業/実験で衝突回避
- `--research`: 研究モデルを使用し精度向上。完了まで最大1分程度
- `--force`: 破壊的変更に注意 (`expand --force`, `parse-prd --force`)
- 依存関係の健全性: `validate-dependencies` → 問題があれば `fix-dependencies`
- ファイル出力: `generate` 実行で各タスクの Markdown を生成

---

## クイックスタート (例)
```pwsh
# 1) 初期化
task-master init -y

# 2) PRD からタスク生成 (タグ分離)
task-master parse-prd requirement/prd.md --tag feature-core --num 10 --research

# 3) 分析→展開
task-master analyze-complexity --tag feature-core --research
task-master expand --all --tag feature-core --research

# 4) 進捗サイクル
task-master next --tag feature-core
# (必要に応じて show / update-subtask / set-status)

# 5) 文書化
task-master generate --tag feature-core
```

---

最終更新: 自動生成リファレンス(本ファイル) — `.claude/commands/tm/` の構成に準拠。必要に応じて追補してください。
