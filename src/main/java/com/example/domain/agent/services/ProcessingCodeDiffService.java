package com.example.domain.agent.services;

import com.example.domain.models.*;
import com.example.domain.services.IDiffService;
import com.example.domain.values.Color;
import com.example.domain.values.Position;
import com.example.domain.values.Style;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Processing code diffs for approval dialog
 */
public class ProcessingCodeDiffService implements IDiffService {
    
    /**
     * Generate a Processing code diff between two states
     */
    @Override
    public DiffResult generateDiff(DrawingState oldState, DrawingState newState) {
        List<String> oldCode = generateProcessingCode(oldState);
        List<String> newCode = generateProcessingCode(newState);
        
        Patch<String> patch = DiffUtils.diff(oldCode, newCode);
        List<String> unified = UnifiedDiffUtils.generateUnifiedDiff(
                "current_canvas.pde", "proposed_canvas.pde", oldCode, patch, 3);
        
        String summary = buildSummary(oldState, newState);
        String unifiedText = String.join("\n", unified);
        
        // Calculate change counts
        int added = Math.max(0, newState.getShapes().size() - oldState.getShapes().size());
        int removed = Math.max(0, oldState.getShapes().size() - newState.getShapes().size());
        int modified = 0; // Simplified - would need shape comparison for accuracy
        
        return new DiffResult(summary, unifiedText, added, removed, modified);
    }
    
    private List<String> generateProcessingCode(DrawingState state) {
        List<String> code = new ArrayList<>();
        
        // Add header comment
        code.add("// Processing code for canvas state");
        code.add("// " + state.getShapes().size() + " shapes");
        code.add("");
        
        // Generate code for each shape
        for (Shape shape : state.getShapes()) {
            if (shape == null || !shape.isVisible()) continue;
            
            code.add("// Shape: " + shape.getId());
            code.addAll(generateShapeCode(shape));
            code.add("");
        }
        
        return code;
    }
    
    private List<String> generateShapeCode(Shape shape) {
        List<String> code = new ArrayList<>();
        Style style = shape.getStyle();
        
        // Apply style
        if (style.hasFill()) {
            Color fill = style.getFillColor();
            code.add(String.format("fill(%d, %d, %d, %d);", 
                fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha()));
        } else {
            code.add("noFill();");
        }
        
        if (style.hasStroke()) {
            Color stroke = style.getStrokeColor();
            code.add(String.format("stroke(%d, %d, %d, %d);", 
                stroke.getRed(), stroke.getGreen(), stroke.getBlue(), stroke.getAlpha()));
            code.add(String.format("strokeWeight(%.1f);", style.getStrokeWeight()));
        } else {
            code.add("noStroke();");
        }
        
        // Draw shape based on type
        Position pos = shape.getPosition();
        
        if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            code.add(String.format("ellipse(%.1f, %.1f, %.1f, %.1f);", 
                pos.getX(), pos.getY(), 
                circle.getRadius() * 2, circle.getRadius() * 2));
                
        } else if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            code.add(String.format("rect(%.1f, %.1f, %.1f, %.1f);", 
                pos.getX(), pos.getY(), 
                rect.getWidth(), rect.getHeight()));
                
        } else if (shape instanceof Line) {
            Line line = (Line) shape;
            Position to = line.getEndPosition();
            code.add(String.format("line(%.1f, %.1f, %.1f, %.1f);", 
                pos.getX(), pos.getY(), to.getX(), to.getY()));
                
        } else if (shape instanceof Text) {
            Text text = (Text) shape;
            code.add(String.format("textAlign(%s, %s);", 
                text.getHorizontalAlign().toString(), 
                text.getVerticalAlign().toString()));
            code.add(String.format("textSize(%.1f);", text.getFontSize()));
            code.add(String.format("text(\"%s\", %.1f, %.1f);", 
                text.getContent().replace("\"", "\\\""), 
                pos.getX(), pos.getY()));
        }
        
        return code;
    }
    
    private String buildSummary(DrawingState oldState, DrawingState newState) {
        int oldCount = oldState.getShapes().size();
        int newCount = newState.getShapes().size();
        int delta = newCount - oldCount;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Changes: ");
        
        if (delta > 0) {
            sb.append(delta).append(" shape(s) added");
        } else if (delta < 0) {
            sb.append(Math.abs(delta)).append(" shape(s) removed");
        } else {
            sb.append("Shape properties modified");
        }
        
        // Describe what types of shapes are being added
        if (delta > 0) {
            List<Shape> newShapes = findNewShapes(oldState, newState);
            if (!newShapes.isEmpty()) {
                Map<Shape.ShapeType, Long> typeCounts = newShapes.stream()
                    .collect(Collectors.groupingBy(Shape::getType, Collectors.counting()));
                
                sb.append(" (");
                sb.append(typeCounts.entrySet().stream()
                    .map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", ")));
                sb.append(")");
            }
        }
        
        return sb.toString();
    }
    
    private List<Shape> findNewShapes(DrawingState oldState, DrawingState newState) {
        Set<String> oldIds = oldState.getShapes().stream()
            .map(Shape::getId)
            .collect(Collectors.toSet());
            
        return newState.getShapes().stream()
            .filter(s -> !oldIds.contains(s.getId()))
            .collect(Collectors.toList());
    }
}