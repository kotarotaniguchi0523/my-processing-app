package com.example.domain.agents.adk;

import com.google.adk.agents.LlmAgent;

/**
 * Builder for creating drawing workflow agents.
 * Single responsibility: Create and configure LLM agents for the drawing workflow.
 */
public class DrawingAgentBuilder {

    private static final String MODEL = "gemini-2.5-pro";

    /**
     * Create Context Analysis Agent - Understands user intent
     */
    public static LlmAgent createContextAgent() {
        return LlmAgent.builder()
            .name("ContextAnalyzer")
            .model(MODEL)
            .description("Analyzes user input to understand drawing intent")
            .instruction(getContextAnalysisInstructions())
            .outputKey("context_analysis")
            .build();
    }

    /**
     * Create Planning Agent - Creates tool-based drawing plan
     */
    public static LlmAgent createPlannerAgent() {
        return LlmAgent.builder()
            .name("DrawingPlanner")
            .model(MODEL)
            .description("Creates drawing plan with specific tool calls")
            .instruction(getPlannerInstructions())
            .outputKey("drawing_plan")
            .build();
    }

    /**
     * Create Execution Agent - Executes the tools
     */
    public static LlmAgent createExecutorAgent() {
        return LlmAgent.builder()
            .name("ToolExecutor")
            .model(MODEL)
            .description("Executes the planned drawing tools")
            .instruction(getExecutorInstructions())
            .outputKey("execution_result")
            .build();
    }

    /**
     * Create Review Agent - Reviews the results
     */
    public static LlmAgent createReviewAgent() {
        return LlmAgent.builder()
            .name("DrawingReviewer")
            .model(MODEL)
            .description("Reviews the drawing execution results")
            .instruction(getReviewerInstructions())
            .outputKey("review_result")
            .build();
    }

    private static String getContextAnalysisInstructions() {
        return """
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
            """;
    }

    private static String getPlannerInstructions() {
        return """
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
            plan:success = true
            plan:message = "Planning complete: Drawing a cat with 5 shapes"

            If you cannot create a plan, set plan:success = false and explain why.
            """;
    }

    private static String getExecutorInstructions() {
        return """
            You are the tool executor. Read the plan from session state and confirm execution.

            Read from session state:
            - plan:tools (array of tool names)
            - plan:parameters (array of parameter objects)

            These will be executed by the orchestrator after your confirmation.

            Provide a summary like:
            execution:ready = true
            execution:tool_count = 5
            execution:summary = "Ready to execute 5 drawing tools to create a cat"

            If the plan is invalid:
            execution:ready = false
            execution:error = "Explanation of what's wrong"
            """;
    }

    private static String getReviewerInstructions() {
        return """
            You are the drawing reviewer. Review the execution results.

            Read from session state:
            - execution:tool_results (array of execution results)
            - execution:success_count (number of successful executions)
            - execution:failure_count (number of failed executions)

            Provide a final review:
            review:success = true/false
            review:score = 0-100 (quality score)
            review:summary = "Successfully drew a cat with 5 shapes"
            review:suggestions = "Consider adding more detail to the whiskers"

            Be encouraging but honest about the results.
            """;
    }
}