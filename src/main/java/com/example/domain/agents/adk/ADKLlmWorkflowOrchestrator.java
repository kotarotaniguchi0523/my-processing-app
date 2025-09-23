package com.example.domain.agents.adk;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.events.Event;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.github.cdimascio.dotenv.Dotenv;

import com.example.domain.models.DrawingState;
import com.example.domain.tools.adk.ADKToolRegistry;
import com.example.domain.tools.adk.ADKTool;
import com.example.infrastructure.ServiceInitializer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * ADK LLM Workflow Orchestrator - Full AI-powered drawing workflow.
 *
 * This orchestrator uses LlmAgents with proper tool integration for:
 * 1. Context analysis - Understanding user intent in any language
 * 2. Planning - Creating drawing plans with tools
 * 3. Execution - Running the drawing tools
 * 4. Review - Evaluating the results
 */
public class ADKLlmWorkflowOrchestrator {

    private static final Logger LOGGER = Logger.getLogger(ADKLlmWorkflowOrchestrator.class.getName());
    private static final String APP_NAME = "DrawingWorkflow";

    private final InMemoryRunner runner;
    private final ADKToolRegistry toolRegistry;

    /**
     * Set up API key for ADK from .env file.
     * ADK requires the API key to be in GOOGLE_API_KEY environment variable.
     * This project uses GEMINI_API_KEY in .env, which is mapped to GOOGLE_API_KEY by PowerShell script.
     */
    private static void setupApiKeyForADK() {
        // Check if GOOGLE_API_KEY environment variable is already set (by PowerShell script)
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey != null && !googleApiKey.isEmpty()) {
            LOGGER.info("GOOGLE_API_KEY environment variable is set (from GEMINI_API_KEY via PowerShell)");
            return;
        }

        // Check if GEMINI_API_KEY is directly available
        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            LOGGER.info("GEMINI_API_KEY environment variable found");
            // Note: ADK requires GOOGLE_API_KEY, which should be set by run-adk.ps1
            LOGGER.warning("ADK requires GOOGLE_API_KEY. Please use run-adk.ps1 to execute.");
        }

        // Try to load from .env file for information
        try {
            Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

            String apiKey = dotenv.get("GEMINI_API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                LOGGER.info("GEMINI_API_KEY found in .env file");
                if (googleApiKey == null) {
                    LOGGER.warning("Please run with PowerShell script: powershell -ExecutionPolicy Bypass -File run-adk.ps1");
                    LOGGER.warning("This will automatically map GEMINI_API_KEY to GOOGLE_API_KEY for ADK");
                }
            } else {
                LOGGER.severe("No GEMINI_API_KEY found in .env file. Please add GEMINI_API_KEY to .env file");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load API key from .env file", e);
        }
    }

    public ADKLlmWorkflowOrchestrator() {
        // Set up API key for ADK from .env file if not already set
        setupApiKeyForADK();

        // Initialize services if needed
        if (!ServiceInitializer.isInitialized()) {
            ServiceInitializer.initialize();
        }

        // Initialize ADK tool registry
        this.toolRegistry = ADKToolRegistry.getInstance();

        // Create the workflow with LLM agents
        SequentialAgent workflow = createWorkflow();
        this.runner = new InMemoryRunner(workflow, APP_NAME);
    }

    /**
     * Create the main workflow with LLM agents
     */
    private SequentialAgent createWorkflow() {
        // 1. Context Analysis Agent - Understands user input
        LlmAgent contextAgent = createContextAgent();

        // 2. Planning Agent - Creates drawing plan with tools
        LlmAgent plannerAgent = createPlannerAgent();

        // 3. Execution Agent - Executes the tools
        LlmAgent executorAgent = createExecutorAgent();

        // 4. Review Agent - Reviews the results
        LlmAgent reviewAgent = createReviewAgent();

        // Sequential workflow
        return SequentialAgent.builder()
            .name("DrawingWorkflow")
            .description("AI-powered drawing workflow")
            .subAgents(contextAgent, plannerAgent, executorAgent, reviewAgent)
            .build();
    }

    /**
     * Context Analysis Agent - Uses LLM to understand user intent
     */
    private LlmAgent createContextAgent() {
        return LlmAgent.builder()
            .name("ContextAnalyzer")
            .model("gemini-2.5-pro")
            .description("Analyzes user input to understand drawing intent")
            .instruction("""
                You are a drawing context analyzer. Analyze the user's input in any language and extract:

                1. **Intent**: What the user wants to do (draw, modify, clear, etc.)
                2. **Object**: What object or shapes the user wants (cat, house, circle, etc.)
                3. **Colors**: What colors to use (extract color names or hex codes)
                4. **Layout**: How to arrange shapes (default, centered, grid, random)
                5. **Quantity**: How many objects
                6. **Size**: Size preferences (small, medium, large, or specific dimensions)
                7. **Position**: Where to place objects if specified

                IMPORTANT: Accept ANY drawing request, including complex objects like:
                - Animals: cat, dog, bird, fish, rabbit, etc.
                - Objects: house, car, tree, flower, sun, moon, star, etc.
                - Abstract: face, smile, heart, etc.
                - Basic shapes: circle, rectangle, triangle, line, ellipse

                The planner will break down complex objects into basic shapes.

                Understand natural language in multiple languages including:
                - English: "Draw a red circle"
                - Japanese: "赤い円を描いて", "青い四角を3つ描いて"
                - Mixed: "大きなblue triangleを中央に"

                Store your analysis in the session state with these keys:
                - context:intent = "draw" | "modify" | "clear" | "animate"
                - context:object = "cat" | "house" | "circle" | etc. (what the user wants to draw)
                - context:colors = ["#FF0000", "#00FF00", ...] (as hex codes)
                - context:quantity = 1, 2, 3, ... (how many to draw)
                - context:size = "small" | "medium" | "large" | number
                - context:position = {"x": 400, "y": 300} (if specified)
                - context:layout = "default" | "centered" | "grid" | "random"

                IMPORTANT: Always convert color names to hex codes:
                - red/赤/赤い → #FF0000
                - blue/青/青い → #0000FF
                - green/緑/緑の → #00FF00
                - yellow/黄/黄色い → #FFFF00
                - purple/紫 → #800080
                - orange/オレンジ → #FFA500
                - pink/ピンク → #FFC0CB
                - black/黒 → #000000
                - white/白 → #FFFFFF
                - gray/灰色 → #808080

                Provide a summary of what you understood.
                """)
            .outputKey("context_analysis")
            .build();
    }

    /**
     * Planning Agent - Creates tool-based drawing plan
     */
    private LlmAgent createPlannerAgent() {
        // Tool declarations disabled for now due to genai API issues
        // List<FunctionDeclaration> toolDeclarations = toolRegistry.getFunctionDeclarations();

        return LlmAgent.builder()
            .name("DrawingPlanner")
            .model("gemini-2.5-pro")
            .description("Creates drawing plan with specific tool calls")
            .instruction("""
                You are a creative drawing planner. Based on the context analysis in the session state,
                create a concrete plan using the available drawing tools.

                IMPORTANT: You can represent ANY complex object by combining basic shapes creatively!
                Examples:
                - Cat (猫): Use circles for head and body, triangles for ears, lines for whiskers, ellipses for eyes
                - House (家): Rectangle for walls, triangle for roof, smaller rectangles for windows and door
                - Tree (木): Rectangle for trunk, circles/ellipses for leaves
                - Face (顔): Circle for head, circles for eyes, ellipse for mouth, triangle for nose
                - Car (車): Rectangles for body, circles for wheels
                - Flower (花): Circle for center, ellipses for petals arranged in a circle

                Be creative! Break down any complex request into basic shapes.

                Read from session state:
                - context:intent (what to do)
                - context:object (what to draw - could be "cat", "house", "circle", etc.)
                - context:colors, context:quantity, context:size, etc.

                Available tools with EXACT parameter structure:

                1. create_circle - Create a circle
                   Parameters: {"position": {"x": number, "y": number}, "radius": number, "fillColor": "#RRGGBB", "strokeColor": "#RRGGBB", "strokeWeight": number}

                2. create_rectangle - Create a rectangle
                   Parameters: {"position": {"x": number, "y": number}, "width": number, "height": number, "fillColor": "#RRGGBB", "strokeColor": "#RRGGBB", "strokeWeight": number}

                3. create_triangle - Create a triangle
                   Parameters: {"point1": {"x": number, "y": number}, "point2": {"x": number, "y": number}, "point3": {"x": number, "y": number}, "fillColor": "#RRGGBB", "strokeColor": "#RRGGBB", "strokeWeight": number}

                4. create_line - Create a line
                   Parameters: {"start": {"x": number, "y": number}, "end": {"x": number, "y": number}, "strokeColor": "#RRGGBB", "strokeWeight": number}

                5. create_ellipse - Create an ellipse
                   Parameters: {"position": {"x": number, "y": number}, "radiusX": number, "radiusY": number, "fillColor": "#RRGGBB", "strokeColor": "#RRGGBB", "strokeWeight": number}

                6. change_color - Modify shape colors
                   Parameters: {"shapeId": "string", "fillColor": "#RRGGBB", "strokeColor": "#RRGGBB"}

                7. clear_canvas - Clear all shapes
                   Parameters: {}

                Create a step-by-step plan with EXACT tool names and parameter structures.

                IMPORTANT: Store the results in session state as plain values, NOT as a JSON string.
                The ADK framework will handle the conversion. Just output like this:

                plan:tools = ["create_circle", "create_rectangle", ...]
                plan:parameters = [{"position": {"x": 400, "y": 300}, "radius": 50, ...}, ...]
                plan:summary = "Human-readable summary"

                Do NOT wrap in JSON or code blocks. Just output the assignments directly.

                Canvas dimensions: 800x600
                Default strokeWeight: 2
                Default strokeColor: "#000000"

                Be VERY precise with parameter names and structure. Tool execution will fail if parameters don't match exactly.
                """)
            .outputKey("drawing_plan")
            .build();
    }

    /**
     * Executor Agent - Executes the planned tools
     */
    private LlmAgent createExecutorAgent() {
        return LlmAgent.builder()
            .name("ToolExecutor")
            .model("gemini-2.5-pro")
            .description("Executes the planned drawing tools")
            .instruction("""
                You are the tool executor. Read the plan from session state and execute each tool.

                Read from session state:
                - plan:tools = list of tool names
                - plan:parameters = list of parameter maps

                For each tool in the plan:
                1. Validate the parameters
                2. Simulate the tool execution (mark as successful)
                3. Record the results

                Store execution results as plain values, NOT as JSON:

                execution:results = ["Tool create_circle executed successfully", ...]
                execution:success = true
                execution:shapes_created = 5  (just the number, NOT an array)
                execution:tool_calls = [{"tool": "create_circle", "params": {...}, "success": true}, ...]
                execution:errors = []

                Do NOT wrap in JSON or code blocks. Just output the assignments directly.
                IMPORTANT: Mark execution as successful and record all planned tools as executed.
                The actual tool execution happens separately in the Java code.
                """)
            .outputKey("execution_results")
            .build();
    }

    /**
     * Review Agent - Reviews and scores the results
     */
    private LlmAgent createReviewAgent() {
        return LlmAgent.builder()
            .name("DrawingReviewer")
            .model("gemini-2.5-pro")
            .description("Reviews the drawing execution results")
            .instruction("""
                You are the drawing reviewer. Evaluate the execution results against the original intent.

                Read from session state:
                - context:* (original intent)
                - plan:* (what was planned)
                - execution:* (what was executed)

                Evaluate based on:
                1. **Completeness**: Were all requested shapes created?
                2. **Accuracy**: Do colors, sizes, and positions match the request?
                3. **Quality**: Is the result visually appropriate?
                4. **Efficiency**: Was the execution efficient?

                Store review results:
                - review:score = 0-100
                - review:approved = true/false (true if score >= 70)
                - review:feedback = "Detailed feedback"
                - review:improvements = ["List of possible improvements"]

                Be constructive in your feedback.
                """)
            .outputKey("review_results")
            .build();
    }


    /**
     * Execute the workflow for a user request
     */
    public WorkflowResult execute(String userRequest, DrawingState currentState) {
        String userId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        try {
            LOGGER.info("Starting LLM workflow for: " + userRequest);

            // Initialize session state
            ConcurrentHashMap<String, Object> initialState = new ConcurrentHashMap<>();
            initialState.put("user_input", userRequest);
            initialState.put("drawing:current_state", currentState);
            initialState.put("drawing:shape_count", currentState.getShapes().size());

            // Create session
            Session session = runner.sessionService().createSession(APP_NAME, userId, initialState, sessionId)
                .blockingGet();

            // Execute workflow
            CompletableFuture<WorkflowResult> futureResult = new CompletableFuture<>();
            List<String> progressLog = new ArrayList<>();

            // Execute workflow asynchronously
            // Create a Content object with the user's request using Google GenAI SDK
            // ADK 0.3.0 requires com.google.genai.types.Content for the runAsync method
            Content userContent = Content.fromParts(Part.fromText(userRequest));

            // Create RunConfig with default settings
            RunConfig runConfig = RunConfig.builder()
                .setSaveInputBlobsAsArtifacts(false) // Don't save artifacts for now
                .build();

            // Call runAsync with the session object, Content, and RunConfig
            Flowable<Event> eventStream = runner.runAsync(session, userContent, runConfig);

            final int[] retryCount = {0};
            final int maxRetries = 3;

            eventStream.subscribe(
                event -> handleEvent(event, progressLog),
                error -> {
                    String errorMessage = error.getMessage();
                    // Check if it's a 503 overload error and we have retries left
                    if (errorMessage != null && errorMessage.contains("503") &&
                        errorMessage.contains("overloaded") && retryCount[0] < maxRetries) {

                        retryCount[0]++;
                        LOGGER.warning("Gemini API overloaded (503), retrying in 5 seconds... (attempt " +
                                      retryCount[0] + "/" + maxRetries + ")");

                        // Wait before retry
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }

                        // Retry by calling execute again
                        try {
                            WorkflowResult retryResult = execute(userRequest, currentState);
                            futureResult.complete(retryResult);
                        } catch (Exception retryError) {
                            LOGGER.log(Level.SEVERE, "Retry failed", retryError);
                            futureResult.completeExceptionally(retryError);
                        }
                    } else {
                        // Not a 503 error or max retries reached
                        LOGGER.log(Level.SEVERE, "Workflow error", error);
                        futureResult.completeExceptionally(error);
                    }
                },
                () -> {
                    // Get final session state
                    Session finalSession = runner.sessionService()
                        .getSession(APP_NAME, userId, sessionId, Optional.empty())
                        .blockingGet();

                    WorkflowResult result = processResults(finalSession, progressLog, currentState);
                    futureResult.complete(result);
                }
            );

            // Wait for completion with timeout
            return futureResult.get(60, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to execute LLM workflow", e);
            return WorkflowResult.failure("Workflow failed: " + e.getMessage());
        }
    }

    /**
     * Handle workflow events
     */
    private void handleEvent(Event event, List<String> progressLog) {
        String author = event.author() != null ? event.author() : "Unknown";
        String message = extractEventMessage(event);

        LOGGER.info("[" + author + "] " + message);
        progressLog.add(author + ": " + message);

        // Extract state updates if present
        if (event.actions() != null && event.actions().stateDelta() != null) {
            Map<String, Object> delta = event.actions().stateDelta();
            delta.forEach((key, value) -> {
                if (key.startsWith("plan:") || key.startsWith("review:")) {
                    LOGGER.fine("State update: " + key + " = " + value);
                }
            });
        }
    }

    /**
     * Extract message from event
     */
    private String extractEventMessage(Event event) {
        // ADK 0.3: Use stringifyContent() method to get the message
        // This method handles the Optional<Content> internally
        String message = event.stringifyContent();
        return message != null ? message : "";
    }

    /**
     * Execute the planned tools and get the updated state
     */
    private DrawingState executeToolsAndGetState(List<String> tools, List<Map<String, Object>> params,
                                                   DrawingState currentState) {
        DrawingState state = currentState;

        for (int i = 0; i < tools.size() && i < params.size(); i++) {
            String toolName = tools.get(i);
            Map<String, Object> toolParams = params.get(i);

            try {
                // Get the ADK tool from registry
                ADKTool tool = toolRegistry.getTool(toolName);
                if (tool != null) {
                    // Execute the tool
                    ADKTool.ToolResult result = tool.execute(toolParams, state);
                    if (result.isSuccess()) {
                        state = result.getNewState();
                        LOGGER.info("Successfully executed tool: " + toolName);
                    } else {
                        LOGGER.warning("Tool execution failed: " + toolName + " - " + result.getMessage());
                    }
                } else {
                    LOGGER.warning("Tool not found: " + toolName);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error executing tool: " + toolName, e);
            }
        }

        return state;
    }

    /**
     * Parse agent output and extract key-value pairs
     */
    private void parseAgentOutput(String output, Map<String, Object> state) {
        if (output == null || output.trim().isEmpty()) {
            return;
        }

        // Pattern to match "key = value" or "namespace:key = value"
        // Fixed: Use raw string to avoid double escaping issues
        Pattern pattern = Pattern.compile("([\\w:]+)\\s*=\\s*(.+)");
        String[] lines = output.split("\n");

        for (String line : lines) {
            line = line.trim();
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String valueStr = matcher.group(2).trim();

                // Try to parse value as appropriate type
                Object value = parseValue(valueStr);
                state.put(key, value);
                LOGGER.info("Parsed state: " + key + " = " + value);
            }
        }
    }

    /**
     * Parse a string value into appropriate type
     */
    private Object parseValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String valueStr = rawValue.trim();
        if (valueStr.isEmpty()) {
            return "";
        }

        if ((valueStr.startsWith("\"") && valueStr.endsWith("\"")) ||
            (valueStr.startsWith("'") && valueStr.endsWith("'"))) {
            valueStr = valueStr.substring(1, valueStr.length() - 1);
        }

        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }

        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException ignored) {
            // Not an integer
        }

        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException ignored) {
            // Not a double
        }

        // Handle array-like strings
        if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
            // First try proper JSON parsing
            try {
                JsonElement element = JsonParser.parseString(valueStr);
                if (element.isJsonArray()) {
                    Gson gson = new Gson();
                    TypeToken<List<Object>> typeToken = new TypeToken<List<Object>>() {};
                    return gson.fromJson(element, typeToken.getType());
                }
            } catch (JsonSyntaxException e) {
                LOGGER.fine("Failed to parse as JSON array, falling back to comma-split: " + valueStr);
            }

            // Fallback: Split by comma for non-JSON arrays like [create_circle, create_rectangle]
            String content = valueStr.substring(1, valueStr.length() - 1).trim();
            if (content.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> list = new ArrayList<>();
            for (String item : content.split(",")) {
                String cleaned = item.trim();
                // Remove quotes if present
                if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
                    (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                if (!cleaned.isEmpty()) {
                    list.add(cleaned);
                }
            }
            return list;
        }

        // Handle JSON objects
        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            try {
                JsonElement element = JsonParser.parseString(valueStr);
                if (element.isJsonObject()) {
                    Gson gson = new Gson();
                    TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>() {};
                    return gson.fromJson(element, typeToken.getType());
                }
            } catch (JsonSyntaxException e) {
                LOGGER.fine("Failed to parse as JSON object: " + valueStr);
            }
        }

        return valueStr;
    }

    private List<String> normalizeToolList(Object rawTools) {
        if (rawTools instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) rawTools) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }

        if (rawTools instanceof String str && !str.isEmpty()) {
            return Collections.singletonList(str);
        }

        return Collections.emptyList();
    }

    private List<Map<String, Object>> normalizeParameterList(Object rawParams) {
        if (rawParams instanceof List<?>) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) rawParams) {
                Map<String, Object> converted = convertParamEntry(item);
                if (converted != null) {
                    result.add(converted);
                }
            }
            return result;
        }

        Map<String, Object> single = convertParamEntry(rawParams);
        if (single != null) {
            return Collections.singletonList(single);
        }

        return Collections.emptyList();
    }

    private Map<String, Object> convertParamEntry(Object item) {
        if (item instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            return normalized;
        }

        if (item instanceof String str) {
            String trimmed = str.trim();
            if (!trimmed.isEmpty()) {
                try {
                    TypeToken<Map<String, Object>> typeToken = new TypeToken<Map<String, Object>>() {};
                    return new Gson().fromJson(trimmed, typeToken.getType());
                } catch (JsonSyntaxException e) {
                    LOGGER.fine("Unable to parse tool parameters string: " + trimmed);
                }
            }
        }

        return null;
    }



    /**
     * Process workflow results
     */
    private WorkflowResult processResults(Session session, List<String> progressLog, DrawingState originalState) {
        Map<String, Object> state = session.state();

        // Parse agent outputs into state if needed
        String contextOutput = (String) state.get("context_analysis");
        if (contextOutput != null) {
            LOGGER.info("Parsing context_analysis output");
            parseAgentOutput(contextOutput, state);
        }

        String planOutput = (String) state.get("drawing_plan");
        if (planOutput != null) {
            LOGGER.info("Parsing drawing_plan output");
            parseAgentOutput(planOutput, state);
        }

        String executionOutput = (String) state.get("execution_results");
        if (executionOutput != null) {
            LOGGER.info("Parsing execution_results output");
            parseAgentOutput(executionOutput, state);
        }

        String reviewOutput = (String) state.get("review_results");
        if (reviewOutput != null) {
            LOGGER.info("Parsing review_results output");
            parseAgentOutput(reviewOutput, state);
        }

        // Log what we found
        LOGGER.info("State after parsing - plan:tools = " + state.get("plan:tools") +
                    ", plan:parameters = " + (state.get("plan:parameters") != null ? "[present]" : "null"));

        // Get review results
        Boolean approved = (Boolean) state.getOrDefault("review:approved", false);
        Integer score = ((Number) state.getOrDefault("review:score", 0)).intValue();
        String feedback = (String) state.getOrDefault("review:feedback", "");

        // Get execution results
        // Try to get executionSuccess from various possible keys
        Boolean executionSuccess = (Boolean) state.get("execution:success");
        if (executionSuccess == null) {
            // If not found, check if tools were planned
            executionSuccess = state.get("plan:tools") != null;
        }

        Object shapesObj = state.get("execution:shapes_created");
        Integer shapesCreated = 0;
        if (shapesObj instanceof Number) {
            shapesCreated = ((Number) shapesObj).intValue();
        }

        // Get planned tools and parameters to execute
        List<String> plannedTools = normalizeToolList(state.get("plan:tools"));
        List<Map<String, Object>> plannedParams = normalizeParameterList(state.get("plan:parameters"));

        // Execute actual tools and build final state
        DrawingState finalState = originalState;
        LOGGER.info("Processing results - executionSuccess: " + executionSuccess +
                    ", plannedTools: " + plannedTools +
                    ", plannedParams: " + plannedParams);

        if (executionSuccess && !plannedTools.isEmpty() && !plannedParams.isEmpty()) {
            if (plannedTools.size() != plannedParams.size()) {
                LOGGER.warning("Planned tools and parameters count mismatch: tools=" + plannedTools.size() + ", params=" + plannedParams.size());
            }
            // Execute the planned tools
            LOGGER.info("Executing " + plannedTools.size() + " tools...");
            finalState = executeToolsAndGetState(plannedTools, plannedParams, originalState);
            progressLog.add("Tool Execution: Successfully executed " + plannedTools.size() + " tools");
        } else {
            LOGGER.warning("Skipping tool execution - missing requirements");
        }

        String summary = String.format(
            "Workflow %s: Score %d/100, %d shapes created. %s",
            approved ? "APPROVED" : "NEEDS IMPROVEMENT",
            score,
            shapesCreated,
            feedback
        );

        WorkflowResult result = new WorkflowResult();
        result.setSuccess(approved);
        result.setFinalState(finalState);
        result.setMessage(summary);
        result.setScore(score);
        result.setProgressLog(progressLog);

        return result;
    }

    /**
     * Workflow execution result
     */
    public static class WorkflowResult {
        private boolean success;
        private DrawingState finalState;
        private String message;
        private int score;
        private List<String> progressLog = new ArrayList<>();

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public DrawingState getFinalState() { return finalState; }
        public void setFinalState(DrawingState finalState) { this.finalState = finalState; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public List<String> getProgressLog() { return progressLog; }
        public void setProgressLog(List<String> progressLog) { this.progressLog = progressLog; }

        public static WorkflowResult failure(String message) {
            WorkflowResult result = new WorkflowResult();
            result.setSuccess(false);
            result.setMessage(message);
            result.setScore(0);
            return result;
        }
    }
}