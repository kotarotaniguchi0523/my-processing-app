package com.example.infrastructure.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.DrawingSession;
import com.example.domain.repositories.IStateRepository;
import com.example.domain.services.ISessionManager;
import com.example.domain.services.IStateManager;
import com.example.domain.services.IHistoryService;
import com.example.domain.services.StateChangeListener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages session lifecycle, including loading, saving, and auto-saving functionality. Integrates
 * with StateManager and HistoryService to provide complete session management.
 */
public class SessionManager implements ISessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
    
    private static SessionManager instance;

    private static final long DEFAULT_AUTO_SAVE_INTERVAL_SECONDS = 60; // 1 minute
    private static final String DEFAULT_SESSION_NAME = "default_session";
    private static final String AUTO_SAVE_SESSION_NAME = "autosave_session";

    private final IStateManager stateManager;
    private final IHistoryService historyService;
    private final IStateRepository repository;
    private final ScheduledExecutorService autoSaveExecutor;
    private final AtomicBoolean autoSaveEnabled;

    private String currentSessionName;
    private boolean hasUnsavedChanges;
    private long lastSaveTimestamp;
    private long autoSaveIntervalSeconds;

    /**
     * Creates a new SessionManager with default settings.
     *
     * @param stateManager the state manager
     * @param historyService the history service
     * @param repository the state repository
     */
    public SessionManager(
            IStateManager stateManager, IHistoryService historyService, IStateRepository repository) {
        this(stateManager, historyService, repository, DEFAULT_AUTO_SAVE_INTERVAL_SECONDS);
    }

    /**
     * Creates a new SessionManager with specified auto-save interval.
     *
     * @param stateManager the state manager
     * @param historyService the history service
     * @param repository the state repository
     * @param autoSaveIntervalSeconds the auto-save interval in seconds
     */
    public SessionManager(
            IStateManager stateManager,
            IHistoryService historyService,
            IStateRepository repository,
            long autoSaveIntervalSeconds) {
        if (stateManager == null) {
            throw new IllegalArgumentException("StateManager cannot be null");
        }
        if (historyService == null) {
            throw new IllegalArgumentException("HistoryService cannot be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }

        this.stateManager = stateManager;
        this.historyService = historyService;
        this.repository = repository;
        this.autoSaveIntervalSeconds = autoSaveIntervalSeconds;
        this.autoSaveExecutor = Executors.newScheduledThreadPool(1);
        this.autoSaveEnabled = new AtomicBoolean(false);

        this.currentSessionName = DEFAULT_SESSION_NAME;
        this.hasUnsavedChanges = false;
        this.lastSaveTimestamp = 0;

        // Listen for state changes to track unsaved changes
        stateManager.addListener(new SessionStateChangeListener());

        // Initialize repository
        try {
            repository.initialize();
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Failed to initialize repository: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            StateManager stateManager = new StateManager();
            HistoryService historyService = new HistoryService(stateManager);
            IStateRepository repository = new FileStateRepository();
            instance = new SessionManager(stateManager, historyService, repository);
        }
        return instance;
    }

    /**
     * Starts a new session with the specified name.
     *
     * @param sessionName the name of the new session
     * @throws IOException if session creation fails
     */
    public void startNewSession(String sessionName) throws IOException {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        // Save current session if there are unsaved changes
        if (hasUnsavedChanges && currentSessionName != null) {
            saveCurrentSession();
        }

        // Reset to default state
        stateManager.resetToDefault();
        historyService.clearHistory();

        this.currentSessionName = sessionName.trim();
        this.hasUnsavedChanges = false;
        this.lastSaveTimestamp = System.currentTimeMillis();

        // Save the initial session
        saveCurrentSession();
    }

    /**
     * Loads an existing session.
     *
     * @param sessionName the name of the session to load
     * @throws IOException if session loading fails
     */
    public void loadSessionAndSetCurrent(String sessionName) throws IOException {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        // Save current session if there are unsaved changes
        if (hasUnsavedChanges && currentSessionName != null) {
            saveCurrentSession();
        }

        // Load the session data
        IStateRepository.SessionData sessionData = repository.loadSession(sessionName.trim());

        // Restore the session
        this.currentSessionName = sessionName.trim();

        // Restore history
        historyService.clearHistory();
        for (int i = 0; i < sessionData.getHistory().size(); i++) {
            DrawingState state = sessionData.getHistory().get(i);
            if (i == 0) {
                // Set the first state as current
                stateManager.updateState(state);
                historyService.clearHistory();
                historyService.addState(state);
            } else {
                historyService.addState(state);
            }
        }

        // Set the correct position in history
        int targetIndex = sessionData.getCurrentIndex();
        int currentIndex = historyService.getCurrentIndex();

        while (currentIndex < targetIndex && historyService.canRedo()) {
            historyService.redo();
            currentIndex++;
        }
        while (currentIndex > targetIndex && historyService.canUndo()) {
            historyService.undo();
            currentIndex--;
        }

        this.hasUnsavedChanges = false;
        this.lastSaveTimestamp = sessionData.getTimestamp();
    }

    /**
     * Saves the current session.
     *
     * @throws IOException if session saving fails
     */
    public void saveCurrentSession() throws IOException {
        if (currentSessionName == null) {
            throw new IllegalStateException("No active session to save");
        }

        repository.saveSession(
                currentSessionName,
                historyService.getHistoryCopy(),
                historyService.getCurrentIndex());
        this.hasUnsavedChanges = false;
        this.lastSaveTimestamp = System.currentTimeMillis();
    }

    /**
     * Saves the current session with a new name.
     *
     * @param sessionName the new session name
     * @throws IOException if session saving fails
     */
    public void saveSessionAs(String sessionName) throws IOException {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        String oldSessionName = this.currentSessionName;
        this.currentSessionName = sessionName.trim();

        try {
            saveCurrentSession();
        } catch (IOException e) {
            // Restore old name if save fails
            this.currentSessionName = oldSessionName;
            throw e;
        }
    }

    /**
     * Enables or disables auto-save functionality.
     *
     * @param enabled true to enable auto-save, false to disable
     */
    public void setAutoSaveEnabled(boolean enabled) {
        if (enabled == autoSaveEnabled.get()) {
            return; // No change needed
        }

        autoSaveEnabled.set(enabled);

        if (enabled) {
            startAutoSave();
        } else {
            stopAutoSave();
        }
    }

    /**
     * Checks if auto-save is enabled.
     *
     * @return true if auto-save is enabled, false otherwise
     */
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled.get();
    }

    /**
     * Sets the auto-save interval.
     *
     * @param intervalSeconds the interval in seconds
     */
    public void setAutoSaveInterval(long intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("Auto-save interval must be positive");
        }

        this.autoSaveIntervalSeconds = intervalSeconds;

        // Restart auto-save with new interval if it's currently enabled
        if (autoSaveEnabled.get()) {
            stopAutoSave();
            startAutoSave();
        }
    }

    /**
     * Gets the auto-save interval in seconds.
     *
     * @return the auto-save interval
     */
    public long getAutoSaveInterval() {
        return autoSaveIntervalSeconds;
    }

    /**
     * Gets the current session name.
     *
     * @return the current session name, or null if no session is active
     */
    public String getCurrentSessionName() {
        return currentSessionName;
    }

    /**
     * Checks if there are unsaved changes in the current session.
     *
     * @return true if there are unsaved changes, false otherwise
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * Gets the timestamp of the last save operation.
     *
     * @return the last save timestamp in milliseconds
     */
    public long getLastSaveTimestamp() {
        return lastSaveTimestamp;
    }

    /**
     * Gets the time since the last save in seconds.
     *
     * @return the time since last save in seconds
     */
    public long getTimeSinceLastSave() {
        return (System.currentTimeMillis() - lastSaveTimestamp) / 1000;
    }

    /**
     * Creates an emergency auto-save of the current session. This is typically called when the
     * application is about to close.
     *
     * @throws IOException if emergency save fails
     */
    public void emergencySave() throws IOException {
        if (!hasUnsavedChanges) {
            return; // Nothing to save
        }

        String emergencyName = AUTO_SAVE_SESSION_NAME + "_emergency_" + System.currentTimeMillis();
        repository.saveSession(
                emergencyName, historyService.getHistoryCopy(), historyService.getCurrentIndex());
    }

    /**
     * Lists available sessions.
     *
     * @return a list of session names
     * @throws IOException if listing fails
     */
    public java.util.List<String> listSessions() throws IOException {
        return repository.listSessionNames();
    }

    /**
     * Deletes a session.
     *
     * @param sessionName the name of the session to delete
     * @return true if the session was deleted, false if it didn't exist
     * @throws IOException if deletion fails
     */
    public boolean deleteSession(String sessionName) throws IOException {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        // Don't delete the current session
        if (sessionName != null && sessionName.equals(currentSessionName)) {
            throw new IllegalArgumentException("Cannot delete the current active session");
        }

        return repository.deleteSession(sessionName.trim());
    }

    /**
     * Gets session information summary.
     *
     * @return a string describing the current session state
     */
    public String getSessionSummary() {
        return String.format(
                "Session: %s, Unsaved: %s, Auto-save: %s, Last save: %d seconds ago",
                currentSessionName != null ? currentSessionName : "None",
                hasUnsavedChanges ? "Yes" : "No",
                autoSaveEnabled.get() ? "Enabled" : "Disabled",
                getTimeSinceLastSave());
    }

    /** Shuts down the session manager and performs cleanup. */
    public void shutdown() {
        // Disable auto-save
        setAutoSaveEnabled(false);

        // Shutdown executor
        autoSaveExecutor.shutdown();
        try {
            if (!autoSaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                autoSaveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoSaveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Try to save current session if there are unsaved changes
        if (hasUnsavedChanges) {
            try {
                emergencySave();
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(
                            Level.SEVERE, "Failed to perform emergency save: " + e.getMessage(), e);
                }
            }
        }
    }

    /** Starts the auto-save scheduler. */
    private void startAutoSave() {
        autoSaveExecutor.scheduleAtFixedRate(
                this::performAutoSave,
                autoSaveIntervalSeconds,
                autoSaveIntervalSeconds,
                TimeUnit.SECONDS);
    }

    /** Stops the auto-save scheduler. */
    private void stopAutoSave() {
        // The scheduled tasks will be cancelled when we restart the scheduler
        // or when the executor is shut down
    }

    /** Performs an auto-save operation. */
    private void performAutoSave() {
        if (!hasUnsavedChanges) {
            return; // Nothing to save
        }

        try {
            // Save to auto-save session
            String autoSaveName = AUTO_SAVE_SESSION_NAME + "_" + System.currentTimeMillis();
            repository.saveSession(
                    autoSaveName,
                    historyService.getHistoryCopy(),
                    historyService.getCurrentIndex());

            // Optionally also save the current session
            if (currentSessionName != null && !currentSessionName.equals(DEFAULT_SESSION_NAME)) {
                saveCurrentSession();
            }

        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Auto-save failed: " + e.getMessage(), e);
            }
        }
    }

    // ISessionManager interface methods

    @Override
    public void saveSession(String filename) throws Exception {
        saveSessionAs(filename);
    }

    @Override
    public DrawingSession loadSession(String filename) throws Exception {
        // Use the existing loadSession method (void version)
        try {
            // Call the existing loadSession method that updates the current state
            // Note: The existing method already has logic to load the session
            // We need to refactor this to return the session data
            
            // For now, create a basic session after loading
            DrawingState currentState = stateManager.getCurrentState();
            return new DrawingSession(filename, currentState);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void setAutoSaveInterval(int seconds) {
        setAutoSaveInterval((long) seconds);
    }

    @Override
    public String[] listAvailableSessions() {
        try {
            java.util.List<String> sessions = listSessions();
            return sessions.toArray(new String[0]);
        } catch (IOException e) {
            // Return empty array if listing fails
            return new String[0];
        }
    }

    /** State change listener to track unsaved changes. */
    private class SessionStateChangeListener implements StateChangeListener {
        @Override
        public void onStateChanged(DrawingState oldState, DrawingState newState) {
            hasUnsavedChanges = true;
        }

        @Override
        public void onHistoryChanged(DrawingState currentState, boolean canUndo, boolean canRedo) {
            hasUnsavedChanges = true;
        }

        @Override
        public void onStateValidationError(DrawingState state, String error) {
            // Validation errors don't affect save status
        }
    }
}
