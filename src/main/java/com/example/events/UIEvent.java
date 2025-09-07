package com.example.events;

/** Represents UI-related events such as mouse clicks, key presses, and user interactions. */
public class UIEvent extends Event {
    private final int mouseX;
    private final int mouseY;
    private final char keyPressed;
    private final String inputText;
    private final String targetComponent;

    // Constructor for mouse events
    public UIEvent(EventType type, int mouseX, int mouseY, String targetComponent) {
        super(type);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.keyPressed = 0;
        this.inputText = null;
        this.targetComponent = targetComponent;
    }

    // Constructor for key events
    public UIEvent(EventType type, char keyPressed, String targetComponent) {
        super(type);
        this.mouseX = -1;
        this.mouseY = -1;
        this.keyPressed = keyPressed;
        this.inputText = null;
        this.targetComponent = targetComponent;
    }

    // Constructor for text input events
    public UIEvent(EventType type, String inputText, String targetComponent) {
        super(type);
        this.mouseX = -1;
        this.mouseY = -1;
        this.keyPressed = 0;
        this.inputText = inputText;
        this.targetComponent = targetComponent;
    }

    // Constructor for simple UI events (no input data)
    public UIEvent(EventType type, String targetComponent) {
        super(type);
        this.mouseX = -1;
        this.mouseY = -1;
        this.keyPressed = 0;
        this.inputText = null;
        this.targetComponent = targetComponent;
    }

    // Getters
    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public char getKeyPressed() {
        return keyPressed;
    }

    public String getInputText() {
        return inputText;
    }

    public String getTargetComponent() {
        return targetComponent;
    }

    // Helper methods
    public boolean hasMousePosition() {
        return mouseX >= 0 && mouseY >= 0;
    }

    public boolean hasKeyPress() {
        return keyPressed != 0;
    }

    public boolean hasInputText() {
        return inputText != null && !inputText.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("UIEvent[type=%s, target=%s", getType(), targetComponent));

        if (hasMousePosition()) {
            sb.append(String.format(", mouse=(%d,%d)", mouseX, mouseY));
        }

        if (hasKeyPress()) {
            sb.append(String.format(", key='%c'", keyPressed));
        }

        if (hasInputText()) {
            sb.append(String.format(", text='%s'", inputText));
        }

        sb.append("]");
        return sb.toString();
    }
}
