# TODO (M3: 承認ワークフロー)

最終更新: 2025-09-09

目的: PRDのM3「承認ワークフロー」を実装。AI→ツール→状態更新の前に、差分提示＋承認/拒否を挟む。Undo/Redoは承認適用単位で積む。

## 受け入れ基準（DoD）
- Diff: 旧State vs 新State のUnified Diffを表示できる
- 承認UI: モーダルでサマリー＋詳細、承認/拒否ボタン、スクロール
- 入力制御: 承認待機中はチャット入力や他操作を無効化
- 適用単位: 承認でのみ `stateService.updateState()` 実行（履歴に積まれる）
- 非同期: ApplicationControllerで承認待機（CompletableFuture）・30秒タイムアウト（棄却）
 - JSON検証: ツール呼出JSONがツールスキーマに対して厳格検証され、無効な呼出は実行前に棄却し会話ログへ理由が記録される

## タスク（Task Master 対応）

12. 承認UI（ApprovalPanel）
- [x] 12.1 承認パネル（サマリー＋詳細Diff）
  - 実装: `SimpleSketch.drawApprovalOverlay()`、`DiffService`
- [x] 12.2 承認/拒否ボタンとイベント
  - 実装: `SimpleSketch.mousePressed()` → `UI_APPROVAL_ACCEPTED/REJECTED`
- [x] 12.3 承認待機中のチャット入力無効化
  - 実装: `approvalPending` でキー/クリック/IMEをブロック
- [x] 12.4 モーダル様動作
  - 実装: オーバーレイ上で他UI操作を遮断、キー操作（Y/N、↑↓）
- [x] 12.5 スタイリング（簡易）
 - 実装: 半透明背景、色分け、中央配置

13. 非同期承認処理
- [x] 13.1 CompletableFutureベースの承認待機
  - 実装: `AgentService.awaitApproval()/completeApproval()`
- [x] 13.2 UI承認イベントのFuture完了接続
  - 実装: `ApplicationController.handleApprovalAccepted/Rejected()` → `agentService.completeApproval(..)`
- [x] 13.3 タイムアウトとキャンセル処理（30秒）
  - 実装: `orTimeout(30, TimeUnit.SECONDS)`（タイムアウト=棄却）
- [x] 13.4 スレッドセーフティ
  - 実装: `AtomicReference<CompletableFuture<Boolean>>`（単一承認待ち）
- [x] 13.5 クリーンアップ
  - 実装: 完了時にオーバーレイ/状態をクリア、Future参照を解放

14. ツール呼出JSONのスキーマ検証
- [x] 14.1 実行前検証をisValidToolCallに組み込み
  - 実装: `AgentService.isValidToolCall()` で `ToolInvoker.validateToolParameters()` を使用
- [x] 14.2 無効時の会話ログ出力
  - 実装: `AgentService.validateToolCalls()` で `promptIntegrator.addToolResult()` に検証失敗理由を記録

## 実装ファイル
- コントローラ: `src/main/java/com/example/application/ApplicationController.java`
- UI: `src/main/java/com/example/SimpleSketch.java`
- Diff: `src/main/java/com/example/agent/diff/DiffService.java`
- イベント: `src/main/java/com/example/events/EventType.java`
- エージェント: `src/main/java/com/example/agent/service/AgentService.java`
 - ツール登録: `src/main/java/com/example/domain/tools/ToolInitializer.java`（clearCanvas 追加）
 - ツール本体: `src/main/java/com/example/domain/tools/impl/ClearCanvasTool.java`

## 残課題 / 改善（任意）
- Diffサマリーを図形ID/種類毎に要約（現在は件数と+/-概算）
- Approvalのテキスト折返し・等幅フォント設定
- Function Calling（SDK）への切替（現状はJSON抽出）

## 確認手順（抜粋）
- 1) 複数変更を誘発する指示（例:「赤い円を3つ追加して」）→ 承認オーバーレイ表示
- 2) Diff確認 → 承認（Y/ボタン）で適用、拒否（N/ボタン）で破棄
- 3) 承認待機中、チャット入力やキー操作がブロックされる
- 4) タイムアウト（30秒）で自動棄却

## M4（抜粋）
- [x] 自動保存（5分）を有効化（アプリ起動時に有効）
  - 実装: `ApplicationController.start()` で `sessionManager.setAutoSaveInterval(300)` + `setAutoSaveEnabled(true)`
