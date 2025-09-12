package com.example.presentation;

import processing.core.PApplet;

import com.example.application.services.AIAgentService;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InputManager handles Japanese IME input integration and text input management. This class follows
 * the Single Responsibility Principle by focusing solely on input handling.
 */
public class InputManager {
  private static final Logger LOGGER = Logger.getLogger(InputManager.class.getName());
  private static final int MARGIN = 10;
  private static final int INNER_GAP_SMALL = 5;
  private static final int INPUT_HEIGHT = 30;
  private static final int CHAT_INPUT_RIGHT_GAP = 70;

  private final PApplet sketch;
  private final ChatManager chatManager;
  private final AIAgentService aiAgentService;

  // Japanese IME Support
  private JTextField japaneseInputField;
  private boolean useJapaneseInput;

  public InputManager(PApplet sketch, ChatManager chatManager, AIAgentService aiAgentService) {
    this.sketch = sketch;
    this.chatManager = chatManager;
    this.aiAgentService = aiAgentService;
    setupJapaneseInput();
  }

  @SuppressWarnings("PMD.UnusedAssignment")
  private void setupJapaneseInput() {
    SwingUtilities.invokeLater(() -> {
      try {
        // Create Japanese input field
        japaneseInputField = new JTextField();
        // Match in-canvas font size for consistency
        japaneseInputField.setFont(new Font("Meiryo UI", Font.PLAIN, 16));

        // Add action listener for Enter key
        japaneseInputField.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            String text = japaneseInputField.getText();
            if (!text.isBlank()) {
              chatManager.addMessage("User: " + text);
              japaneseInputField.setText("");
              chatManager.setCurrentInput("");
              // Call AI asynchronously to avoid blocking EDT
              new Thread(() -> {
                try {
                  AIAgentService.AgentReply reply = aiAgentService.handleUserPrompt(text);
                  SwingUtilities.invokeLater(() -> {
                    chatManager.addMessage("Assistant: " + reply.message);
                  });
                } catch (Exception ex) {
                  SwingUtilities
                      .invokeLater(() -> chatManager.addMessage("Assistant: (エラーが発生しました)"));
                }
              }, "AI-Call-JPInput").start();
            }
          }
        });

        // Add focus listener
        japaneseInputField.addFocusListener(new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent e) {
            chatManager.setInputFocused(true);
            useJapaneseInput = true;
          }

          @Override
          public void focusLost(FocusEvent e) {
            // Keep focus status for a moment
            SwingUtilities.invokeLater(() -> {
              if (!japaneseInputField.hasFocus()) {
                useJapaneseInput = false;
                chatManager.setInputFocused(false);
              }
            });
          }
        });

        // Add key listener to sync text with currentInput
        japaneseInputField.addKeyListener(new java.awt.event.KeyAdapter() {
          @Override
          public void keyReleased(java.awt.event.KeyEvent e) {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
              chatManager.setCurrentInput(japaneseInputField.getText());
            }
          }
        });

        // Get the Processing window's frame
        Object nativeWindow = sketch.getSurface().getNative();
        javax.swing.JFrame frame = null;

        // Try to get the parent frame
        if (nativeWindow instanceof java.awt.Component) {
          java.awt.Component comp = (java.awt.Component) nativeWindow;
          java.awt.Window window = SwingUtilities.getWindowAncestor(comp);
          if (window instanceof javax.swing.JFrame) {
            frame = (javax.swing.JFrame) window;
          }
        }

        if (frame != null) {
          // Use absolute positioning
          frame.setLayout(null);

          // Place the input field (initial position - will be updated later)
          updateJapaneseInputPosition();
          frame.add(japaneseInputField);

          // Initially hide the field - will show when user clicks on input area
          japaneseInputField.setVisible(false);
          chatManager.setJapaneseInputVisible(false);
        } else {
          if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Could not find JFrame for Japanese input field");
          }
          useJapaneseInput = false;
        }
      } catch (Exception e) {
        if (LOGGER.isLoggable(Level.SEVERE)) {
          LOGGER.log(Level.SEVERE, "Failed to setup Japanese input field: " + e.getMessage(), e);
        }
        useJapaneseInput = false;
      }
    });
  }

  public void updateJapaneseInputPosition() {
    if (japaneseInputField != null) {
      SwingUtilities.invokeLater(() -> {
        // This would need the actual chatInputY from SketchUIRenderer
        int chatInputY = getChatInputYFromRenderer();
        japaneseInputField.setBounds(MARGIN + INNER_GAP_SMALL, chatInputY,
            sketch.width - MARGIN * 2 - CHAT_INPUT_RIGHT_GAP, INPUT_HEIGHT);
      });
    }
  }

  public void showJapaneseInput() {
    if (japaneseInputField != null) {
      SwingUtilities.invokeLater(() -> {
        japaneseInputField.setVisible(true);
        japaneseInputField.requestFocus();
        japaneseInputField.setText(chatManager.getCurrentInput());
        chatManager.setJapaneseInputVisible(true);
      });
    }
  }

  public void hideJapaneseInput() {
    if (japaneseInputField != null && useJapaneseInput) {
      SwingUtilities.invokeLater(() -> {
        japaneseInputField.setVisible(false);
        chatManager.setJapaneseInputVisible(false);
      });
    }
  }

  // This is a temporary method - would need proper integration with SketchUIRenderer
  private int getChatInputYFromRenderer() {
    // Mock calculation for now - in real implementation this would come from SketchUIRenderer
    int canvasAreaHeight = (int) ((sketch.height - MARGIN * 3) * 50 / 100.0);
    int chatAreaY = MARGIN + canvasAreaHeight + MARGIN;
    int chatAreaHeight = (int) ((sketch.height - MARGIN * 3) * 40 / 100.0);
    return chatAreaY + chatAreaHeight - INPUT_HEIGHT - MARGIN;
  }

  // Getters
  public boolean isUsingJapaneseInput() {
    return useJapaneseInput;
  }

  public boolean isJapaneseInputVisible() {
    return japaneseInputField != null && japaneseInputField.isVisible();
  }

}
