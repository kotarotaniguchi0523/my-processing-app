package com.example.presentation;

import processing.core.PApplet;

/**
 * ThemeManager handles color themes and styling for the UI.
 * Manages dark/light mode switching and provides color constants.
 */
public class ThemeManager {
    
    private final PApplet sketch;
    private boolean darkMode = false;
    
    // Theme colors
    private int colorBgStart;
    private int colorBgEnd;
    private int colorSurface;
    private int colorSurfaceMuted;
    private int colorBorder;
    private int colorTextPrimary;
    private int colorTextSecondary;
    private int colorAccent;
    private int colorUserBubble;
    private int colorAssistantBubble;
    private int colorSystemBubble;
    
    public ThemeManager(PApplet sketch) {
        this.sketch = sketch;
        applyTheme();
    }
    
    public void toggleTheme() {
        darkMode = !darkMode;
        applyTheme();
    }
    
    public boolean isDarkMode() {
        return darkMode;
    }
    
    private void applyTheme() {
        if (darkMode) {
            colorBgStart = sketch.color(18, 22, 28);
            colorBgEnd = sketch.color(28, 32, 38);
            colorSurface = sketch.color(34, 40, 48);
            colorSurfaceMuted = sketch.color(40, 46, 54);
            colorBorder = sketch.color(70, 80, 92);
            colorTextPrimary = sketch.color(235);
            colorTextSecondary = sketch.color(190);
            colorAccent = sketch.color(88, 142, 255);
            colorUserBubble = sketch.color(70, 74, 120);
            colorAssistantBubble = sketch.color(248);
            colorSystemBubble = sketch.color(210);
        } else {
            colorBgStart = sketch.color(245);
            colorBgEnd = sketch.color(232, 240, 255);
            colorSurface = sketch.color(255);
            colorSurfaceMuted = sketch.color(248, 249, 252);
            colorBorder = sketch.color(210);
            colorTextPrimary = sketch.color(28);
            colorTextSecondary = sketch.color(90);
            colorAccent = sketch.color(60, 110, 255);
            colorUserBubble = sketch.color(60, 60, 60);
            colorAssistantBubble = sketch.color(255);
            colorSystemBubble = sketch.color(230);
        }
    }
    
    // Getters for all colors
    public int getColorBgStart() { return colorBgStart; }
    public int getColorBgEnd() { return colorBgEnd; }
    public int getColorSurface() { return colorSurface; }
    public int getColorSurfaceMuted() { return colorSurfaceMuted; }
    public int getColorBorder() { return colorBorder; }
    public int getColorTextPrimary() { return colorTextPrimary; }
    public int getColorTextSecondary() { return colorTextSecondary; }
    public int getColorAccent() { return colorAccent; }
    public int getColorUserBubble() { return colorUserBubble; }
    public int getColorAssistantBubble() { return colorAssistantBubble; }
    public int getColorSystemBubble() { return colorSystemBubble; }
}