package com.cmulagent.core.tool;

import com.cmulagent.core.agent.AgentOrchestrator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bootstrap component that registers all 5 built-in tools on application startup.
 */
@Component
public class ToolBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ToolBootstrap.class);

    private final ToolRegistry toolRegistry;
    private final AgentOrchestrator agentOrchestrator;

    public ToolBootstrap(ToolRegistry toolRegistry, AgentOrchestrator agentOrchestrator) {
        this.toolRegistry = toolRegistry;
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostConstruct
    public void registerBuiltInTools() {
        log.info("Registering built-in tools...");

        registerFileSystemTools();
        registerBashTool();
        registerSearchTools();
        registerAgentTool();
        registerWebTools();

        log.info("Built-in tools registered. Total: {}", toolRegistry.getAll().size());
    }

    private void registerFileSystemTools() {
        FileSystemTool fsTool = new FileSystemTool();

        toolRegistry.register("read_file",
                ToolSpec.builder()
                        .name("read_file")
                        .description("Read the contents of a file within the workspace")
                        .category("file")
                        .inputSchema(Map.of("path", Map.of("type", "string", "description", "Relative file path within workspace")))
                        .build(),
                fsTool.readFileExecutor());

        toolRegistry.register("write_file",
                ToolSpec.builder()
                        .name("write_file")
                        .description("Write content to a file within the workspace")
                        .category("file")
                        .inputSchema(Map.of(
                                "path", Map.of("type", "string", "description", "Relative file path within workspace"),
                                "content", Map.of("type", "string", "description", "Content to write to the file")
                        ))
                        .build(),
                fsTool.writeFileExecutor());

        toolRegistry.register("list_dir",
                ToolSpec.builder()
                        .name("list_dir")
                        .description("List contents of a directory within the workspace")
                        .category("file")
                        .inputSchema(Map.of(
                                "path", Map.of("type", "string", "description", "Relative directory path within workspace"),
                                "recursive", Map.of("type", "boolean", "description", "Whether to list recursively", "default", false)
                        ))
                        .build(),
                fsTool.listDirExecutor());
    }

    private void registerBashTool() {
        BashTool bashTool = new BashTool();

        toolRegistry.register("bash",
                ToolSpec.builder()
                        .name("bash")
                        .description("Execute a shell command with a safety whitelist (common dev commands only)")
                        .category("execution")
                        .inputSchema(Map.of("command", Map.of("type", "string", "description", "Shell command to execute")))
                        .build(),
                bashTool.executor());
    }

    private void registerSearchTools() {
        SearchTool searchTool = new SearchTool();

        toolRegistry.register("grep",
                ToolSpec.builder()
                        .name("grep")
                        .description("Search for a regex pattern in files within the workspace")
                        .category("search")
                        .inputSchema(Map.of(
                                "pattern", Map.of("type", "string", "description", "Regular expression pattern to search for"),
                                "path", Map.of("type", "string", "description", "Directory to search in (default: '.')", "default", "."),
                                "filePattern", Map.of("type", "string", "description", "Glob pattern for files to search (default: '*')", "default", "*")
                        ))
                        .build(),
                searchTool.grepExecutor());

        toolRegistry.register("glob",
                ToolSpec.builder()
                        .name("glob")
                        .description("Find files matching a glob pattern within the workspace")
                        .category("search")
                        .inputSchema(Map.of(
                                "pattern", Map.of("type", "string", "description", "Glob pattern (e.g. '**/*.java')"),
                                "path", Map.of("type", "string", "description", "Directory to search in (default: '.')", "default", ".")
                        ))
                        .build(),
                searchTool.globExecutor());
    }

    private void registerAgentTool() {
        AgentTool agentTool = new AgentTool(agentOrchestrator);

        toolRegistry.register("agent",
                ToolSpec.builder()
                        .name("agent")
                        .description("Delegate a subtask to another agent for parallel or specialized execution")
                        .category("orchestration")
                        .inputSchema(Map.of(
                                "agentName", Map.of("type", "string", "description", "Name or ID of the agent spec to invoke"),
                                "prompt", Map.of("type", "string", "description", "The prompt/task to send to the sub-agent")
                        ))
                        .build(),
                agentTool.executor());
    }

    private void registerWebTools() {
        WebTool webTool = new WebTool();

        toolRegistry.register("web_fetch",
                ToolSpec.builder()
                        .name("web_fetch")
                        .description("Fetch the content of a URL and return it as plain text")
                        .category("web")
                        .inputSchema(Map.of("url", Map.of("type", "string", "description", "URL to fetch")))
                        .build(),
                webTool.fetchExecutor());

        toolRegistry.register("web_search",
                ToolSpec.builder()
                        .name("web_search")
                        .description("Search the web (requires API configuration; use WEB_FETCH as fallback)")
                        .category("web")
                        .inputSchema(Map.of("query", Map.of("type", "string", "description", "Search query string")))
                        .build(),
                webTool.searchExecutor());
    }
}