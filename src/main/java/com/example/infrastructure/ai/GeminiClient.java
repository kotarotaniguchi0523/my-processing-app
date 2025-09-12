package com.example.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin wrapper around Google Gen AI Java SDK Client for Gemini model calls. Responsibility: hold a
 * singleton client instance & provide simple text generation helper. Keeps external dependency
 * localized to infrastructure layer.
 */
public class GeminiClient {
  private static final Logger LOGGER = Logger.getLogger(GeminiClient.class.getName());
  public static final String DEFAULT_MODEL = "gemini-2.5-pro"; // fast + tool-calling capable

  private static volatile GeminiClient instance;
  private final Client client;
  private final String model;

  private GeminiClient(String model) {
    // If GOOGLE_API_KEY env var set, new Client() will pick it; otherwise user must configure.
    this.client = new Client();
    this.model = model != null ? model : DEFAULT_MODEL;
    LOGGER.info(() -> "GeminiClient initialized with model=" + this.model);
  }

  public static GeminiClient getInstance() {
    if (instance == null) {
      synchronized (GeminiClient.class) {
        if (instance == null) {
          instance = new GeminiClient(DEFAULT_MODEL);
        }
      }
    }
    return instance;
  }

  public static GeminiClient getInstance(String model) {
    if (instance == null) {
      synchronized (GeminiClient.class) {
        if (instance == null) {
          instance = new GeminiClient(model);
        }
      }
    }
    return instance;
  }

  /**
   * Simple text prompt generation. No tools, no multimodal.
   *
   * @param prompt user/system aggregated prompt
   * @return response text or empty if failure
   */
  public Optional<String> generateText(String prompt) {
    Objects.requireNonNull(prompt, "prompt");
    try {
      GenerateContentResponse response = client.models.generateContent(model, prompt, null);
      String text = response.text();
      if (text == null || text.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(text.trim());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Gemini generateText failed: " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
