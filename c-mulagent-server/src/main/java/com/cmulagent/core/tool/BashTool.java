package com.cmulagent.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Built-in tool for executing shell commands with a safety whitelist.
 * Only allows common dev commands and rejects dangerous operations.
 */
public class BashTool {

    private static final Logger log = LoggerFactory.getLogger(BashTool.class);

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "ls", "cat", "grep", "find", "echo", "mkdir", "touch", "cp", "mv",
            "python", "python3", "node", "npm", "mvn", "gradle", "git", "curl", "wget",
            "javac", "java", "go", "rustc", "cargo", "dir", "type", "where",
            "chmod", "chown", "pwd", "whoami", "uname", "ps", "netstat",
            "df", "du", "env", "printenv", "ping", "nslookup", "dig",
            "head", "tail", "wc", "sort", "uniq", "cut", "sed", "awk", "tr",
            "diff", "tar", "zip", "unzip", "gzip", "gunzip"
    );

    private static final long TIMEOUT_SECONDS = 30;

    public ToolExecutor executor() {
        return params -> {
            try {
                String command = (String) params.get("command");
                if (command == null || command.isBlank()) {
                    return "Error: 'command' parameter is required";
                }

                validateCommand(command);

                ProcessBuilder pb;
                if (isWindows()) {
                    pb = new ProcessBuilder("cmd.exe", "/c", command);
                } else {
                    pb = new ProcessBuilder("sh", "-c", command);
                }
                pb.redirectErrorStream(true);

                Process process = pb.start();
                StringBuilder output = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return "Error: Command timed out after " + TIMEOUT_SECONDS + " seconds";
                }

                int exitCode = process.exitValue();
                String result = output.toString().trim();
                if (exitCode != 0 && result.isEmpty()) {
                    return "Command exited with code " + exitCode;
                }
                return result.isEmpty() ? "Command completed (no output)" : result;

            } catch (SecurityException e) {
                return "Error: " + e.getMessage();
            } catch (Exception e) {
                log.error("BashTool execution failed", e);
                return "Error executing command: " + e.getMessage();
            }
        };
    }

    private void validateCommand(String command) {
        String trimmed = command.trim();

        // Split on command separators &&, ||, |, ;
        String[] segments = trimmed.split("\\s*(&&|\\|\\||[|;])\\s*");

        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;

            // Extract the base command (first word)
            String baseCmd = segment.split("\\s+")[0];

            // Handle paths like /usr/bin/git -> git, ./script, ../cmd
            if (baseCmd.contains("/") || baseCmd.contains("\\")) {
                baseCmd = baseCmd.substring(baseCmd.lastIndexOf('/') + 1);
                baseCmd = baseCmd.substring(baseCmd.lastIndexOf('\\') + 1);
            }

            // Strip .exe/.bat/.cmd extension on Windows
            if (baseCmd.endsWith(".exe") || baseCmd.endsWith(".bat") || baseCmd.endsWith(".cmd")) {
                baseCmd = baseCmd.substring(0, baseCmd.lastIndexOf('.'));
            }

            if (!ALLOWED_COMMANDS.contains(baseCmd)) {
                throw new SecurityException("Command not allowed: " + baseCmd);
            }
        }

        // Additional dangerous pattern checks
        String lowerCmd = trimmed.toLowerCase();
        if (lowerCmd.contains("rm -rf") || lowerCmd.contains("rm  -rf")
            || lowerCmd.contains("format") || lowerCmd.contains("dd if=")
            || lowerCmd.contains("mkfs") || lowerCmd.contains(":(){ :|:& };:")) {
            throw new SecurityException("Dangerous command pattern detected");
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}