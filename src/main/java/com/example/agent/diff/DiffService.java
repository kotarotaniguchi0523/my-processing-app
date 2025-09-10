package com.example.agent.diff;

import com.example.domain.models.DrawingState;
import com.example.engine.YamlParser;
import com.example.engine.YamlParser.YamlParseException;

import java.util.Arrays;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.UnifiedDiffUtils;

/**
 * DiffService creates human-readable diffs between two DrawingState instances.
 * Uses java-diff-utils to generate a unified diff from YAML representations.
 */
public class DiffService {

    private final YamlParser yamlParser;

    public DiffService() {
        this.yamlParser = new YamlParser();
    }

    /** Result of a diff operation */
    public static class DiffResult {
        public final String summary;
        public final String unifiedDiff;

        public DiffResult(String summary, String unifiedDiff) {
            this.summary = summary;
            this.unifiedDiff = unifiedDiff;
        }
    }

    /**
     * Compute a unified diff between two states with a simple summary.
     */
    public DiffResult diff(DrawingState oldState, DrawingState newState) throws YamlParseException {
        String oldYaml = yamlParser.toYaml(oldState);
        String newYaml = yamlParser.toYaml(newState);

        List<String> original = Arrays.asList(oldYaml.split("\n"));
        List<String> revised = Arrays.asList(newYaml.split("\n"));

        Patch<String> patch = DiffUtils.diff(original, revised);
        List<String> unified = UnifiedDiffUtils.generateUnifiedDiff(
                "old_state.yaml", "new_state.yaml", original, patch, 3);

        String summary = buildSummary(oldState, newState, patch);
        String unifiedText = String.join("\n", unified);
        return new DiffResult(summary, unifiedText);
    }

    private String buildSummary(DrawingState oldState, DrawingState newState, Patch<String> patch) {
        int oldCount = oldState.getShapes().size();
        int newCount = newState.getShapes().size();
        int delta = newCount - oldCount;

        StringBuilder sb = new StringBuilder();
        sb.append("変更サマリー: 図形 ").append(oldCount).append(" → ").append(newCount);
        if (delta > 0) sb.append(" (＋").append(delta).append(")");
        else if (delta < 0) sb.append(" (－").append(Math.abs(delta)).append(")");

        int added = 0, removed = 0;
        // Rough estimation from patch deltas
        for (var deltaChunk : patch.getDeltas()) {
            switch (deltaChunk.getType()) {
                case INSERT:
                    added++;
                    break;
                case DELETE:
                    removed++;
                    break;
                default:
                    break;
            }
        }
        sb.append(" / 差分: +").append(added).append(", -").append(removed);
        return sb.toString();
    }
}
