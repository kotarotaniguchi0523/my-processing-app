package com.example.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Tool;
import io.github.cdimascio.dotenv.Dotenv;

import com.google.common.util.concurrent.RateLimiter;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin wrapper around Google Gen AI Java SDK Client for Gemini model calls. Responsibility: hold a
 * singleton client instance & provide simple text generation helper. Keeps external dependency
 * localized to infrastructure layer.
 */
public class GeminiClient {
  private static final Logger LOGGER = Logger.getLogger(GeminiClient.class.getName());
  public static final String DEFAULT_MODEL = "gemini-2.5-flash"; // faster and more available model

  private static volatile GeminiClient instance;
  private static final Dotenv dotenv = loadDotenv();
  private final Client client;
  private final String model;
  private final String fallbackModel;
  // Rate limiting & concurrency control
  private final RateLimiter rateLimiter;
  private final Semaphore concurrentPermits;
  private final ExecutorService executor;
  // Retry & timeout settings
  private final int maxRetries;
  private final long initialBackoffMs;
  private final long maxBackoffMs;
  private final long totalTimeoutMs;
  private static final SecureRandom RNG = new SecureRandom();

  private static Dotenv loadDotenv() {
    try {
      return Dotenv.configure().ignoreIfMissing().load();
    } catch (Exception e) {
      LOGGER.warning("Failed to load .env file: " + e.getMessage());
      return null;
    }
  }

  private GeminiClient(String model) {
    // Try to get API key from .env file first
    String apiKey = getApiKeyFromDotenv();

    if (apiKey != null && !apiKey.isEmpty()) {
      // Use Builder pattern with explicit API key from .env
      this.client = Client.builder().apiKey(apiKey).build();
      LOGGER.info("Client initialized with API key from .env file");
    } else {
      // Fall back to default constructor (will look for GOOGLE_API_KEY env var)
      this.client = new Client();
      LOGGER.info("Client initialized with environment variable");
    }

    // Model selection with env override
    String modelOverride = getEnv("GEMINI_MODEL", null);
    this.model = modelOverride != null && !modelOverride.isBlank() ? modelOverride
        : (model != null ? model : DEFAULT_MODEL);
    this.fallbackModel = getEnv("GEMINI_FALLBACK_MODEL", "gemini-1.5-flash-8b");

    // Rate/Concurrency from env (defaults conservative)
    double qps = getDoubleEnv("GEMINI_RATE_QPS", 0.8d); // 0.8 req/sec default
    int concurrency = getIntEnv("GEMINI_MAX_CONCURRENCY", 1); // single-flight default
    this.rateLimiter = RateLimiter.create(Math.max(0.1d, qps));
    this.concurrentPermits = new Semaphore(Math.max(1, concurrency));

    // Retry/Timeout
    this.maxRetries = getIntEnv("GEMINI_MAX_ATTEMPTS", 5);
    this.initialBackoffMs = getLongEnv("GEMINI_BACKOFF_INITIAL_MS", 3000L);
    this.maxBackoffMs = getLongEnv("GEMINI_BACKOFF_MAX_MS", 30000L);
    this.totalTimeoutMs = getLongEnv("GEMINI_TOTAL_TIMEOUT_MS", 60000L);

    // Small bounded pool; concurrency is also enforced by semaphore
    int poolSize = Math.max(1, Math.min(4, concurrency));
    this.executor = Executors.newFixedThreadPool(poolSize, r -> {
      Thread t = new Thread(r, "GeminiClient-Worker");
      t.setDaemon(true);
      return t;
    });

    LOGGER.info(() -> String.format(Locale.ROOT,
        "GeminiClient initialized model=%s, qps=%.2f, concurrency=%d, maxRetries=%d", this.model,
        qps, concurrency, this.maxRetries));
  }

  private static String getApiKeyFromDotenv() {
    if (dotenv != null) {
      // Try GEMINI_API_KEY first
      String apiKey = dotenv.get("GEMINI_API_KEY");
      if (apiKey != null && !apiKey.isEmpty()) {
        LOGGER.info("API key found in .env file (GEMINI_API_KEY)");
        return apiKey;
      }

      // Then try GOOGLE_API_KEY
      apiKey = dotenv.get("GOOGLE_API_KEY");
      if (apiKey != null && !apiKey.isEmpty()) {
        LOGGER.info("API key found in .env file (GOOGLE_API_KEY)");
        return apiKey;
      }
    }
    return null;
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

    // Basic prompt size guard (character-based)
    int maxChars = getIntEnv("GEMINI_MAX_PROMPT_CHARS", 8000);
    String safePrompt = prompt.length() > maxChars ? prompt.substring(0, maxChars) : prompt;

    long started = System.currentTimeMillis();
    long backoff = initialBackoffMs;

    // Acquire rate and concurrency controls
    rateLimiter.acquire();
    boolean acquired = false;
    try {
      if (!concurrentPermits.tryAcquire()) {
        // Respect backpressure: block briefly then acquire
        concurrentPermits.acquire();
        acquired = true;
      } else {
        acquired = true;
      }

      for (int attempt = 0; attempt < maxRetries; attempt++) {
        if ((System.currentTimeMillis() - started) > totalTimeoutMs) {
          LOGGER.log(Level.WARNING, "Gemini call aborted due to total timeout");
          return Optional.empty();
        }
        try {
          GenerateContentResponse response = client.models.generateContent(model, safePrompt, null);
          String text = response.text();
          if (text == null || text.isBlank()) {
            return Optional.empty();
          }
          return Optional.of(text.trim());
        } catch (Exception e) {
          String msg = e.getMessage() == null ? "" : e.getMessage();
          boolean retriable = isRetriable(msg);
          if (retriable && attempt < maxRetries - 1) {
            long jitter = (long) (backoff * (0.2 + 0.8 * RNG.nextDouble()));
            long sleepMs = Math.min(maxBackoffMs, backoff + jitter);
            String logMsg = String.format(Locale.ROOT,
                "Gemini overloaded/ratelimited (attempt %d/%d). Retry in %d ms...", attempt + 1,
                maxRetries, sleepMs);
            LOGGER.log(Level.INFO, logMsg);
            try {
              Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              LOGGER.log(Level.WARNING, "Retry interrupted", ie);
              return Optional.empty();
            }
            backoff = Math.min(maxBackoffMs, backoff * 2);
            // Re-acquire rate limiter for next attempt
            rateLimiter.acquire();
            continue;
          }
          // Final attempt failed with retriable error: try fallback model once
          if (retriable && attempt == maxRetries - 1 && fallbackModel != null
              && !fallbackModel.isBlank() && !fallbackModel.equals(model)) {
            try {
              LOGGER.log(Level.INFO, () -> "Trying fallback model: " + fallbackModel);
              rateLimiter.acquire(); // respect rate limit for fallback too
              GenerateContentResponse fb =
                  client.models.generateContent(fallbackModel, safePrompt, null);
              String text = fb.text();
              if (text != null && !text.isBlank()) {
                return Optional.of(text.trim());
              }
            } catch (Exception fbEx) {
              LOGGER.log(Level.WARNING, "Fallback model failed: " + fbEx.getMessage(), fbEx);
            }
          }
          LOGGER.log(Level.WARNING, "Gemini generateText failed: " + msg, e);
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.WARNING, "Interrupted while acquiring concurrency permit", ie);
      return Optional.empty();
    } finally {
      if (acquired) {
        concurrentPermits.release();
      }
    }
  }

  /**
   * Generate content with function calling support.
   * 
   * @param prompt user prompt
   * @param config GenerateContentConfig with tools
   * @return response with potential function calls
   */
  public Optional<GenerateContentResponse> generateContentWithTools(String prompt, GenerateContentConfig config) {
    Objects.requireNonNull(prompt, "prompt");
    Objects.requireNonNull(config, "config");
    
    // Basic prompt size guard
    int maxChars = getIntEnv("GEMINI_MAX_PROMPT_CHARS", 8000);
    String safePrompt = prompt.length() > maxChars ? prompt.substring(0, maxChars) : prompt;
    
    long started = System.currentTimeMillis();
    long backoff = initialBackoffMs;
    
    // Acquire rate and concurrency controls
    rateLimiter.acquire();
    boolean acquired = false;
    try {
      if (!concurrentPermits.tryAcquire()) {
        concurrentPermits.acquire();
        acquired = true;
      } else {
        acquired = true;
      }
      
      for (int attempt = 0; attempt < maxRetries; attempt++) {
        if ((System.currentTimeMillis() - started) > totalTimeoutMs) {
          LOGGER.log(Level.WARNING, "Gemini call aborted due to total timeout");
          return Optional.empty();
        }
        try {
          GenerateContentResponse response = client.models.generateContent(model, safePrompt, config);
          return Optional.of(response);
        } catch (Exception e) {
          String msg = e.getMessage() == null ? "" : e.getMessage();
          boolean retriable = isRetriable(msg);
          if (retriable && attempt < maxRetries - 1) {
            long jitter = (long) (backoff * (0.2 + 0.8 * RNG.nextDouble()));
            long sleepMs = Math.min(maxBackoffMs, backoff + jitter);
            LOGGER.log(Level.INFO,
                String.format(Locale.ROOT,
                    "Gemini overloaded/ratelimited (attempt %d/%d). Retry in %d ms...",
                    attempt + 1, maxRetries, sleepMs));
            try {
              Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              LOGGER.log(Level.WARNING, "Retry interrupted", ie);
              return Optional.empty();
            }
            backoff = Math.min(maxBackoffMs, backoff * 2);
            rateLimiter.acquire();
            continue;
          }
          LOGGER.log(Level.WARNING, "Gemini generateContent with tools failed: " + msg, e);
          return Optional.empty();
        }
      }
      return Optional.empty();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      LOGGER.log(Level.WARNING, "Interrupted while acquiring concurrency permit", ie);
      return Optional.empty();
    } finally {
      if (acquired) {
        concurrentPermits.release();
      }
    }
  }

  /** Asynchronous helper using internal bounded executor. */
  public CompletableFuture<Optional<String>> generateTextAsync(String prompt) {
    return CompletableFuture.supplyAsync(() -> generateText(prompt), executor);
  }

  // --- Helpers ---
  private static boolean isRetriable(String msg) {
    if (msg == null)
      return false;
    String m = msg.toLowerCase(Locale.ROOT);
    return m.contains("503") || m.contains("429") || m.contains("overload") || m.contains("rate");
  }

  private static String getEnv(String key, String def) {
    String v = null;
    if (dotenv != null) {
      try {
        v = dotenv.get(key);
      } catch (Exception ignored) {
      }
    }
    if (v == null || v.isBlank()) {
      v = System.getenv(key);
    }
    return (v == null || v.isBlank()) ? def : v;
  }

  private static int getIntEnv(String key, int def) {
    try {
      String v = getEnv(key, null);
      return v == null ? def : Integer.parseInt(v.trim());
    } catch (Exception ignored) {
      return def;
    }
  }

  private static long getLongEnv(String key, long def) {
    try {
      String v = getEnv(key, null);
      return v == null ? def : Long.parseLong(v.trim());
    } catch (Exception ignored) {
      return def;
    }
  }

  private static double getDoubleEnv(String key, double def) {
    try {
      String v = getEnv(key, null);
      return v == null ? def : Double.parseDouble(v.trim());
    } catch (Exception ignored) {
      return def;
    }
  }
}
