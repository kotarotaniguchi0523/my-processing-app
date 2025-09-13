AI Agents Architecture Guide

This project implements an AI-assisted vector drawing app on Processing (Java) using Clean/Hexagonal Architecture. The agent interprets user chat, optionally calls drawing tools, proposes state changes, and asks for approval before applying.

Structure Overview
- Domain: Pure models and business rules.
  - `src/main/java/com/example/domain/models` — `DrawingState`, shapes (`Circle`, `Ellipse`, `Rectangle`, `Line`, `Text`), immutable value objects, YAML helpers.
  - `src/main/java/com/example/domain/services` — service interfaces (`IStateManager`, `IHistoryService`, `ShapeOps`, etc.).
  - `src/main/java/com/example/domain/tools` — tool contracts (`IAgentTool`) and `ToolRegistry`.
  - `src/main/java/com/example/domain/events` — processing events and listener interface `IAIProcessingListener`.
- Application: Use cases and orchestration.
  - `application/services` — `AIAgentService` (agent orchestrator), `ConversationService`, `PromptBuilder`, `JsonHistoryRepository`, `ToolCoordinator` (AFC integration), `StateService`.
  - `application/usecase` — concrete use cases and `UseCaseFacade` implementing `ToolUseCasePort` for tools.
  - `application/ports` — extension points: `ConversationPort`, `PromptBuilderPort`, `ToolCoordinatorPort` (future adapters can implement these).
- Infrastructure: External systems and concrete adapters.
  - `infrastructure/ai` — `GeminiClient` (Google GenAI SDK wrapper), `DrawingFunctions` (tool functions exposed to the LLM).
  - `infrastructure/services` — `StateManager`, `HistoryService`, `ToolInvoker`.
  - `infrastructure/adapters` — adapters wiring infra to domain interfaces.
- Presentation: Processing-based UI and interaction.
  - `presentation` — `SimpleSketch` setup and render loop, `SketchUIRenderer` and renderers (`CanvasRenderer`, `ChatRenderer`, `InputRenderer`, `DiffApprovalRenderer`), `ChatManager`, `InputManager`, `SketchEventHandler`.

Agent Flow (End-to-End)
1) User types a message in chat
   - `ChatManager` buffers input; `AiChatHelper.sendAndAppendAsync(...)` sends text to `AIAgentService` on a background thread and appends UI messages.

2) Orchestration in `AIAgentService`
   - Fires events to `IAIProcessingListener` (implemented by `ChatManager`) for live status.
   - Records the turn via `ConversationService` and persists via `JsonHistoryRepository` (file: `.agent_history.json`).
   - Updates `ToolCoordinator` with the current `DrawingState`.
   - Builds prompt/context via `PromptBuilder`.
   - Prepares `GenerateContentConfig` with `ToolCoordinator.createToolConfig()` for Automatic Function Calling (AFC) using methods in `infrastructure/ai/DrawingFunctions`.
   - Calls `GeminiClient.generateContentWithTools(...)` to let the LLM decide tool calls.

3) State proposal + approval
   - `ToolCoordinator` exposes and applies `DrawingFunctions`; resulting proposed state is compared to the previous one.
   - If different, `AIAgentService` triggers an approval callback configured by the UI (`SimpleSketch#setupApprovalCallback`).
   - UI shows the diff modal via `presentation/DiffApprovalRenderer` and waits for Approve/Reject.
   - On Approve: `AIAgentService.applyApprovedState(...)` updates `currentState`, records history, and emits completion events. On Reject: state remains unchanged and a chat message is appended.

Key Components
- `AIAgentService`: Central coordinator for chat → LLM → tools → proposed state → approval → apply. Emits processing events and manages `.agent_history.json` persistence.
- `ToolCoordinator`: Bridges domain state and AFC. Exposes selected `DrawingFunctions` methods to the LLM and infers human-readable tool calls from state diffs.
- `DrawingFunctions` (infrastructure/ai): The concrete drawing operations used by AFC. They mutate an internal `DrawingState` and are strictly side-effect scoped to tool execution.
- `ToolRegistry` + `IAgentTool` (domain): Registry for structured, validated tools used by programmatic execution paths (separate from AFC). Tools return `UseCaseResult` with a new `DrawingState`.
- `ToolInvoker` (infrastructure/services): Validates against a tool’s JSON schema and executes `IAgentTool` with the current state.
- `ConversationService` and `PromptBuilder`: Maintain compact chat history and generate context/system prompts.
- `ChatManager` + `DiffApprovalRenderer`: UI feedback and approval UX implemented in Processing.

Events and Telemetry
- Listener: `IAIProcessingListener`
  - `onProcessingStarted`, `onPromptAnalyzing`, `onToolSelecting`, `onToolExecuting`, `onProcessingCompleted`, `onProcessingFailed`.
- UI subscribes via `ChatManager` to display running commentary (thinking, tool selection/execution, success/failure).

Clean Architecture Notes
- Dependency direction points inward: domain has no Processing/SDK dependencies.
- Application orchestrates domain and only depends on domain ports; infrastructure implements adapters (e.g., `GeminiClient`, `StateManager`, `HistoryService`).
- Presentation layer only draws UI and routes input; it talks to application services.

Running the App
- Prerequisites: JDK 17+, Maven.
- Optional: set `GOOGLE_API_KEY` or `GEMINI_API_KEY` in environment or `.env`.
- Run: `mvn -q -DskipTests exec:java`
  - Without an API key, the UI works and AI is disabled gracefully (warning logged).

Extending the Agent
1) Add a new structured tool (domain-driven)
   - Create class in `domain/tools/impl` implementing `IAgentTool`.
     - Provide `name()`, a JSON-like `schema()` (Map), and `execute(Map, DrawingState)` returning `UseCaseResult` with a new `DrawingState`.
   - Register it in `domain/tools/ToolRegistry#initializeInternal(...)` and (optionally) wire a `ToolUseCasePort` if it needs app-level use cases.
   - If you want AFC to call it directly, also expose a corresponding method in `infrastructure/ai/DrawingFunctions` and list it in `application/services/ToolCoordinator#getDrawingMethods()`.

2) Add or change a drawing operation for AFC
   - Implement the method in `infrastructure/ai/DrawingFunctions` using pure operations over `DrawingState`.
   - Add the reflective method reference in `ToolCoordinator#getDrawingMethods()` so Gemini can call it.
   - Consider how to infer a `ToolCall` from the resulting state in `ToolCoordinator#inferToolCallsFromStateChange(...)` for clearer chat logs.

3) Integrate another model/provider
   - Implement an adapter mirroring `GeminiClient` responsibilities and hide the external SDK in `infrastructure/...`.
   - Keep the application layer calling a thin interface/facade; do not let SDK types leak into domain/application.

4) Add a new presentation or conversation adapter
   - Implement `ConversationPort`/`PromptBuilderPort`/`ToolCoordinatorPort` in `infrastructure/adapters` or `adapter/...` and compose in a separate entry point.

Approval UX
- The modal is rendered in `presentation/DiffApprovalRenderer` and bound from `SimpleSketch`.
- It computes a summary from `DrawingState` diffs and prevents unintended changes by requiring explicit user consent.

Persistence
- Conversation + tool calls are stored in `.agent_history.json` by `JsonHistoryRepository` using `domain/models/AgentHistory` schema. On startup, `AIAgentService` loads this if present and seeds the session.

Conventions
- Immutability is preferred for domain models; builders create new states rather than mutating.
- All state transitions produce a fresh `DrawingState` and are validated by `StateService`/`StateManager`.
- Tools must not mutate input `DrawingState` in-place; always return a new state.

Troubleshooting
- No API key: You’ll see a warning and AI features disable; local tools and UI still work.
- Long prompts/timeouts: `GeminiClient` has backoff, QPS and concurrency guards. Tune with env vars like `GEMINI_RATE_QPS`, `GEMINI_MAX_CONCURRENCY`, `GEMINI_TOTAL_TIMEOUT_MS`.

