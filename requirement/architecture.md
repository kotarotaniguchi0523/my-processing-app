# AIコーディングエージェント アーキテクチャ設計図 (v2)

## 1. システム全体アーキテクチャ

### 1.1 改訂版レイヤードアーキテクチャ

エージェントの責務を明確化し、UIとの連携をイベント駆動にするためレイヤー構造を更新。

```
┌─────────────────────────────────────────────────────┐
│                 Presentation Layer (UI)              │
│  (View Components / UI Controller / Event Handler)   │
└───────────────────────┬─────────────────────────────┘
                        │ (UI Events / State Updates)
                        ↓
┌───────────────────────┴─────────────────────────────┐
│                 Application Layer                    │
│      (AgentService / SessionManager / Use Cases)     │
└───────────────────────┬─────────────────────────────┘
                        │ (Commands / Queries)
                        ↓
┌───────────────────────┴─────────────────────────────┐
│                   Domain Layer                       │
│ (Core Models / Tools / Services / Repositories)      │
└───────────────────────┬─────────────────────────────┘
                        │ (External API Calls / I/O)
                        ↓
┌───────────────────────┴─────────────────────────────┐
│                Infrastructure Layer                  │
│     (AI Client / File Storage / HTTP Client)         │
└─────────────────────────────────────────────────────┘
```

### 1.2 コンポーネント構成図

ユーザー承認ワークフローとツール実行の仕組みをコンポーネントとして明示。

```
┌────────────────────────────────────────────────────────────────┐
│                        Main Application                          │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │                  Presentation (Swing/JavaFX)                 │ │
│ │ ┌──────────────┐ ┌─────────────┐ ┌───────────────────────┐ │ │
│ │ │ DrawingCanvas│ │  ChatPanel  │ │    ApprovalPanel      │ │ │
│ │ └───────┬──────┘ └──────┬──────┘ └───────────┬───────────┘ │ │
│ │         └──────────────┤ EventHandler ├────────────┘         │ │
│ │                        └──────┬──────┘                        │ │
│ └──────────────────────────────┼──────────────────────────────┘ │
│                                │ (User Input / Approval Events)   │
│                                ↓                                │
│ ┌──────────────────────────────┴──────────────────────────────┐ │
│ │                      Application (Core Logic)                │ │
│ │                        ┌──────────────┐                      │ │
│ │                        │ AgentService │ (Orchestrator)        │ │
│ │                        └──────┬──────┘                      │ │
│ │                               │                              │ │
│ │    ┌──────────────────────────┼──────────────────────────┐    │ │
│ │    ↓                          ↓                          ↓    │ │
│ │ ┌──────────────┐   ┌──────────────┐   ┌───────────────────┐  │ │
│ │ │  DiffService │   │ ToolInvoker  │   │   StateService    │  │ │
│ │ └──────────────┘   └──────┬──────┘   └───────────────────┘  │ │
│ └───────────────────────────│────────────────────────────────┘ │
│                             │ (Tool Definitions)               │
│                             ↓                                │
│ ┌───────────────────────────┴───────────────────────────────┐ │
│ │                       Domain (Business Rules)              │ │
│ │ ┌────────────────┐ ┌────────────────┐ ┌──────────────────┐ │ │
│ │ │  ToolRegistry  │ │  Shape Models  │ │ StateRepository  │ │ │
│ │ └────────────────┘ └────────────────┘ └──────────────────┘ │ │
│ └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────┬─────────────────────────────┘
                                   │ (API Calls / File I/O)
                                   ↓
┌──────────────────────────────────┴─────────────────────────────┐
│                Infrastructure (External Interfaces)              │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐   │
│   │  AI Client   │   │ FileStorage  │   │   HttpClient     │   │
│   │(Claude/Gemini)│   │  (.yaml)   │   │    (OkHttp)      │   │
│   └──────────────┘   └──────────────┘   └──────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

## 2. パッケージ構造

### 2.1 ディレクトリ構成

```
com.example.aiagent/
├── Main.java                      // アプリケーションエントリポイント
│
├── application/                   // アプリケーション層: ユースケースとフロー制御
│   ├── AgentService.java          // AIエージェントの思考ループとオーケストレーション
│   ├── SessionManager.java        // セッションの保存・読み込み管理
│   └── ToolInvoker.java           // ツールの安全な実行と結果のハンドリング
│
├── presentation/                  // プレゼンテーション層: UI関連
│   ├── ui/
│   │   ├── MainWindow.java        // メインウィンドウフレーム
│   │   ├── DrawingCanvas.java     // Processing描画キャンバス
│   │   ├── ChatPanel.java         // チャットUIパネル
│   │   └── ApprovalPanel.java     // 承認オーバーレイUI
│   └── controllers/
│       ├── UIController.java      // UIコンポーネント間の連携と状態管理
│       └── EventHandler.java      // ユーザーからの入力イベント処理
│
├── domain/                        // ドメイン層: ビジネスルールとエンティティ
│   ├── models/                    // データモデル
│   │   ├── State.java             // 描画状態全体のスナップショット
│   │   ├── Shape.java             // 図形の基底クラス
│   │   └── ... (Circle, etc.)
│   ├── services/                  // ドメインサービス
│   │   ├── StateService.java      // 状態の履歴(Undo/Redo)と永続化の管理
│   │   └── DiffService.java       // 状態間の差分計算
│   ├── tools/                     // AIが利用するツール群
│   │   ├── IAgentTool.java        // 全ツールの共通インターフェース
│   │   ├── ToolRegistry.java      // ツールの登録とスキーマ管理
│   │   └── impl/
│   │       ├── CreateShapeTool.java // 具体的なツールの実装
│   │       └── ...
│   └── repositories/
│       └── IStateRepository.java    // 状態の永続化インターフェース
│
├── infrastructure/                // インフラストラクチャ層: 外部システム連携
│   ├── ai/
│   │   ├── AIClient.java          // AI APIクライアントのインターフェース
│   │   ├── ClaudeClient.java      // Claude APIの具体的な実装
│   │   ├── GeminiClient.java      // Gemini APIの具体的な実装
│   │   └── PromptBuilder.java     // 動的なプロンプト生成
│   ├── storage/
│   │   └── FileStateRepository.java // ファイルベースの状態永続化実装
│   └── parser/
│       └── YamlParser.java        // YAMLのシリアライズ/デシリアライズ
│
└── common/                        // 共通コンポーネント
    ├── utils/
    │   └── ...
    └── exceptions/
        └── ...
```

## 3. クラス設計（主要クラスの責務）

### AgentService

- ユーザー入力をトリガーとして思考ループを開始する。
- AIにプロンプトを送信し、応答（テキスト or ツール呼び出し）を受け取る。
- ツール呼び出しが必要な場合、`ToolInvoker`に実行を依頼する。
- 承認が必要な操作の場合、`UIController`に承認UIの表示を要求し、ユーザーの応答を待つ。

### ToolInvoker

- `ToolRegistry`から指定されたツールを取得する。
- ツールのパラメータを検証し、安全に`execute()`メソッドを実行する。
- ツールの実行結果（生成されたYAMLやエラー）を`AgentService`に返す。

### ToolRegistry

- アプリケーション起動時に、実装されている全ての`IAgentTool`を登録する。
- AIに渡すためのツール一覧とJSONスキーマを生成する。

### DiffService

- 2つの状態（YAML）を受け取り、`java-diff-utils`を用いて差分を計算する。
- 差分情報をUIが表示しやすい形式に変換して返す。

### UIController

- UI全体の状態（例: エージェント処理中、承認待ちなど）を管理する。
- `AgentService`からの要求に応じて、`ApprovalPanel`の表示/非表示を切り替える。
- `StateService`の状態が更新されたら、`DrawingCanvas`に再描画を指示する。

## 4. データフロー

### 4.1 ユーザー承認を伴うメインフロー

1. **User Input**: ユーザーがチャットに指示を入力 (`ChatPanel` -> `EventHandler`)
2. **Orchestration**: `EventHandler`が`AgentService`のメソッドを呼び出す。
3. **AI Request**: `AgentService`が`AIClient`を介してAI APIにリクエスト（ツールスキーマを含む）。
4. **Tool Call Response**: AIが実行すべきツール（例: `createShape`）とそのパラメータを返す。
5. **Tool Execution**: `AgentService`が`ToolInvoker`にツールの実行を指示。ツールは新しい状態のYAMLを生成する。
6. **Diff Calculation**: `AgentService`が`DiffService`を使い、現在と新しい状態の差分を計算する。
7. **Approval Request**: `AgentService`が処理を一時停止し、`UIController`に差分データと共に承認UIの表示を要求する。
8. **User Approval**: ユーザーが`ApprovalPanel`上で「適用」をクリック。イベントが`EventHandler`に伝わる。
9. **State Update**: `EventHandler`が`AgentService`の待機処理を再開させ、`AgentService`は`StateService`に新しい状態で更新するよう指示する。
10. **Redraw**: `StateService`の更新を検知した`UIController`が、`DrawingCanvas`に再描画を命令する。

## 5. API通信設計

### 5.1 AIへのリクエスト構造 (システムプロンプト)

システムプロンプトには、AIの役割定義に加え、`ToolRegistry`が生成したツールスキーマが含まれます。

```json
You are a helpful assistant that generates drawing commands in YAML format by calling available tools.

AVAILABLE_TOOLS:
[
  {
    "type": "function",
    "function": {
      "name": "createShape",
      "description": "Create a new shape on the canvas.",
      "parameters": {
        "type": "object",
        "properties": {
          "shape_type": { "type": "string", "enum": ["ellipse", "rectangle"] },
          "x": { "type": "number", "description": "Center x-coordinate" },
          "y": { "type": "number", "description": "Center y-coordinate" },
          "fill_color": { "type": "string", "description": "Fill color in hex format" }
        },
        "required": ["shape_type", "x", "y"]
      }
    }
  },
  {
    "type": "function",
    "function": { ... for modifyShape ... }
  }
]
```

## 6. 非同期処理設計

### 6.1 UIとエージェントの非同期連携

UIの応答性を保つため、時間のかかるAI関連の処理はすべてバックグラウンドスレッドで実行されます。

- UIスレッド (EDT): Swing/JavaFXコンポーネントの操作のみを担当。
- エージェントスレッドプール: `AgentService`内のAI API通信、ツール実行、差分計算などを担当。

UIスレッドとエージェントスレッドの連携には`CompletableFuture`や`SwingWorker`/`Platform.runLater`を使用します。

#### ユーザー承認の待機メカニズム

`AgentService`はユーザーの承認が必要になると、`CompletableFuture<Boolean>`を生成し、それを待機(`.join()`)します。UI側でユーザーがボタンを押すと、`EventHandler`がこのFutureを`.complete(true/false)`で完了させ、エージェントの処理を再開させます。

## 7. 拡張性設計

### 7.1 新しいツールの追加

新しい機能（例: 星形を描くツール）を追加する手順は以下の通りです。

1. `domain/tools/impl/`に`CreateStarTool.java`クラスを作成し、`IAgentTool`インターフェースを実装する。
2. ツールの名前、説明、パラメータースキーマ、実行ロジック（YAMLを返す）を記述する。
3. `ToolRegistry`に新しいツールを登録する。

これだけで、AIは自動的に新しい「星形を描く」能力を獲得し、ユーザーはその機能を自然言語で利用できるようになります。
