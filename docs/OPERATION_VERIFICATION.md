# 動作確認手順書（my-processing-app）

本書は Processing ベースのアプリ（SimpleSketch）について、環境準備から基本機能の動作確認までを手短にまとめた手順です。実行方法の詳細は `docs/RUNNING.md` も参照してください。

## 前提条件
- Java: JDK 17
- Maven: 3.6+（推奨 3.8+）
- OS: Windows 10/11（他 OS でも可）
- ネットワーク: 不要（AI 連携は任意）

## 起動まで（ビルド/実行）
- 確認: `mvn -v` で JDK/Maven を確認
- 依存解決/コンパイル: `mvn compile`
- 実行（既定のメイン）: `mvn exec:java`
  - 解析系で失敗する場合（Checkstyle/PMD/SpotBugs/テストを一時スキップ）:
    - `mvn -Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true -DskipTests exec:java`

起動すると 3 分割 UI（上: キャンバス／中: チャット／下: ステータス）が表示され、右下インジケータとともに Status が「Ready」になります。

## 基本 UI 確認
- 画面構成: 
  - 上: Drawing Canvas（白）
  - 中: Chat Interface（ログ＋入力欄＋Sendボタン）
  - 下: Status Area（状態表示＋インジケータ）
- 日本語入力: チャット入力欄をクリックすると日本語 IME 用のテキストボックスが表示され、Enter で送信、Shift+Enter で改行。

## 手描きストロークについて（仕様）
- 本アプリは PRD に従い、ユーザーの手描き機能は提供しません。
- 描画は AI/コマンドにより生成された `DrawingState`（YAML）を元に自動描画されます。

## チャット経由の描画コマンド確認（英語/日本語）
- 入力例（英語）:
  - `circle 100,200 diameter 60 red`
  - `rectangle 200,120 size 120x60 blue`
- 入力例（日本語）:
  - `赤い円 100,200 直径60`
  - `青い四角 300,150 幅100 高さ50`
- 期待結果:
  - 指定位置・サイズ・色の図形がキャンバスに描画される
  - チャットログに `User: ...` と `System: Executed AddShapeCommand` が追加される
  - ステータスが一時的に `Processing...` → `Ready` に戻る

## クリア動作の確認
- キーボード: `c` キーを押下 → キャンバスがクリアされる
- チャット: `clear` と送信 → キャンバスがクリアされ、`System: Canvas cleared` とログ表示

## セッション保存/読込/一覧の確認
1) 図形をいくつか描画
2) `save demo1` を送信 → `System: Session saved as 'demo1'` が表示
3) `list` を送信 → 保存済みセッション一覧がログ表示される
4) `clear` でいったん消去
5) `load demo1` を送信 → 先ほどの図形群が再度表示される（`System: Session loaded: 'demo1' (...)`）

保存先はプロジェクト直下のセッション管理領域（`FileStateRepository` 管理）です。不要になったファイルは OS のファイルエクスプローラーから削除可能です。

## エラーハンドリング確認（任意）
- 存在しないファイルを読込: `load notfound` → チャットに `ERROR: ...` が表示
- 不正な modify: `modify 999 size 100` → `ERROR: ...` のエラーメッセージ

注: 現状、図形 ID は UI に表示していないため `modify <id> ...` は ID 取得手段がないと検証が難しいです（将来の UI/ログ改善対象）。

## 終了確認
- ウィンドウを閉じるとアプリが終了します。
- 重要なリソースリークや例外がコンソールに出ていないことを確認。

## AI 連携（任意）
API キーを設定すれば、自然言語→AI→ツール呼び出し→状態更新（M2）まで動作します。
- `.env` に追記（例）
  - `GEMINI_API_KEY=xxxxxxxxxxxxxxxx`
  - または `GOOGLE_API_KEY=xxxxxxxxxxxxxxxx`
- 環境変数として設定（同名キー）

`APIKeyManager` は `.env`／環境変数／System Properties を順に参照します。AI が返すツール呼び出し(JSON)は検証後、`ToolInvoker` により適用されます。

## トラブルシューティング（要点）
- 実行エラー時は `docs/RUNNING.md` の「トラブルシューティング チェックリスト」を参照
- 解析系でブロックされる場合は一時的にスキップフラグを付与
  - `-Dcheckstyle.skip=true -Dpmd.skip=true -Dspotbugs.skip=true -DskipTests`
- PowerShell のクォートや全角スペース混在に注意
- 文字化けや日本語未表示時はフォント設定（`MS Gothic`）が適用されているか確認

以上で基本的な動作確認は完了です。追加の検証観点があれば追記できます。
