# ADK LLM Workflow実行方法

## 必須設定

1. `.env`ファイルに以下のAPIキーを設定してください：
   ```
   GEMINI_API_KEY=your_api_key_here
   ```

## 実行方法

### Windows PowerShellを使用する場合（推奨）

```powershell
powershell -ExecutionPolicy Bypass -File run-adk.ps1
```

このスクリプトは自動的に：
- `.env`ファイルから`GEMINI_API_KEY`を読み込みます
- `GEMINI_API_KEY`を`GOOGLE_API_KEY`環境変数にマッピングします（ADKの要件）
- アプリケーションをコンパイルして実行します

### 手動で環境変数を設定する場合

```batch
# コマンドプロンプト（CMD）の場合：
set GOOGLE_API_KEY=your_api_key_here
mvn compile
mvn exec:java

# PowerShellの場合：
$env:GOOGLE_API_KEY="your_api_key_here"
mvn compile
mvn exec:java
```

## 重要な注意事項

- **APIキーをソースコードにハードコーディングしないでください**
- 必ず`.env`ファイルから読み込むようにしてください
- このプロジェクトは`GEMINI_API_KEY`を使用します
- ADK内部では`GOOGLE_API_KEY`環境変数が必要なため、PowerShellスクリプトが自動的にマッピングします

## トラブルシューティング

### APIキーが見つからないエラー
```
API key must either be provided or set in the environment variable GOOGLE_API_KEY
```
→ `.env`ファイルにAPIキーが設定されているか確認し、PowerShellスクリプトを使用して実行してください

### LLMモデルについて
- すべてのLLMエージェントは`gemini-2.5-pro`を使用します
- 他のモデルは使用しないでください