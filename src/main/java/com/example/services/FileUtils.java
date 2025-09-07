package com.example.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Utility class for file operations used by the state management services. */
public class FileUtils {

    /**
     * Creates a directory if it doesn't exist.
     *
     * @param path the directory path
     * @throws IOException if directory creation fails
     */
    public static void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Writes text content to a file, creating parent directories if needed.
     *
     * @param filePath the file path
     * @param content the content to write
     * @throws IOException if writing fails
     */
    public static void writeTextFile(Path filePath, String content) throws IOException {
        ensureDirectoryExists(filePath.getParent());
        Files.write(
                filePath,
                content.getBytes("UTF-8"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Reads text content from a file.
     *
     * @param filePath the file path
     * @return the file content as string
     * @throws IOException if reading fails
     */
    public static String readTextFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath.toString());
        }
        return new String(Files.readAllBytes(filePath), "UTF-8");
    }

    /**
     * Checks if a file exists.
     *
     * @param filePath the file path
     * @return true if file exists, false otherwise
     */
    public static boolean fileExists(Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    /**
     * Deletes a file if it exists.
     *
     * @param filePath the file path
     * @return true if file was deleted, false if it didn't exist
     * @throws IOException if deletion fails
     */
    public static boolean deleteFile(Path filePath) throws IOException {
        return Files.deleteIfExists(filePath);
    }

    /**
     * Gets the size of a file in bytes.
     *
     * @param filePath the file path
     * @return the file size in bytes
     * @throws IOException if getting size fails
     */
    public static long getFileSize(Path filePath) throws IOException {
        return Files.size(filePath);
    }

    /**
     * Gets the last modified timestamp of a file.
     *
     * @param filePath the file path
     * @return the timestamp in milliseconds
     * @throws IOException if getting timestamp fails
     */
    public static long getLastModified(Path filePath) throws IOException {
        return Files.getLastModifiedTime(filePath).toMillis();
    }

    /**
     * Lists all files with a specific extension in a directory.
     *
     * @param directoryPath the directory path
     * @param extension the file extension (without dot)
     * @return list of file names (without extension)
     * @throws IOException if listing fails
     */
    public static List<String> listFilesWithExtension(Path directoryPath, String extension)
            throws IOException {
        List<String> fileNames = new ArrayList<>();

        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return fileNames;
        }

        String extensionWithDot = "." + extension;

        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(directoryPath, "*" + extensionWithDot)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    String fileName = file.getFileName().toString();
                    // Remove extension
                    String nameWithoutExtension =
                            fileName.substring(0, fileName.length() - extensionWithDot.length());
                    fileNames.add(nameWithoutExtension);
                }
            }
        }

        return fileNames;
    }

    /**
     * Creates a safe file name by removing or replacing invalid characters.
     *
     * @param fileName the original file name
     * @return a safe file name
     */
    public static String createSafeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // Replace invalid characters with underscores
        String safe = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Remove leading/trailing whitespace and dots
        safe = safe.trim().replaceAll("^\\.+|\\.+$", "");

        // Ensure it's not empty after cleaning
        if (safe.isEmpty()) {
            safe = "unnamed";
        }

        // Truncate if too long (most file systems support 255 characters)
        if (safe.length() > 200) { // Leave room for extension
            safe = safe.substring(0, 200);
        }

        return safe;
    }

    /**
     * Creates a backup copy of a file.
     *
     * @param originalPath the original file path
     * @param backupSuffix the suffix to add to the backup file name
     * @throws IOException if backup creation fails
     */
    public static void createBackup(Path originalPath, String backupSuffix) throws IOException {
        if (!Files.exists(originalPath)) {
            return;
        }

        String fileName = originalPath.getFileName().toString();
        String backupFileName;

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            backupFileName =
                    fileName.substring(0, dotIndex) + backupSuffix + fileName.substring(dotIndex);
        } else {
            backupFileName = fileName + backupSuffix;
        }

        Path backupPath = originalPath.getParent().resolve(backupFileName);
        Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Cleans up old backup files, keeping only the specified number of most recent ones.
     *
     * @param directoryPath the directory containing backup files
     * @param baseFileName the base name of the files
     * @param extension the file extension
     * @param maxBackups the maximum number of backups to keep
     * @throws IOException if cleanup fails
     */
    public static void cleanupOldBackups(
            Path directoryPath, String baseFileName, String extension, int maxBackups)
            throws IOException {
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return;
        }

        String pattern = baseFileName + "*." + extension;
        List<Path> backupFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, pattern)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    backupFiles.add(file);
                }
            }
        }

        // Sort by last modified time (newest first)
        backupFiles.sort(
                (a, b) -> {
                    try {
                        return Long.compare(
                                Files.getLastModifiedTime(b).toMillis(),
                                Files.getLastModifiedTime(a).toMillis());
                    } catch (IOException e) {
                        return 0;
                    }
                });

        // Delete old backups
        for (int i = maxBackups; i < backupFiles.size(); i++) {
            Files.deleteIfExists(backupFiles.get(i));
        }
    }

    /**
     * Validates that a path is safe to use (not attempting directory traversal).
     *
     * @param basePath the base directory path
     * @param relativePath the relative path to validate
     * @return the normalized path if safe
     * @throws IOException if the path is unsafe
     */
    public static Path validateSafePath(Path basePath, String relativePath) throws IOException {
        Path resolvedPath = basePath.resolve(relativePath).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IOException("Path traversal attempt detected: " + relativePath);
        }

        return resolvedPath;
    }
}
