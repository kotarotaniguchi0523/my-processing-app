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
  // Layout constants are now sourced from SketchUIRenderer

  private final PApplet sketch;
  private final ChatManager chatManager;
  private final AIAgentService aiAgentService;
  private final SketchUIRenderer uiRenderer;

  // Japanese IME Support
  private JTextField japaneseInputField;
  private boolean useJapaneseInput;

  public InputManager(PApplet sketch, ChatManager chatManager, AIAgentService aiAgentService,
      SketchUIRenderer uiRenderer) {
    this.sketch = sketch;
    this.chatManager = chatManager;
    this.aiAgentService = aiAgentService;
    this.uiRenderer = uiRenderer;
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
              AiChatHelper.sendAndAppendAsync(aiAgentService, chatManager, text,
                  () -> chatManager.flushQueueToUI());
              japaneseInputField.setText("");
              chatManager.setCurrentInput("");
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
        int chatInputY = uiRenderer.getChatInputY();
        int chatInputLeft = uiRenderer.getChatInputLeft();
        int chatInputWidth = uiRenderer.getChatInputWidth();
        int inputHeight = uiRenderer.getInputHeight();
        japaneseInputField.setBounds(chatInputLeft, chatInputY, chatInputWidth, inputHeight);
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

  // No longer needed heuristic; we now rely on uiRenderer for exact layout

  // Getters
  public boolean isUsingJapaneseInput() {
    return useJapaneseInput;
  }

  public boolean isJapaneseInputVisible() {
    return japaneseInputField != null && japaneseInputField.isVisible();
  }

}
