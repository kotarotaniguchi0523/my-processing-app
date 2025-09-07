package com.example.services;

import com.example.domain.models.DrawingState;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** File-based implementation of IStateRepository that stores states and sessions as YAML files. */
public class FileStateRepository implements IStateRepository {

    private static final String STATE_EXTENSION = "yml";
    private static final String SESSION_EXTENSION = "session.yml";
    private static final String STATE_DIR = "states";
    private static final String SESSION_DIR = "sessions";
    private static final int MAX_BACKUPS = 5;

    private final Path baseDirectory;
    private final Path stateDirectory;
    private final Path sessionDirectory;
    private final Yaml yaml;

    /** Creates a new FileStateRepository with the default directory. */
    public FileStateRepository() {
        this(Paths.get(System.getProperty("user.home"), ".drawing-app"));
    }

    /**
     * Creates a new FileStateRepository with the specified base directory.
     *
     * @param baseDirectory the base directory for storing files
     */
    public FileStateRepository(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.stateDirectory = baseDirectory.resolve(STATE_DIR);
        this.sessionDirectory = baseDirectory.resolve(SESSION_DIR);

        // Configure YAML output format
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    @Override
    public void initialize() throws IOException {
        FileUtils.ensureDirectoryExists(baseDirectory);
        FileUtils.ensureDirectoryExists(stateDirectory);
        FileUtils.ensureDirectoryExists(sessionDirectory);
    }

    @Override
    public void saveState(String name, DrawingState state) throws IOException {
        validateStateName(name);
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        initialize();

        String safeName = FileUtils.createSafeFileName(name);
        Path filePath = stateDirectory.resolve(safeName + "." + STATE_EXTENSION);

        // Create backup if file exists
        if (FileUtils.fileExists(filePath)) {
            FileUtils.createBackup(filePath, "_backup_" + System.currentTimeMillis());
            FileUtils.cleanupOldBackups(stateDirectory, safeName, STATE_EXTENSION, MAX_BACKUPS);
        }

        // Serialize state to YAML
        Map<String, Object> stateMap = state.toYamlMap();
        stateMap.put("repositoryMetadata", createMetadataMap(name, state));

        String yamlContent = yaml.dump(stateMap);
        FileUtils.writeTextFile(filePath, yamlContent);
    }

    @Override
    public DrawingState loadState(String name) throws IOException {
        validateStateName(name);

        String safeName = FileUtils.createSafeFileName(name);
        Path filePath = stateDirectory.resolve(safeName + "." + STATE_EXTENSION);

        if (!FileUtils.fileExists(filePath)) {
            throw new IOException("State not found: " + name);
        }

        String yamlContent = FileUtils.readTextFile(filePath);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = (Map<String, Object>) yaml.load(yamlContent);

            if (stateMap == null) {
                throw new IOException("Invalid YAML format in state file: " + name);
            }

            return DrawingState.fromYamlMap(stateMap);

        } catch (Exception e) {
            throw new IOException("Failed to load state '" + name + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean stateExists(String name) {
        try {
            validateStateName(name);
            String safeName = FileUtils.createSafeFileName(name);
            Path filePath = stateDirectory.resolve(safeName + "." + STATE_EXTENSION);
            return FileUtils.fileExists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteState(String name) throws IOException {
        validateStateName(name);

        String safeName = FileUtils.createSafeFileName(name);
        Path filePath = stateDirectory.resolve(safeName + "." + STATE_EXTENSION);

        return FileUtils.deleteFile(filePath);
    }

    @Override
    public List<String> listStateNames() throws IOException {
        initialize();
        return FileUtils.listFilesWithExtension(stateDirectory, STATE_EXTENSION);
    }

    @Override
    public StateMetadata getStateMetadata(String name) throws IOException {
        validateStateName(name);

        String safeName = FileUtils.createSafeFileName(name);
        Path filePath = stateDirectory.resolve(safeName + "." + STATE_EXTENSION);

        if (!FileUtils.fileExists(filePath)) {
            throw new IOException("State not found: " + name);
        }

        long timestamp = FileUtils.getLastModified(filePath);
        long fileSize = FileUtils.getFileSize(filePath);

        // Read the file to get shape count
        int shapeCount = 0;
        String description = "";

        try {
            String yamlContent = FileUtils.readTextFile(filePath);
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = (Map<String, Object>) yaml.load(yamlContent);

            if (stateMap != null) {
                // Get shape count
                Object shapesObj = stateMap.get("shapes");
                if (shapesObj instanceof List) {
                    shapeCount = ((List<?>) shapesObj).size();
                }

                // Get description from metadata
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata =
                        (Map<String, Object>) stateMap.get("repositoryMetadata");
                if (metadata != null) {
                    description = (String) metadata.getOrDefault("description", "");
                }
            }
        } catch (Exception e) {
            // Continue with default values if parsing fails
        }

        return new StateMetadata(name, timestamp, fileSize, shapeCount, description);
    }

    @Override
    public void saveSession(String sessionName, List<DrawingState> history, int currentIndex)
            throws IOException {
        validateSessionName(sessionName);
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("History cannot be null or empty");
        }
        if (currentIndex < 0 || currentIndex >= history.size()) {
            throw new IllegalArgumentException("Invalid current index: " + currentIndex);
        }

        initialize();

        String safeName = FileUtils.createSafeFileName(sessionName);
        Path filePath = sessionDirectory.resolve(safeName + "." + SESSION_EXTENSION);

        // Create backup if file exists
        if (FileUtils.fileExists(filePath)) {
            FileUtils.createBackup(filePath, "_backup_" + System.currentTimeMillis());
            FileUtils.cleanupOldBackups(sessionDirectory, safeName, SESSION_EXTENSION, MAX_BACKUPS);
        }

        // Create session data structure
        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("type", "session");
        sessionData.put("sessionName", sessionName);
        sessionData.put("currentIndex", currentIndex);
        sessionData.put("timestamp", System.currentTimeMillis());
        sessionData.put("historySize", history.size());

        // Serialize all states in history
        List<Map<String, Object>> historyMaps = new ArrayList<>();
        for (DrawingState state : history) {
            historyMaps.add(state.toYamlMap());
        }
        sessionData.put("history", historyMaps);

        String yamlContent = yaml.dump(sessionData);
        FileUtils.writeTextFile(filePath, yamlContent);
    }

    @Override
    public SessionData loadSession(String sessionName) throws IOException {
        validateSessionName(sessionName);

        String safeName = FileUtils.createSafeFileName(sessionName);
        Path filePath = sessionDirectory.resolve(safeName + "." + SESSION_EXTENSION);

        if (!FileUtils.fileExists(filePath)) {
            throw new IOException("Session not found: " + sessionName);
        }

        String yamlContent = FileUtils.readTextFile(filePath);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionMap = (Map<String, Object>) yaml.load(yamlContent);

            if (sessionMap == null) {
                throw new IOException("Invalid YAML format in session file: " + sessionName);
            }

            // Validate session type
            String type = (String) sessionMap.get("type");
            if (!"session".equals(type)) {
                throw new IOException("Invalid session file format");
            }

            int currentIndex = ((Number) sessionMap.get("currentIndex")).intValue();
            long timestamp = ((Number) sessionMap.get("timestamp")).longValue();

            // Deserialize history
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> historyMaps =
                    (List<Map<String, Object>>) sessionMap.get("history");
            List<DrawingState> history = new ArrayList<>();

            for (Map<String, Object> stateMap : historyMaps) {
                DrawingState state = DrawingState.fromYamlMap(stateMap);
                history.add(state);
            }

            return new SessionData(sessionName, history, currentIndex, timestamp);

        } catch (Exception e) {
            throw new IOException(
                    "Failed to load session '" + sessionName + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listSessionNames() throws IOException {
        initialize();
        List<String> sessionFiles =
                FileUtils.listFilesWithExtension(sessionDirectory, SESSION_EXTENSION);

        // Remove the .session part from the names
        List<String> sessionNames = new ArrayList<>();
        for (String fileName : sessionFiles) {
            if (fileName.endsWith(".session")) {
                sessionNames.add(fileName.substring(0, fileName.length() - 8));
            } else {
                sessionNames.add(fileName);
            }
        }

        return sessionNames;
    }

    @Override
    public boolean deleteSession(String sessionName) throws IOException {
        validateSessionName(sessionName);

        String safeName = FileUtils.createSafeFileName(sessionName);
        Path filePath = sessionDirectory.resolve(safeName + "." + SESSION_EXTENSION);

        return FileUtils.deleteFile(filePath);
    }

    @Override
    public void cleanup() throws IOException {
        // Clean up old backup files
        if (FileUtils.fileExists(stateDirectory)) {
            List<String> stateNames = listStateNames();
            for (String name : stateNames) {
                String safeName = FileUtils.createSafeFileName(name);
                FileUtils.cleanupOldBackups(stateDirectory, safeName, STATE_EXTENSION, MAX_BACKUPS);
            }
        }

        if (FileUtils.fileExists(sessionDirectory)) {
            List<String> sessionNames = listSessionNames();
            for (String name : sessionNames) {
                String safeName = FileUtils.createSafeFileName(name);
                FileUtils.cleanupOldBackups(
                        sessionDirectory, safeName, SESSION_EXTENSION, MAX_BACKUPS);
            }
        }
    }

    /**
     * Gets the base directory used by this repository.
     *
     * @return the base directory path
     */
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Gets the state directory used by this repository.
     *
     * @return the state directory path
     */
    public Path getStateDirectory() {
        return stateDirectory;
    }

    /**
     * Gets the session directory used by this repository.
     *
     * @return the session directory path
     */
    public Path getSessionDirectory() {
        return sessionDirectory;
    }

    /** Creates metadata map for a state. */
    private Map<String, Object> createMetadataMap(String name, DrawingState state) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", name);
        metadata.put("shapeCount", state.getShapes().size());
        metadata.put("saveTimestamp", System.currentTimeMillis());
        metadata.put("description", "Drawing state with " + state.getShapes().size() + " shapes");
        return metadata;
    }

    /** Validates a state name. */
    private void validateStateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("State name cannot be null or empty");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("State name too long (max 200 characters)");
        }
    }

    /** Validates a session name. */
    private void validateSessionName(String sessionName) {
        if (sessionName == null || sessionName.isBlank()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (sessionName.length() > 200) {
            throw new IllegalArgumentException("Session name too long (max 200 characters)");
        }
    }
}
