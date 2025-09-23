package com.example.domain.agents;

import com.example.domain.models.DrawingState;
import java.util.*;

/**
 * Shared context data structure for passing information between agents.
 * Contains structured representation of the drawing request and its interpretation.
 */
public class AgentContext {

    private final String originalUserRequest;
    private final DrawingState currentState;
    private final Map<String, Object> metadata;
    private DrawingInterpretation interpretation;
    private DrawingPlan drawingPlan;
    private ReviewResult reviewResult;
    private int currentIteration;
    private final int maxIterations;

    public AgentContext(String userRequest, DrawingState currentState) {
        this(userRequest, currentState, 4); // Default max 4 iterations
    }

    public AgentContext(String userRequest, DrawingState currentState, int maxIterations) {
        this.originalUserRequest = userRequest;
        this.currentState = currentState;
        this.metadata = new HashMap<>();
        this.currentIteration = 0;
        this.maxIterations = maxIterations;
    }

    /**
     * Structured interpretation of the user's drawing request.
     */
    public static class DrawingInterpretation {
        private final String subject;  // What to draw (e.g., "秋田犬", "house")
        private final List<ComponentSpec> components;  // Broken down components
        private final ColorScheme colorScheme;
        private final LayoutStrategy layout;
        private final Map<String, String> attributes;  // Additional attributes
        private final String style;  // Drawing style

        public DrawingInterpretation(String subject, List<ComponentSpec> components,
                                     ColorScheme colorScheme, LayoutStrategy layout) {
            this.subject = subject;
            this.components = components;
            this.colorScheme = colorScheme;
            this.layout = layout;
            this.attributes = new HashMap<>();
            this.style = "default";
        }

        // Backward compatibility constructor
        public DrawingInterpretation(String originalRequest, String subject, String style) {
            this.subject = subject;
            this.style = style;
            this.components = new ArrayList<>();
            this.colorScheme = new ColorScheme("#000000", "#FFFFFF");
            this.layout = new LayoutStrategy("centered", 1.0);
            this.attributes = new HashMap<>();
            this.attributes.put("originalRequest", originalRequest);
        }

        // Getters
        public String getSubject() { return subject; }
        public String getStyle() { return style; }
        public List<ComponentSpec> getComponents() { return Collections.unmodifiableList(components); }
        public ColorScheme getColorScheme() { return colorScheme; }
        public LayoutStrategy getLayout() { return layout; }
        public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }

        public void addAttribute(String key, String value) {
            attributes.put(key, value);
        }

        // Backward compatibility methods
        public void addComponent(String name, String type, Map<String, Object> properties) {
            // Convert to ComponentSpec format
            String relativeSize = (String) properties.getOrDefault("size", "medium");
            String position = (String) properties.getOrDefault("position", "center");
            Object zOrderObj = properties.getOrDefault("zOrder", 0);
            int zOrder = (zOrderObj instanceof Integer) ? (Integer) zOrderObj : 0;
            // ComponentSpec spec = new ComponentSpec(name, type, relativeSize, position, zOrder); // Unused variable - commented out

            // Since components is final, we need to use a different approach
            // Store component info in attributes instead
            attributes.put("component_" + name + "_type", type);
            attributes.put("component_" + name + "_size", relativeSize);
            attributes.put("component_" + name + "_position", position);
            attributes.put("component_" + name + "_zOrder", String.valueOf(zOrder));
        }

        public void setColorScheme(Map<String, String> colorSchemeMap) {
            // This method exists for backward compatibility but can't modify the final field
            // The color scheme should be set via constructor
            if (colorSchemeMap.containsKey("primary")) {
                ((ColorScheme) colorScheme).addAccentColor(colorSchemeMap.get("primary"));
            }
        }

        public void setComposition(Map<String, Object> compositionMap) {
            // Add composition info as attributes
            for (Map.Entry<String, Object> entry : compositionMap.entrySet()) {
                attributes.put("composition_" + entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        public Map<String, Object> getComposition() {
            Map<String, Object> composition = new HashMap<>();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getKey().startsWith("composition_")) {
                    String key = entry.getKey().substring("composition_".length());
                    composition.put(key, entry.getValue());
                }
            }
            return composition;
        }
    }

    /**
     * Specification for a single component of the drawing.
     */
    public static class ComponentSpec {
        private final String name;  // e.g., "head", "body", "ear"
        private final String shapeType;  // e.g., "circle", "rectangle"
        private final String relativeSize;  // e.g., "large", "medium", "small"
        private final String position;  // e.g., "center", "top-left"
        private final int zOrder;  // Drawing order (higher = on top)

        public ComponentSpec(String name, String shapeType, String relativeSize,
                           String position, int zOrder) {
            this.name = name;
            this.shapeType = shapeType;
            this.relativeSize = relativeSize;
            this.position = position;
            this.zOrder = zOrder;
        }

        // Getters
        public String getName() { return name; }
        public String getShapeType() { return shapeType; }
        public String getRelativeSize() { return relativeSize; }
        public String getPosition() { return position; }
        public int getZOrder() { return zOrder; }
    }

    /**
     * Color scheme for the drawing.
     */
    public static class ColorScheme {
        private final String primaryColor;
        private final String secondaryColor;
        private final List<String> accentColors;

        public ColorScheme(String primaryColor, String secondaryColor) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.accentColors = new ArrayList<>();
        }

        public void addAccentColor(String color) {
            accentColors.add(color);
        }

        // Getters
        public String getPrimaryColor() { return primaryColor; }
        public String getSecondaryColor() { return secondaryColor; }
        public List<String> getAccentColors() { return Collections.unmodifiableList(accentColors); }
    }

    /**
     * Layout strategy for arranging components.
     */
    public static class LayoutStrategy {
        private final String alignment;  // "centered", "left", "right"
        private final double scale;  // Overall scale factor
        private final Map<String, RelativePosition> positions;

        public LayoutStrategy(String alignment, double scale) {
            this.alignment = alignment;
            this.scale = scale;
            this.positions = new HashMap<>();
        }

        public void addPosition(String componentName, RelativePosition position) {
            positions.put(componentName, position);
        }

        // Getters
        public String getAlignment() { return alignment; }
        public double getScale() { return scale; }
        public Map<String, RelativePosition> getPositions() { return Collections.unmodifiableMap(positions); }
    }

    /**
     * Relative position for a component.
     */
    public static class RelativePosition {
        private final double x;  // 0.0 to 1.0 (relative to canvas)
        private final double y;  // 0.0 to 1.0 (relative to canvas)
        private final String relativeTo;  // null or component name

        public RelativePosition(double x, double y, String relativeTo) {
            this.x = x;
            this.y = y;
            this.relativeTo = relativeTo;
        }

        // Getters
        public double getX() { return x; }
        public double getY() { return y; }
        public String getRelativeTo() { return relativeTo; }
    }

    /**
     * Concrete drawing plan with tool commands.
     */
    public static class DrawingPlan {
        private final List<ToolCommand> commands;
        private final String explanation;
        private final Map<String, Object> parameters;
        private String description;

        public DrawingPlan(List<ToolCommand> commands, String explanation) {
            this.commands = commands;
            this.explanation = explanation;
            this.description = explanation;
            this.parameters = new HashMap<>();
        }

        // Backward compatibility constructor
        public DrawingPlan() {
            this.commands = new ArrayList<>();
            this.explanation = "";
            this.description = "";
            this.parameters = new HashMap<>();
        }

        // Getters
        public List<ToolCommand> getCommands() { return Collections.unmodifiableList(commands); }
        public String getExplanation() { return explanation; }
        public String getDescription() { return description; }
        public Map<String, Object> getParameters() { return Collections.unmodifiableMap(parameters); }

        // Backward compatibility methods
        public void setDescription(String description) {
            this.description = description;
        }

        public void addCommand(ToolCommand command) {
            ((List<ToolCommand>) commands).add(command);
        }

        public void setCommands(List<ToolCommand> commands) {
            ((List<ToolCommand>) this.commands).clear();
            ((List<ToolCommand>) this.commands).addAll(commands);
        }
    }

    /**
     * Tool command representation.
     */
    public static class ToolCommand {
        private final String toolName;
        private final Map<String, Object> arguments;
        private String purpose;  // Why this command is needed

        public ToolCommand(String toolName, Map<String, Object> arguments, String purpose) {
            this.toolName = toolName;
            this.arguments = new HashMap<>(arguments);
            this.purpose = purpose;
        }

        // Backward compatibility constructor
        public ToolCommand(String toolName, Map<String, Object> arguments) {
            this.toolName = toolName;
            this.arguments = new HashMap<>(arguments);
            this.purpose = "Tool execution";
        }

        // Default constructor
        public ToolCommand() {
            this.toolName = "";
            this.arguments = new HashMap<>();
            this.purpose = "";
        }

        // Getters
        public String getToolName() { return toolName; }
        public Map<String, Object> getArguments() { return Collections.unmodifiableMap(arguments); }
        public String getPurpose() { return purpose; }

        // Backward compatibility setters
        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public void setToolName(String toolName) {
            // Cannot modify final field, this is for backward compatibility only
        }

        public void setArguments(Map<String, Object> arguments) {
            this.arguments.clear();
            this.arguments.putAll(arguments);
        }
    }

    /**
     * Review result from the review agent.
     */
    public static class ReviewResult {
        private boolean approved;
        private double score;  // 0.0 to 100.0
        private final List<String> improvements;
        private final Map<String, Double> criteriaScores;
        private final List<String> strengths;
        private String recommendation;

        public ReviewResult(boolean approved, double score, List<String> improvements) {
            this.approved = approved;
            this.score = score;
            this.improvements = new ArrayList<>(improvements);
            this.criteriaScores = new HashMap<>();
            this.strengths = new ArrayList<>();
            this.recommendation = "";
        }

        // Default constructor for backward compatibility
        public ReviewResult() {
            this.approved = false;
            this.score = 0.0;
            this.improvements = new ArrayList<>();
            this.criteriaScores = new HashMap<>();
            this.strengths = new ArrayList<>();
            this.recommendation = "";
        }

        public void addCriteriaScore(String criteria, double score) {
            criteriaScores.put(criteria, score);
        }

        // Getters
        public boolean isApproved() { return approved; }
        public double getScore() { return score; }
        public List<String> getImprovements() { return Collections.unmodifiableList(improvements); }
        public List<String> getStrengths() { return Collections.unmodifiableList(strengths); }
        public String getRecommendation() { return recommendation; }
        public Map<String, Double> getCriteriaScores() { return Collections.unmodifiableMap(criteriaScores); }
        public Map<String, Double> getCriteria() { return Collections.unmodifiableMap(criteriaScores); }

        // Backward compatibility setters
        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public void addImprovement(String improvement) {
            this.improvements.add(improvement);
        }

        public void setImprovements(List<String> improvements) {
            this.improvements.clear();
            this.improvements.addAll(improvements);
        }

        public void setStrengths(List<String> strengths) {
            this.strengths.clear();
            this.strengths.addAll(strengths);
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }

        public void setCriteria(Map<String, Double> criteria) {
            this.criteriaScores.clear();
            this.criteriaScores.putAll(criteria);
        }
    }

    // Main context getters and setters
    public String getOriginalUserRequest() { return originalUserRequest; }
    public DrawingState getCurrentState() { return currentState; }
    public DrawingInterpretation getInterpretation() { return interpretation; }
    public void setInterpretation(DrawingInterpretation interpretation) { this.interpretation = interpretation; }
    public DrawingPlan getDrawingPlan() { return drawingPlan; }
    public void setDrawingPlan(DrawingPlan drawingPlan) { this.drawingPlan = drawingPlan; }
    public ReviewResult getReviewResult() { return reviewResult; }
    public void setReviewResult(ReviewResult reviewResult) { this.reviewResult = reviewResult; }
    public int getCurrentIteration() { return currentIteration; }
    public void incrementIteration() { this.currentIteration++; }
    public int getMaxIterations() { return maxIterations; }
    public boolean hasMoreIterations() { return currentIteration < maxIterations; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Create a copy of the context for the next iteration.
     */
    public AgentContext copyForNextIteration() {
        AgentContext newContext = new AgentContext(originalUserRequest, currentState, maxIterations);
        newContext.interpretation = this.interpretation;
        newContext.drawingPlan = this.drawingPlan;
        newContext.reviewResult = this.reviewResult;
        newContext.currentIteration = this.currentIteration + 1;
        newContext.metadata.putAll(this.metadata);
        return newContext;
    }
}