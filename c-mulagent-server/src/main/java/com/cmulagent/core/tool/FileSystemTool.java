package com.cmulagent.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Built-in tool for file system operations: READ_FILE, WRITE_FILE, LIST_DIR.
 * All paths are resolved relative to the configured workspace directory.
 */
public class FileSystemTool {

    private static final Logger log = LoggerFactory.getLogger(FileSystemTool.class);

    private static final String SYS_PROP_WORKSPACE = "cmulagent.workspace";
    private static final String DEFAULT_WORKSPACE = "data/workspace";

    private final Path workspaceDir;

    public FileSystemTool() {
        String ws = System.getProperty(SYS_PROP_WORKSPACE, DEFAULT_WORKSPACE);
        this.workspaceDir = Paths.get(ws).toAbsolutePath().normalize();
        try {
            Files.createDirectories(workspaceDir);
            log.info("FileSystemTool workspace: {}", workspaceDir);
        } catch (IOException e) {
            log.error("Failed to create workspace directory: {}", workspaceDir, e);
        }
    }

    public ToolExecutor readFileExecutor() {
        return params -> {
            try {
                Path filePath = resolvePath((String) params.get("path"));
                if (!Files.exists(filePath)) {
                    return "Error: File not found: " + filePath;
                }
                if (!Files.isRegularFile(filePath)) {
                    return "Error: Not a file: " + filePath;
                }
                return Files.readString(filePath);
            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (IOException e) {
                log.error("READ_FILE failed", e);
                return "Error reading file: " + e.getMessage();
            }
        };
    }

    public ToolExecutor writeFileExecutor() {
        return params -> {
            try {
                Path filePath = resolvePath((String) params.get("path"));
                String content = (String) params.get("content");
                if (content == null) {
                    return "Error: 'content' parameter is required";
                }
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return "File written successfully: " + filePath;
            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (IOException e) {
                log.error("WRITE_FILE failed", e);
                return "Error writing file: " + e.getMessage();
            }
        };
    }

    public ToolExecutor listDirExecutor() {
        return params -> {
            try {
                Path dirPath = resolvePath((String) params.get("path"));
                boolean recursive = Boolean.parseBoolean(String.valueOf(params.getOrDefault("recursive", false)));

                if (!Files.exists(dirPath)) {
                    return "Error: Directory not found: " + dirPath;
                }
                if (!Files.isDirectory(dirPath)) {
                    return "Error: Not a directory: " + dirPath;
                }

                int maxDepth = recursive ? Integer.MAX_VALUE : 1;
                try (Stream<Path> stream = Files.walk(dirPath, maxDepth)) {
                    return stream
                            .filter(p -> !p.equals(dirPath))
                            .map(p -> {
                                Path relative = dirPath.relativize(p);
                                String type = Files.isDirectory(p) ? "[DIR] " : "[FILE]";
                                return type + relative.toString();
                            })
                            .collect(Collectors.joining("\n"));
                }
            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (IOException e) {
                log.error("LIST_DIR failed", e);
                return "Error listing directory: " + e.getMessage();
            }
        };
    }

    private Path resolvePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new SecurityException("Path parameter is required");
        }
        if (rawPath.contains("..")) {
            throw new SecurityException("Path traversal detected: " + rawPath);
        }
        Path resolved = workspaceDir.resolve(rawPath).normalize();
        if (!resolved.startsWith(workspaceDir)) {
            throw new SecurityException("Path is outside workspace: " + rawPath);
        }
        return resolved;
    }
}