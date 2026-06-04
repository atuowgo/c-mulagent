package com.cmulagent.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Built-in tool for GREP and GLOB search within the workspace.
 */
public class SearchTool {

    private static final Logger log = LoggerFactory.getLogger(SearchTool.class);

    private static final String SYS_PROP_WORKSPACE = "cmulagent.workspace";
    private static final String DEFAULT_WORKSPACE = "data/workspace";
    private static final int MAX_RESULTS = 100;

    private final Path workspaceDir;

    public SearchTool() {
        String ws = System.getProperty(SYS_PROP_WORKSPACE, DEFAULT_WORKSPACE);
        this.workspaceDir = Paths.get(ws).toAbsolutePath().normalize();
    }

    public ToolExecutor grepExecutor() {
        return params -> {
            try {
                String patternStr = (String) params.get("pattern");
                String searchPath = (String) params.getOrDefault("path", ".");
                String filePattern = (String) params.getOrDefault("filePattern", "*");

                if (patternStr == null || patternStr.isBlank()) {
                    return "Error: 'pattern' parameter is required";
                }

                Path searchDir = resolvePath(searchPath);
                Pattern regex = Pattern.compile(patternStr);
                PathMatcher fileMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);

                List<String> results = new ArrayList<>();
                Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (fileMatcher.matches(file.getFileName())) {
                            try {
                                List<String> lines = Files.readAllLines(file);
                                for (int i = 0; i < lines.size() && results.size() < MAX_RESULTS; i++) {
                                    if (regex.matcher(lines.get(i)).find()) {
                                        Path relative = workspaceDir.relativize(file);
                                        results.add(relative + ":" + (i + 1) + ": " + lines.get(i).trim());
                                    }
                                }
                            } catch (IOException e) {
                                // skip unreadable files
                            }
                        }
                        return results.size() >= MAX_RESULTS ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }
                });

                if (results.isEmpty()) {
                    return "No matches found for pattern: " + patternStr;
                }
                if (results.size() >= MAX_RESULTS) {
                    results.add("... (results truncated at " + MAX_RESULTS + ")");
                }
                return String.join("\n", results);

            } catch (PatternSyntaxException e) {
                return "Error: Invalid regex pattern: " + e.getMessage();
            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (IOException e) {
                log.error("GREP search failed", e);
                return "Error during grep search: " + e.getMessage();
            }
        };
    }

    public ToolExecutor globExecutor() {
        return params -> {
            try {
                String globPattern = (String) params.get("pattern");
                String searchPath = (String) params.getOrDefault("path", ".");

                if (globPattern == null || globPattern.isBlank()) {
                    return "Error: 'pattern' parameter is required";
                }

                Path searchDir = resolvePath(searchPath);
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

                List<String> results = new ArrayList<>();
                Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (matcher.matches(file)) {
                            Path relative = workspaceDir.relativize(file);
                            results.add(relative.toString());
                        }
                        return results.size() >= MAX_RESULTS ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });

                if (results.isEmpty()) {
                    return "No files found matching pattern: " + globPattern;
                }
                if (results.size() >= MAX_RESULTS) {
                    results.add("... (results truncated at " + MAX_RESULTS + ")");
                }
                return String.join("\n", results);

            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (IOException e) {
                log.error("GLOB search failed", e);
                return "Error during glob search: " + e.getMessage();
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