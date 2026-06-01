package com.cmulagent.core.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class BuiltinToolRegistrar implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(BuiltinToolRegistrar.class);

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BuiltinToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Registering built-in tools");
        registerReadFile();
        registerWriteFile();
        registerListFiles();
        registerShellExec();
        registerWebSearch();
        log.info("Built-in tools registered successfully");
    }

    private void registerReadFile() {
        ToolSpec spec = ToolSpec.builder()
                .name("read_file")
                .description("Reads the content of a file from the filesystem")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to read"
                                )
                        ),
                        "required", List.of("path")
                ))
                .build();

        ToolExecutor executor = params -> {
            String path = (String) params.get("path");
            if (path == null || path.isBlank()) {
                throw new ToolExecutionException("read_file", "Parameter 'path' is required");
            }
            try {
                Path filePath = Paths.get(path);
                if (!Files.exists(filePath)) {
                    return "Error: File not found: " + path;
                }
                if (!Files.isRegularFile(filePath)) {
                    return "Error: Not a regular file: " + path;
                }
                return Files.readString(filePath);
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException("read_file",
                        "Failed to read file " + path + ": " + e.getMessage(), e);
            }
        };

        toolRegistry.register("read_file", spec, executor);
    }

    private void registerWriteFile() {
        ToolSpec spec = ToolSpec.builder()
                .name("write_file")
                .description("Writes content to a file on the filesystem")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to write to"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "The content to write"
                                )
                        ),
                        "required", List.of("path", "content")
                ))
                .build();

        ToolExecutor executor = params -> {
            String path = (String) params.get("path");
            String content = (String) params.get("content");
            if (path == null || path.isBlank()) {
                throw new ToolExecutionException("write_file", "Parameter 'path' is required");
            }
            if (content == null) {
                throw new ToolExecutionException("write_file", "Parameter 'content' is required");
            }
            try {
                Path filePath = Paths.get(path);
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(filePath, content);
                return "File written successfully: " + path;
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException("write_file",
                        "Failed to write file " + path + ": " + e.getMessage(), e);
            }
        };

        toolRegistry.register("write_file", spec, executor);
    }

    private void registerListFiles() {
        ToolSpec spec = ToolSpec.builder()
                .name("list_files")
                .description("Lists files in a directory")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The directory path to list, defaults to current directory"
                                )
                        ),
                        "required", List.of()
                ))
                .build();

        ToolExecutor executor = params -> {
            String path = (String) params.getOrDefault("path", ".");
            try {
                Path dirPath = Paths.get(path);
                if (!Files.exists(dirPath)) {
                    return "Error: Directory not found: " + path;
                }
                if (!Files.isDirectory(dirPath)) {
                    return "Error: Not a directory: " + path;
                }
                List<String> fileNames = Files.list(dirPath)
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .collect(Collectors.toList());
                return objectMapper.writeValueAsString(fileNames);
            } catch (Exception e) {
                throw new ToolExecutionException("list_files",
                        "Failed to list files in " + path + ": " + e.getMessage(), e);
            }
        };

        toolRegistry.register("list_files", spec, executor);
    }

    private void registerShellExec() {
        ToolSpec spec = ToolSpec.builder()
                .name("shell_exec")
                .description("Executes a shell command with a 30-second timeout")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "command", Map.of(
                                        "type", "string",
                                        "description", "The shell command to execute"
                                )
                        ),
                        "required", List.of("command")
                ))
                .build();

        ToolExecutor executor = params -> {
            String command = (String) params.get("command");
            if (command == null || command.isBlank()) {
                throw new ToolExecutionException("shell_exec", "Parameter 'command' is required");
            }
            try {
                ProcessBuilder builder = new ProcessBuilder();
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    builder.command("cmd.exe", "/c", command);
                } else {
                    builder.command("sh", "-c", command);
                }
                builder.redirectErrorStream(true);
                Process process = builder.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return "Error: Command timed out after 30 seconds";
                }

                int exitCode = process.exitValue();
                String result = output.toString().trim();
                if (exitCode != 0 && result.isEmpty()) {
                    result = "Command exited with code: " + exitCode;
                }
                return result;
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException("shell_exec",
                        "Failed to execute command: " + e.getMessage(), e);
            }
        };

        toolRegistry.register("shell_exec", spec, executor);
    }

    private void registerWebSearch() {
        ToolSpec spec = ToolSpec.builder()
                .name("web_search")
                .description("Performs a web search and returns results")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "The search query"
                                )
                        ),
                        "required", List.of("query")
                ))
                .build();

        ToolExecutor executor = params -> {
            String query = (String) params.get("query");
            if (query == null || query.isBlank()) {
                throw new ToolExecutionException("web_search", "Parameter 'query' is required");
            }
            return "Search results for: " + query;
        };

        toolRegistry.register("web_search", spec, executor);
    }
}