# 実装ブレイクダウン計画（MVP → 初回完成品）

この計画は、`prd.md` と `architecture.md` を根拠に、最小構成のMVPからユーザー承認ワークフローまでを完成させるための段階的なブレイクダウンです。各マイルストーンは「動く状態」を提供し、段階的に価値を積み上げます。

- 目的: 初回完成品（v1.0）＝ チャットUI + 図形ツール実行 + AI連携（ツール呼出）+ 差分表示 + 承認ワークフロー + Undo/Redo
- 前提: Java 17, Processing 4.47, Maven, SnakeYAML 2.2, OkHttp 4.12, Gson 2.10.1, java-diff-utils 4.12

---

## 全体マイルストーン

- M0: 基盤整備（リポジトリ整頓・依存導入・基本ラン）
- M1: MVP（描画エリア + チャットUI + YAML描画エンジン + 基本ツール）
- M2: AI連携（プロンプト/ツールスキーマ + ツール呼び出し実行）
- M3: 承認ワークフロー（差分計算/表示 + Approve/Reject + Undo/Redo）
- M4: 安定化と体験向上（保存/ロード、エラーハンドリング、微調整）

各マイルストーンの完了時点で、定義済みの受け入れ基準を満たすことをDoD（Definition of Done）とします。

---

## M0: 基盤整備（0.5〜1日）

目的: 依存関係と基本構造を確立し、以降の開発がスムーズに進む状態にする。

タスク
- 依存ライブラリの追加（pom.xml）
  - SnakeYAML 2.2 / OkHttp 4.12.0 / Gson 2.10.1 / java-diff-utils 4.12
- パッケージ構成のスケルトン作成（空クラスOK）
  - `application`（AgentService, SessionManager, ToolInvoker）
  - `presentation.ui`（MainWindow/DrawingCanvas/ChatPanel/ApprovalPanel）
  - `presentation.controllers`（UIController/EventHandler）
  - `domain.models`（State/Shape系）
  - `domain.services`（StateService/DiffService）
  - `domain.tools`（IAgentTool/ToolRegistry, impl/）
  - `infrastructure.ai`（AIClient/ClaudeClient/GeminiClient/PromptBuilder）
  - `infrastructure.storage`（FileStateRepository）
  - `infrastructure.parser`（YamlParser）
- 実行確認：`App`/`SimpleSketch`がビルド・起動できること

受け入れ基準（DoD）
- プロジェクトがクリーンビルド・実行可能
- 依存が競合なく解決
- スケルトンのクラス配置が `architecture.md` に準拠

---

## M1: MVP（1.5〜3日）

目的: AI連携なしで、ユーザ操作→ツール実行→YAML状態→描画更新が成立する最小機能。

タスク
1) Presentation/UI
- 描画エリア（DrawingCanvas）と簡易チャット（ChatPanel）の分割レイアウト
- 入力欄（Enter送信/Shift+Enter改行、文字数制限 500）
- チャットログ最大100件（スクロール）

2) YAML描画エンジン（Domain + Infra）
- `YamlParser`: `State` ⇄ YAML のシリアライズ/デシリアライズ
- `State` モデル（settings/shapes/arrays/lists の最小サブセット）
- `DrawingCanvas`: `State` から Processing 描画（ellipse, rectangle だけ先行）

3) 基本ツール（Domain.tools）
- `IAgentTool` インターフェース定義（name, schema, execute(params, state)）
- `createShape`（ellipse/rectangle）, `modifyShape`, `deleteShape` の最小実装
- `ToolRegistry` に登録し、アプリ起動時にロード

4) イベント配線（Application）
- `EventHandler` → `AgentService` → `ToolInvoker` → `StateService` → `UIController`
- チャット入力: `createShape` の簡易キーワード（例: "赤い円 100,200 直径60"）で擬似パース→実行

5) 最低限のユニットテスト
- YamlParser（往復整合）
- createShape（正常系/簡易異常系）

受け入れ基準（DoD）
- 手動入力コマンドで図形が追加・変更・削除でき、キャンバスが更新される
- YAMLを保存→読み込みで同じ描画が再現
- クラッシュなく10分程度操作可能

---

## M2: AI連携（2〜4日）

目的: ツールスキーマをプロンプトに含め、AIが「ツール呼出JSON」を返し、実行される状態。

タスク
1) AIクライアント
- `AIClient` 抽象 + `ClaudeClient` or `GeminiClient` のどちらか1つ先行実装
- APIキーの環境変数管理、タイムアウト/リトライ

2) PromptBuilder
- システムプロンプトにツール一覧/JSONスキーマ埋め込み
- 会話履歴（ユーザ/アシスタント/ツール実行結果）組み立て

3) ツール呼出統合
- AI応答: ツール呼出JSON or テキスト
- ツール呼出JSONのバリデーション→`ToolInvoker` 実行→新`State` 生成
- 実行結果を会話履歴に格納

4) フォールバック/安全装置
- スキーマ外の応答はテキストで案内に切替
- パラメータ検証エラー時のユーザ向けエラーメッセージ

5) スモークテスト
- 代表指示（円を追加、四角の色変更）で end-to-end 成功

受け入れ基準（DoD）
- 簡単な自然言語指示でAIがツールを選択し、描画が変化
- 異常応答でもクラッシュせずユーザに説明

---

## M3: 承認ワークフロー（2〜4日）

目的: 差分の計算・表示、承認/拒否、Undo/Redo を備えた安全な更新フロー。

タスク
1) DiffService + 表示
- `java-diff-utils` を用いて `State`(YAML) の差分をテキスト化
- Cline風表記（+/-/~）でハイライト（Markdown風テキストまたは簡易整形）

2) ApprovalPanel
- 差分サマリ + 詳細、Approve/Reject ボタン
- 承認待ち中はチャット入力を無効化

3) 非同期待機
- `AgentService` が `CompletableFuture<Boolean>` で承認を待機
- `EventHandler` がユーザ操作で complete(true/false)

4) StateService（履歴）
- 承認単位での Undo/Redo スタック（最大50）
- Redo 後の分岐破棄など基本ルール

5) E2E テスト（手動/最小自動）
- 3つ以上の図形一括追加やキャンバスクリア時に承認が必須
- Reject で変更なし、Approve で反映
- Undo/Redo が正しく遷移

受け入れ基準（DoD）
- 承認必須条件が機能し、差分が見やすく提示
- Undo/Redo が承認単位で動作

---

## M4: 安定化・体験向上（1〜3日）

目的: 利便性と堅牢性の底上げ。

タスク
- エラーメッセージの日本語整備（解決策の提示）
- セッション自動保存（5分）/手動保存・読み込み（.yaml）
- 軽微なパフォーマンス最適化（描画/差分計算）
- 最低限のドキュメント（RUNNING.md/コマンド・環境変数）

受け入れ基準（DoD）
- 1時間連続操作でクラッシュなし
- 基本ユースケースの応答時間が体感良好（指示→反映 5秒以内の目安）

---

## WBS（主要タスク一覧）

- 0.x: 基盤
  - 0.1 依存導入（SnakeYAML/OkHttp/Gson/diff-utils）
  - 0.2 パッケージとクラスのスケルトン生成
  - 0.3 ベース起動確認
- 1.x: MVP
  - 1.1 UIレイアウト（Canvas/Chat）
  - 1.2 YamlParser/State モデル（最小）
  - 1.3 DrawingCanvas（ellipse/rectangle）
  - 1.4 IAgentTool/ToolRegistry
  - 1.5 create/modify/deleteShape（最小）
  - 1.6 イベント配線（手入力→ツール実行）
  - 1.7 テスト（Yaml往復/ツール基本）
- 2.x: AI
  - 2.1 AIClient（1種）
  - 2.2 PromptBuilder（ツールスキーマ組込）
  - 2.3 ツール呼出JSON実行
  - 2.4 フォールバック/検証
  - 2.5 スモークテスト
- 3.x: 承認/履歴
  - 3.1 DiffService（表示整形）
  - 3.2 ApprovalPanel + 入力無効
  - 3.3 承認待機（Future）
  - 3.4 StateService（Undo/Redo）
  - 3.5 E2E テスト
- 4.x: 仕上げ
  - 4.1 エラーメッセージ整備
  - 4.2 セッション保存/読み込み（自動/手動）
  - 4.3 軽量最適化
  - 4.4 最低限ドキュメント

---

## リスクと対策（抜粋）

- AI応答のスキーマ逸脱
  - 対策: 厳格バリデーション + 丁寧なプロンプト + フォールバック
- 差分の可読性
  - 対策: サマリ + 詳細、色分け/記号、プレビューは将来拡張
- 処理遅延
  - 対策: 非同期化、タイムアウト/リトライ、描画/差分のプロファイル

---

## 受け入れテスト観点（サンプル）

- 指示: 「赤い円を中央に直径60で1つ」→ キャンバスに反映
- 指示: 「四角の色を青に変更」→ 対象ID指定で変更
- 指示: 「図形を5つ追加」→ 承認オーバーレイ表示→ Approve→ 反映 / Reject→ 反映なし
- Undo/Redo が承認単位で動作

---

## 次のアクション（今着手すること）

1) M0-0.1: pom.xml に依存を追加
2) M0-0.2: パッケージ/クラスのスケルトン生成（空でもOK）
3) M1-1.1: UIレイアウトを `App` に組み込み開始（既存レイアウトの分割強化）

上記が完了すれば、M1のYAML描画エンジン実装に進みます。
