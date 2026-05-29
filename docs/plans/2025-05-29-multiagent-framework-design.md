# 多 Agent 协作框架设计文档

**日期**: 2025-05-29
**版本**: v1.0

---

## 1. 概述

一个基于 Anthropic Java SDK 的多 Agent 协作框架，支持树形编排（主 Agent 拆解 → 分发 → 汇总），提供全功能 Web 可视化仪表盘。

**核心差异化能力**:
- 每个 Agent 独立指定 LLM 端点，主任务走强模型，子任务走廉价模型，最大化盘活资源
- 自然语言 → DAG 编排 → 执行调度，全链路闭环
- 全程可追溯：每轮对话、每次工具调用持久化到 SQLite
- 条件节点支持运行时动态分叉

**部署形态**: Spring Boot + React 前后端分离 Web 应用

---

## 2. 整体架构

```
+-----------------------------------------------------+
|                 Presentation Layer                  |
|                                                     |
|  +----------+  +----------+  +------------------+   |
|  | 实时仪表盘 |  | DAG设计器 |  | NL → 编排 生成器  |   |
|  +----------+  +----------+  +------------------+   |
|       |             |               |               |
|       +------+------+       +-------+               |
|              |              |                       |
|         WebSocket      REST/WebSocket               |
|         (实时事件流)     (业务API)                    |
|              |              |                       |
+--------------+--------------+-----------------------+
|                  API & Gateway                       |
|         REST (业务)  +  WebSocket (实时推送)         |
+-----------------------------------------------------+
|              Orchestration Layer                     |
|  +----------------+  +----------+  +-------------+   |
|  | TaskDecomposer |  |Dispatcher|  | Aggregator  |   |
|  | 任务拆解        |  |Agent分发 |  | 结果汇总     |   |
|  +----------------+  +----------+  +-------------+   |
+-----------------------------------------------------+
|                 Agent Runtime                        |
|  +----------+  +----------+  +------------------+   |
|  | Agent池  |  | Tool引擎  |  | ContextManager   |   |
|  | 生命周期  |  | 工具注册表 |  | 上下文/压缩/缓存  |   |
|  +----------+  +----------+  +------------------+   |
+-----------------------------------------------------+
|                 LLM Adapter                          |
|  ClaudeClient | OpenAiCompatClient | OllamaClient   |
+-----------------------------------------------------+
```

---

## 3. Agent 定义与生命周期

### AgentSpec

| 属性 | 说明 |
|------|------|
| `name` | 唯一标识，如 `code-reviewer`、`planner` |
| `role` | 系统提示词，定义 Agent 角色和行为边界 |
| `baseUrl` | 模型服务端点，如 `https://api.anthropic.com` / `http://localhost:11434/v1` |
| `model` | 模型名，如 `claude-sonnet-4-20250514`、`qwen2.5:14b` |
| `apiKey` | 认证密钥（支持占位符引用环境变量） |
| `tools` | 可用工具白名单 |
| `maxSteps` | 最大执行轮次，防止无限循环 |
| `outputFormat` | 输出格式约束（JSON schema / Markdown） |

### 生命周期

```
IDLE → PENDING → RUNNING → COMPLETED / FAILED / CANCELLED
  ↑                           │
  +--------- RESET ----------+
```

- **IDLE**: 在池中等待
- **PENDING**: 已分配任务，等待调度
- **RUNNING**: 正在执行（LLM 交互 + 工具调用循环）
- **COMPLETED / FAILED / CANCELLED**: 终态
- **RESET**: 完成后可重置回 IDLE 复用

每个状态变更通过 WebSocket 推送事件到前端。

---

## 4. 任务编排与调度

### 树形编排流程

```
任务输入（自然语言描述）
       │
       ▼
TaskDecomposer（主 Agent，强推理模型）
  分析任务 → 拆解为子任务 DAG
       │
       ▼
  TaskPlan { subtasks[], dependencies[] }
       │
       ▼
Dispatcher → 拓扑排序 → 按批次调度
  Batch 1: [A, B] 并行（无依赖）
  Batch 2: [C] 依赖 A+B 完成
  Batch 3: [D] 依赖 C + 条件节点
       │
       ▼
Aggregator → 收集 SubTaskResult[] → 整合输出最终结果
```

### 编排生成方式

- **自然语言 → DAG**: 用户描述需求，TaskDecomposer 生成 TaskPlan，前端 DAG 预览确认
- **拖拽 DAG 设计器**: 手动创建节点、连线
- **模板复用**: 常用编排存为模板一键加载

### 调度策略

| 策略 | 说明 |
|------|------|
| `SEQUENTIAL` | 严格串行 |
| `PARALLEL_BATCH` | 同批次无依赖并行，批次间串行 |
| `RESOURCE_AWARE` | 并行时考虑端点负载，避免同一本地模型冲突 |

### 条件节点

DAG 支持 `CONDITIONAL` 类型边，运行时根据上游结果表达式（如 `$tester.status == FAILED`）决定是否触发下游节点。

---

## 5. Tool 引擎与 Skill 机制

### Tool vs Skill

- **Tool（工具）**: 原子操作，由 LLM 通过 `tool_use` 自主调用，无状态。如 READ_FILE、BASH、GREP
- **Skill（技能）**: 复合工作流，由用户或编排层触发，注入 Agent 的 role 和约束。如 "代码审查"、"任务拆解"

Skill 定义"做什么、怎么做"，Tool 提供"用什么做"。

### 内置 Tool

| Tool | 说明 |
|------|------|
| FileSystemTool | READ_FILE, WRITE_FILE, LIST_DIR |
| BashTool | 执行 shell 命令 |
| SearchTool | GREP, GLOB |
| AgentTool | 调用子 Agent（树形编排核心，禁止嵌套） |
| WebTool | WEB_FETCH, WEB_SEARCH |

### 内置 Skill

- 代码审查、任务拆解、测试生成、结果汇总
- Skill 和 Tool 均支持 SPI 插件扩展

### 上下文管理

- **装配**: 将 AgentSpec.role + 任务 prompt + 工具定义组装为初始上下文
- **追踪**: 记录每轮 messages 列表
- **压缩**: token 超过窗口 80% 时触发，保留最近 3 轮，更早消息替换为摘要
- **缓存**: 对长 system prompt 使用 Anthropic prompt caching

---

## 6. 数据模型

### 表结构

| 表 | 记录内容 |
|------|------|
| `task_plan` | 任务 ID、描述、状态、创建时间 |
| `subtask` | 子任务 ID、绑定 Agent、prompt、依赖关系、状态 |
| `agent_execution` | Agent 实例 ID、模型、开始/结束、总 token、关联 subtask |
| `message_record` | 每轮 user/assistant/tool_result 消息 |
| `tool_invocation` | 工具名、输入、输出、耗时、所属 agent_execution |
| `agent_spec` | Agent 定义（baseUrl、model、role 等） |
| `skill_template` | Skill 模板 |
| `task_template` | 编排模板 |

数据库: SQLite（WAL 模式）

---

## 7. API 设计

### REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/tasks` | 提交任务（NL 描述或 TaskPlan JSON） |
| `GET` | `/api/tasks/{id}` | 查询任务状态与结果 |
| `DELETE` | `/api/tasks/{id}` | 取消任务 |
| `POST` | `/api/tasks/{id}/retry` | 重试失败子任务 |
| `GET` | `/api/agents` | 列出注册的 AgentSpec |
| `POST` | `/api/agents` | 注册新 AgentSpec |
| `GET` | `/api/agents/{name}` | 查看 AgentSpec 详情 |
| `POST` | `/api/tools` | 注册自定义 Tool (SPI) |
| `GET` | `/api/skills` | 列出 Skill 库 |
| `GET` | `/api/templates` | 列出编排模板 |
| `GET` | `/api/resources` | 查看端点资源负载 |

### WebSocket: `/ws/events`

事件类型: `agent.state.changed`, `agent.tool.invoked`, `agent.tool.result`, `task.progress`, `agent.log`, `resource.slot.changed`

---

## 8. 可视化界面

### 页面结构

```
+-----------------------------------------------------+
|  Header: 项目选择 | 快速发起 | 通知 | 设置              |
+----------+----------------------+--------------------+
|          |                      |                    |
|  左侧栏   |     中央主区域        |    右侧属性面板      |
|  导航     |     (动态切换)        |                    |
|          |                      |                    |
| -仪表盘  |  仪表盘→Agent状态卡片  | 选中Agent详情       |
| -编排器  |  编排器→DAG画布+NL输入 | -运行日志           |
| -结果    |  结果→产物预览/对比    | -工具调用历史        |
| -Skill库 |                      | -Token消耗          |
|          |                      |                    |
+----------+----------------------+--------------------+
```

### 四个核心页面

| 页面 | 功能 |
|------|------|
| **仪表盘** | Agent 实时状态卡片、当前任务进度条、端点资源负载 |
| **编排器** | NL 输入 → DAG 预览 + 拖拽画布手动调整 |
| **结果视图** | 最终产物展示、子结果展开、多结果对比、导出 |
| **Skill 库** | 内置 Skill 列表 + 自定义导入 |

---

## 9. 错误处理与可靠性

### 三级错误分类

| 级别 | 示例 | 处理 |
|------|------|------|
| 可重试 | LLM 超时、429 限流 | 指数退避，最多 3 次 |
| 可降级 | 模型不可用 | 同端点替代模型或默认备用端点 |
| 致命 | API Key 无效、工具异常 | 标记 FAILED，判断是否影响下游 |

### DAG 级容错

- 子任务失败不会阻止无依赖的其他子任务继续
- 依赖失败子任务的下游自动标记 SKIPPED
- Aggregator 最终报告包含完整的失败原因和跳过的节点
- 支持单子任务重试（保留原 AgentSpec 和 prompt）

### 数据持久化

- 任务每步状态变更实时写入 SQLite
- 崩溃后可从断点恢复，已完成子任务不重跑

---

## 10. 技术选型

### 后端

| 层次 | 方案 |
|------|------|
| 框架 | Spring Boot 3.x + WebFlux |
| LLM SDK | anthropic-java + OkHttp（OpenAI 兼容自封装） |
| 数据库 | SQLite + HikariCP + JdbcTemplate |
| WebSocket | Spring WebFlux Reactive WebSocket |

### 前端

| 层次 | 方案 |
|------|------|
| 框架 | React 18 + TypeScript |
| DAG 画布 | ReactFlow |
| 状态管理 | Zustand |
| 构建 | Vite |

---

## 11. 项目结构

```
c-mulagent/
├── c-mulagent-server/          # Spring Boot 后端
│   └── src/main/java/...
│       ├── core/
│       │   ├── agent/          # AgentSpec, AgentRuntime, AgentExecutor
│       │   ├── orchestration/  # TaskDecomposer, Dispatcher, Aggregator
│       │   ├── tool/           # ToolRegistry, ToolSpec, ToolExecutor, SPI
│       │   └── skill/          # SkillLoader, SkillTemplate
│       ├── llm/                # LLM 适配层
│       │   ├── AnthropicAdapter
│       │   ├── OpenAiCompatAdapter
│       │   ├── OllamaAdapter
│       │   └── LLMClientFactory
│       ├── context/            # ContextManager, CompactionStrategy
│       ├── resource/           # ResourceSlot, EndpointLoadBalancer
│       ├── event/              # WebSocket 事件模型与推送
│       ├── persistence/        # SQLite 仓储
│       └── api/                # REST Controller + WebSocket Handler
│
├── c-mulagent-ui/              # React 前端
│   └── src/
│       ├── pages/              # Dashboard, Orchestrator, Results, Skills
│       ├── components/         # DAGCanvas, AgentCard, LogViewer
│       └── hooks/              # useWebSocket, useAgentState
│
└── docs/plans/
```

---

## 12. 核心交互流程

```
用户输入自然语言需求
  → TaskDecomposer 拆解为 TaskPlan
  → 静态校验（无环、引用有效、字段齐全）
  → 渲染 DAG 预览（用户可在画布调整）
  → 用户确认执行
  → Dispatcher 拓扑排序 + 分批调度
  → WebSocket 实时推送进度
  → Aggregator 汇总
  → 结果视图展示 + 可导出/存为模板
```