package com.example.services;

import com.example.domain.models.DrawingState;
import java.io.IOException;
import java.util.List;

/**
 * Interface for persisting and loading drawing states. Provides abstraction for different storage
 * implementations (file system, database, etc.).
 */
public interface IStateRepository {

  /**
   * Saves a drawing state with the specified name.
   *
   * @param name the name to save the state under
   * @param state the state to save
   * @throws IOException if an error occurs during save
   * @throws IllegalArgumentException if name or state is null or invalid
   */
  void saveState(String name, DrawingState state) throws IOException;

  /**
   * Loads a drawing state by name.
   *
   * @param name the name of the state to load
   * @return the loaded drawing state
   * @throws IOException if an error occurs during load or state is not found
   * @throws IllegalArgumentException if name is null or empty
   */
  DrawingState loadState(String name) throws IOException;

  /**
   * Checks if a state with the specified name exists.
   *
   * @param name the name to check
   * @return true if the state exists, false otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean stateExists(String name);

  /**
   * Deletes a state with the specified name.
   *
   * @param name the name of the state to delete
   * @return true if the state was deleted, false if it didn't exist
   * @throws IOException if an error occurs during deletion
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean deleteState(String name) throws IOException;

  /**
   * Lists all available state names.
   *
   * @return a list of state names
   * @throws IOException if an error occurs while listing states
   */
  List<String> listStateNames() throws IOException;

  /**
   * Gets metadata about a saved state (timestamp, size, etc.).
   *
   * @param name the name of the state
   * @return metadata about the state
   * @throws IOException if an error occurs or state doesn't exist
   * @throws IllegalArgumentException if name is null or empty
   */
  StateMetadata getStateMetadata(String name) throws IOException;

  /**
   * Saves the entire session history.
   *
   * @param sessionName the name of the session
   * @param history the list of states representing the session history
   * @param currentIndex the current position in the history
   * @throws IOException if an error occurs during save
   * @throws IllegalArgumentException if parameters are invalid
   */
  void saveSession(String sessionName, List<DrawingState> history, int currentIndex)
      throws IOException;

  /**
   * Loads a session history.
   *
   * @param sessionName the name of the session to load
   * @return the session data
   * @throws IOException if an error occurs during load or session is not found
   * @throws IllegalArgumentException if sessionName is null or empty
   */
  SessionData loadSession(String sessionName) throws IOException;

  /**
   * Lists all available session names.
   *
   * @return a list of session names
   * @throws IOException if an error occurs while listing sessions
   */
  List<String> listSessionNames() throws IOException;

  /**
   * Deletes a session with the specified name.
   *
   * @param sessionName the name of the session to delete
   * @return true if the session was deleted, false if it didn't exist
   * @throws IOException if an error occurs during deletion
   * @throws IllegalArgumentException if sessionName is null or empty
   */
  boolean deleteSession(String sessionName) throws IOException;

  /**
   * Cleans up old or temporary files.
   *
   * @throws IOException if an error occurs during cleanup
   */
  void cleanup() throws IOException;

  /**
   * Validates the repository storage (checks permissions, creates directories, etc.).
   *
   * @throws IOException if the repository cannot be properly initialized
   */
  void initialize() throws IOException;

  /** Data class for state metadata. */
  class StateMetadata {
    private final String name;
    private final long timestamp;
    private final long fileSize;
    private final int shapeCount;
    private final String description;

    public StateMetadata(
        String name, long timestamp, long fileSize, int shapeCount, String description) {
      this.name = name;
      this.timestamp = timestamp;
      this.fileSize = fileSize;
      this.shapeCount = shapeCount;
      this.description = description != null ? description : "";
    }

    public String getName() {
      return name;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public long getFileSize() {
      return fileSize;
    }

    public int getShapeCount() {
      return shapeCount;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return String.format(
          "StateMetadata{name='%s', timestamp=%d, fileSize=%d, shapes=%d}",
          name, timestamp, fileSize, shapeCount);
    }
  }

  /** Data class for session data. */
  class SessionData {
    private final List<DrawingState> history;
    private final int currentIndex;
    private final String sessionName;
    private final long timestamp;

    public SessionData(
        String sessionName, List<DrawingState> history, int currentIndex, long timestamp) {
      this.sessionName = sessionName;
      this.history = history;
      this.currentIndex = currentIndex;
      this.timestamp = timestamp;
    }

    public List<DrawingState> getHistory() {
      return history;
    }

    public int getCurrentIndex() {
      return currentIndex;
    }

    public String getSessionName() {
      return sessionName;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String toString() {
      return String.format(
          "SessionData{name='%s', history=%d states, index=%d, timestamp=%d}",
          sessionName, history.size(), currentIndex, timestamp);
    }
  }
}
